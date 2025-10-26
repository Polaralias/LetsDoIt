package com.letsdoit.app.integrations.calendar

import android.content.pm.ProviderInfo
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import com.letsdoit.app.testing.FakeCalendarProvider
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class CalendarBridgeTest {
    private lateinit var bridge: CalendarBridge
    private val zoneId = ZoneId.of("Europe/London")

    @Before
    fun setUp() {
        FakeCalendarProvider.reset()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = FakeCalendarProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = CalendarContract.AUTHORITY })
        Shadows.shadowOf(context.contentResolver).registerProvider(CalendarContract.AUTHORITY, provider)
        bridge = CalendarBridge(context)
    }

    @After
    fun tearDown() {
        FakeCalendarProvider.reset()
    }

    @Test
    fun updateBuildsContentValues() {
        val originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        try {
            val start = Instant.parse("2024-05-01T09:00:00Z")
            val end = start.plus(1, ChronoUnit.HOURS)
            val eventId = assertNotNull(bridge.insertEvent("Daily stand-up", start))
            FakeCalendarProvider.lastUpdateValues = null
            val newStart = start.plus(1, ChronoUnit.DAYS)
            val newEnd = end.plus(1, ChronoUnit.DAYS)
            val updated = bridge.update(eventId, "Updated stand-up", newStart, newEnd, null)
            assertTrue(updated)
            val values = FakeCalendarProvider.lastUpdateValues
            assertNotNull(values)
            assertEquals("Updated stand-up", values.getAsString(CalendarContract.Events.TITLE))
            assertEquals(newStart.toEpochMilli(), values.getAsLong(CalendarContract.Events.DTSTART))
            assertEquals(newEnd.toEpochMilli(), values.getAsLong(CalendarContract.Events.DTEND))
            assertEquals(zoneId.id, values.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
            assertNull(values.get(CalendarContract.Events.DURATION))
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }
}
