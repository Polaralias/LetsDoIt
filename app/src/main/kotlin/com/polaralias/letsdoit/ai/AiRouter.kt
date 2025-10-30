package com.polaralias.letsdoit.ai

import com.polaralias.letsdoit.ai.diagnostics.AiDiagnosticsSink
import com.polaralias.letsdoit.ai.model.AiParseResult
import com.polaralias.letsdoit.ai.model.AiSchemaValidator
import com.polaralias.letsdoit.ai.model.SchemaValidationResult
import com.polaralias.letsdoit.ai.model.averageConfidence
import com.polaralias.letsdoit.ai.process.AiMetricsRecorder
import com.polaralias.letsdoit.ai.process.AiCallMetric
import com.polaralias.letsdoit.ai.process.AiParseCache
import com.polaralias.letsdoit.ai.process.AiPreprocessor
import com.polaralias.letsdoit.ai.process.PreprocessResult
import com.polaralias.letsdoit.ai.provider.AiImageProvider
import com.polaralias.letsdoit.ai.provider.AiTextProvider
import com.polaralias.letsdoit.ai.provider.AiProviderException
import com.polaralias.letsdoit.ai.provider.ProviderResponse
import com.polaralias.letsdoit.ai.settings.AiImageProviderId
import com.polaralias.letsdoit.ai.settings.AiSettings
import com.polaralias.letsdoit.ai.settings.AiSettingsRepository
import com.polaralias.letsdoit.ai.settings.AiTextProviderId
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@Singleton
class AiRouter @Inject constructor(
    private val settingsRepository: AiSettingsRepository,
    private val preprocessor: AiPreprocessor,
    private val cache: AiParseCache,
    private val validator: AiSchemaValidator,
    private val moshi: Moshi,
    private val metrics: AiMetricsRecorder,
    private val textProviders: Map<AiTextProviderId, @JvmSuppressWildcards AiTextProvider>,
    private val imageProviders: Map<AiImageProviderId, @JvmSuppressWildcards AiImageProvider>,
    private val errorMapper: AiErrorMapper,
    private val diagnostics: AiDiagnosticsSink
) {
    private val adapter = moshi.adapter(AiParseResult::class.java)
    private val sessionMutex = Mutex()
    private val sessionResults = LinkedHashMap<String, AiParseResult>()

    suspend fun parseTasks(input: AiInput): AiResult<AiParseResult> {
        cache.get(input)?.let { return AiResult.Success(it) }
        sessionMutex.withLock {
            sessionResults[input.transcript]?.let { return AiResult.Success(it) }
        }
        val outcome = runCatching { executeParsing(input) }
        return outcome.fold(
            onSuccess = { result ->
                cache.put(input, result)
                sessionMutex.withLock {
                    sessionResults[input.transcript] = result
                    if (sessionResults.size > 50) {
                        val iterator = sessionResults.iterator()
                        if (iterator.hasNext()) {
                            iterator.next()
                            iterator.remove()
                        }
                    }
                }
                AiResult.Success(result)
            },
            onFailure = { error ->
                val mapping = errorMapper.fromException(error)
                diagnostics.log(mapping.summary)
                AiResult.Failure(mapping.error)
            }
        )
    }

    suspend fun splitSubtasks(title: String, notes: String?): List<String> {
        val settings = settingsRepository.settings.first()
        val provider = textProviders[settings.textProvider] ?: error("Missing text provider")
        val response = provider.splitSubtasks(title, notes)
        return when (response) {
            is ProviderResponse.Success -> decodeList(response.body)
            is ProviderResponse.Failure -> emptyList()
        }
    }

    suspend fun draftPlan(title: String, notes: String?): List<com.polaralias.letsdoit.ai.model.PlannedStep> {
        val settings = settingsRepository.settings.first()
        val provider = textProviders[settings.textProvider] ?: error("Missing text provider")
        val response = provider.draftPlan(title, notes)
        return when (response) {
            is ProviderResponse.Success -> decodePlan(response.body)
            is ProviderResponse.Failure -> emptyList()
        }
    }

    suspend fun generateStickers(prompt: String, variants: Int, size: String): GeneratedAccentPack {
        val settings = settingsRepository.settings.first()
        val providerId = settings.imageProvider
        val provider = imageProviders[providerId] ?: throw IllegalStateException("Image provider unavailable")
        val images = provider.generateStickers(prompt, variants, size)
        return GeneratedAccentPack(images)
    }

    private suspend fun executeParsing(input: AiInput): AiParseResult {
        var attempt = 0
        var escalate = false
        var highReasoning = false
        var escalateReason: String? = null
        var validationRetries = 0
        var lastResult: AiParseResult? = null
        var preprocessData: PreprocessResult? = null
        var lastProviderId: AiTextProviderId? = null
        while (attempt < 3) {
            val settings = settingsRepository.settings.first()
            val providerId = settings.textProvider
            lastProviderId = providerId
            val provider = textProviders[providerId] ?: throw AiProviderException(providerId.name, null, false, "unavailable")
            val model = selectModel(settings, escalate, highReasoning)
            val reasoning = when {
                highReasoning -> "high"
                escalate -> "medium"
                else -> "standard"
            }
            val metadata = buildMap {
                putAll(input.metadata)
                put("reasoning", reasoning)
                escalateReason?.let { put("escalate_reason", it) }
            }
            preprocessData = preprocessor.buildPrompt(input, settings, model, attempt, escalateReason, metadata)
            val timeout = if (escalate || highReasoning || preprocessData.prompt.heuristicEscalate) settings.timeoutEscalated else settings.timeoutMini
            val start = System.nanoTime()
            val response = runCatching {
                withTimeout(timeout) {
                    provider.parse(preprocessData.prompt)
                }
            }.getOrElse {
                escalate = true
                escalateReason = "timeout"
                attempt += 1
                metrics.record(AiCallMetric(providerId.name, model, (System.nanoTime() - start) / 1_000_000, false, escalate))
                continue
            }
            when (response) {
                is ProviderResponse.Failure -> {
                    metrics.record(AiCallMetric(providerId.name, model, (System.nanoTime() - start) / 1_000_000, false, escalate))
                    if (response.retryable && !escalate) {
                        escalate = true
                        escalateReason = response.message
                        attempt += 1
                        continue
                    }
                    throw AiProviderException(providerId.name, response.status, response.retryable, response.message)
                }
                is ProviderResponse.Success -> {
                    val duration = (System.nanoTime() - start) / 1_000_000
                    val parsed = adapter.fromJson(response.body) ?: throw AiProviderException(providerId.name, null, false, "invalid_response")
                    val validation = validator.validate(parsed)
                    metrics.record(AiCallMetric(providerId.name, model, duration, validation is SchemaValidationResult.Valid, escalate))
                    when (validation) {
                        is SchemaValidationResult.Invalid -> {
                            validationRetries += 1
                            if (validationRetries >= 2) {
                                escalate = true
                                escalateReason = "schema_failed"
                            }
                            attempt += 1
                            continue
                        }
                        SchemaValidationResult.Valid -> {
                            val payload = parsed.payload
                            val averageConfidence = payload?.tasks?.let { averageConfidence(it) } ?: 1.0
                            val shouldEscalate = shouldEscalate(parsed, preprocessData, settings, averageConfidence)
                            if (shouldEscalate && !highReasoning) {
                                if (!escalate) {
                                    escalate = true
                                    escalateReason = buildEscalateReason(parsed, preprocessData)
                                    attempt += 1
                                    continue
                                } else if (settings.escalateToHighReasoning) {
                                    highReasoning = true
                                    escalateReason = "high_reasoning"
                                    attempt += 1
                                    continue
                                }
                            }
                            if (parsed.needs_deeper_reasoning && settings.escalateToHighReasoning && !highReasoning) {
                                highReasoning = true
                                escalateReason = "deeper_reasoning"
                                attempt += 1
                                continue
                            }
                            lastResult = parsed
                            break
                        }
                    }
                }
            }
            if (lastResult == null) {
                attempt += 1
            } else {
                break
            }
        }
        val providerName = lastProviderId?.name ?: "unknown"
        return lastResult ?: throw AiProviderException(providerName, null, false, "no_result")
    }

    private fun shouldEscalate(
        result: AiParseResult,
        preprocess: PreprocessResult?,
        settings: AiSettings,
        averageConfidence: Double,
    ): Boolean {
        if (preprocess == null) return false
        if (preprocess.heuristicFlags.isNotEmpty()) return true
        if (!result.can_handle) return true
        if (averageConfidence < settings.confidenceThreshold) return true
        if (result.complexity_score > settings.complexityThreshold) return true
        if (result.payload?.tasks?.isEmpty() == true) return true
        return false
    }

    private fun buildEscalateReason(result: AiParseResult, preprocess: PreprocessResult?): String {
        if (result.needs_deeper_reasoning) return "needs_deeper_reasoning"
        if (!result.can_handle) return "self_assessment"
        if (preprocess == null) return "heuristics"
        if (preprocess.heuristicFlags.isNotEmpty()) return preprocess.heuristicFlags.joinToString(",")
        return "confidence"
    }

    private fun decodeList(body: String): List<String> {
        val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
        return adapter.fromJson(body) ?: emptyList()
    }

    private fun decodePlan(body: String): List<com.polaralias.letsdoit.ai.model.PlannedStep> {
        val type = Types.newParameterizedType(List::class.java, com.polaralias.letsdoit.ai.model.PlannedStep::class.java)
        val adapter = moshi.adapter<List<com.polaralias.letsdoit.ai.model.PlannedStep>>(type)
        return adapter.fromJson(body) ?: emptyList()
    }

    private fun selectModel(settings: AiSettings, escalated: Boolean, highReasoning: Boolean): String {
        return when {
            highReasoning -> when (settings.textProvider) {
                AiTextProviderId.openai -> "gpt-5"
                AiTextProviderId.gemini -> "gemini-1.5-pro"
            }
            escalated -> when (settings.textProvider) {
                AiTextProviderId.openai -> "gpt-5"
                AiTextProviderId.gemini -> "gemini-1.5-pro"
            }
            else -> when (settings.textProvider) {
                AiTextProviderId.openai -> settings.textModelOpenAi
                AiTextProviderId.gemini -> settings.textModelGemini
            }
        }
    }
}

data class GeneratedAccentPack(val images: List<ByteArray>)
