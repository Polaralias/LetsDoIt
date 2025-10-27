package com.polaralias.letsdoit.ui.util

import com.polaralias.letsdoit.recurrence.RecurrenceCalculator
import com.polaralias.letsdoit.recurrence.RecurrenceRule
import com.polaralias.letsdoit.recurrence.RecurrenceSummaryFormatter
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

data class RecurrenceDisplay(
    val summary: String,
    val nextDue: Instant
)

fun computeRecurrenceDisplay(
    ruleValue: String?,
    dueAt: Instant?,
    zoneId: ZoneId,
    clock: Clock = Clock.system(zoneId)
): RecurrenceDisplay? {
    val rule = RecurrenceRule.fromRRule(ruleValue) ?: return null
    val formatter = RecurrenceSummaryFormatter(zoneId)
    val calculator = RecurrenceCalculator(zoneId)
    val now = Instant.now(clock)
    val base = dueAt ?: now
    val summary = formatter.summarise(rule, base)
    val nextInstant = when {
        dueAt != null && dueAt.isAfter(now) -> dueAt
        else -> calculator.nextOccurrence(base, rule, now) ?: dueAt ?: return null
    }
    return RecurrenceDisplay(summary = summary, nextDue = nextInstant)
}
