package com.letsdoit.app.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.letsdoit.app.ai.AiActionResult
import com.letsdoit.app.ai.AiService
import com.letsdoit.app.ai.PlanSuggestion
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.NewTask
import com.letsdoit.app.data.task.TaskRepository
import com.letsdoit.app.integrations.calendar.CalendarBridge
import com.letsdoit.app.nlp.NaturalLanguageParser
import com.letsdoit.app.data.model.Subtask
import com.letsdoit.app.data.subtask.NewSubtask
import com.letsdoit.app.data.subtask.SubtaskRepository
import com.letsdoit.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class TasksListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val parser: NaturalLanguageParser,
    private val calendarBridge: CalendarBridge,
    private val aiService: AiService,
    private val subtaskRepository: SubtaskRepository
) : ViewModel() {
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    val tasks: Flow<PagingData<Task>> = taskRepository.observeTasks().cachedIn(viewModelScope)

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _preview = MutableStateFlow<AiPreviewState?>(null)
    val preview: StateFlow<AiPreviewState?> = _preview.asStateFlow()

    private val _events = MutableStateFlow<TasksListEvent?>(null)
    val events: StateFlow<TasksListEvent?> = _events.asStateFlow()

    private val subtasksCache = mutableMapOf<Long, MutableStateFlow<List<Subtask>>>()

    fun onInputChanged(value: String) {
        _input.value = value
    }

    fun addTask() {
        val text = _input.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val parsed = parser.parse(text)
            val listId = taskRepository.ensureDefaultList()
            val newTask = NewTask(
                listId = listId,
                title = parsed.title,
                dueAt = parsed.dueAt,
                repeatRule = parsed.repeatRule,
                remindOffsetMinutes = parsed.remindOffsetMinutes
            )
            val taskId = taskRepository.addTask(newTask)
            parsed.dueAt?.let { due ->
                val eventId = calendarBridge.insertEvent(parsed.title, due)
                if (eventId != null) {
                    taskRepository.linkCalendarEvent(taskId, eventId)
                }
            }
            _suggestions.value = emptyList()
            _input.value = ""
        }
    }

    fun toggle(task: Task) {
        viewModelScope.launch {
            val newValue = !task.completed
            taskRepository.updateCompletion(task.id, newValue)
        }
    }

    fun remove(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
        }
    }

    fun removeFromCalendar(taskId: Long) {
        viewModelScope.launch {
            taskRepository.removeFromCalendar(taskId)
        }
    }

    fun updateRecurrence(taskId: Long, repeatRule: String?, remindOffsetMinutes: Int?) {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId) ?: return@launch
            val updated = task.copy(repeatRule = repeatRule, remindOffsetMinutes = remindOffsetMinutes)
            taskRepository.updateTask(updated)
        }
    }

    fun observeSubtasks(taskId: Long): StateFlow<List<Subtask>> {
        return subtasksCache.getOrPut(taskId) {
            val flow = MutableStateFlow<List<Subtask>>(emptyList())
            viewModelScope.launch {
                subtaskRepository.observeSubtasks(taskId).collectLatest { list ->
                    flow.value = list
                }
            }
            flow
        }.asStateFlow()
    }

    fun onToggleSubtask(subtask: Subtask) {
        viewModelScope.launch {
            subtaskRepository.updateCompletion(subtask.id, !subtask.done)
        }
    }

    fun onMoveSubtask(taskId: Long, fromIndex: Int, toIndex: Int) {
        val cache = subtasksCache[taskId] ?: return
        val current = cache.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        if (fromIndex == toIndex) return
        val reordered = current.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
        cache.value = reordered
        viewModelScope.launch {
            subtaskRepository.reorder(taskId, reordered.map { it.id })
        }
    }

    fun onSplitIntoSubtasks(task: Task) {
        viewModelScope.launch {
            when (val result = aiService.splitIntoSubtasks(task.title, task.notes)) {
                is AiActionResult.Success -> {
                    _preview.value = AiPreviewState.Split(task.id, result.data)
                }
                AiActionResult.MissingKey -> emitEvent(TasksListEvent.ShowToast(R.string.ai_missing_key, R.string.action_settings))
                AiActionResult.Offline -> emitEvent(TasksListEvent.ShowToast(R.string.ai_offline, null))
            }
        }
    }

    fun onDraftPlan(task: Task) {
        viewModelScope.launch {
            when (val result = aiService.draftPlan(task.title, task.notes)) {
                is AiActionResult.Success -> {
                    _preview.value = AiPreviewState.Plan(task.id, result.data)
                }
                AiActionResult.MissingKey -> emitEvent(TasksListEvent.ShowToast(R.string.ai_missing_key, R.string.action_settings))
                AiActionResult.Offline -> emitEvent(TasksListEvent.ShowToast(R.string.ai_offline, null))
            }
        }
    }

    fun dismissPreview() {
        _preview.value = null
    }

    fun acceptPreview() {
        val state = _preview.value ?: return
        _preview.value = null
        when (state) {
            is AiPreviewState.Split -> {
                viewModelScope.launch {
                    val ids = subtaskRepository.createSubtasks(state.taskId, state.items.map { NewSubtask(title = it) })
                    emitEvent(TasksListEvent.ShowSnackbar(R.string.subtasks_created, R.string.action_undo, state.taskId, ids))
                }
            }
            is AiPreviewState.Plan -> {
                viewModelScope.launch {
                    val newItems = state.steps.map { suggestion ->
                        NewSubtask(
                            title = suggestion.title,
                            dueAt = suggestion.dueAt,
                            startAt = suggestion.startAt,
                            durationMinutes = suggestion.durationMinutes
                        )
                    }
                    val ids = subtaskRepository.createSubtasks(state.taskId, newItems)
                    emitEvent(TasksListEvent.ShowSnackbar(R.string.plan_created, R.string.action_undo, state.taskId, ids))
                }
            }
        }
    }

    fun undoSubtasks(ids: List<Long>) {
        viewModelScope.launch {
            subtaskRepository.deleteSubtasks(ids)
        }
    }

    private fun emitEvent(event: TasksListEvent) {
        _events.value = event
    }

    fun clearEvent() {
        _events.value = null
    }
}

sealed interface AiPreviewState {
    val taskId: Long

    data class Split(override val taskId: Long, val items: List<String>) : AiPreviewState
    data class Plan(override val taskId: Long, val steps: List<PlanSuggestion>) : AiPreviewState
}

sealed interface TasksListEvent {
    data class ShowToast(@StringRes val message: Int, @StringRes val action: Int?) : TasksListEvent
    data class ShowSnackbar(@StringRes val message: Int, @StringRes val actionLabel: Int?, val taskId: Long, val ids: List<Long>) : TasksListEvent
}
