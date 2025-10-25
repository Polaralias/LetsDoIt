package com.letsdoit.app.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultBackupStatusRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : BackupStatusRepository {
    private val lastSuccessKey = longPreferencesKey("backup_last_success")
    private val lastErrorCodeKey = stringPreferencesKey("backup_last_error_code")
    private val lastErrorMessageKey = stringPreferencesKey("backup_last_error_message")
    private val lastErrorAtKey = longPreferencesKey("backup_last_error_at")

    override val status: Flow<BackupStatus> = dataStore.data.map { preferences ->
        val lastSuccess = preferences[lastSuccessKey]?.let { Instant.ofEpochMilli(it) }
        val errorCode = preferences[lastErrorCodeKey]
        val error = if (errorCode != null) {
            val error = runCatching { BackupError.valueOf(errorCode) }.getOrDefault(BackupError.Unknown)
            val message = preferences[lastErrorMessageKey]
            val at = preferences[lastErrorAtKey]?.let { Instant.ofEpochMilli(it) }
            if (at != null) {
                BackupStatusError(error = error, message = message, at = at)
            } else {
                null
            }
        } else {
            null
        }
        BackupStatus(lastSuccessAt = lastSuccess, lastError = error)
    }

    override suspend fun recordSuccess(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[lastSuccessKey] = timestampMillis
            preferences.remove(lastErrorCodeKey)
            preferences.remove(lastErrorMessageKey)
            preferences.remove(lastErrorAtKey)
        }
    }

    override suspend fun recordError(error: BackupError, message: String?, timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[lastErrorCodeKey] = error.name
            if (message == null) {
                preferences.remove(lastErrorMessageKey)
            } else {
                preferences[lastErrorMessageKey] = message
            }
            preferences[lastErrorAtKey] = timestampMillis
        }
    }
}
