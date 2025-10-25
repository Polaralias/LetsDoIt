package com.letsdoit.app.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class WeekdaySpecifier(val ordinal: Int?, val dayOfWeek: DayOfWeek)

data class RecurrenceRule(
    val frequency: Frequency,
    val interval: Int = 1,
    val weekDays: List<WeekdaySpecifier> = emptyList(),
    val monthDays: List<Int> = emptyList(),
    val months: List<Int> = emptyList(),
    val count: Int? = null,
    val until: Instant? = null
) {
    fun toRRule(): String {
        val segments = mutableListOf<String>()
        segments += "FREQ=${frequency.name}"
        if (interval > 1) {
            segments += "INTERVAL=$interval"
        }
        if (weekDays.isNotEmpty()) {
            val token = weekDays.joinToString(",") { spec ->
                val prefix = spec.ordinal?.toString() ?: ""
                "$prefix${spec.dayOfWeek.name.take(2)}"
            }
            segments += "BYDAY=$token"
        }
        if (monthDays.isNotEmpty()) {
            segments += "BYMONTHDAY=${monthDays.joinToString(",")}"
        }
        if (months.isNotEmpty()) {
            segments += "BYMONTH=${months.joinToString(",")}"
        }
        count?.let { segments += "COUNT=$it" }
        until?.let { segments += "UNTIL=${formatter.format(LocalDateTime.ofInstant(it, ZoneOffset.UTC))}" }
        return segments.joinToString(";")
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withLocale(Locale.UK)

        fun fromRRule(value: String?): RecurrenceRule? {
            if (value.isNullOrBlank()) return null
            val parts = value.trim().split(';').mapNotNull { segment ->
                val idx = segment.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = segment.substring(0, idx).uppercase(Locale.UK)
                val data = segment.substring(idx + 1)
                key to data
            }.toMap()
            val freqValue = parts["FREQ"] ?: return null
            val frequency = runCatching { Frequency.valueOf(freqValue.uppercase(Locale.UK)) }.getOrNull() ?: return null
            val interval = parts["INTERVAL"]?.toIntOrNull()?.takeIf { it > 0 } ?: 1
            val byDay = parts["BYDAY"]?.split(',')?.mapNotNull { token ->
                if (token.isBlank()) return@mapNotNull null
                val trimmed = token.trim()
                val dayPart = trimmed.takeLast(2)
                val ordinalPart = trimmed.dropLast(2)
                val day = dayFromToken(dayPart) ?: return@mapNotNull null
                val ordinal = ordinalPart.takeIf { it.isNotBlank() }?.toIntOrNull()
                WeekdaySpecifier(ordinal = ordinal, dayOfWeek = day)
            } ?: emptyList()
            val byMonthDay = parts["BYMONTHDAY"]?.split(',')?.mapNotNull { it.toIntOrNull() }?.filter { it in 1..31 }
                ?: emptyList()
            val byMonth = parts["BYMONTH"]?.split(',')?.mapNotNull { it.toIntOrNull() }?.filter { it in 1..12 } ?: emptyList()
            val count = parts["COUNT"]?.toIntOrNull()?.takeIf { it > 0 }
            val until = parts["UNTIL"]?.let { raw ->
                runCatching {
                    val dt = LocalDateTime.parse(raw, formatter)
                    dt.toInstant(ZoneOffset.UTC)
                }.getOrNull()
            }
            return RecurrenceRule(
                frequency = frequency,
                interval = interval,
                weekDays = byDay,
                monthDays = byMonthDay,
                months = byMonth,
                count = count,
                until = until
            )
        }

        private fun dayFromToken(token: String): DayOfWeek? {
            return when (token.uppercase(Locale.UK)) {
                "MO" -> DayOfWeek.MONDAY
                "TU" -> DayOfWeek.TUESDAY
                "WE" -> DayOfWeek.WEDNESDAY
                "TH" -> DayOfWeek.THURSDAY
                "FR" -> DayOfWeek.FRIDAY
                "SA" -> DayOfWeek.SATURDAY
                "SU" -> DayOfWeek.SUNDAY
                else -> null
            }
        }
    }
}
