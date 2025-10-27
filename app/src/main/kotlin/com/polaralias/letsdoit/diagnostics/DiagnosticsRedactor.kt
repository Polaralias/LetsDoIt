package com.polaralias.letsdoit.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsRedactor @Inject constructor() {
    private val assignmentPatterns = listOf(
        Regex("(?i)(api[_-]?key|token|secret|backup[_-]?key)\s*[:=]\s*([\"']?)([^\"'\s]+)\2"),
        Regex("(?i)(clickup[_-]?token)\s*[:=]\s*([\"']?)([^\"'\s]+)\2")
    )
    private val jsonPatterns = listOf(
        Regex("(?i)(\"(?:api[_-]?key|token|secret|authorization|bearer|backup[_-]?key)\"\s*:\s*\")([^\"\n]*)\""),
        Regex("(?i)(\"prompt\"\s*:\s*\")([^\"\n]*)\"")
    )
    private val openAiPattern = Regex("sk-[A-Za-z0-9]{10,}")
    private val clickUpLoosePattern = Regex("(?i)clickup[a-z0-9_-]*[\s:=]+([^\s]+)")
    private val urlPattern = Regex("https?://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+")
    fun redact(input: String): String {
        var output = input
        assignmentPatterns.forEach { pattern ->
            output = pattern.replace(output) { match ->
                val label = match.groupValues[1]
                "$label=[REDACTED]"
            }
        }
        jsonPatterns.forEach { pattern ->
            output = pattern.replace(output) { match ->
                val label = match.groupValues[1]
                val replacement = if (label.contains("prompt", ignoreCase = true)) {
                    "[REDACTED_PROMPT]"
                } else {
                    "[REDACTED]"
                }
                buildString {
                    append(label)
                    append('"')
                    append(replacement)
                    append('"')
                }
            }
        }
        output = openAiPattern.replace(output) { _ -> "sk-[REDACTED]" }
        output = clickUpLoosePattern.replace(output) { match ->
            val value = match.groupValues[1]
            match.value.replace(value, "[REDACTED]")
        }
        output = urlPattern.replace(output) { _ -> "[REDACTED_URL]" }
        return output
    }
}
