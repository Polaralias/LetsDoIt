package com.polaralias.letsdoit.nlp

import com.polaralias.letsdoit.recurrence.Frequency
import com.polaralias.letsdoit.recurrence.RecurrenceCalculator
import com.polaralias.letsdoit.recurrence.RecurrenceRule
import com.polaralias.letsdoit.recurrence.WeekdaySpecifier
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTask(
    val title: String,
    val dueAt: Instant?,
    val repeatRule: String?,
    val remindOffsetMinutes: Int?
)

@Singleton
class NaturalLanguageParser @Inject constructor(private val clock: Clock) {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val isoRegex = Regex("(\\d{4}-\\d{2}-\\d{2})(?:[ T](\\d{2}:\\d{2}))?\$", RegexOption.IGNORE_CASE)
    private val inMinutesRegex = Regex("in (\\d{1,3}) minutes?\$", RegexOption.IGNORE_CASE)
    private val tomorrowRegex = Regex("tomorrow(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val nextWeekdayRegex = Regex("next (monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val weekdayRegex = Regex("(monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val everyWeekdayRegex = Regex("every weekday(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val remindRegex = Regex("remind me (\\d{1,3}) minutes? before", RegexOption.IGNORE_CASE)
    private val everyWeeksRegex = Regex("every(?:\\s+(\\d+))?\\s+weeks? on (monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    private val monthlyOrdinalRegex = Regex("on the (first|second|third|fourth|last) (monday|tuesday|wednesday|thursday|friday|saturday|sunday) of each month(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    private val monthlyDayRegex = Regex("(?:repeat )?monthly on day (\\d{1,2})(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)

    fun parse(rawInput: String): ParsedTask {
        var working = rawInput.trim()
        var dueAt: Instant? = null
        var repeatRule: RecurrenceRule? = null
        var remindOffset: Int? = null
        val now = ZonedDateTime.now(clock)
        val calculator = RecurrenceCalculator(zoneId)

        remindRegex.find(working)?.let { match ->
            remindOffset = match.groupValues[1].toIntOrNull()
            working = working.removeRange(match.range).trim()
        }

        everyWeekdayRegex.find(working)?.let { match ->
            val hour = match.groupValues[1]
            val minute = match.groupValues[2]
            val meridiem = match.groupValues[3]
            val time = buildTime(hour, minute, meridiem)
            val nextDue = nextWeekdayOccurrence(now, time)
            dueAt = nextDue.toInstant()
            repeatRule = RecurrenceRule(
                frequency = Frequency.WEEKLY,
                weekDays = listOf(
                    WeekdaySpecifier(null, DayOfWeek.MONDAY),
                    WeekdaySpecifier(null, DayOfWeek.TUESDAY),
                    WeekdaySpecifier(null, DayOfWeek.WEDNESDAY),
                    WeekdaySpecifier(null, DayOfWeek.THURSDAY),
                    WeekdaySpecifier(null, DayOfWeek.FRIDAY)
                )
            )
            working = working.removeRange(match.range).trim()
        }

        if (repeatRule == null) {
            everyWeeksRegex.find(working)?.let { match ->
                val interval = match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: 1
                val day = parseDayOfWeek(match.groupValues[2])
                val time = buildTime(match.groupValues[3], match.groupValues[4], match.groupValues[5])
                val base = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
                val rule = RecurrenceRule(
                    frequency = Frequency.WEEKLY,
                    interval = interval,
                    weekDays = listOf(WeekdaySpecifier(null, day))
                )
                val nextDue = calculator.nextOccurrence(base.toInstant(), rule, now.toInstant()) ?: base.toInstant()
                dueAt = nextDue
                repeatRule = rule
                working = working.removeRange(match.range).trim()
            }
        }

        if (repeatRule == null) {
            monthlyOrdinalRegex.find(working)?.let { match ->
                val ordinal = parseOrdinalWord(match.groupValues[1])
                val day = parseDayOfWeek(match.groupValues[2])
                val time = buildTime(match.groupValues[3], match.groupValues[4], match.groupValues[5])
                val nextDue = nextMonthlyOrdinal(now, ordinal, day, time)
                val rule = RecurrenceRule(
                    frequency = Frequency.MONTHLY,
                    weekDays = listOf(WeekdaySpecifier(ordinal, day))
                )
                dueAt = nextDue.toInstant()
                repeatRule = rule
                working = working.removeRange(match.range).trim()
            }
        }

        if (repeatRule == null) {
            monthlyDayRegex.find(working)?.let { match ->
                val dayValue = match.groupValues[1].toInt().coerceIn(1, 31)
                val time = buildTime(match.groupValues[2], match.groupValues[3], match.groupValues[4])
                val nextDue = nextMonthlyDay(now, dayValue, time)
                val rule = RecurrenceRule(
                    frequency = Frequency.MONTHLY,
                    monthDays = listOf(dayValue)
                )
                dueAt = nextDue.toInstant()
                repeatRule = rule
                working = working.removeRange(match.range).trim()
            }
        }

        if (dueAt == null) {
            inMinutesRegex.find(working)?.let { match ->
                val minutes = match.groupValues[1].toLong()
                dueAt = now.plusMinutes(minutes).toInstant()
                working = working.removeRange(match.range).trim()
            }
        }

        if (dueAt == null) {
            tomorrowRegex.find(working)?.let { match ->
                val time = buildTime(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                val next = now.plusDays(1).withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
                dueAt = next.toInstant()
                working = working.removeRange(match.range).trim()
            }
        }

        if (dueAt == null) {
            nextWeekdayRegex.find(working)?.let { match ->
                val day = match.groupValues[1]
                val time = buildTime(match.groupValues[2], match.groupValues[3], match.groupValues[4])
                val targetDay = parseDayOfWeek(day)
                val next = nextSpecificWeekday(now, targetDay, time, true)
                dueAt = next.toInstant()
                working = working.removeRange(match.range).trim()
            }
        }

        if (dueAt == null) {
            weekdayRegex.find(working)?.let { match ->
                val day = match.groupValues[1]
                val time = buildTime(match.groupValues[2], match.groupValues[3], match.groupValues[4])
                val targetDay = parseDayOfWeek(day)
                val next = nextSpecificWeekday(now, targetDay, time, false)
                dueAt = next.toInstant()
                working = working.removeRange(match.range).trim()
            }
        }

        if (dueAt == null) {
            isoRegex.find(working)?.let { match ->
                val datePart = match.groupValues[1]
                val timePart = match.groupValues[2]
                val localDate = LocalDate.parse(datePart)
                val localTime = if (timePart.isNotBlank()) {
                    LocalTime.parse(timePart)
                } else {
                    LocalTime.MIDNIGHT
                }
                val zoned = ZonedDateTime.of(localDate, localTime, zoneId)
                dueAt = zoned.toInstant()
                working = working.removeRange(match.range).trim()
            }
        }

        val title = if (working.isBlank()) rawInput.trim() else working
        return ParsedTask(title = title, dueAt = dueAt, repeatRule = repeatRule?.toRRule(), remindOffsetMinutes = remindOffset)
    }

    private fun buildTime(hourPart: String, minutePart: String, meridiem: String): LocalTime {
        var hour = hourPart.toInt()
        val minute = minutePart.takeIf { it.isNotBlank() }?.toInt() ?: 0
        if (meridiem.isNotBlank()) {
            val lower = meridiem.lowercase()
            if (lower == "pm" && hour < 12) {
                hour += 12
            }
            if (lower == "am" && hour == 12) {
                hour = 0
            }
        }
        return LocalTime.of(hour % 24, minute)
    }

    private fun parseOrdinalWord(value: String): Int {
        return when (value.lowercase(Locale.UK)) {
            "first" -> 1
            "second" -> 2
            "third" -> 3
            "fourth" -> 4
            "last" -> -1
            else -> 1
        }
    }

    private fun nextMonthlyOrdinal(now: ZonedDateTime, ordinal: Int, dayOfWeek: DayOfWeek, time: LocalTime): ZonedDateTime {
        var monthCursor = now.withDayOfMonth(1).withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        while (true) {
            val candidate = if (ordinal < 0) {
                monthCursor.with(java.time.temporal.TemporalAdjusters.lastInMonth(dayOfWeek))
            } else {
                monthCursor.with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek))
            }
            if (candidate.isAfter(now)) {
                return candidate
            }
            monthCursor = monthCursor.plusMonths(1)
        }
    }

    private fun nextMonthlyDay(now: ZonedDateTime, dayOfMonth: Int, time: LocalTime): ZonedDateTime {
        var monthCursor = now.withDayOfMonth(1).withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        while (true) {
            val length = monthCursor.toLocalDate().lengthOfMonth()
            val day = dayOfMonth.coerceAtMost(length)
            val candidate = monthCursor.withDayOfMonth(day)
            if (candidate.isAfter(now)) {
                return candidate
            }
            monthCursor = monthCursor.plusMonths(1)
        }
    }

    private fun parseDayOfWeek(value: String): DayOfWeek {
        return when (value.lowercase()) {
            "monday" -> DayOfWeek.MONDAY
            "tuesday" -> DayOfWeek.TUESDAY
            "wednesday" -> DayOfWeek.WEDNESDAY
            "thursday" -> DayOfWeek.THURSDAY
            "friday" -> DayOfWeek.FRIDAY
            "saturday" -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
    }

    private fun nextWeekdayOccurrence(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        var candidate = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        var daysToAdd = 0L
        while (!isWeekday(candidate.plusDays(daysToAdd))) {
            daysToAdd += 1
        }
        candidate = candidate.plusDays(daysToAdd)
        if (!isWeekday(now) || candidate.isBefore(now)) {
            candidate = candidate.plusDays(1)
            while (!isWeekday(candidate)) {
                candidate = candidate.plusDays(1)
            }
            candidate = candidate.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        }
        if (candidate.isBefore(now)) {
            candidate = candidate.plusDays(1)
            while (!isWeekday(candidate)) {
                candidate = candidate.plusDays(1)
            }
            candidate = candidate.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        }
        return candidate
    }

    private fun nextSpecificWeekday(
        now: ZonedDateTime,
        target: DayOfWeek,
        time: LocalTime,
        skipToday: Boolean
    ): ZonedDateTime {
        var candidate = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        var daysToAdd = ((target.value - now.dayOfWeek.value + 7) % 7).toLong()
        if (skipToday && daysToAdd == 0L) {
            daysToAdd = 7
        }
        candidate = candidate.plusDays(daysToAdd)
        if (!skipToday && daysToAdd == 0L && candidate.isBefore(now)) {
            candidate = candidate.plusDays(7)
        }
        return candidate
    }

    private fun isWeekday(zoned: ZonedDateTime): Boolean {
        return when (zoned.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> false
            else -> true
        }
    }
}
