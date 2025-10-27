package com.polaralias.letsdoit.ai

data class AiInput(
    val transcript: String,
    val projectName: String?,
    val timezone: String?,
    val metadata: Map<String, String> = emptyMap()
)
