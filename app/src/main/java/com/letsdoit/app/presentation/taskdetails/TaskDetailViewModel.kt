package com.letsdoit.app.presentation.taskdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.core.util.Constants
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.nlp.NlpEngine
import com.letsdoit.app.domain.usecase.project.GetSelectedProjectUseCase
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
    private val getSelectedProjectUseCase: GetSelectedProjectUseCase,
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
            val listId = getSelectedProjectUseCase.getSync() ?: Constants.DEMO_LIST_ID
            // Initialize with a new empty task
            _uiState.value = _uiState.value.copy(
                task = Task(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
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
                    suggestedPriority = nlpResult.detectedPriority,
                    suggestedRecurrence = nlpResult.recurrenceRule
                )
            }
        }
    }

    fun onRecurrenceChange(rule: String?) {
        _uiState.value.task?.let { task ->
            _uiState.value = _uiState.value.copy(task = task.copy(recurrenceRule = rule))
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
            val suggestedRecurrence = _uiState.value.suggestedRecurrence
            var newTask = task

            if (suggestedDate != null) {
                newTask = newTask.copy(dueDate = suggestedDate)
            }
            if (suggestedPriority != null) {
                newTask = newTask.copy(priority = suggestedPriority)
            }
            if (suggestedRecurrence != null) {
                newTask = newTask.copy(recurrenceRule = suggestedRecurrence)
            }

            val cleanTitle = NlpEngine.parse(task.title).cleanTitle
            newTask = newTask.copy(title = cleanTitle)

            _uiState.value = _uiState.value.copy(
                task = newTask,
                suggestedDueDate = null,
                suggestedPriority = null,
                suggestedRecurrence = null
            )
        }
    }

    fun saveTask() {
        var taskToSave = _uiState.value.task ?: return

        // Final synchronous parse to catch any pending input (race condition fix)
        val nlpResult = NlpEngine.parse(taskToSave.title)

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

             if (taskToSave.recurrenceRule == null && nlpResult.recurrenceRule != null) {
                 taskToSave = taskToSave.copy(recurrenceRule = nlpResult.recurrenceRule)
                 appliedChange = true
             }

             if (appliedChange) {
                 taskToSave = taskToSave.copy(title = nlpResult.cleanTitle)
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
