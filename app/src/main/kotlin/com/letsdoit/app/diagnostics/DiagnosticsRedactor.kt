package com.letsdoit.app.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsRedactor @Inject constructor() {
    private val assignmentPatterns = listOf(
        Regex("(?i)(api[_-]?key)\\s*[:=]\\s*([^\\s]+)"),
        Regex("(?i)(clickup[_-]?token)\\s*[:=]\\s*([^\\s]+)")
    )
    private val openAiPattern = Regex("sk-[A-Za-z0-9]{10,}")
    private val clickUpLoosePattern = Regex("(?i)clickup[a-z0-9_-]*[\\s:=]+([^\\s]+)")

    fun redact(input: String): String {
        var output = input
        assignmentPatterns.forEach { pattern ->
            output = pattern.replace(output) { match ->
                val label = match.groupValues[1]
                "$label=[REDACTED]"
            }
        }
        output = openAiPattern.replace(output) { _ -> "sk-[REDACTED]" }
        output = clickUpLoosePattern.replace(output) { match ->
            val value = match.groupValues[1]
            match.value.replace(value, "[REDACTED]")
        }
        return output
    }
}
