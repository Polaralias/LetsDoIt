package com.letsdoit.app.nlp

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NaturalLanguageParserTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val parser = NaturalLanguageParser(clock)

    @Test
    fun parsesRelativeMinutes() {
        val result = parser.parse("in 45 minutes")
        assertEquals("in 45 minutes", result.title)
        assertEquals(Instant.parse("2025-01-01T10:45:00Z"), result.dueAt)
    }

    @Test
    fun parsesTomorrowMorning() {
        val result = parser.parse("tomorrow 9am")
        assertEquals("tomorrow 9am", result.title)
        assertEquals(Instant.parse("2025-01-02T09:00:00Z"), result.dueAt)
    }

    @Test
    fun parsesNextWeekdayTime() {
        val result = parser.parse("next Wednesday at 14:30")
        assertEquals(Instant.parse("2025-01-08T14:30:00Z"), result.dueAt)
    }

    @Test
    fun parsesEveryWeekdayRepeat() {
        val result = parser.parse("every weekday at 8")
        assertEquals(Instant.parse("2025-01-01T08:00:00Z"), result.dueAt)
        assertNotNull(result.repeat)
        assertEquals("WEEKDAY@08:00", result.repeat?.expression)
    }

    @Test
    fun parsesIsoDateTime() {
        val result = parser.parse("2025-11-04 10:00")
        assertEquals(Instant.parse("2025-11-04T10:00:00Z"), result.dueAt)
    }
}
