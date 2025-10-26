package com.letsdoit.app.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsRedactorTest {
    private val redactor = DiagnosticsRedactor()

    @Test
    fun redactsOpenAiKeys() {
        val input = "Error using key sk-testValue123456789"
        val output = redactor.redact(input)
        assertFalse(output.contains("sk-testValue123456789"))
        assertTrue(output.contains("sk-[REDACTED]"))
    }

    @Test
    fun redactsAssignedApiKeys() {
        val input = "api_key=ABC12345SECRET"
        val output = redactor.redact(input)
        assertTrue(output.contains("api_key=[REDACTED]"))
        assertFalse(output.contains("ABC12345SECRET"))
    }

    @Test
    fun redactsClickUpTokens() {
        val input = "clickup_token: pk_live_abcdef"
        val output = redactor.redact(input)
        assertTrue(output.contains("clickup_token=[REDACTED]"))
        assertFalse(output.contains("pk_live_abcdef"))
    }

    @Test
    fun redactsJsonBodies() {
        val input = """{"error":{"apiKey":"sk-example","prompt":"Collect the receipts","detailUrl":"https://secrets.example.com"}}"""
        val output = redactor.redact(input)
        assertFalse(output.contains("sk-example"))
        assertFalse(output.contains("Collect the receipts"))
        assertFalse(output.contains("https://"))
        assertTrue(output.contains("[REDACTED]"))
        assertTrue(output.contains("[REDACTED_PROMPT]"))
    }
}
