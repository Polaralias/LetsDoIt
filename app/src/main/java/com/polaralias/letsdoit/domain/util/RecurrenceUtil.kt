package com.polaralias.letsdoit.domain.util

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

object RecurrenceUtil {

    fun calculateNextDueDate(currentDueDate: LocalDateTime, rrule: String): LocalDateTime? {
        val rules = parseRRule(rrule)
        val freq = rules["FREQ"] ?: return null

        return when (freq) {
            "DAILY" -> currentDueDate.plusDays(1)
            "WEEKLY" -> {
                val byDay = rules["BYDAY"]
                if (byDay != null) {
                    // e.g., MO, TU, WE
                    // Find next occurrence matching one of the days
                    val days = byDay.split(",").mapNotNull { parseDay(it) }
                    if (days.isEmpty()) {
                         currentDueDate.plusWeeks(1)
                    } else {
                        // Find the soonest day in the list that is after currentDueDate
                        var nextDate: LocalDateTime? = null
                        for (day in days) {
                             val date = currentDueDate.with(TemporalAdjusters.next(day))
                             if (nextDate == null || date.isBefore(nextDate)) {
                                 nextDate = date
                             }
                        }
                        // If we are strictly following RRule, we should check if today matches and if we should move to next week?
                        // But simplify: just find next occurrence.
                        // Wait, "next Monday" from "Monday" is +1 week.
                        // temporalAdjusters.next(Monday) returns next monday (at least 1 day later).

                        // Issue: if we have multiple days, e.g. MO,WE. And today is MO. Next is WE.
                        // TemporalAdjusters.next(WE) gives next Wednesday.
                        // TemporalAdjusters.next(MO) gives next Monday.
                        // We take the minimum.

                        nextDate
                    }
                } else {
                    currentDueDate.plusWeeks(1)
                }
            }
            "MONTHLY" -> currentDueDate.plusMonths(1)
            else -> null
        }
    }

    private fun parseRRule(rrule: String): Map<String, String> {
        return rrule.split(";").associate { part ->
            val split = part.split("=")
            if (split.size == 2) {
                split[0] to split[1]
            } else {
                "" to ""
            }
        }
    }

    private fun parseDay(dayStr: String): DayOfWeek? {
        return when (dayStr) {
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
