package com.polaralias.letsdoit.presentation.calendar

import com.polaralias.letsdoit.data.mapper.toEpochMilli
import com.polaralias.letsdoit.domain.model.CalendarEvent
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import com.polaralias.letsdoit.domain.repository.TaskRepository
import com.polaralias.letsdoit.domain.usecase.calendar.GetCalendarEventsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val getCalendarEventsUseCase: GetCalendarEventsUseCase = mockk()
    private val taskRepository: TaskRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk()
    private lateinit var viewModel: CalendarViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        coEvery { preferencesRepository.getSelectedListIdFlow() } returns flowOf(null) // All lists
        coEvery { taskRepository.getTasksFlow(null) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel = CalendarViewModel(getCalendarEventsUseCase, taskRepository, preferencesRepository)

        val state = viewModel.uiState.value
        assertEquals(LocalDate.now(), state.selectedDate)
        assertEquals(false, state.permissionGranted)
        assertEquals(emptyList<AgendaItem>(), state.items)
    }

    @Test
    fun `permission granted triggers data load`() = runTest {
        // Given
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val event = CalendarEvent(1, "Event", "Desc", startOfDay + 1000, startOfDay + 2000, 0, false)
        coEvery { getCalendarEventsUseCase(any(), any()) } returns listOf(event)

        viewModel = CalendarViewModel(getCalendarEventsUseCase, taskRepository, preferencesRepository)

        // When
        viewModel.onPermissionResult(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.permissionGranted)
        assertEquals(1, state.items.size)
        assertTrue(state.items[0] is AgendaItem.EventItem)
    }

    @Test
    fun `tasks are filtered by date`() = runTest {
         // Given
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val taskToday = Task(
            id = "1",
            listId = "list1",
            title = "Today Task",
            description = null,
            status = "open",
            dueDate = java.time.Instant.ofEpochMilli(startOfDay + 3600000).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            priority = 0,
            createdAt = LocalDateTime.now()
        )
        val taskTomorrow = Task(
            id = "2",
            listId = "list1",
            title = "Tomorrow Task",
            description = null,
            status = "open",
            dueDate = java.time.Instant.ofEpochMilli(startOfDay + 86400000 + 3600000).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            priority = 0,
            createdAt = LocalDateTime.now()
        )

        coEvery { taskRepository.getTasksFlow(null) } returns flowOf(listOf(taskToday, taskTomorrow))

        viewModel = CalendarViewModel(getCalendarEventsUseCase, taskRepository, preferencesRepository)

        // When
        viewModel.onPermissionResult(false) // No calendar events
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertTrue(state.items[0] is AgendaItem.TaskItem)
        assertEquals("Today Task", (state.items[0] as AgendaItem.TaskItem).task.title)
    }
}
