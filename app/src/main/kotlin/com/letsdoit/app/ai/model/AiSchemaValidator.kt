package com.letsdoit.app.ai.model


class AiSchemaValidator {
    fun validate(result: AiParseResult): SchemaValidationResult {
        val errors = mutableListOf<String>()
        if (result.complexity_score < 0.0 || result.complexity_score > 1.0) {
            errors.add("complexity_score_out_of_range")
        }
        val payload = result.payload
        if (payload != null) {
            if (payload.tasks.isEmpty()) {
                errors.add("tasks_empty")
            } else {
                payload.tasks.forEachIndexed { index, task ->
                    validateTask(task, "task_$index", errors)
                }
            }
        }
        return if (errors.isEmpty()) SchemaValidationResult.Valid else SchemaValidationResult.Invalid(errors)
    }

    private fun validateTask(task: AiTask, path: String, errors: MutableList<String>) {
        if (task.title.isBlank()) {
            errors.add("${path}_title_blank")
        }
        task.priority?.let {
            if (it !in setOf("high", "med", "normal", "low")) {
                errors.add("${path}_priority_invalid")
            }
        }
        task.duration?.let {
            if (it.value != null && it.value < 0) {
                errors.add("${path}_duration_negative")
            }
            it.unit?.let { unit ->
                if (unit !in setOf("minute", "hour", "day")) {
                    errors.add("${path}_duration_unit")
                }
            }
        }
        task.confidence?.let {
            if (it < 0.0 || it > 1.0 || it.isNaN() || it.isInfinite()) {
                errors.add("${path}_confidence_out_of_range")
            }
        }
        task.source_text_span?.let {
            if (it.size != 2 || it.any { value -> value < 0 }) {
                errors.add("${path}_span_invalid")
            } else if (it[0] > it[1]) {
                errors.add("${path}_span_order")
            }
        }
        task.subtasks.forEachIndexed { index, sub ->
            validateTask(sub, "${path}_sub_$index", errors)
        }
    }
}

sealed interface SchemaValidationResult {
    data object Valid : SchemaValidationResult
    data class Invalid(val errors: List<String>) : SchemaValidationResult
}

fun mapPriorityToLevel(value: String?): Int? {
    return when (value) {
        "high" -> 0
        "med" -> 1
        "normal" -> 2
        "low" -> 3
        else -> null
    }
}

fun averageConfidence(tasks: List<AiTask>): Double? {
    val confidences = collectConfidences(tasks)
    if (confidences.isEmpty()) {
        return null
    }
    return confidences.average()
}

private fun collectConfidences(tasks: List<AiTask>): List<Double> {
    val values = mutableListOf<Double>()
    tasks.forEach { task ->
        task.confidence?.takeIf { !it.isNaN() && !it.isInfinite() }?.let(values::add)
        if (task.subtasks.isNotEmpty()) {
            values.addAll(collectConfidences(task.subtasks))
        }
    }
    return values
}
