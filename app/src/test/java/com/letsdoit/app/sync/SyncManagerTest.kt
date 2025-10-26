package com.letsdoit.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskSyncMetaDao
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.sync.SyncErrorCode
import com.letsdoit.app.data.sync.SyncReport
import com.letsdoit.app.data.sync.SyncResultBadge
import com.letsdoit.app.data.sync.SyncStatus
import com.letsdoit.app.data.sync.SyncStatusRepository
import com.letsdoit.app.data.sync.TaskSyncMeta
import com.letsdoit.app.data.sync.TaskSyncStateManager
import com.letsdoit.app.network.ClickUpService
import com.letsdoit.app.network.ClickUpTaskDto
import com.letsdoit.app.network.ClickUpTaskUpdate
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class SyncManagerTest {
    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskSyncMetaDao: TaskSyncMetaDao
    private lateinit var syncStateManager: TaskSyncStateManager
    private lateinit var statusRepository: FakeStatusRepository
    private lateinit var service: FakeClickUpService
    private lateinit var manager: SyncManager
    private val clock: Clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))

    @BeforeTest
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        taskSyncMetaDao = database.taskSyncMetaDao()
        syncStateManager = TaskSyncStateManager(taskSyncMetaDao)
        statusRepository = FakeStatusRepository()
        service = FakeClickUpService()
        manager = SyncManager(taskDao, syncStateManager, service, statusRepository, clock)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun etagCapturedAndUsedForConditionalPut() = runTest {
        val taskId = seedTask()
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-1",
            etag = null,
            remoteUpdatedAt = null,
            needsPush = false,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        service.enqueueGet(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-1",
                    name = "Remote title",
                    text_content = "Remote notes",
                    due_date = null,
                    date_updated = Instant.parse("2024-01-01T00:10:00Z").toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-1")
            )
        )

        manager.runFullSync()

        val pulledMeta = syncStateManager.find(taskId)
        assertEquals("etag-1", pulledMeta?.etag)
        assertEquals(1L, statusRepository.status.value.totalPulls)

        syncStateManager.markNeedsPush(taskId)
        service.enqueueUpdate(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-1",
                    name = "Remote updated",
                    text_content = "Updated notes",
                    due_date = null,
                    date_updated = Instant.parse("2024-01-01T00:20:00Z").toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-2")
            )
        )

        manager.runFullSync()

        assertEquals("etag-1", service.lastIfMatch)
        val pushedMeta = syncStateManager.find(taskId)
        assertEquals("etag-2", pushedMeta?.etag)
        assertEquals(1L, statusRepository.status.value.totalPushes)
    }

    @Test
    fun resolvesPreconditionFailureByRefreshing() = runTest {
        val taskId = seedTask()
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-2",
            etag = "etag-old",
            remoteUpdatedAt = null,
            needsPush = true,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        service.enqueueUpdate(errorResponse(412, "precondition"))
        service.enqueueGet(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-2",
                    name = "Server source",
                    text_content = null,
                    due_date = null,
                    date_updated = Instant.parse("2024-01-01T00:30:00Z").toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-new")
            )
        )

        manager.runFullSync()

        assertEquals("etag-old", service.lastIfMatch)
        val resolvedMeta = syncStateManager.find(taskId)
        assertEquals("etag-new", resolvedMeta?.etag)
        assertEquals(1L, statusRepository.status.value.conflictsResolved)
        val refreshed = taskDao.getById(taskId)
        assertEquals("Server source", refreshed?.title)
    }

    @Test
    fun handlesRateLimitWithRetryAfter() = runTest {
        val taskId = seedTask()
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-3",
            etag = null,
            remoteUpdatedAt = null,
            needsPush = true,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        service.enqueueUpdate(errorResponse(429, "limited", Headers.headersOf("Retry-After", "30")))

        val report = manager.runFullSync()

        val rateLimited = assertIs<SyncReport.RateLimited>(report)
        assertEquals(30L, rateLimited.retryAfterSeconds)
        val status = statusRepository.status.value
        assertEquals(SyncResultBadge.Warning, status.lastResult)
        assertEquals(SyncErrorCode.RateLimited, status.lastError?.code)
        assertEquals(30L, status.lastRetryAfterSeconds)
    }

    @Test
    fun rateLimitWithoutRetryAfterUsesBackoff() = runTest {
        val taskId = seedTask()
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-4",
            etag = null,
            remoteUpdatedAt = null,
            needsPush = true,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        service.enqueueUpdate(errorResponse(429, "limited"))

        val report = manager.runFullSync()

        val rateLimited = assertIs<SyncReport.RateLimited>(report)
        assertEquals(null, rateLimited.retryAfterSeconds)
        assertEquals("Rate limited", rateLimited.error.message)
        val status = statusRepository.status.value
        assertEquals(null, status.lastRetryAfterSeconds)
    }

    @Test
    fun remoteWinsWhenMoreThanTwoMinutesAhead() = runTest {
        val taskId = seedTask()
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-remote",
            etag = null,
            remoteUpdatedAt = null,
            needsPush = false,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        val remoteUpdatedAt = Instant.parse("2024-01-01T00:02:01Z")
        service.enqueueGet(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-remote",
                    name = "Server truth",
                    text_content = null,
                    due_date = null,
                    date_updated = remoteUpdatedAt.toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-remote")
            )
        )

        manager.runFullSync()

        val updated = taskDao.getById(taskId)
        assertEquals("Server truth", updated?.title)
        val savedMeta = syncStateManager.find(taskId)
        assertEquals(remoteUpdatedAt, savedMeta?.remoteUpdatedAt)
    }

    @Test
    fun localWinsWhenMoreThanTwoMinutesAhead() = runTest {
        val taskId = seedTask()
        val existing = taskDao.getById(taskId)!!
        val localUpdatedAt = Instant.parse("2024-01-01T00:05:00Z")
        taskDao.upsert(existing.copy(title = "Local revision", updatedAt = localUpdatedAt))
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-local",
            etag = null,
            remoteUpdatedAt = null,
            needsPush = false,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        val remoteUpdatedAt = Instant.parse("2024-01-01T00:02:30Z")
        service.enqueueGet(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-local",
                    name = "Remote stale",
                    text_content = null,
                    due_date = null,
                    date_updated = remoteUpdatedAt.toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-local")
            )
        )
        val pushedUpdatedAt = Instant.parse("2024-01-01T00:05:30Z")
        service.enqueueUpdate(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-local",
                    name = "Local revision",
                    text_content = null,
                    due_date = null,
                    date_updated = pushedUpdatedAt.toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-local-new")
            )
        )

        manager.runFullSync()

        assertEquals("etag-local", service.lastIfMatch)
        val finalTask = taskDao.getById(taskId)
        assertEquals("Local revision", finalTask?.title)
        val savedMeta = syncStateManager.find(taskId)
        assertEquals(pushedUpdatedAt, savedMeta?.remoteUpdatedAt)
        assertEquals("etag-local-new", savedMeta?.etag)
    }

    @Test
    fun serverWinsWhenWithinTwoMinutes() = runTest {
        val taskId = seedTask()
        val existing = taskDao.getById(taskId)!!
        val localUpdatedAt = Instant.parse("2024-01-01T00:05:00Z")
        taskDao.upsert(existing.copy(title = "Local tweak", updatedAt = localUpdatedAt))
        val meta = TaskSyncMeta(
            taskId = taskId,
            remoteId = "remote-tie",
            etag = null,
            remoteUpdatedAt = null,
            needsPush = false,
            lastSyncedAt = null,
            lastPulledAt = null,
            lastPushedAt = null
        )
        syncStateManager.save(meta)
        val remoteUpdatedAt = Instant.parse("2024-01-01T00:04:00Z")
        service.enqueueGet(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-tie",
                    name = "Server tie",
                    text_content = null,
                    due_date = null,
                    date_updated = remoteUpdatedAt.toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-tie")
            )
        )

        manager.runFullSync()

        val updated = taskDao.getById(taskId)
        assertEquals("Server tie", updated?.title)
        val savedMeta = syncStateManager.find(taskId)
        assertEquals(remoteUpdatedAt, savedMeta?.remoteUpdatedAt)
    }

    private suspend fun seedTask(): Long {
        val spaceDao = database.spaceDao()
        val listDao = database.listDao()
        spaceDao.upsert(SpaceEntity(name = "Space"))
        val spaceId = spaceDao.listAll().first().id
        listDao.upsert(ListEntity(spaceId = spaceId, name = "List"))
        val listId = listDao.listAll().first().id
        val now = Instant.parse("2024-01-01T00:00:00Z")
        return taskDao.upsert(
            TaskEntity(
                listId = listId,
                title = "Local",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private class FakeClickUpService : ClickUpService {
        private val getQueue = ArrayDeque<Response<ClickUpTaskDto>>()
        private val updateQueue = ArrayDeque<Response<ClickUpTaskDto>>()
        var lastIfMatch: String? = null

        fun enqueueGet(response: Response<ClickUpTaskDto>) {
            getQueue.addLast(response)
        }

        fun enqueueUpdate(response: Response<ClickUpTaskDto>) {
            updateQueue.addLast(response)
        }

        override suspend fun getTeams(): Response<Unit> = Response.success(Unit)

        override suspend fun getTasks(listId: String): Response<Unit> = Response.success(Unit)

        override suspend fun getTask(taskId: String): Response<ClickUpTaskDto> {
            return if (getQueue.isNotEmpty()) {
                getQueue.removeFirst()
            } else {
                throw IllegalStateException("No queued GET response")
            }
        }

        override suspend fun updateTask(
            taskId: String,
            payload: ClickUpTaskUpdate,
            etag: String?
        ): Response<ClickUpTaskDto> {
            lastIfMatch = etag
            return if (updateQueue.isNotEmpty()) {
                updateQueue.removeFirst()
            } else {
                throw IllegalStateException("No queued update response")
            }
        }
    }

    private class FakeStatusRepository : SyncStatusRepository {
        private val _status = MutableStateFlow(
            SyncStatus(
                lastFullSync = null,
                lastResult = SyncResultBadge.Success,
                totalPushes = 0,
                totalPulls = 0,
                conflictsResolved = 0,
                lastError = null,
                lastRetryAfterSeconds = null
            )
        )
        override val status: Flow<SyncStatus> = _status

        override suspend fun record(report: SyncReport, completedAt: Instant) {
            _status.update { current ->
                val summary = report.summary
                val resultBadge = when (report) {
                    is SyncReport.Success -> SyncResultBadge.Success
                    is SyncReport.RateLimited -> SyncResultBadge.Warning
                    is SyncReport.Failure -> if (report.retryable) SyncResultBadge.Warning else SyncResultBadge.Error
                }
                val error = when (report) {
                    is SyncReport.Success -> null
                    is SyncReport.RateLimited -> report.error
                    is SyncReport.Failure -> report.error
                }
                current.copy(
                    lastFullSync = completedAt,
                    lastResult = resultBadge,
                    totalPushes = current.totalPushes + summary.pushes,
                    totalPulls = current.totalPulls + summary.pulls,
                    conflictsResolved = current.conflictsResolved + summary.conflicts,
                    lastError = error,
                    lastRetryAfterSeconds = when (report) {
                        is SyncReport.RateLimited -> report.retryAfterSeconds
                        else -> null
                    }
                )
            }
        }
    }

    private fun errorResponse(
        code: Int,
        message: String,
        headers: Headers = Headers.headersOf()
    ): Response<ClickUpTaskDto> {
        val body = message.toResponseBody("text/plain".toMediaTypeOrNull())
        val raw = okhttp3.Response.Builder()
            .code(code)
            .protocol(Protocol.HTTP_1_1)
            .message(message)
            .request(Request.Builder().url("https://example.com").build())
            .headers(headers)
            .build()
        return Response.error(body, raw)
    }
}
