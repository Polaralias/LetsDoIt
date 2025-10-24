package com.letsdoit.app.nlp

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NaturalLanguageParserTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val parser = NaturalLanguageParser(clock)

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun parsesRelativeMinutes() {
        val result = parser.parse("in 45 minutes")
        assertEquals("in 45 minutes", result.title)
        assertEquals(Instant.parse("2025-01-01T10:45:00Z"), result.dueAt)
        assertNull(result.repeatRule)
    }

    @Test
    fun parsesEveryWeekdayAtEight() {
        val result = parser.parse("every weekday at 8")
        assertEquals(Instant.parse("2025-01-02T08:00:00Z"), result.dueAt)
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result.repeatRule)
    }

    @Test
    fun parsesEveryTwoWeeksMonday() {
        val result = parser.parse("every 2 weeks on Monday 9am")
        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO", result.repeatRule)
        assertEquals(Instant.parse("2025-01-13T09:00:00Z"), result.dueAt)
    }

    @Test
    fun parsesMonthlyOrdinalRule() {
        val result = parser.parse("on the first Friday of each month at 10")
        assertEquals("FREQ=MONTHLY;BYDAY=1FR", result.repeatRule)
        assertEquals(Instant.parse("2025-02-07T10:00:00Z"), result.dueAt)
    }

    @Test
    fun parsesMonthlyOnDayRule() {
        val result = parser.parse("repeat monthly on day 15 at 18:00")
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=15", result.repeatRule)
        assertEquals(Instant.parse("2025-01-15T18:00:00Z"), result.dueAt)
    }

    @Test
    fun parsesReminderOffset() {
        val result = parser.parse("pay rent remind me 10 minutes before")
        assertEquals("pay rent remind me 10 minutes before", result.title)
        assertEquals(10, result.remindOffsetMinutes)
    }
}
