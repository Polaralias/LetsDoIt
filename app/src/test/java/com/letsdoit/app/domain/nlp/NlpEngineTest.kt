package com.letsdoit.app.domain.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.Month

class NlpEngineTest {

    // Reference time: Monday, Jan 1st 2024, 10:00 AM
    private val now = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0)

    @Test
    fun `parse priority high`() {
        val input = "Buy milk priority high"
        val result = NlpEngine.parse(input, now)
        assertEquals("Buy milk", result.cleanTitle)
        assertEquals(2, result.detectedPriority)
    }

    @Test
    fun `parse priority 1`() {
        val input = "Fix bug prio 1"
        val result = NlpEngine.parse(input, now)
        assertEquals("Fix bug", result.cleanTitle)
        assertEquals(1, result.detectedPriority)
    }

    @Test
    fun `parse tomorrow`() {
        val input = "Meeting tomorrow"
        val result = NlpEngine.parse(input, now)
        assertEquals("Meeting", result.cleanTitle)
        val expected = LocalDateTime.of(2024, Month.JANUARY, 2, 9, 0)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse tomorrow at 5pm`() {
        val input = "Meeting tomorrow at 5pm"
        val result = NlpEngine.parse(input, now)
        assertEquals("Meeting", result.cleanTitle)
        val expected = LocalDateTime.of(2024, Month.JANUARY, 2, 17, 0)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse today at 2pm`() {
        val input = "Lunch today at 2pm"
        val result = NlpEngine.parse(input, now)
        assertEquals("Lunch", result.cleanTitle)
        // 2pm is 14:00. Since it's after 10:00 AM, it stays today.
        val expected = LocalDateTime.of(2024, Month.JANUARY, 1, 14, 0)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse at 9am (passed time)`() {
        // "at 9am" when now is 10am -> should be tomorrow 9am
        val input = "Gym at 9am"
        val result = NlpEngine.parse(input, now)
        assertEquals("Gym", result.cleanTitle)
        val expected = LocalDateTime.of(2024, Month.JANUARY, 2, 9, 0)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse next friday`() {
        // Jan 1st is Monday. Next Friday is Jan 5th.
        val input = "Party next Friday"
        val result = NlpEngine.parse(input, now)
        assertEquals("Party", result.cleanTitle)
        val expected = LocalDateTime.of(2024, Month.JANUARY, 5, 9, 0)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse in 2 hours`() {
        val input = "Check emails in 2 hours"
        val result = NlpEngine.parse(input, now)
        assertEquals("Check emails", result.cleanTitle)
        val expected = now.plusHours(2)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse combined`() {
        val input = "Deploy to prod next Monday at 10am priority urgent"
        val result = NlpEngine.parse(input, now)
        assertEquals("Deploy to prod", result.cleanTitle)
        assertEquals(1, result.detectedPriority)
        // Next Monday from Jan 1st (Mon) is Jan 8th.
        val expected = LocalDateTime.of(2024, Month.JANUARY, 8, 10, 0)
        assertEquals(expected, result.detectedDate)
    }

    @Test
    fun `parse daily`() {
        val input = "Walk dog daily"
        val result = NlpEngine.parse(input, now)
        assertEquals("Walk dog", result.cleanTitle)
        assertEquals("FREQ=DAILY", result.recurrenceRule)
    }

    @Test
    fun `parse every day`() {
        val input = "Walk dog every day"
        val result = NlpEngine.parse(input, now)
        assertEquals("Walk dog", result.cleanTitle)
        assertEquals("FREQ=DAILY", result.recurrenceRule)
    }

    @Test
    fun `parse weekly`() {
        val input = "Report weekly"
        val result = NlpEngine.parse(input, now)
        assertEquals("Report", result.cleanTitle)
        assertEquals("FREQ=WEEKLY", result.recurrenceRule)
    }

    @Test
    fun `parse every Monday`() {
        val input = "Meeting every Monday"
        val result = NlpEngine.parse(input, now)
        assertEquals("Meeting", result.cleanTitle)
        assertEquals("FREQ=WEEKLY;BYDAY=MO", result.recurrenceRule)
    }
}
