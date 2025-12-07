package com.letsdoit.app.domain.repository

import com.letsdoit.app.domain.model.CalendarAccount
import com.letsdoit.app.domain.model.Task

interface CalendarRepository {
    suspend fun getCalendars(): List<CalendarAccount>
    suspend fun addEvent(task: Task, calendarId: Long): Long?
    suspend fun updateEvent(task: Task)
    suspend fun deleteEvent(eventId: Long)
}
