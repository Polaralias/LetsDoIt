package com.polaralias.letsdoit.sync

import com.polaralias.letsdoit.data.db.dao.TaskDao
import com.polaralias.letsdoit.data.db.entities.TaskEntity
import com.polaralias.letsdoit.data.sync.SyncError
import com.polaralias.letsdoit.data.sync.SyncErrorCode
import com.polaralias.letsdoit.data.sync.SyncReport
import com.polaralias.letsdoit.data.sync.SyncStatusRepository
import com.polaralias.letsdoit.data.sync.SyncSummary
import com.polaralias.letsdoit.data.sync.TaskSyncMeta
import com.polaralias.letsdoit.data.sync.TaskSyncStateManager
import com.polaralias.letsdoit.network.ClickUpService
import com.polaralias.letsdoit.network.ClickUpTaskDto
import com.polaralias.letsdoit.network.ClickUpTaskUpdate
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import retrofit2.Response

@Singleton
class SyncManager @Inject constructor(
    private val taskDao: TaskDao,
    private val syncStateManager: TaskSyncStateManager,
    private val clickUpService: ClickUpService,
    private val statusRepository: SyncStatusRepository,
    private val clock: Clock
) {
    suspend fun runFullSync(): SyncReport {
        return withContext(Dispatchers.IO) {
            var pushes = 0
            var pulls = 0
            var conflicts = 0
            val metas = syncStateManager.list()
            for (meta in metas) {
                val remoteId = meta.remoteId ?: continue
                if (meta.needsPush) {
                    continue
                }
                when (val result = pullTask(meta, remoteId)) {
                    is OperationResult.Completed -> pulls += result.pulls
                    is OperationResult.RateLimited -> return@withContext finishRateLimited(pushes, pulls, conflicts, result)
                    is OperationResult.Failed -> return@withContext finishFailure(pushes, pulls, conflicts, result)
                }
            }
            val pending = syncStateManager.listPendingPushes()
            for (meta in pending) {
                val remoteId = meta.remoteId ?: continue
                val task = taskDao.getById(meta.taskId) ?: continue
                when (val result = pushTask(task, meta, remoteId)) {
                    is OperationResult.Completed -> {
                        pushes += result.pushes
                        conflicts += result.conflicts
                    }
                    is OperationResult.RateLimited -> return@withContext finishRateLimited(pushes, pulls, conflicts, result)
                    is OperationResult.Failed -> return@withContext finishFailure(pushes, pulls, conflicts, result)
                }
            }
            val summary = SyncSummary(pushes = pushes, pulls = pulls, conflicts = conflicts)
            val report = SyncReport.Success(summary)
            statusRepository.record(report, clock.instant())
            report
        }
    }

    private suspend fun pullTask(meta: TaskSyncMeta, remoteId: String): OperationResult {
        val local = taskDao.getById(meta.taskId) ?: return OperationResult.Completed()
        val response = clickUpService.getTask(remoteId)
        if (response.isSuccessful) {
            val body = response.body()
            val etag = response.headers()[HEADER_ETAG]
            if (body != null) {
                return reconcile(local, meta, remoteId, body, etag)
            }
            syncStateManager.markPulled(meta.taskId, etag, meta.remoteUpdatedAt, clock.instant())
            return OperationResult.Completed(pulls = 1)
        }
        return handleErrorResponse(response)
    }

    private suspend fun pushTask(task: TaskEntity, meta: TaskSyncMeta, remoteId: String): OperationResult {
        val payload = buildUpdatePayload(task)
        val response = clickUpService.updateTask(remoteId, payload, meta.etag)
        if (response.isSuccessful) {
            val body = response.body()
            val etag = response.headers()[HEADER_ETAG] ?: meta.etag
            val remoteUpdatedAt = body?.date_updated?.let { Instant.ofEpochMilli(it) } ?: meta.remoteUpdatedAt
            syncStateManager.markPushed(meta.taskId, etag, remoteUpdatedAt, clock.instant())
            if (body != null) {
                applyRemoteTask(meta.taskId, body)
            }
            return OperationResult.Completed(pushes = 1)
        }
        if (response.code() == HTTP_PRECONDITION_FAILED) {
            return resolvePrecondition(meta, remoteId)
        }
        return handleErrorResponse(response)
    }

    private suspend fun resolvePrecondition(meta: TaskSyncMeta, remoteId: String): OperationResult {
        val refetch = clickUpService.getTask(remoteId)
        if (refetch.isSuccessful) {
            val body = refetch.body()
            val etag = refetch.headers()[HEADER_ETAG]
            if (body != null) {
                applyRemoteTask(meta.taskId, body)
            }
            val remoteUpdatedAt = body?.date_updated?.let { Instant.ofEpochMilli(it) } ?: meta.remoteUpdatedAt
            syncStateManager.markPulled(meta.taskId, etag, remoteUpdatedAt, clock.instant())
            return OperationResult.Completed(conflicts = 1)
        }
        return handleErrorResponse(refetch)
    }

    private suspend fun reconcile(
        local: TaskEntity,
        meta: TaskSyncMeta,
        remoteId: String,
        remote: ClickUpTaskDto,
        etag: String?
    ): OperationResult {
        val localUpdatedAt = local.updatedAt
        val remoteUpdatedAt = Instant.ofEpochMilli(remote.date_updated)
        val remoteIsFresher = remoteUpdatedAt.isAfter(localUpdatedAt.plusSeconds(FRESHNESS_TOLERANCE_SECONDS))
        val localIsFresher = localUpdatedAt.isAfter(remoteUpdatedAt.plusSeconds(FRESHNESS_TOLERANCE_SECONDS))
        return when {
            remoteIsFresher -> {
                applyRemoteTask(meta.taskId, remote)
                syncStateManager.markPulled(meta.taskId, etag, remoteUpdatedAt, clock.instant())
                OperationResult.Completed(pulls = 1)
            }
            localIsFresher -> {
                val ifMatch = etag ?: meta.etag
                val response = clickUpService.updateTask(remoteId, buildUpdatePayload(local), ifMatch)
                if (response.isSuccessful) {
                    val body = response.body()
                    val newEtag = response.headers()[HEADER_ETAG] ?: ifMatch
                    val responseUpdatedAt = body?.date_updated?.let { Instant.ofEpochMilli(it) } ?: remoteUpdatedAt
                    syncStateManager.markPushed(meta.taskId, newEtag, responseUpdatedAt, clock.instant())
                    if (body != null) {
                        applyRemoteTask(meta.taskId, body)
                    }
                    OperationResult.Completed(pushes = 1)
                } else if (response.code() == HTTP_PRECONDITION_FAILED) {
                    resolvePrecondition(meta, remoteId)
                } else {
                    handleErrorResponse(response)
                }
            }
            else -> {
                applyRemoteTask(meta.taskId, remote)
                syncStateManager.markPulled(meta.taskId, etag, remoteUpdatedAt, clock.instant())
                OperationResult.Completed(pulls = 1)
            }
        }
    }

    private suspend fun applyRemoteTask(taskId: Long, remote: ClickUpTaskDto) {
        val current = taskDao.getById(taskId) ?: return
        val dueAt = remote.due_date?.let { Instant.ofEpochMilli(it) }
        val updatedAt = Instant.ofEpochMilli(remote.date_updated)
        val completed = remote.status?.status?.equals("complete", ignoreCase = true) ?: current.completed
        val updated = current.copy(
            title = remote.name,
            notes = remote.text_content,
            dueAt = dueAt,
            updatedAt = updatedAt,
            completed = completed
        )
        taskDao.upsert(updated)
    }

    private fun buildUpdatePayload(task: TaskEntity): ClickUpTaskUpdate {
        return ClickUpTaskUpdate(
            name = task.title,
            text_content = task.notes,
            due_date = task.dueAt?.toEpochMilli()
        )
    }

    private fun handleErrorResponse(response: Response<*>): OperationResult {
        val code = response.code()
        if (code == HTTP_RATE_LIMIT) {
            val retrySeconds = parseRetryAfter(response.headers())
            val error = SyncError(
                code = SyncErrorCode.RateLimited,
                message = retrySeconds?.let { "Rate limited for $it seconds" } ?: "Rate limited",
                at = clock.instant()
            )
            return OperationResult.RateLimited(retrySeconds?.let { max(1L, it) }, error)
        }
        val retryable = code >= 500
        val error = SyncError(
            code = mapErrorCode(code),
            message = response.message().ifBlank { "HTTP $code" },
            at = clock.instant()
        )
        return OperationResult.Failed(error, retryable)
    }

    private fun parseRetryAfter(headers: Headers): Long? {
        val raw = headers[HEADER_RETRY_AFTER] ?: return null
        return raw.toLongOrNull()
    }

    private fun mapErrorCode(code: Int): SyncErrorCode {
        return when (code) {
            401 -> SyncErrorCode.Unauthorised
            403 -> SyncErrorCode.Forbidden
            404 -> SyncErrorCode.NotFound
            409 -> SyncErrorCode.Conflict
            HTTP_PRECONDITION_FAILED -> SyncErrorCode.PreconditionFailed
            HTTP_RATE_LIMIT -> SyncErrorCode.RateLimited
            else -> SyncErrorCode.Unknown
        }
    }

    private suspend fun finishRateLimited(
        pushes: Int,
        pulls: Int,
        conflicts: Int,
        result: OperationResult.RateLimited
    ): SyncReport {
        val summary = SyncSummary(pushes = pushes, pulls = pulls, conflicts = conflicts)
        val report = SyncReport.RateLimited(summary, result.retryAfterSeconds, result.error)
        statusRepository.record(report, clock.instant())
        return report
    }

    private suspend fun finishFailure(
        pushes: Int,
        pulls: Int,
        conflicts: Int,
        result: OperationResult.Failed
    ): SyncReport {
        val summary = SyncSummary(pushes = pushes, pulls = pulls, conflicts = conflicts)
        val report = SyncReport.Failure(summary, result.error, result.retryable)
        statusRepository.record(report, clock.instant())
        return report
    }

    private sealed interface OperationResult {
        data class Completed(
            val pushes: Int = 0,
            val pulls: Int = 0,
            val conflicts: Int = 0
        ) : OperationResult

        data class RateLimited(val retryAfterSeconds: Long?, val error: SyncError) : OperationResult

        data class Failed(val error: SyncError, val retryable: Boolean) : OperationResult
    }

    companion object {
        private const val HEADER_ETAG = "ETag"
        private const val HEADER_RETRY_AFTER = "Retry-After"
        private const val HTTP_PRECONDITION_FAILED = 412
        private const val HTTP_RATE_LIMIT = 429
        private const val DEFAULT_RETRY_AFTER = 60L
        private const val FRESHNESS_TOLERANCE_SECONDS = 120L
    }
}
