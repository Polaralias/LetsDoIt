package com.polaralias.letsdoit.ai.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AiTextProviderId { openai, gemini }

enum class AiImageProviderId { openai, gemini }

data class AiSettings(
    val textProvider: AiTextProviderId = AiTextProviderId.openai,
    val textModelOpenAi: String = "gpt-5-mini",
    val textModelGemini: String = "gemini-1.5-flash",
    val imageProvider: AiImageProviderId = AiImageProviderId.openai,
    val imageModelOpenAi: String = "gpt-image-1",
    val imageModelGemini: String = "gemini-images",
    val miniComfortWindow: Int = 4000,
    val confidenceThreshold: Double = 0.75,
    val complexityThreshold: Double = 0.4,
    val timeoutMini: Long = 12_000,
    val timeoutEscalated: Long = 25_000,
    val escalateToHighReasoning: Boolean = true
)

@Singleton
class AiSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val defaults = AiSettings()
    private val textProviderKey = stringPreferencesKey("ai_text_provider")
    private val textModelOpenAiKey = stringPreferencesKey("ai_text_model_openai")
    private val textModelGeminiKey = stringPreferencesKey("ai_text_model_gemini")
    private val imageProviderKey = stringPreferencesKey("ai_image_provider")
    private val imageModelOpenAiKey = stringPreferencesKey("ai_image_model_openai")
    private val imageModelGeminiKey = stringPreferencesKey("ai_image_model_gemini")
    private val miniComfortKey = intPreferencesKey("ai_mini_comfort_window")
    private val confidenceKey = stringPreferencesKey("ai_confidence_threshold")
    private val complexityKey = stringPreferencesKey("ai_complexity_threshold")
    private val timeoutMiniKey = longPreferencesKey("ai_timeout_mini")
    private val timeoutEscalatedKey = longPreferencesKey("ai_timeout_escalated")
    private val escalateHighKey = booleanPreferencesKey("ai_escalate_high_reasoning")

    val settings: Flow<AiSettings> = dataStore.data.map { preferences ->
        AiSettings(
            textProvider = preferences[textProviderKey]?.let(::decodeTextProvider) ?: defaults.textProvider,
            textModelOpenAi = preferences[textModelOpenAiKey] ?: defaults.textModelOpenAi,
            textModelGemini = preferences[textModelGeminiKey] ?: defaults.textModelGemini,
            imageProvider = preferences[imageProviderKey]?.let(::decodeImageProvider) ?: defaults.imageProvider,
            imageModelOpenAi = preferences[imageModelOpenAiKey] ?: defaults.imageModelOpenAi,
            imageModelGemini = preferences[imageModelGeminiKey] ?: defaults.imageModelGemini,
            miniComfortWindow = preferences[miniComfortKey] ?: defaults.miniComfortWindow,
            confidenceThreshold = preferences[confidenceKey]?.toDoubleOrNull() ?: defaults.confidenceThreshold,
            complexityThreshold = preferences[complexityKey]?.toDoubleOrNull() ?: defaults.complexityThreshold,
            timeoutMini = preferences[timeoutMiniKey] ?: defaults.timeoutMini,
            timeoutEscalated = preferences[timeoutEscalatedKey] ?: defaults.timeoutEscalated,
            escalateToHighReasoning = preferences[escalateHighKey] ?: defaults.escalateToHighReasoning
        )
    }

    suspend fun updateTextProvider(provider: AiTextProviderId) {
        dataStore.edit { preferences ->
            preferences[textProviderKey] = provider.name
        }
    }

    suspend fun updateTextModelOpenAi(model: String) {
        dataStore.edit { preferences ->
            preferences[textModelOpenAiKey] = model
        }
    }

    suspend fun updateTextModelGemini(model: String) {
        dataStore.edit { preferences ->
            preferences[textModelGeminiKey] = model
        }
    }

    suspend fun updateImageProvider(provider: AiImageProviderId) {
        dataStore.edit { preferences ->
            preferences[imageProviderKey] = provider.name
        }
    }

    suspend fun updateImageModelOpenAi(model: String) {
        dataStore.edit { preferences ->
            preferences[imageModelOpenAiKey] = model
        }
    }

    suspend fun updateImageModelGemini(model: String) {
        dataStore.edit { preferences ->
            preferences[imageModelGeminiKey] = model
        }
    }

    suspend fun updateMiniComfortWindow(value: Int) {
        dataStore.edit { preferences ->
            preferences[miniComfortKey] = value
        }
    }

    suspend fun updateConfidenceThreshold(value: Double) {
        dataStore.edit { preferences ->
            preferences[confidenceKey] = value.toString()
        }
    }

    suspend fun updateComplexityThreshold(value: Double) {
        dataStore.edit { preferences ->
            preferences[complexityKey] = value.toString()
        }
    }

    suspend fun updateTimeoutMini(value: Long) {
        dataStore.edit { preferences ->
            preferences[timeoutMiniKey] = value
        }
    }

    suspend fun updateTimeoutEscalated(value: Long) {
        dataStore.edit { preferences ->
            preferences[timeoutEscalatedKey] = value
        }
    }

    suspend fun updateEscalateToHighReasoning(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[escalateHighKey] = enabled
        }
    }

    private fun decodeTextProvider(value: String): AiTextProviderId {
        return runCatching { AiTextProviderId.valueOf(value) }.getOrDefault(defaults.textProvider)
    }

    private fun decodeImageProvider(value: String): AiImageProviderId {
        return runCatching { AiImageProviderId.valueOf(value) }.getOrDefault(defaults.imageProvider)
    }
}
