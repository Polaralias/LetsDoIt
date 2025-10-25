package com.letsdoit.app.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SyncStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock
) : SyncStatusRepository {
    private val lastFullSyncKey = longPreferencesKey("sync_last_full")
    private val lastResultKey = stringPreferencesKey("sync_last_result")
    private val pushCountKey = longPreferencesKey("sync_total_push")
    private val pullCountKey = longPreferencesKey("sync_total_pull")
    private val conflictsKey = longPreferencesKey("sync_total_conflict")
    private val lastErrorCodeKey = stringPreferencesKey("sync_last_error_code")
    private val lastErrorMessageKey = stringPreferencesKey("sync_last_error_message")
    private val lastErrorAtKey = longPreferencesKey("sync_last_error_at")

    override val status: Flow<SyncStatus> = dataStore.data.map { preferences ->
        val lastFullSync = preferences[lastFullSyncKey]?.let { Instant.ofEpochMilli(it) }
        val badge = preferences[lastResultKey]?.let { value ->
            runCatching { SyncResultBadge.valueOf(value) }.getOrDefault(SyncResultBadge.Success)
        } ?: SyncResultBadge.Success
        val error = preferences[lastErrorCodeKey]?.let { codeValue ->
            val code = runCatching { SyncErrorCode.valueOf(codeValue) }.getOrDefault(SyncErrorCode.Unknown)
            val message = preferences[lastErrorMessageKey]
            val at = preferences[lastErrorAtKey]?.let { Instant.ofEpochMilli(it) }
            if (message != null && at != null) {
                SyncError(code = code, message = message, at = at)
            } else {
                null
            }
        }
        SyncStatus(
            lastFullSync = lastFullSync,
            lastResult = badge,
            totalPushes = preferences[pushCountKey] ?: 0L,
            totalPulls = preferences[pullCountKey] ?: 0L,
            conflictsResolved = preferences[conflictsKey] ?: 0L,
            lastError = error
        )
    }

    override suspend fun record(report: SyncReport, completedAt: Instant) {
        dataStore.edit { preferences ->
            val currentPushes = preferences[pushCountKey] ?: 0L
            val currentPulls = preferences[pullCountKey] ?: 0L
            val currentConflicts = preferences[conflictsKey] ?: 0L
            val summary = report.summary
            preferences[pushCountKey] = currentPushes + summary.pushes
            preferences[pullCountKey] = currentPulls + summary.pulls
            preferences[conflictsKey] = currentConflicts + summary.conflicts
            preferences[lastFullSyncKey] = completedAt.toEpochMilli()
            when (report) {
                is SyncReport.Success -> {
                    preferences[lastResultKey] = SyncResultBadge.Success.name
                    preferences.remove(lastErrorCodeKey)
                    preferences.remove(lastErrorMessageKey)
                    preferences.remove(lastErrorAtKey)
                }
                is SyncReport.RateLimited -> {
                    preferences[lastResultKey] = SyncResultBadge.Warning.name
                    preferences[lastErrorCodeKey] = report.error.code.name
                    preferences[lastErrorMessageKey] = report.error.message
                    preferences[lastErrorAtKey] = report.error.at.toEpochMilli()
                }
                is SyncReport.Failure -> {
                    val badge = if (report.retryable) SyncResultBadge.Warning else SyncResultBadge.Error
                    preferences[lastResultKey] = badge.name
                    preferences[lastErrorCodeKey] = report.error.code.name
                    preferences[lastErrorMessageKey] = report.error.message
                    preferences[lastErrorAtKey] = report.error.at.toEpochMilli()
                }
            }
        }
    }
}
