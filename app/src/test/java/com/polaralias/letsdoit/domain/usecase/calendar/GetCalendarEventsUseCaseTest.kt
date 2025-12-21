package com.polaralias.letsdoit.domain.usecase.calendar

import com.polaralias.letsdoit.domain.model.CalendarEvent
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCalendarEventsUseCaseTest {

    private val calendarRepository: CalendarRepository = mockk()
    private val useCase = GetCalendarEventsUseCase(calendarRepository)

    @Test
    fun `invoke delegates to repository with correct arguments`() = runTest {
        // Given
        val start = 1000L
        val end = 2000L
        val expectedEvents = listOf(
            CalendarEvent(1, "Event 1", "Desc 1", 1000, 1100, 0, false),
            CalendarEvent(2, "Event 2", "Desc 2", 1200, 1300, 0, true)
        )

        coEvery { calendarRepository.getEvents(start, end) } returns expectedEvents

        // When
        val result = useCase(start, end)

        // Then
        assertEquals(expectedEvents, result)
        coVerify { calendarRepository.getEvents(start, end) }
    }
}
