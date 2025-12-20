package com.polaralias.letsdoit.domain.nlp

import java.time.LocalDateTime

data class NlpResult(
    val cleanTitle: String,
    val detectedDate: LocalDateTime?,
    val detectedPriority: Int?,
    val recurrenceRule: String? = null
)
