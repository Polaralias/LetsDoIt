package com.letsdoit.app.presentation.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.usecase.project.GetSelectedProjectUseCase
import com.letsdoit.app.domain.usecase.task.GetTasksUseCase
import com.letsdoit.app.domain.usecase.task.UpdateTaskUseCase
import com.letsdoit.app.domain.util.TaskStatusUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getSelectedProjectUseCase: GetSelectedProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanbanState())
    val uiState: StateFlow<KanbanState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null

    init {
        observeSelectedProject()
    }

    private fun observeSelectedProject() {
        getSelectedProjectUseCase()
            .onEach { listId ->
                loadTasks(listId)
            }
            .launchIn(viewModelScope)
    }

    private fun loadTasks(listId: String?) {
        tasksJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)

        tasksJob = getTasksUseCase(listId)
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
        val todoTasks = tasks.filter { TaskStatusUtil.isTodo(it.status) }
        val inProgressTasks = tasks.filter { TaskStatusUtil.isInProgress(it.status) }
        val doneTasks = tasks.filter { TaskStatusUtil.isCompleted(it.status) }

        return mapOf(
            KanbanColumn.TODO to todoTasks,
            KanbanColumn.IN_PROGRESS to inProgressTasks,
            KanbanColumn.DONE to doneTasks
        )
    }

    fun moveTask(task: Task, targetColumn: KanbanColumn) {
        val newStatus = when (targetColumn) {
            KanbanColumn.TODO -> TaskStatusUtil.OPEN
            KanbanColumn.IN_PROGRESS -> TaskStatusUtil.IN_PROGRESS
            KanbanColumn.DONE -> TaskStatusUtil.COMPLETED
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
