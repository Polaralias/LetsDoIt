package com.letsdoit.app.ai.provider

sealed interface ProviderResponse {
    data class Success(val body: String) : ProviderResponse
    data class Failure(val message: String, val retryable: Boolean = false) : ProviderResponse
}

data class AiParsePrompt(
    val transcript: String,
    val projectName: String?,
    val timezone: String?,
    val model: String,
    val heuristicEscalate: Boolean,
    val normalisedDates: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val attempt: Int = 0,
    val escalateReason: String? = null
)

interface AiTextProvider {
    suspend fun parse(input: AiParsePrompt): ProviderResponse
    suspend fun splitSubtasks(title: String, notes: String?): ProviderResponse
    suspend fun draftPlan(title: String, notes: String?): ProviderResponse
}

interface AiImageProvider {
    suspend fun generateStickers(prompt: String, variants: Int, size: String): List<ByteArray>
}
