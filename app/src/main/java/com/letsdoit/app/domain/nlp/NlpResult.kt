package com.letsdoit.app.domain.nlp

import java.time.LocalDateTime

data class NlpResult(
    val cleanTitle: String,
    val detectedDate: LocalDateTime?,
    val detectedPriority: Int?,
    val recurrenceRule: String? = null
)
