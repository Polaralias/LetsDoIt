package com.polaralias.letsdoit.testing

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract

class FakeCalendarProvider : ContentProvider() {
    companion object {
        private const val CALENDARS = 1
        private const val EVENTS = 2
        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(CalendarContract.AUTHORITY, "calendars", CALENDARS)
            addURI(CalendarContract.AUTHORITY, "events", EVENTS)
            addURI(CalendarContract.AUTHORITY, "events/#", EVENTS)
        }
        val events: MutableMap<Long, ContentValues> = mutableMapOf()
        var lastUpdateValues: ContentValues? = null
        private var nextId = 1L

        fun reset() {
            events.clear()
            lastUpdateValues = null
            nextId = 1L
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (matcher.match(uri)) {
            CALENDARS -> {
                val cursor = MatrixCursor(
                    projection ?: arrayOf(
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.IS_PRIMARY
                    )
                )
                cursor.addRow(arrayOf<Any>(1L, 1))
                cursor
            }
            EVENTS -> {
                val cursor = MatrixCursor(
                    projection ?: arrayOf(
                        CalendarContract.Events._ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                        CalendarContract.Events.EVENT_TIMEZONE,
                        CalendarContract.Events.DURATION
                    )
                )
                events.forEach { (id, values) ->
                    val row = projection?.map { key -> values.get(key) }?.toTypedArray()
                        ?: arrayOf(
                            id,
                            values.get(CalendarContract.Events.TITLE),
                            values.get(CalendarContract.Events.DTSTART),
                            values.get(CalendarContract.Events.DTEND),
                            values.get(CalendarContract.Events.EVENT_TIMEZONE),
                            values.get(CalendarContract.Events.DURATION)
                        )
                    cursor.addRow(row)
                }
                cursor
            }
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (matcher.match(uri) != EVENTS || values == null) return null
        val id = nextId++
        val copy = ContentValues(values)
        events[id] = copy
        return ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return if (matcher.match(uri) == EVENTS) {
            val id = ContentUris.parseId(uri)
            if (events.remove(id) != null) 1 else 0
        } else {
            0
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        if (matcher.match(uri) != EVENTS || values == null) return 0
        val id = ContentUris.parseId(uri)
        val copy = ContentValues(values)
        events[id] = copy
        lastUpdateValues = copy
        return 1
    }

    override fun getType(uri: Uri): String? = null
}
