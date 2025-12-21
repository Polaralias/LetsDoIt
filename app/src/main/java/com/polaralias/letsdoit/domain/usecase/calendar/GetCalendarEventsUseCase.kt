package com.polaralias.letsdoit.domain.usecase.calendar

import com.polaralias.letsdoit.domain.model.CalendarEvent
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import javax.inject.Inject

class GetCalendarEventsUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(start: Long, end: Long): List<CalendarEvent> {
        return calendarRepository.getEvents(start, end)
    }
}
