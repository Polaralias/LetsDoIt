package com.polaralias.letsdoit.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class RecurrenceSummaryFormatter(private val zoneId: ZoneId) {
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM").withLocale(Locale.UK)

    fun summarise(rule: RecurrenceRule, start: Instant): String {
        return when (rule.frequency) {
            Frequency.DAILY -> dailySummary(rule)
            Frequency.WEEKLY -> weeklySummary(rule)
            Frequency.MONTHLY -> monthlySummary(rule, start)
            Frequency.YEARLY -> yearlySummary(rule, start)
        }
    }

    private fun dailySummary(rule: RecurrenceRule): String {
        return if (rule.interval == 1) {
            "Every day"
        } else {
            "Every ${rule.interval} days"
        }
    }

    private fun weeklySummary(rule: RecurrenceRule): String {
        val days = rule.weekDays.filter { it.ordinal == null }.map { it.dayOfWeek }.distinct()
        val dayText = if (days.isEmpty()) {
            "on ${DayStringFormatter.formatDay(null)}"
        } else {
            "on ${DayStringFormatter.joinDays(days)}"
        }
        return if (rule.interval == 1) {
            "Every week $dayText"
        } else {
            "Every ${rule.interval} weeks $dayText"
        }
    }

    private fun monthlySummary(rule: RecurrenceRule, start: Instant): String {
        val base = start.atZone(zoneId)
        return when {
            rule.monthDays.isNotEmpty() -> {
                val sorted = rule.monthDays.sorted()
                val day = if (sorted.size == 1) "day ${sorted.first()}" else "days ${sorted.joinToString(", ")}"
                intervalPrefix(rule.interval, "month") + " on $day"
            }
            rule.weekDays.any { it.ordinal != null } -> {
                val parts = rule.weekDays.filter { it.ordinal != null }.map { spec ->
                    val name = spec.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK)
                    val ordinal = ordinalName(spec.ordinal ?: 1)
                    "$ordinal $name"
                }
                intervalPrefix(rule.interval, "month") + " on the ${parts.joinToString(" and ")}"
            }
            else -> {
                val day = base.dayOfMonth
                intervalPrefix(rule.interval, "month") + " on day $day"
            }
        }
    }

    private fun yearlySummary(rule: RecurrenceRule, start: Instant): String {
        val base = start.atZone(zoneId)
        val months = rule.months.takeIf { it.isNotEmpty() } ?: listOf(base.monthValue)
        val dateText = if (rule.monthDays.isNotEmpty()) {
            val day = rule.monthDays.first()
            val monthName = monthName(months.first())
            "$day $monthName"
        } else if (rule.weekDays.any { it.ordinal != null }) {
            val spec = rule.weekDays.first { it.ordinal != null }
            val ordinal = ordinalName(spec.ordinal ?: 1)
            val day = spec.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK)
            "$ordinal $day of ${monthName(months.first())}"
        } else {
            val date = base.withMonth(months.first())
            dateFormatter.format(date)
        }
        return if (rule.interval == 1) {
            "Every year on $dateText"
        } else {
            "Every ${rule.interval} years on $dateText"
        }
    }

    private fun intervalPrefix(interval: Int, unit: String): String {
        return if (interval == 1) {
            "Every $unit"
        } else {
            "Every $interval ${unit}s"
        }
    }

    private fun ordinalName(value: Int): String {
        val positive = if (value < 0) -value else value
        val suffix = when (positive) {
            1 -> "first"
            2 -> "second"
            3 -> "third"
            4 -> "fourth"
            else -> "${positive}th"
        }
        return if (value < 0) "last" else suffix
    }

    private fun monthName(value: Int): String {
        return java.time.Month.of(value).getDisplayName(TextStyle.FULL, Locale.UK)
    }
}

private object DayStringFormatter {
    fun formatDay(day: DayOfWeek?): String {
        return day?.getDisplayName(TextStyle.FULL, Locale.UK) ?: "the same day"
    }

    fun joinDays(days: List<DayOfWeek>): String {
        val names = days.sortedBy { it.value }.map { it.getDisplayName(TextStyle.SHORT, Locale.UK) }
        return when (names.size) {
            0 -> "${formatDay(null)}"
            1 -> names.first()
            2 -> names.joinToString(" and ")
            else -> names.dropLast(1).joinToString(", ") + ", and " + names.last()
        }
    }
}
