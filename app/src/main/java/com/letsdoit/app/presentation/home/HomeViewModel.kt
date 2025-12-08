package com.letsdoit.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.domain.usecase.project.GetSelectedProjectUseCase
import com.letsdoit.app.domain.usecase.task.GetTasksUseCase
import com.letsdoit.app.domain.usecase.task.RefreshTasksUseCase
import com.letsdoit.app.domain.usecase.task.ToggleTaskStatusUseCase
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
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val refreshTasksUseCase: RefreshTasksUseCase,
    private val getSelectedProjectUseCase: GetSelectedProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var currentListId: String? = null

    init {
        observeSelectedProject()
    }

    private fun observeSelectedProject() {
        getSelectedProjectUseCase()
            .onEach { listId ->
                currentListId = listId
                loadTasks(listId)
                if (listId != null) {
                    refresh(listId)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTasks(listId: String?) {
        tasksJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)

        tasksJob = getTasksUseCase(listId)
            .onEach { tasks ->
                _uiState.value = _uiState.value.copy(
                    tasks = tasks,
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

    fun refresh(listId: String? = currentListId) {
         if (listId == null) return
         viewModelScope.launch {
            try {
                refreshTasksUseCase(listId)
            } catch (e: Exception) {
                // Handle error
            }
         }
    }

    fun onTaskChecked(taskId: String) {
        viewModelScope.launch {
            try {
                toggleTaskStatusUseCase(taskId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
