package com.letsdoit.app.integrations.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarBridge @Inject constructor(@ApplicationContext private val context: Context) {
    fun insertEvent(title: String, start: Instant, end: Instant? = null): Long? {
        val calendarId = findPrimaryCalendarId() ?: return null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, start.toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            if (end != null) {
                put(CalendarContract.Events.DTEND, end.toEpochMilli())
            } else {
                put(CalendarContract.Events.DURATION, "PT30M")
            }
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        return ContentUris.parseId(uri)
    }

    fun update(eventId: Long, title: String, start: Instant, end: Instant? = null, timezoneId: String? = null): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, start.toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, timezoneId ?: TimeZone.getDefault().id)
            if (end != null) {
                put(CalendarContract.Events.DTEND, end.toEpochMilli())
                putNull(CalendarContract.Events.DURATION)
            } else {
                putNull(CalendarContract.Events.DTEND)
                put(CalendarContract.Events.DURATION, "PT30M")
            }
        }
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.update(uri, values, null, null)
        return rows > 0
    }

    fun delete(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(uri, null, null)
        return rows > 0
    }

    private fun findPrimaryCalendarId(): Long? {
        val resolver = context.contentResolver
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE}=1",
            null,
            null
        ).use { cursor ->
            if (cursor == null) return null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val primary = cursor.getInt(1)
                if (primary == 1) {
                    return id
                }
                if (cursor.isLast) {
                    return id
                }
            }
        }
        return null
    }
}
