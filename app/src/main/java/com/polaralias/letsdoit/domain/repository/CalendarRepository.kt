package com.polaralias.letsdoit.domain.repository

import com.polaralias.letsdoit.domain.model.CalendarAccount
import com.polaralias.letsdoit.domain.model.CalendarEvent
import com.polaralias.letsdoit.domain.model.Task

interface CalendarRepository {
    suspend fun getCalendars(): List<CalendarAccount>
    suspend fun getEvents(start: Long, end: Long): List<CalendarEvent>
    suspend fun addEvent(task: Task, calendarId: Long): Long?
    suspend fun updateEvent(task: Task)
    suspend fun deleteEvent(eventId: Long)
}
