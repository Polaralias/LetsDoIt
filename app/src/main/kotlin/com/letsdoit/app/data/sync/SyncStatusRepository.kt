package com.letsdoit.app.data.sync

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface SyncStatusRepository {
    val status: Flow<SyncStatus>
    suspend fun record(report: SyncReport, completedAt: Instant)
}

data class SyncStatus(
    val lastFullSync: Instant?,
    val lastResult: SyncResultBadge,
    val totalPushes: Long,
    val totalPulls: Long,
    val conflictsResolved: Long,
    val lastError: SyncError?,
    val lastRetryAfterSeconds: Long?
)

data class SyncError(
    val code: SyncErrorCode,
    val message: String,
    val at: Instant
)

enum class SyncResultBadge {
    Success,
    Warning,
    Error
}

enum class SyncErrorCode {
    Unauthorised,
    Forbidden,
    NotFound,
    Conflict,
    PreconditionFailed,
    RateLimited,
    Unknown
}

data class SyncSummary(
    val pushes: Int = 0,
    val pulls: Int = 0,
    val conflicts: Int = 0
)

sealed interface SyncReport {
    val summary: SyncSummary

    data class Success(override val summary: SyncSummary) : SyncReport

    data class RateLimited(
        override val summary: SyncSummary,
        val retryAfterSeconds: Long?,
        val error: SyncError
    ) : SyncReport

    data class Failure(
        override val summary: SyncSummary,
        val error: SyncError,
        val retryable: Boolean
    ) : SyncReport
}
