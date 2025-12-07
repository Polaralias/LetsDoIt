package com.letsdoit.app.presentation.taskdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.core.util.Constants
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.usecase.task.CreateTaskUseCase
import com.letsdoit.app.domain.usecase.task.GetTaskUseCase
import com.letsdoit.app.domain.usecase.task.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.value.task?.let { task ->
            _uiState.value = _uiState.value.copy(task = task.copy(description = description))
        }
    }

    fun saveTask() {
        val currentTask = _uiState.value.task ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, saveError = null)
            try {
                if (taskId == "new" || taskId == null) {
                    createTaskUseCase(currentTask)
                } else {
                    updateTaskUseCase(currentTask)
                }
                 _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(isLoading = false, saveError = e.message)
            }
        }
    }
}
