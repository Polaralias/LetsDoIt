package com.polaralias.letsdoit.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecurrenceCalculatorTest {
    private val zoneId = ZoneOffset.UTC
    private lateinit var calculator: RecurrenceCalculator

    @Before
    fun setup() {
        calculator = RecurrenceCalculator(zoneId)
    }

    @Test
    fun nextWeekdayRuleAdvancesToFollowingWeekday() {
        val rule = RecurrenceRule.fromRRule("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")!!
        val start = Instant.parse("2025-01-02T08:00:00Z")
        val after = Instant.parse("2025-01-02T09:00:00Z")
        val next = calculator.nextOccurrence(start, rule, after)
        assertEquals(Instant.parse("2025-01-03T08:00:00Z"), next)
    }

    @Test
    fun everyTwoWeeksRuleSkipsOneWeek() {
        val rule = RecurrenceRule(
            frequency = Frequency.WEEKLY,
            interval = 2,
            weekDays = listOf(WeekdaySpecifier(null, DayOfWeek.MONDAY))
        )
        val start = Instant.parse("2025-01-06T09:00:00Z")
        val after = Instant.parse("2025-01-07T00:00:00Z")
        val next = calculator.nextOccurrence(start, rule, after)
        assertEquals(Instant.parse("2025-01-20T09:00:00Z"), next)
    }

    @Test
    fun monthlyOrdinalRollsToNextMonth() {
        val rule = RecurrenceRule.fromRRule("FREQ=MONTHLY;BYDAY=1FR")!!
        val start = Instant.parse("2025-01-03T10:00:00Z")
        val after = Instant.parse("2025-01-04T00:00:00Z")
        val next = calculator.nextOccurrence(start, rule, after)
        assertEquals(Instant.parse("2025-02-07T10:00:00Z"), next)
    }

    @Test
    fun monthlyDayRuleKeepsDayNumber() {
        val rule = RecurrenceRule.fromRRule("FREQ=MONTHLY;BYMONTHDAY=15")!!
        val start = Instant.parse("2025-01-15T18:00:00Z")
        val after = Instant.parse("2025-01-16T00:00:00Z")
        val next = calculator.nextOccurrence(start, rule, after)
        assertEquals(Instant.parse("2025-02-15T18:00:00Z"), next)
    }
}
