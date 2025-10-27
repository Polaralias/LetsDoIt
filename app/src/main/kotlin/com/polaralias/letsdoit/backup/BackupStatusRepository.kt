package com.polaralias.letsdoit.backup

import kotlinx.coroutines.flow.Flow

interface BackupStatusRepository {
    val status: Flow<BackupStatus>
    suspend fun recordSuccess(timestampMillis: Long)
    suspend fun recordError(error: BackupError, message: String?, timestampMillis: Long)
}
