package com.letsdoit.app.domain.nlp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.regex.Pattern

object NlpEngine {

    // Regex patterns for various date/time formats
    // We use case-insensitive compilation

    // Priority: priority 1, priority high, etc.
    private val PRIORITY_PATTERN = Pattern.compile("\\b(priority|prio)\\s*(\\d+|high|urgent|normal|low)\\b", Pattern.CASE_INSENSITIVE)

    // "tomorrow", "today", "yesterday"
    private val TODAY_PATTERN = Pattern.compile("\\b(today)\\b", Pattern.CASE_INSENSITIVE)
    private val TOMORROW_PATTERN = Pattern.compile("\\b(tomorrow|tmr)\\b", Pattern.CASE_INSENSITIVE)
    private val YESTERDAY_PATTERN = Pattern.compile("\\b(yesterday)\\b", Pattern.CASE_INSENSITIVE)

    // "next Friday", "next mon"
    private val NEXT_DAY_PATTERN = Pattern.compile("\\bnext\\s+(monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\\b", Pattern.CASE_INSENSITIVE)

    // "in X minutes/hours/days"
    private val IN_TIME_PATTERN = Pattern.compile("\\bin\\s+(\\d+)\\s*(min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days)\\b", Pattern.CASE_INSENSITIVE)

    // "at 5pm", "at 17:00"
    private val AT_TIME_PATTERN = Pattern.compile("\\bat\\s+(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE)

    fun parse(input: String, now: LocalDateTime = LocalDateTime.now()): NlpResult {
        var cleanTitle = input
        var detectedDate: LocalDateTime? = null
        var detectedPriority: Int? = null

        // 1. Detect Priority
        val priorityMatcher = PRIORITY_PATTERN.matcher(cleanTitle)
        if (priorityMatcher.find()) {
            val value = priorityMatcher.group(2)!!.lowercase()
            detectedPriority = when (value) {
                "urgent", "1" -> 1
                "high", "2" -> 2
                "normal", "3" -> 3
                "low", "4" -> 4
                else -> null
            }
            if (detectedPriority != null) {
                cleanTitle = cleanTitle.replace(priorityMatcher.group(0), "").trim()
            }
        }

        // 2. Detect Relative Date (Today, Tomorrow, Yesterday)
        var date: LocalDate? = null

        val tomorrowMatcher = TOMORROW_PATTERN.matcher(cleanTitle)
        if (tomorrowMatcher.find()) {
            date = now.toLocalDate().plusDays(1)
            cleanTitle = cleanTitle.replace(tomorrowMatcher.group(0), "").trim()
        } else {
            val todayMatcher = TODAY_PATTERN.matcher(cleanTitle)
            if (todayMatcher.find()) {
                date = now.toLocalDate()
                cleanTitle = cleanTitle.replace(todayMatcher.group(0), "").trim()
            } else {
                val yesterdayMatcher = YESTERDAY_PATTERN.matcher(cleanTitle)
                if (yesterdayMatcher.find()) {
                    date = now.toLocalDate().minusDays(1)
                    cleanTitle = cleanTitle.replace(yesterdayMatcher.group(0), "").trim()
                }
            }
        }

        // 3. Detect "Next [Day]"
        if (date == null) {
            val nextDayMatcher = NEXT_DAY_PATTERN.matcher(cleanTitle)
            if (nextDayMatcher.find()) {
                val dayStr = nextDayMatcher.group(1)!!.lowercase()
                val targetDayOfWeek = getDayOfWeek(dayStr)
                if (targetDayOfWeek != null) {
                    date = now.toLocalDate().with(TemporalAdjusters.next(targetDayOfWeek))
                    cleanTitle = cleanTitle.replace(nextDayMatcher.group(0), "").trim()
                }
            }
        }

        // 4. Detect "In X time"
        // This sets the full LocalDateTime
        val inTimeMatcher = IN_TIME_PATTERN.matcher(cleanTitle)
        if (inTimeMatcher.find()) {
            val amount = inTimeMatcher.group(1)!!.toLong()
            val unit = inTimeMatcher.group(2)!!.lowercase()

            val tempDate = when {
                unit.startsWith("min") -> now.plusMinutes(amount)
                unit.startsWith("h") -> now.plusHours(amount)
                unit.startsWith("d") -> now.plusDays(amount)
                else -> now
            }

            detectedDate = tempDate
            date = tempDate.toLocalDate() // update date part just in case

            cleanTitle = cleanTitle.replace(inTimeMatcher.group(0), "").trim()
        }

        // 5. Detect "at [Time]"
        // This refines the time part of the detected date, or assumes today if no date detected yet.
        val atTimeMatcher = AT_TIME_PATTERN.matcher(cleanTitle)
        if (atTimeMatcher.find()) {
            val hourStr = atTimeMatcher.group(1)!!
            val minuteStr = atTimeMatcher.group(3) // group 2 includes colon
            val amPm = atTimeMatcher.group(4)?.lowercase()

            var hour = hourStr.toInt()
            val minute = minuteStr?.toInt() ?: 0

            if (amPm == "pm" && hour < 12) {
                hour += 12
            } else if (amPm == "am" && hour == 12) {
                hour = 0
            }

            val time = LocalTime.of(hour, minute)

            if (detectedDate != null) {
                 detectedDate = detectedDate!!.with(time)
            } else if (date != null) {
                detectedDate = LocalDateTime.of(date, time)
            } else {
                // No date detected, assume today at this time.
                // If the time has already passed today, assume tomorrow.
                val todayAtTime = LocalDateTime.of(now.toLocalDate(), time)
                if (todayAtTime.isBefore(now)) {
                    detectedDate = todayAtTime.plusDays(1)
                } else {
                    detectedDate = todayAtTime
                }
            }
            cleanTitle = cleanTitle.replace(atTimeMatcher.group(0), "").trim()
        } else {
            // No time specified.
            if (detectedDate == null && date != null) {
                // Default to 9 AM
                detectedDate = LocalDateTime.of(date, LocalTime.of(9, 0))
            }
        }

        // Cleanup extra spaces
        cleanTitle = cleanTitle.replace("\\s+".toRegex(), " ").trim()

        return NlpResult(cleanTitle, detectedDate, detectedPriority)
    }

    private fun getDayOfWeek(input: String): DayOfWeek? {
        return when {
            input.startsWith("mon") -> DayOfWeek.MONDAY
            input.startsWith("tue") -> DayOfWeek.TUESDAY
            input.startsWith("wed") -> DayOfWeek.WEDNESDAY
            input.startsWith("thu") -> DayOfWeek.THURSDAY
            input.startsWith("fri") -> DayOfWeek.FRIDAY
            input.startsWith("sat") -> DayOfWeek.SATURDAY
            input.startsWith("sun") -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}
