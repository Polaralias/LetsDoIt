package com.letsdoit.app.ai.speech

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

enum class SpeechEngineId { Local, GoogleCloud, Azure }

data class SpeechSettings(
    val engine: SpeechEngineId = SpeechEngineId.Local,
    val languageTag: String = Locale.getDefault().toLanguageTag(),
    val offlinePreferred: Boolean = false,
    val speakerLabels: Boolean = false
)

@Singleton
class SpeechSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val defaults = SpeechSettings()
    private val engineKey = stringPreferencesKey("speech_engine")
    private val languageKey = stringPreferencesKey("speech_language")
    private val offlineKey = booleanPreferencesKey("speech_offline_preferred")
    private val speakerKey = booleanPreferencesKey("speech_speaker_labels")

    val settings: Flow<SpeechSettings> = dataStore.data.map { preferences ->
        SpeechSettings(
            engine = preferences[engineKey]?.let(::decodeEngine) ?: defaults.engine,
            languageTag = preferences[languageKey] ?: defaults.languageTag,
            offlinePreferred = preferences[offlineKey] ?: defaults.offlinePreferred,
            speakerLabels = preferences[speakerKey] ?: defaults.speakerLabels
        )
    }

    suspend fun updateEngine(engine: SpeechEngineId) {
        dataStore.edit { preferences ->
            preferences[engineKey] = engine.name
        }
    }

    suspend fun updateLanguage(locale: Locale) {
        dataStore.edit { preferences ->
            preferences[languageKey] = locale.toLanguageTag()
        }
    }

    suspend fun updateOfflinePreferred(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[offlineKey] = enabled
        }
    }

    suspend fun updateSpeakerLabels(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[speakerKey] = enabled
        }
    }

    private fun decodeEngine(value: String): SpeechEngineId {
        return runCatching { SpeechEngineId.valueOf(value) }.getOrDefault(defaults.engine)
    }
}
