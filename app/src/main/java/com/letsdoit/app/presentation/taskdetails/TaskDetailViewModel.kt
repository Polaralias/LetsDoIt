package com.letsdoit.app.presentation.taskdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.core.util.Constants
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.nlp.NlpEngine
import com.letsdoit.app.domain.usecase.task.CreateTaskUseCase
import com.letsdoit.app.domain.usecase.task.GetTaskUseCase
import com.letsdoit.app.domain.usecase.task.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailState())
    val uiState: StateFlow<TaskDetailState> = _uiState.asStateFlow()

    private val taskId: String? = savedStateHandle["taskId"]

    private var nlpJob: Job? = null

    init {
        if (taskId != null && taskId != "new") {
            loadTask(taskId)
        } else {
            // Initialize with a new empty task
            _uiState.value = _uiState.value.copy(
                task = Task(
                    id = UUID.randomUUID().toString(),
                    listId = Constants.DEMO_LIST_ID,
                    title = "",
                    description = "",
                    status = "Open",
                    dueDate = null,
                    priority = 0,
                    isSynced = false
                )
            )
        }
    }

    private fun loadTask(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val task = getTaskUseCase(id)
                _uiState.value = _uiState.value.copy(
                    task = task,
                    isLoading = false,
                    loadError = if (task == null) "Task not found" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = e.message
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value.task?.let { task ->
            _uiState.value = _uiState.value.copy(task = task.copy(title = title))

            // Debounce NLP parsing
            nlpJob?.cancel()
            nlpJob = viewModelScope.launch {
                delay(500) // 500ms debounce
                val nlpResult = NlpEngine.parse(title)
                _uiState.value = _uiState.value.copy(
                    suggestedDueDate = nlpResult.detectedDate,
                    suggestedPriority = nlpResult.detectedPriority
                )
            }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.value.task?.let { task ->
            _uiState.value = _uiState.value.copy(task = task.copy(description = description))
        }
    }

    fun applySuggestion() {
        _uiState.value.task?.let { task ->
            val suggestedDate = _uiState.value.suggestedDueDate
            val suggestedPriority = _uiState.value.suggestedPriority
            var newTask = task

            if (suggestedDate != null) {
                newTask = newTask.copy(dueDate = suggestedDate)
            }
            if (suggestedPriority != null) {
                newTask = newTask.copy(priority = suggestedPriority)
            }

            val cleanTitle = NlpEngine.parse(task.title).cleanTitle
            newTask = newTask.copy(title = cleanTitle)

            _uiState.value = _uiState.value.copy(
                task = newTask,
                suggestedDueDate = null,
                suggestedPriority = null
            )
        }
    }

    fun saveTask() {
        var taskToSave = _uiState.value.task ?: return

        // Final synchronous parse to catch any pending input (race condition fix)
        // This ensures that even if debounce didn't fire, we still capture the intent
        // if the user hasn't manually set these fields.

        val nlpResult = NlpEngine.parse(taskToSave.title)

        // If due date is not manually set, try to use NLP result
        if (taskToSave.dueDate == null && nlpResult.detectedDate != null) {
             taskToSave = taskToSave.copy(dueDate = nlpResult.detectedDate)
             // If we used the NLP date, we should also clean the title
             // (assuming the user intent was to have the date extracted)
             taskToSave = taskToSave.copy(title = nlpResult.cleanTitle)
        }

        // If priority is default (0) and we detected a priority, use it
        if (taskToSave.priority == 0 && nlpResult.detectedPriority != null) {
             taskToSave = taskToSave.copy(priority = nlpResult.detectedPriority!!)
             // Note: Priority extraction also removes text from title in NlpEngine,
             // so if we already cleaned it for date, it might be cleaner,
             // but NlpEngine.parse parses from the full title string each time.
             // If we just updated title above, we should probably re-parse or trust the cleanTitle
             // from the single parse call which handles both.
             // The single parse call 'nlpResult' has 'cleanTitle' with BOTH date and priority removed.
             // So if we apply EITHER date OR priority from NLP, we should probably use the clean title.
             // Logic refinement:
             // If we apply ANY NLP suggestion, we use the clean title.

             // Let's re-evaluate logic:
             // 1. We have 'nlpResult' from 'taskToSave.title'.
             // 2. We check if we should apply Date.
             // 3. We check if we should apply Priority.
             // 4. If we applied either, we likely want the Clean Title.

             // However, what if user set Date manually but not Priority?
             // If we apply Priority from NLP, we should clean the priority part from title.
             // But NlpEngine returns a fully clean title.
             // If the user typed "Buy milk tomorrow" (and set date manually to next week),
             // NLP detects "tomorrow". If we don't apply NLP date (because manual date exists),
             // we probably shouldn't clean "tomorrow" from title either.

             // This is getting complex for a quick fix.
             // Let's stick to the requirement: "On save, apply the detected date if the user hasn't manually overridden it."

             // Safe approach:
             // If we apply the date, we use the clean title (at least regarding date).
             // Since we use one parse call, 'cleanTitle' has both removed.
             // If we apply ONLY priority, we might accidentally remove date text that wasn't applied?
             // Example: "Buy milk tomorrow". User manually set date to next week.
             // detectedDate = tomorrow. task.dueDate != null. We DO NOT apply date.
             // detectedPriority = null.
             // We don't change title. Correct.

             // Example: "Buy milk priority high". User manual date = null.
             // detectedDate = null.
             // detectedPriority = 2. task.priority = 0. We apply priority.
             // Should we clean title? Yes, "priority high" should be removed.

             // Example: "Buy milk tomorrow priority high". Manual date = next week.
             // detectedDate = tomorrow. We do NOT apply date.
             // detectedPriority = 2. We apply priority.
             // If we use 'nlpResult.cleanTitle', it removes "tomorrow" AND "priority high".
             // We end up with "Buy milk".
             // But we didn't honor "tomorrow". The user effectively overrode "tomorrow" with "next week".
             // So removing "tomorrow" from title is probably desired (it was interpreted/handled/discarded).

             // So, if we apply ANY NLP result OR if the result matches what is already set?
             // Let's keep it simple: If we apply something, we use clean title.
             // If we don't apply anything, we leave title alone.

             // Actually, the safest simplistic implementation:
             // If we update the task based on NLP, we update the title to cleanTitle.

             if (taskToSave.title != nlpResult.cleanTitle) {
                  var appliedChange = false

                  if (taskToSave.dueDate == null && nlpResult.detectedDate != null) {
                      taskToSave = taskToSave.copy(dueDate = nlpResult.detectedDate)
                      appliedChange = true
                  }

                  if (taskToSave.priority == 0 && nlpResult.detectedPriority != null) {
                      taskToSave = taskToSave.copy(priority = nlpResult.detectedPriority!!)
                      appliedChange = true
                  }

                  if (appliedChange) {
                      taskToSave = taskToSave.copy(title = nlpResult.cleanTitle)
                  }
             }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, saveError = null)
            try {
                if (taskId == "new" || taskId == null) {
                    createTaskUseCase(taskToSave)
                } else {
                    updateTaskUseCase(taskToSave)
                }
                 _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(isLoading = false, saveError = e.message)
            }
        }
    }
}
