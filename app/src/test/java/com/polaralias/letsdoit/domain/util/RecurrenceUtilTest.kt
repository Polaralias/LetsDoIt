package com.polaralias.letsdoit.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class RecurrenceUtilTest {

    @Test
    fun `calculateNextDueDate with DAILY returns next day`() {
        val now = LocalDateTime.of(2023, 1, 1, 10, 0)
        val result = RecurrenceUtil.calculateNextDueDate(now, "FREQ=DAILY")
        assertEquals(LocalDateTime.of(2023, 1, 2, 10, 0), result)
    }

    @Test
    fun `calculateNextDueDate with WEEKLY returns next week`() {
        val now = LocalDateTime.of(2023, 1, 1, 10, 0) // Sunday
        val result = RecurrenceUtil.calculateNextDueDate(now, "FREQ=WEEKLY")
        assertEquals(LocalDateTime.of(2023, 1, 8, 10, 0), result)
    }

    @Test
    fun `calculateNextDueDate with WEEKLY and BYDAY returns next occurrence`() {
        // Jan 1 2023 is Sunday.
        // We want next Monday (Jan 2).
        val now = LocalDateTime.of(2023, 1, 1, 10, 0)
        val result = RecurrenceUtil.calculateNextDueDate(now, "FREQ=WEEKLY;BYDAY=MO")
        assertEquals(LocalDateTime.of(2023, 1, 2, 10, 0), result)
    }

    @Test
    fun `calculateNextDueDate with WEEKLY and BYDAY same day returns next week`() {
        // Jan 2 2023 is Monday.
        // If BYDAY=MO, it should return Jan 9 (next week).
        val now = LocalDateTime.of(2023, 1, 2, 10, 0)
        val result = RecurrenceUtil.calculateNextDueDate(now, "FREQ=WEEKLY;BYDAY=MO")
        assertEquals(LocalDateTime.of(2023, 1, 9, 10, 0), result)
    }
}
