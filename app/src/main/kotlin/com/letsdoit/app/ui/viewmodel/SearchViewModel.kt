package com.letsdoit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.R
import com.letsdoit.app.data.model.TaskWithSubtasks
import com.letsdoit.app.data.search.SearchRepository
import com.letsdoit.app.data.search.SmartFilterKind
import com.letsdoit.app.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow

data class SearchUiState(
    val query: String = "",
    val history: List<String> = emptyList(),
    val results: List<TaskWithSubtasks> = emptyList(),
    val selectedFilter: SmartFilterKind? = null,
    val mode: SearchMode = SearchMode.None,
    val searchActive: Boolean = false
)

sealed interface SearchMode {
    data object None : SearchMode
    data class Query(val value: String) : SearchMode
    data class Filter(val kind: SmartFilterKind) : SearchMode
}

enum class QuickAction {
    Complete,
    DueToday,
    Priority
}

data class UndoAction(
    val taskId: Long,
    val type: QuickAction,
    val previousCompleted: Boolean? = null,
    val previousDueAt: Instant? = null,
    val previousPriority: Int? = null
)

sealed interface SearchEvent {
    data class ShowUndo(val message: Int, val undo: UndoAction, val detail: Int? = null) : SearchEvent
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val taskRepository: TaskRepository,
    private val clock: Clock
) : ViewModel() {
    private val queryInput = MutableStateFlow("")
    private val modeState = MutableStateFlow<SearchMode>(SearchMode.None)
    private val searchActive = MutableStateFlow(false)
    private val eventsChannel = MutableSharedFlow<SearchEvent>()

    private val historyState = searchRepository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val resultsState = modeState.flatMapLatest { mode ->
        when (mode) {
            SearchMode.None -> kotlinx.coroutines.flow.flowOf(emptyList())
            is SearchMode.Query -> searchRepository.search(mode.value)
            is SearchMode.Filter -> searchRepository.smartFilter(mode.kind)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        queryInput,
        historyState,
        resultsState,
        modeState,
        searchActive
    ) { query, history, results, mode, active ->
        SearchUiState(
            query = query,
            history = history,
            results = results,
            selectedFilter = (mode as? SearchMode.Filter)?.kind,
            mode = mode,
            searchActive = active
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    val events: kotlinx.coroutines.flow.SharedFlow<SearchEvent> = eventsChannel

    fun onQueryChange(value: String) {
        queryInput.value = value
    }

    fun onSearchActiveChange(active: Boolean) {
        searchActive.value = active
    }

    fun submitQuery() {
        val query = queryInput.value.trim()
        if (query.isEmpty()) {
            modeState.value = SearchMode.None
            searchActive.value = false
            return
        }
        modeState.value = SearchMode.Query(query)
        searchActive.value = false
        viewModelScope.launch {
            searchRepository.recordQuery(query)
        }
    }

    fun selectHistory(value: String) {
        queryInput.value = value
        submitQuery()
    }

    fun selectFilter(kind: SmartFilterKind) {
        val current = (modeState.value as? SearchMode.Filter)?.kind
        if (current == kind) {
            clearFilter()
        } else {
            modeState.value = SearchMode.Filter(kind)
            queryInput.value = ""
            searchActive.value = false
        }
    }

    fun clearFilter() {
        modeState.value = SearchMode.None
    }

    fun performToggle(task: TaskWithSubtasks) {
        viewModelScope.launch {
            val previous = task.task.completed
            taskRepository.updateCompletion(task.task.id, !previous)
            eventsChannel.emit(
                SearchEvent.ShowUndo(
                    message = if (previous) R.string.search_action_reopened else R.string.search_action_completed,
                    undo = UndoAction(taskId = task.task.id, type = QuickAction.Complete, previousCompleted = previous)
                )
            )
        }
    }

    fun setDueToday(task: TaskWithSubtasks) {
        viewModelScope.launch {
            val previous = task.task.dueAt
            val zone = clock.zone
            val today = LocalDate.now(clock)
            val due = today.plusDays(1).atStartOfDay(zone).minusMinutes(1).toInstant()
            taskRepository.setDueDate(task.task.id, due)
            eventsChannel.emit(
                SearchEvent.ShowUndo(
                    message = R.string.search_action_due_today,
                    undo = UndoAction(taskId = task.task.id, type = QuickAction.DueToday, previousDueAt = previous)
                )
            )
        }
    }

    fun setPriority(task: TaskWithSubtasks, priority: Int) {
        viewModelScope.launch {
            val previous = task.task.priority
            if (previous == priority) return@launch
            taskRepository.setPriority(task.task.id, priority)
            eventsChannel.emit(
                SearchEvent.ShowUndo(
                    message = R.string.search_action_priority,
                    undo = UndoAction(taskId = task.task.id, type = QuickAction.Priority, previousPriority = previous),
                    detail = priorityLabel(priority)
                )
            )
        }
    }

    fun undo(action: UndoAction) {
        viewModelScope.launch {
            when (action.type) {
                QuickAction.Complete -> {
                    val previous = action.previousCompleted ?: return@launch
                    taskRepository.updateCompletion(action.taskId, previous)
                }
                QuickAction.DueToday -> {
                    taskRepository.setDueDate(action.taskId, action.previousDueAt)
                }
                QuickAction.Priority -> {
                    val previous = action.previousPriority ?: return@launch
                    taskRepository.setPriority(action.taskId, previous)
                }
            }
        }
    }

    private fun priorityLabel(priority: Int): Int {
        return when (priority) {
            0 -> R.string.search_priority_high
            1 -> R.string.search_priority_medium
            3 -> R.string.search_priority_low
            else -> R.string.search_priority_normal
        }
    }
}
