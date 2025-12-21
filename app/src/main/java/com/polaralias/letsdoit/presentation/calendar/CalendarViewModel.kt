package com.polaralias.letsdoit.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.letsdoit.data.mapper.toEpochMilli
import com.polaralias.letsdoit.domain.model.CalendarEvent
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.TaskRepository
import com.polaralias.letsdoit.domain.usecase.calendar.GetCalendarEventsUseCase
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed class AgendaItem {
    abstract val time: Long

    data class TaskItem(val task: Task) : AgendaItem() {
        override val time: Long = task.dueDate?.toEpochMilli() ?: 0L
    }
    data class EventItem(val event: CalendarEvent) : AgendaItem() {
        override val time: Long = event.start
    }
}

data class CalendarUiState(
    val items: List<AgendaItem> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val permissionGranted: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarEventsUseCase: GetCalendarEventsUseCase,
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _permissionGranted = MutableStateFlow(false)

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        viewModelScope.launch {
            combine(
                _selectedDate,
                _permissionGranted,
                preferencesRepository.getSelectedListIdFlow().flatMapLatest { listId ->
                    taskRepository.getTasksFlow(listId)
                }
            ) { date, permission, tasks ->
                Triple(date, permission, tasks)
            }.collectLatest { (date, permission, tasks) ->
                _uiState.update { it.copy(isLoading = true, selectedDate = date, permissionGranted = permission) }

                val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val events = if (permission) {
                    getCalendarEventsUseCase(startOfDay, endOfDay)
                } else {
                    emptyList()
                }

                val filteredTasks = tasks.filter { task ->
                    task.dueDate?.let {
                        val taskMillis = it.toEpochMilli()
                        taskMillis in startOfDay until endOfDay
                    } ?: false
                }

                val items = (events.map { AgendaItem.EventItem(it) } + filteredTasks.map { AgendaItem.TaskItem(it) })
                    .sortedBy { it.time }

                _uiState.update {
                    it.copy(
                        items = items,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _permissionGranted.value = isGranted
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }
}
