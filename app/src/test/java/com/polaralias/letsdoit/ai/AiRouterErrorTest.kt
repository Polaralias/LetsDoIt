package com.polaralias.letsdoit.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.polaralias.letsdoit.ai.diagnostics.AiDiagnosticsSink
import com.polaralias.letsdoit.ai.model.AiSchemaValidator
import com.polaralias.letsdoit.ai.process.AiMetricsRecorder
import com.polaralias.letsdoit.ai.process.AiParseCache
import com.polaralias.letsdoit.ai.process.AiPreprocessor
import com.polaralias.letsdoit.ai.prompts.PromptLoader
import com.polaralias.letsdoit.ai.prompts.PromptRepository
import com.polaralias.letsdoit.ai.provider.AiParsePrompt
import com.polaralias.letsdoit.ai.provider.AiTextProvider
import com.polaralias.letsdoit.ai.provider.ProviderResponse
import com.polaralias.letsdoit.ai.settings.AiSettingsRepository
import com.polaralias.letsdoit.ai.settings.AiTextProviderId
import com.polaralias.letsdoit.diagnostics.DiagnosticsRedactor
import com.squareup.moshi.Moshi
import java.io.File
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AiRouterErrorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var settingsRepository: AiSettingsRepository
    private lateinit var promptRepository: PromptRepository
    private val redactor = DiagnosticsRedactor()
    private val errorMapper = AiErrorMapper(redactor)

    @Before
    fun setup() {
        scope = CoroutineScope(Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(folder.root, "settings.preferences_pb")
        }
        settingsRepository = AiSettingsRepository(dataStore)
        promptRepository = PromptRepository(object : PromptLoader {
            override fun load(name: String): String = "Test prompt"
        })
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun routerReturnsNetworkError() = runBlocking {
        val failure = ProviderResponse.Failure(
            message = "Timed out contacting https://api.example.com with key sk-test123",
            retryable = true,
            status = 500
        )
        val (router, diagnostics) = createRouter(failure)
        val result = router.parseTasks(AiInput("Plan the launch", "Project", "UTC"))
        assertTrue(result is AiResult.Failure)
        val error = (result as AiResult.Failure).error
        assertTrue(error is AiError.Network)
        val log = diagnostics.latest()
        assertFalse(log.contains("https://"))
        assertFalse(log.contains("sk-test123"))
    }

    @Test
    fun routerReturnsAuthError() = runBlocking {
        val failure = ProviderResponse.Failure(
            message = "Authorisation failed for key sk-live-secret",
            retryable = false,
            status = 401
        )
        val (router, diagnostics) = createRouter(failure)
        val result = router.parseTasks(AiInput("Draft the memo", null, null))
        assertTrue(result is AiResult.Failure)
        val error = (result as AiResult.Failure).error
        assertTrue(error is AiError.Auth)
        val log = diagnostics.latest()
        assertFalse(log.contains("sk-live-secret"))
    }

    @Test
    fun routerReturnsRateLimitedError() = runBlocking {
        val failure = ProviderResponse.Failure(
            message = """{"prompt":"Summarise the quarterly report"}""",
            retryable = false,
            status = 429
        )
        val (router, diagnostics) = createRouter(failure)
        val result = router.parseTasks(AiInput("Review results", null, null))
        assertTrue(result is AiResult.Failure)
        val error = (result as AiResult.Failure).error
        assertTrue(error is AiError.RateLimited)
        val log = diagnostics.latest()
        assertFalse(log.contains("Summarise the quarterly report"))
        assertTrue(log.contains("[REDACTED_PROMPT]"))
    }

    private fun createRouter(failure: ProviderResponse.Failure): Pair<AiRouter, RecordingDiagnosticsSink> {
        val preprocessor = AiPreprocessor(Clock.systemUTC(), promptRepository)
        val cache = AiParseCache()
        val validator = AiSchemaValidator()
        val metrics = AiMetricsRecorder()
        val provider = object : AiTextProvider {
            override suspend fun parse(input: AiParsePrompt): ProviderResponse = failure
            override suspend fun splitSubtasks(title: String, notes: String?): ProviderResponse = ProviderResponse.Success("[]")
            override suspend fun draftPlan(title: String, notes: String?): ProviderResponse = ProviderResponse.Success("[]")
        }
        val diagnostics = RecordingDiagnosticsSink()
        val router = AiRouter(
            settingsRepository = settingsRepository,
            preprocessor = preprocessor,
            cache = cache,
            validator = validator,
            moshi = Moshi.Builder().build(),
            metrics = metrics,
            textProviders = mapOf(AiTextProviderId.openai to provider),
            imageProviders = emptyMap(),
            errorMapper = errorMapper,
            diagnostics = diagnostics
        )
        return router to diagnostics
    }

    private class RecordingDiagnosticsSink : AiDiagnosticsSink {
        private val last = java.util.concurrent.atomic.AtomicReference("")

        override fun log(summary: String) {
            last.set(summary)
        }

        fun latest(): String = last.get()
    }
}
