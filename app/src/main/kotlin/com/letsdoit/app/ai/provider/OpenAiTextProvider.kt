package com.letsdoit.app.ai.provider

import com.letsdoit.app.ai.model.AiParsePayload
import com.letsdoit.app.ai.model.AiParseResult
import com.letsdoit.app.ai.model.AiProject
import com.letsdoit.app.ai.model.AiTask
import com.letsdoit.app.ai.model.PlannedStep
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiTextProvider @Inject constructor(
    moshi: Moshi
) : AiTextProvider {
    private val adapter = moshi.adapter(AiParseResult::class.java)
    private val listAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
    private val planAdapter = moshi.adapter<List<PlannedStep>>(Types.newParameterizedType(List::class.java, PlannedStep::class.java))

    override suspend fun parse(input: AiParsePrompt): ProviderResponse {
        val tasks = extractTasks(input)
        val complexity = if (tasks.isEmpty()) 0.0 else minOf(1.0, tasks.size * 0.05)
        val averageConfidence = if (tasks.isEmpty()) 0.0 else 0.8
        val result = AiParseResult(
            can_handle = averageConfidence >= 0.75 && complexity <= 0.4,
            complexity_score = complexity,
            reason = if (tasks.isEmpty()) "No tasks detected" else "Heuristic extraction",
            payload = AiParsePayload(
                tasks = tasks,
                project = AiProject(input.projectName, input.timezone)
            ),
            needs_deeper_reasoning = false
        )
        return ProviderResponse.Success(adapter.toJson(result))
    }

    override suspend fun splitSubtasks(title: String, notes: String?): ProviderResponse {
        val parts = listOfNotNull(title, notes)
            .joinToString(" ")
            .split('.', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val body = listAdapter.toJson(parts)
        return ProviderResponse.Success(body)
    }

    override suspend fun draftPlan(title: String, notes: String?): ProviderResponse {
        val items = extractPlanItems(title, notes)
        val body = planAdapter.toJson(items)
        return ProviderResponse.Success(body)
    }

    private fun extractTasks(input: AiParsePrompt): List<AiTask> {
        val text = input.transcript
        if (text.isBlank()) return emptyList()
        val lines = text.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        var cursor = 0
        return lines.mapIndexed { index, segment ->
            val start = text.indexOf(segment, cursor)
            val safeStart = if (start >= 0) start else cursor
            val end = safeStart + segment.length
            cursor = end
            AiTask(
                id = "t$index",
                title = segment.take(80),
                notes = segment,
                priority = null,
                duration = null,
                dates = null,
                confidence = 0.8,
                subtasks = emptyList(),
                recurrence = null,
                labels = emptyList(),
                assignees = emptyList(),
                dependencies = emptyList(),
                source_text_span = listOf(safeStart, end)
            )
        }
    }

    private fun extractPlanItems(title: String, notes: String?): List<PlannedStep> {
        val items = listOfNotNull(title.takeIf { it.isNotBlank() }, notes?.takeIf { it.isNotBlank() })
            .flatMap { it.split('.', '\n', ';') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (items.isEmpty()) {
            listOf(PlannedStep(title = title, startAt = null, durationMinutes = null))
        } else {
            items.map { entry ->
                PlannedStep(
                    title = entry,
                    startAt = null,
                    durationMinutes = 60
                )
            }
        }
    }

}
