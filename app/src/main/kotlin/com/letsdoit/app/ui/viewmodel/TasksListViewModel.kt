package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.ai.AiService
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.NewTask
import com.letsdoit.app.data.task.TaskRepository
import com.letsdoit.app.integrations.alarm.AlarmScheduler
import com.letsdoit.app.integrations.calendar.CalendarBridge
import com.letsdoit.app.nlp.NaturalLanguageParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TasksListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val parser: NaturalLanguageParser,
    private val alarmScheduler: AlarmScheduler,
    private val calendarBridge: CalendarBridge,
    private val aiService: AiService
) : ViewModel() {
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    val tasks: StateFlow<List<Task>> = taskRepository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

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
                repeatRule = parsed.repeat?.expression
            )
            val taskId = taskRepository.addTask(newTask)
            parsed.dueAt?.let { due ->
                alarmScheduler.schedule(taskId, due, parsed.title)
                calendarBridge.insertEvent(parsed.title, due)
            }
            _suggestions.value = aiService.suggestSubtasks(parsed.title)
            _input.value = ""
        }
    }

    fun toggle(task: Task) {
        viewModelScope.launch {
            val newValue = !task.completed
            taskRepository.updateCompletion(task.id, newValue)
            if (newValue) {
                alarmScheduler.cancel(task.id)
            } else if (task.dueAt != null) {
                alarmScheduler.schedule(task.id, task.dueAt, task.title)
            }
        }
    }

    fun remove(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
            alarmScheduler.cancel(task.id)
        }
    }
}
