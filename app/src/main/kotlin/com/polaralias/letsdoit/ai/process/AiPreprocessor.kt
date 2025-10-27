package com.polaralias.letsdoit.ai.process

import com.polaralias.letsdoit.ai.AiInput
import com.polaralias.letsdoit.ai.provider.AiParsePrompt
import com.polaralias.letsdoit.ai.prompts.PromptRepository
import com.polaralias.letsdoit.ai.settings.AiSettings
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiPreprocessor @Inject constructor(
    private val clock: Clock,
    private val promptRepository: PromptRepository
) {
    private val dependencyKeywords = listOf("after", "depends on", "only once", "blocked by", "then")
    private val escalationPatterns = listOf("except bank holidays", "unless", "only if")
    private val recurrenceKeywords = listOf("every", "daily", "weekly", "monthly", "yearly")
    private val dateFormatter = DateTimeFormatter.ISO_DATE

    fun buildPrompt(input: AiInput, settings: AiSettings, model: String, attempt: Int, escalateReason: String?, metadata: Map<String, String>): PreprocessResult {
        val transcript = input.transcript
        val textLength = transcript.length
        val heuristicFlags = mutableSetOf<String>()
        if (textLength > 6000) {
            heuristicFlags.add("length")
        }
        val lower = transcript.lowercase(Locale.UK)
        dependencyKeywords.forEach { keyword ->
            if (lower.contains(keyword)) {
                heuristicFlags.add("dependency")
            }
        }
        escalationPatterns.forEach { keyword ->
            if (lower.contains(keyword)) {
                heuristicFlags.add("conditional")
            }
        }
        val recurrenceFound = recurrenceKeywords.any(lower::contains)
        val normalisedDates = detectDates(transcript, input.timezone)
        if (normalisedDates.size > 1) {
            heuristicFlags.add("multiple_dates")
        }
        if (normalisedDates.isNotEmpty()) {
            heuristicFlags.add("dates")
        }
        val prompt = AiParsePrompt(
            instructions = promptRepository.parseTasks,
            transcript = transcript,
            projectName = input.projectName,
            timezone = input.timezone,
            model = model,
            heuristicEscalate = heuristicFlags.isNotEmpty() || textLength > settings.miniComfortWindow,
            normalisedDates = normalisedDates,
            metadata = metadata,
            attempt = attempt,
            escalateReason = escalateReason
        )
        return PreprocessResult(
            prompt = prompt,
            heuristicFlags = heuristicFlags,
            recurrenceDetected = recurrenceFound,
            textLength = textLength
        )
    }

    private fun detectDates(transcript: String, timezone: String?): Map<String, String> {
        val zone = timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val today = LocalDate.now(clock.withZone(zone))
        val lower = transcript.lowercase(Locale.UK)
        val matches = mutableMapOf<String, String>()
        if ("today" in lower) {
            matches["today"] = today.format(dateFormatter)
        }
        if ("tomorrow" in lower) {
            matches["tomorrow"] = today.plusDays(1).format(dateFormatter)
        }
        if ("next week" in lower) {
            matches["next week"] = today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY).format(dateFormatter)
        }
        if ("next month" in lower) {
            matches["next month"] = today.plusMonths(1).withDayOfMonth(1).format(dateFormatter)
        }
        return matches
    }
}

data class PreprocessResult(
    val prompt: AiParsePrompt,
    val heuristicFlags: Set<String>,
    val recurrenceDetected: Boolean,
    val textLength: Int
)
