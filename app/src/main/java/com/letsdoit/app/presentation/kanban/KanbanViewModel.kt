package com.letsdoit.app.presentation.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.usecase.task.GetTasksUseCase
import com.letsdoit.app.domain.usecase.task.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanbanState())
    val uiState: StateFlow<KanbanState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        getTasksUseCase()
            .onEach { tasks ->
                val columns = groupTasks(tasks)
                _uiState.value = _uiState.value.copy(
                    columns = columns,
                    isLoading = false,
                    error = null
                )
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
            .launchIn(viewModelScope)
    }

    private fun groupTasks(tasks: List<Task>): Map<KanbanColumn, List<Task>> {
        val todoTasks = tasks.filter { isTodo(it.status) }
        val inProgressTasks = tasks.filter { isInProgress(it.status) }
        val doneTasks = tasks.filter { isDone(it.status) }

        return mapOf(
            KanbanColumn.TODO to todoTasks,
            KanbanColumn.IN_PROGRESS to inProgressTasks,
            KanbanColumn.DONE to doneTasks
        )
    }

    private fun isTodo(status: String): Boolean {
        return status.equals("Open", ignoreCase = true) ||
                status.equals("To Do", ignoreCase = true) ||
                status.equals("todo", ignoreCase = true) ||
                status.isBlank()
    }

    private fun isInProgress(status: String): Boolean {
        return status.equals("In Progress", ignoreCase = true) ||
                status.equals("doing", ignoreCase = true)
    }

    private fun isDone(status: String): Boolean {
        return status.equals("Completed", ignoreCase = true) ||
                status.equals("Done", ignoreCase = true) ||
                status.equals("complete", ignoreCase = true) ||
                status.equals("closed", ignoreCase = true)
    }

    fun moveTask(task: Task, targetColumn: KanbanColumn) {
        val newStatus = when (targetColumn) {
            KanbanColumn.TODO -> "Open"
            KanbanColumn.IN_PROGRESS -> "In Progress"
            KanbanColumn.DONE -> "Completed"
        }

        if (!task.status.equals(newStatus, ignoreCase = true)) {
            val updatedTask = task.copy(status = newStatus)
            viewModelScope.launch {
                try {
                    updateTaskUseCase(updatedTask)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = "Failed to update task: ${e.message}")
                }
            }
        }
    }
}
