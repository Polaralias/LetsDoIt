package com.letsdoit.app.backup

import java.time.Instant

enum class BackupError {
    AuthRequired,
    Remote,
    Snapshot,
    Crypto,
    NotFound,
    Unknown
}

sealed interface BackupResult {
    data class Success(val info: BackupInfo) : BackupResult
    data class Failure(val error: BackupError, val message: String? = null) : BackupResult
}

sealed interface RestoreResult {
    data object Success : RestoreResult
    data class Failure(val error: BackupError, val message: String? = null) : RestoreResult
}

data class BackupInfo(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val sizeBytes: Long
)

data class BackupStatus(
    val lastSuccessAt: Instant? = null,
    val lastError: BackupStatusError? = null
)

data class BackupStatusError(
    val error: BackupError,
    val message: String?,
    val at: Instant
)
