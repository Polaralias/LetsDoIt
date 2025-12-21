package com.polaralias.letsdoit.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.polaralias.letsdoit.data.mapper.toEpochMilli
import com.polaralias.letsdoit.domain.model.CalendarAccount
import com.polaralias.letsdoit.domain.model.CalendarEvent
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarRepository {

    companion object {
        private const val TAG = "CalendarRepository"
    }

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
            Log.e(TAG, "Error getting calendars", e)
        }
        return@withContext calendars
    }

    override suspend fun getEvents(start: Long, end: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEvent>()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.ALL_DAY
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIndex = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
                val colorIndex = cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(idIndex),
                            title = cursor.getString(titleIndex) ?: "No Title",
                            description = cursor.getString(descIndex),
                            start = cursor.getLong(beginIndex),
                            end = cursor.getLong(endIndex),
                            color = cursor.getInt(colorIndex),
                            allDay = cursor.getInt(allDayIndex) == 1
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing for getEvents", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting events", e)
        }
        return@withContext events
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
            Log.e(TAG, "Error adding event", e)
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
            Log.e(TAG, "Error updating event", e)
        }
        Unit
    }

    override suspend fun deleteEvent(eventId: Long) = withContext(Dispatchers.IO) {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        try {
            context.contentResolver.delete(deleteUri, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
        }
        Unit
    }
}
