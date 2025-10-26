package com.letsdoit.app.ai

import com.letsdoit.app.ai.provider.AiProviderException
import com.letsdoit.app.ai.provider.ProviderResponse
import com.letsdoit.app.diagnostics.DiagnosticsRedactor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class AiErrorMapping(val error: AiError, val summary: String)

@Singleton
class AiErrorMapper @Inject constructor(
    private val redactor: DiagnosticsRedactor
) {
    fun fromProvider(providerId: String, failure: ProviderResponse.Failure): AiErrorMapping {
        val safeMessage = sanitise(failure.message)
        val status = failure.status
        val error = when {
            status == 401 || status == 403 -> AiError.Auth
            status == 429 -> AiError.RateLimited
            failure.retryable || (status != null && status in 500..599) -> AiError.Network
            else -> AiError.Unknown(safeMessage)
        }
        val summary = buildSummary(providerId, status, safeMessage)
        return AiErrorMapping(error, summary)
    }

    fun fromException(throwable: Throwable): AiErrorMapping {
        return when (throwable) {
            is AiProviderException -> fromProvider(throwable.providerId, ProviderResponse.Failure(throwable.detail, throwable.retryable, throwable.status))
            else -> {
                val safeMessage = sanitise(throwable.message)
                val summary = buildSummary("unknown", null, safeMessage)
                AiErrorMapping(AiError.Unknown(safeMessage), summary)
            }
        }
    }

    private fun sanitise(message: String?): String {
        if (message.isNullOrBlank()) return "[redacted]"
        val redacted = redactor.redact(message)
        val normalised = redacted.replace("\n", " ").replace("\r", " ")
        val collapsed = normalised.replace(Regex("\\s+"), " ").trim()
        if (collapsed.isEmpty()) return "[redacted]"
        val limit = min(collapsed.length, 200)
        return collapsed.take(limit)
    }

    private fun buildSummary(providerId: String, status: Int?, message: String): String {
        val parts = mutableListOf("provider=$providerId")
        status?.let { parts.add("status=$it") }
        parts.add("message=$message")
        return parts.joinToString(separator = " | ")
    }
}
