package com.polaralias.letsdoit.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class RecurrenceCalculator(private val zoneId: ZoneId) {
    fun nextOccurrence(start: Instant, rule: RecurrenceRule, after: Instant): Instant? {
        val base = start.atZone(zoneId)
        val baseTime = base.toLocalTime()
        var candidate = base
        var occurrences = 0
        val limit = rule.count ?: Int.MAX_VALUE
        var guard = 0
        while (occurrences < limit && guard < 20000) {
            if (!candidate.isBefore(base)) {
                val instant = candidate.toInstant()
                if (rule.until != null && instant.isAfter(rule.until)) {
                    return null
                }
                if (matches(rule, base, candidate)) {
                    occurrences += 1
                    if (instant.isAfter(after)) {
                        return instant
                    }
                }
            }
            candidate = nextCandidate(base, candidate, baseTime, rule)
            guard += 1
        }
        return null
    }

    private fun matches(rule: RecurrenceRule, base: java.time.ZonedDateTime, candidate: java.time.ZonedDateTime): Boolean {
        if (candidate.isBefore(base)) return false
        if (candidate.toLocalTime() != base.toLocalTime()) return false
        return when (rule.frequency) {
            Frequency.DAILY -> {
                val days = ChronoUnit.DAYS.between(base.toLocalDate(), candidate.toLocalDate()).toInt()
                days % rule.interval == 0
            }
            Frequency.WEEKLY -> matchesWeekly(rule, base, candidate)
            Frequency.MONTHLY -> matchesMonthly(rule, base, candidate)
            Frequency.YEARLY -> matchesYearly(rule, base, candidate)
        }
    }

    private fun matchesWeekly(rule: RecurrenceRule, base: java.time.ZonedDateTime, candidate: java.time.ZonedDateTime): Boolean {
        val baseWeekStart = base.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val candidateWeekStart = candidate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeks = ChronoUnit.WEEKS.between(baseWeekStart, candidateWeekStart).toInt()
        if (weeks % rule.interval != 0) return false
        val days = rule.weekDays.filter { it.ordinal == null }
        val allowed = if (days.isEmpty()) listOf(base.dayOfWeek) else days.map { it.dayOfWeek }
        return allowed.contains(candidate.dayOfWeek)
    }

    private fun matchesMonthly(rule: RecurrenceRule, base: java.time.ZonedDateTime, candidate: java.time.ZonedDateTime): Boolean {
        val months = ChronoUnit.MONTHS.between(base.toLocalDate().withDayOfMonth(1), candidate.toLocalDate().withDayOfMonth(1)).toInt()
        if (months % rule.interval != 0) return false
        if (rule.monthDays.isNotEmpty()) {
            return rule.monthDays.contains(candidate.dayOfMonth)
        }
        val ordinals = rule.weekDays.filter { it.ordinal != null }
        if (ordinals.isNotEmpty()) {
            return ordinals.any { spec -> matchesOrdinal(spec, candidate.toLocalDate()) }
        }
        return candidate.dayOfMonth == base.dayOfMonth
    }

    private fun matchesYearly(rule: RecurrenceRule, base: java.time.ZonedDateTime, candidate: java.time.ZonedDateTime): Boolean {
        val years = ChronoUnit.YEARS.between(base.toLocalDate().withDayOfYear(1), candidate.toLocalDate().withDayOfYear(1)).toInt()
        if (years % rule.interval != 0) return false
        val months = if (rule.months.isNotEmpty()) rule.months else listOf(base.monthValue)
        if (!months.contains(candidate.monthValue)) return false
        if (rule.monthDays.isNotEmpty()) {
            return rule.monthDays.contains(candidate.dayOfMonth)
        }
        val ordinals = rule.weekDays.filter { it.ordinal != null }
        if (ordinals.isNotEmpty()) {
            return ordinals.any { spec -> matchesOrdinal(spec, candidate.toLocalDate()) }
        }
        return candidate.dayOfMonth == base.dayOfMonth
    }

    private fun matchesOrdinal(spec: WeekdaySpecifier, date: LocalDate): Boolean {
        if (date.dayOfWeek != spec.dayOfWeek) return false
        val ordinal = spec.ordinal ?: return false
        return if (ordinal > 0) {
            val occurrence = (date.dayOfMonth - 1) / 7 + 1
            occurrence == ordinal
        } else {
            val remaining = date.lengthOfMonth() - date.dayOfMonth
            val occurrence = remaining / 7 + 1
            -occurrence == ordinal
        }
    }

    private fun nextCandidate(
        base: java.time.ZonedDateTime,
        current: java.time.ZonedDateTime,
        baseTime: java.time.LocalTime,
        rule: RecurrenceRule
    ): java.time.ZonedDateTime {
        var date = current.toLocalDate().plusDays(1)
        var guard = 0
        while (guard < 20000) {
            val candidate = date.atTime(baseTime).atZone(zoneId)
            if (!candidate.isBefore(base) && matches(rule, base, candidate)) {
                return candidate
            }
            date = date.plusDays(1)
            guard += 1
        }
        return current.plusYears(10)
    }
}
