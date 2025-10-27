package com.polaralias.letsdoit.ai.speech

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

enum class SpeechEngineId {
    Local,
    GoogleCloud,
    Azure;

    fun isCloud(): Boolean = this != Local
}

data class SpeechSettings(
    val engine: SpeechEngineId = SpeechEngineId.Local,
    val languageTag: String = Locale.getDefault().toLanguageTag(),
    val offlinePreferred: Boolean = false,
    val speakerLabels: Boolean = false,
    val cloudConsent: Boolean = false
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
    private val consentKey = booleanPreferencesKey("speech_cloud_consent")

    val settings: Flow<SpeechSettings> = dataStore.data.map { preferences ->
        SpeechSettings(
            engine = preferences[engineKey]?.let(::decodeEngine)?.let { engine ->
                val consent = preferences[consentKey] ?: defaults.cloudConsent
                if (engine.isCloud() && !consent) SpeechEngineId.Local else engine
            } ?: defaults.engine,
            languageTag = preferences[languageKey] ?: defaults.languageTag,
            offlinePreferred = preferences[offlineKey] ?: defaults.offlinePreferred,
            speakerLabels = preferences[speakerKey] ?: defaults.speakerLabels,
            cloudConsent = preferences[consentKey] ?: defaults.cloudConsent
        )
    }

    suspend fun updateEngine(engine: SpeechEngineId) {
        dataStore.edit { preferences ->
            val consent = preferences[consentKey] ?: defaults.cloudConsent
            preferences[engineKey] = if (engine.isCloud() && !consent) {
                SpeechEngineId.Local.name
            } else {
                engine.name
            }
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

    suspend fun updateCloudConsent(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[consentKey] = enabled
            if (!enabled) {
                preferences[engineKey] = SpeechEngineId.Local.name
            }
        }
    }

    private fun decodeEngine(value: String): SpeechEngineId {
        return runCatching { SpeechEngineId.valueOf(value) }.getOrDefault(defaults.engine)
    }
}
