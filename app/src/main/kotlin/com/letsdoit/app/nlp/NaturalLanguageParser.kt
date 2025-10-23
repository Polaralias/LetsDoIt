package com.letsdoit.app.nlp

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTask(
    val title: String,
    val dueAt: Instant?,
    val repeat: RepeatPattern?
)

data class RepeatPattern(val expression: String)

@Singleton
class NaturalLanguageParser @Inject constructor(private val clock: Clock) {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val isoRegex = Regex("(\\d{4}-\\d{2}-\\d{2})(?:[ T](\\d{2}:\\d{2}))?\$", RegexOption.IGNORE_CASE)
    private val inMinutesRegex = Regex("in (\\d{1,3}) minutes?\$", RegexOption.IGNORE_CASE)
    private val tomorrowRegex = Regex("tomorrow(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val nextWeekdayRegex = Regex("next (monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val weekdayRegex = Regex("(monday|tuesday|wednesday|thursday|friday|saturday|sunday)(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)
    private val everyWeekdayRegex = Regex("every weekday(?: at)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\$", RegexOption.IGNORE_CASE)

    fun parse(rawInput: String): ParsedTask {
        var working = rawInput.trim()
        var dueAt: Instant? = null
        var repeat: RepeatPattern? = null
        val now = ZonedDateTime.now(clock)

        everyWeekdayRegex.find(working)?.let { match ->
            val hour = match.groupValues[1]
            val minute = match.groupValues[2]
            val meridiem = match.groupValues[3]
            val time = buildTime(hour, minute, meridiem)
            val nextDue = nextWeekdayOccurrence(now, time)
            dueAt = nextDue.toInstant()
            repeat = RepeatPattern("WEEKDAY@${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            working = working.removeRange(match.range).trim()
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
        return ParsedTask(title = title, dueAt = dueAt, repeat = repeat)
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
