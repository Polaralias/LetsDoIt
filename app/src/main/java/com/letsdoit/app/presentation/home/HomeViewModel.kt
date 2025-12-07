package com.letsdoit.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.core.util.Constants
import com.letsdoit.app.domain.usecase.task.GetTasksUseCase
import com.letsdoit.app.domain.usecase.task.RefreshTasksUseCase
import com.letsdoit.app.domain.usecase.task.ToggleTaskStatusUseCase
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
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val refreshTasksUseCase: RefreshTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadTasks()
        refresh()
    }

    private fun loadTasks() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        getTasksUseCase()
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

    fun refresh() {
         viewModelScope.launch {
            try {
                // Using hardcoded list ID for Phase 1 as no List selection UI exists yet
                refreshTasksUseCase(Constants.DEMO_LIST_ID)
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
