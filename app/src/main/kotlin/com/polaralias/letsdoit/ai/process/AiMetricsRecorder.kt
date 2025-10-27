package com.polaralias.letsdoit.ai.process

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AiCallMetric(
    val provider: String,
    val model: String,
    val durationMs: Long,
    val success: Boolean,
    val escalated: Boolean
)

@Singleton
class AiMetricsRecorder @Inject constructor() {
    private val entries = MutableStateFlow<List<AiCallMetric>>(emptyList())

    val metrics: Flow<List<AiCallMetric>> = entries

    fun record(metric: AiCallMetric) {
        entries.value = (entries.value + metric).takeLast(100)
    }

    val latestSummary: Flow<Map<String, Long>> = entries.map { list ->
        list.groupingBy { it.provider }.fold(0L) { acc, item -> acc + item.durationMs }
    }
}
