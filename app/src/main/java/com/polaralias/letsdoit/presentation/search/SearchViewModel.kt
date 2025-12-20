package com.polaralias.letsdoit.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.domain.model.SearchFilter
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.usecase.task.SearchTasksUseCase
import com.polaralias.letsdoit.domain.usecase.task.ToggleTaskStatusUseCase
import com.polaralias.letsdoit.domain.util.TaskStatusUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTasksUseCase: SearchTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filterState = MutableStateFlow(SearchFilter())
    val filterState: StateFlow<SearchFilter> = _filterState.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Task>> = combine(_query, _filterState) { query, filter ->
        Pair(query, filter)
    }
        .debounce(300)
        .flatMapLatest { (query, filter) ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                searchTasksUseCase(query, filter)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onTaskChecked(taskId: String) {
        viewModelScope.launch {
            try {
                toggleTaskStatusUseCase(taskId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleStatusFilter(statusCategory: String) {
        val currentStatus = _filterState.value.status.toMutableSet()
        val targetStatuses = if (statusCategory == "Active") {
            TaskStatusUtil.TODO_STATUSES + TaskStatusUtil.IN_PROGRESS_STATUSES
        } else {
            TaskStatusUtil.COMPLETED_STATUSES
        }

        if (targetStatuses.all { currentStatus.contains(it) }) {
            currentStatus.removeAll(targetStatuses)
        } else {
            currentStatus.addAll(targetStatuses)
        }

        _filterState.value = _filterState.value.copy(status = currentStatus.toList())
    }

    fun togglePriorityFilter(priority: Int) {
        val current = _filterState.value.priority.toMutableList()
        if (current.contains(priority)) {
            current.remove(priority)
        } else {
            current.add(priority)
        }
        _filterState.value = _filterState.value.copy(priority = current)
    }

    fun isStatusSelected(statusCategory: String): Boolean {
        val currentStatus = _filterState.value.status
        val targetStatuses = if (statusCategory == "Active") {
            TaskStatusUtil.TODO_STATUSES + TaskStatusUtil.IN_PROGRESS_STATUSES
        } else {
            TaskStatusUtil.COMPLETED_STATUSES
        }
        return currentStatus.isNotEmpty() && targetStatuses.all { currentStatus.contains(it) }
    }
}
