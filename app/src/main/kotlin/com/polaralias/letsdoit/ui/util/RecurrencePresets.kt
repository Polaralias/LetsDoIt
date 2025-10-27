package com.polaralias.letsdoit.ui.util

import com.polaralias.letsdoit.recurrence.Frequency
import com.polaralias.letsdoit.recurrence.RecurrenceRule
import com.polaralias.letsdoit.recurrence.WeekdaySpecifier
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class RecurrencePreset(
    val id: String,
    val title: String,
    val build: (Instant?) -> RecurrenceRule,
    val matches: (RecurrenceRule, Instant?) -> Boolean
)

fun recurrencePresets(zoneId: ZoneId, dueAt: Instant?): List<RecurrencePreset> {
    val weekdayRule = RecurrencePreset(
        id = "weekdays",
        title = "Weekdays",
        build = {
            RecurrenceRule(
                frequency = Frequency.WEEKLY,
                weekDays = listOf(
                    WeekdaySpecifier(null, DayOfWeek.MONDAY),
                    WeekdaySpecifier(null, DayOfWeek.TUESDAY),
                    WeekdaySpecifier(null, DayOfWeek.WEDNESDAY),
                    WeekdaySpecifier(null, DayOfWeek.THURSDAY),
                    WeekdaySpecifier(null, DayOfWeek.FRIDAY)
                )
            )
        },
        matches = { rule, _ ->
            rule.frequency == Frequency.WEEKLY &&
                rule.weekDays.map { it.dayOfWeek }.toSet() == setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
        }
    )
    val dueDate = dueAt?.atZone(zoneId)
    val weeklyDay = dueDate?.dayOfWeek ?: DayOfWeek.MONDAY
    val weeklyTitle = "Every week on ${weeklyDay.getDisplayName(TextStyle.FULL, Locale.UK)}"
    val weeklyRule = RecurrencePreset(
        id = "weekly",
        title = weeklyTitle,
        build = {
            RecurrenceRule(
                frequency = Frequency.WEEKLY,
                weekDays = listOf(WeekdaySpecifier(null, weeklyDay))
            )
        },
        matches = { rule, _ ->
            rule.frequency == Frequency.WEEKLY &&
                rule.interval == 1 &&
                rule.weekDays.map { it.dayOfWeek } == listOf(weeklyDay)
        }
    )
    val monthDay = dueDate?.dayOfMonth ?: 1
    val monthlyDayRule = RecurrencePreset(
        id = "monthly_day",
        title = "Every month on day $monthDay",
        build = {
            RecurrenceRule(
                frequency = Frequency.MONTHLY,
                monthDays = listOf(monthDay)
            )
        },
        matches = { rule, _ ->
            rule.frequency == Frequency.MONTHLY &&
                rule.interval == 1 &&
                rule.monthDays == listOf(monthDay)
        }
    )
    val ordinal = dueDate?.let { ((it.dayOfMonth - 1) / 7) + 1 } ?: 1
    val ordinalRule = RecurrencePreset(
        id = "monthly_ordinal",
        title = "Every month on the ${ordinalName(ordinal)} ${weeklyDay.getDisplayName(TextStyle.FULL, Locale.UK)}",
        build = {
            RecurrenceRule(
                frequency = Frequency.MONTHLY,
                weekDays = listOf(WeekdaySpecifier(ordinal, weeklyDay))
            )
        },
        matches = { rule, _ ->
            rule.frequency == Frequency.MONTHLY &&
                rule.weekDays == listOf(WeekdaySpecifier(ordinal, weeklyDay))
        }
    )
    val yearlyRule = RecurrencePreset(
        id = "yearly",
        title = "Every year on ${monthDay} ${dueDate?.month?.getDisplayName(TextStyle.FULL, Locale.UK) ?: "January"}",
        build = {
            val monthValue = dueDate?.monthValue ?: 1
            RecurrenceRule(
                frequency = Frequency.YEARLY,
                months = listOf(monthValue),
                monthDays = listOf(monthDay)
            )
        },
        matches = { rule, _ ->
            val monthValue = dueDate?.monthValue ?: 1
            rule.frequency == Frequency.YEARLY &&
                rule.months == listOf(monthValue) &&
                rule.monthDays == listOf(monthDay)
        }
    )
    val dailyRule = RecurrencePreset(
        id = "daily",
        title = "Every day",
        build = { RecurrenceRule(frequency = Frequency.DAILY) },
        matches = { rule, _ -> rule.frequency == Frequency.DAILY && rule.interval == 1 }
    )
    return listOf(dailyRule, weekdayRule, weeklyRule, monthlyDayRule, ordinalRule, yearlyRule)
}

private fun ordinalName(value: Int): String {
    return when (value) {
        1 -> "first"
        2 -> "second"
        3 -> "third"
        4 -> "fourth"
        else -> "${value}th"
    }
}
