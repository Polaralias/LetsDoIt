package com.letsdoit.app.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.letsdoit.app.data.mapper.toEpochMilli
import com.letsdoit.app.domain.model.CalendarAccount
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarRepository {

    override suspend fun getCalendars(): List<CalendarAccount> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<CalendarAccount>()
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountNameIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val ownerIndex = cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)

                while (cursor.moveToNext()) {
                    calendars.add(
                        CalendarAccount(
                            id = cursor.getLong(idIndex),
                            name = cursor.getString(nameIndex) ?: "Unknown",
                            accountName = cursor.getString(accountNameIndex) ?: "",
                            ownerName = cursor.getString(ownerIndex) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext calendars
    }

    override suspend fun addEvent(task: Task, calendarId: Long): Long? = withContext(Dispatchers.IO) {
        val dueDate = task.dueDate ?: return@withContext null
        val startMillis = dueDate.toEpochMilli()
        val endMillis = startMillis + 3600000 // Default 1 hour duration

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, task.description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateEvent(task: Task) = withContext(Dispatchers.IO) {
        val eventId = task.calendarEventId ?: return@withContext
        val dueDate = task.dueDate ?: return@withContext // If due date removed, maybe we should delete event? For now just return.

        val startMillis = dueDate.toEpochMilli()
        val endMillis = startMillis + 3600000

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, task.description)
        }

        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        try {
            context.contentResolver.update(updateUri, values, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Unit
    }

    override suspend fun deleteEvent(eventId: Long) = withContext(Dispatchers.IO) {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        try {
            context.contentResolver.delete(deleteUri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Unit
    }
}
