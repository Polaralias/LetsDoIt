package com.letsdoit.app.ai.diagnostics

import com.letsdoit.app.diagnostics.DiagnosticsManager
import javax.inject.Inject
import javax.inject.Singleton

interface AiDiagnosticsSink {
    fun log(summary: String)
}

@Singleton
class DiagnosticsManagerSink @Inject constructor(
    private val diagnosticsManager: DiagnosticsManager
) : AiDiagnosticsSink {
    override fun log(summary: String) {
        if (summary.isBlank()) return
        diagnosticsManager.log("AI", summary)
    }
}
