package com.letsdoit.app.backup

interface BackupManager {
    suspend fun backupNow(): BackupResult
    suspend fun restoreLatest(): RestoreResult
    suspend fun listBackups(): List<BackupInfo>
}
