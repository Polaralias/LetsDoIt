package com.polaralias.letsdoit.domain.ai

data class Suggestion(
    val title: String,
    val confidence: Float,
    val reason: String
)
