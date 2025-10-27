package com.polaralias.letsdoit.ai.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiProject(
    val name: String?,
    val timezone: String?
)

@JsonClass(generateAdapter = true)
data class AiDuration(
    val value: Int?,
    val unit: String?
)

@JsonClass(generateAdapter = true)
data class AiDates(
    val due: String?,
    val earliest_start: String?,
    val latest_finish: String?
)

@JsonClass(generateAdapter = true)
data class AiTask(
    val id: String?,
    val title: String,
    val notes: String?,
    val labels: List<String> = emptyList(),
    val priority: String?,
    val duration: AiDuration?,
    val dates: AiDates?,
    val assignees: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val subtasks: List<AiTask> = emptyList(),
    val recurrence: String?,
    val confidence: Double?,
    val source_text_span: List<Int>?
)

@JsonClass(generateAdapter = true)
data class AiParsePayload(
    val tasks: List<AiTask>,
    val project: AiProject?
)

@JsonClass(generateAdapter = true)
data class AiParseResult(
    val can_handle: Boolean,
    val complexity_score: Double,
    val reason: String?,
    val payload: AiParsePayload?,
    val needs_deeper_reasoning: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PlannedStep(
    val title: String,
    val startAt: Long?,
    val durationMinutes: Int?
)
