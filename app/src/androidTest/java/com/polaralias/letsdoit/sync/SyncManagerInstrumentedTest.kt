package com.polaralias.letsdoit.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import com.polaralias.letsdoit.data.db.entities.TaskEntity
import com.polaralias.letsdoit.data.sync.SyncReport
import com.polaralias.letsdoit.data.sync.SyncResultBadge
import com.polaralias.letsdoit.data.sync.SyncStatus
import com.polaralias.letsdoit.data.sync.SyncStatusRepository
import com.polaralias.letsdoit.data.sync.TaskSyncMeta
import com.polaralias.letsdoit.data.sync.TaskSyncStateManager
import com.polaralias.letsdoit.network.ClickUpService
import com.polaralias.letsdoit.network.ClickUpTaskDto
import com.polaralias.letsdoit.network.ClickUpTaskUpdate
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class SyncManagerInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var manager: SyncManager
    private lateinit var stateManager: TaskSyncStateManager
    private lateinit var service: InstrumentedFakeClickUpService
    private lateinit var statusRepository: InstrumentedFakeStatusRepository
    private val clock: Clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))

    @BeforeTest
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        stateManager = TaskSyncStateManager(database.taskSyncMetaDao())
        service = InstrumentedFakeClickUpService()
        statusRepository = InstrumentedFakeStatusRepository()
        manager = SyncManager(database.taskDao(), stateManager, service, statusRepository, clock)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun serverVersionWinsAfterConflict() = runTest {
        val taskId = seedTask()
        stateManager.save(
            TaskSyncMeta(
                taskId = taskId,
                remoteId = "remote-conflict",
                etag = "etag-client",
                remoteUpdatedAt = null,
                needsPush = true,
                lastSyncedAt = null,
                lastPulledAt = null,
                lastPushedAt = null
            )
        )
        service.enqueueUpdate(errorResponse(412, "conflict"))
        service.enqueueGet(
            Response.success(
                ClickUpTaskDto(
                    id = "remote-conflict",
                    name = "Server wins",
                    text_content = null,
                    due_date = null,
                    date_updated = Instant.parse("2024-01-01T00:05:00Z").toEpochMilli(),
                    status = null
                ),
                Headers.headersOf("ETag", "etag-server")
            )
        )

        manager.runFullSync()

        val task = database.taskDao().getById(taskId)
        assertEquals("Server wins", task?.title)
        assertEquals(1L, statusRepository.status.value.conflictsResolved)
        assertEquals(SyncResultBadge.Success, statusRepository.status.value.lastResult)
    }

    private suspend fun seedTask(): Long {
        val spaceDao = database.spaceDao()
        val listDao = database.listDao()
        spaceDao.upsert(SpaceEntity(name = "Space"))
        val spaceId = spaceDao.listAll().first().id
        listDao.upsert(ListEntity(spaceId = spaceId, name = "List"))
        val listId = listDao.listAll().first().id
        val now = Instant.parse("2024-01-01T00:00:00Z")
        return database.taskDao().upsert(
            TaskEntity(
                listId = listId,
                title = "Local",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun errorResponse(code: Int, message: String, headers: Headers = Headers.headersOf()): Response<ClickUpTaskDto> {
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

    private class InstrumentedFakeClickUpService : ClickUpService {
        private val getQueue = ArrayDeque<Response<ClickUpTaskDto>>()
        private val updateQueue = ArrayDeque<Response<ClickUpTaskDto>>()

        fun enqueueGet(response: Response<ClickUpTaskDto>) {
            getQueue.addLast(response)
        }

        fun enqueueUpdate(response: Response<ClickUpTaskDto>) {
            updateQueue.addLast(response)
        }

        override suspend fun getTeams(): Response<Unit> = Response.success(Unit)

        override suspend fun getTasks(listId: String): Response<Unit> = Response.success(Unit)

        override suspend fun getTask(taskId: String): Response<ClickUpTaskDto> {
            return getQueue.removeFirst()
        }

        override suspend fun updateTask(
            taskId: String,
            payload: ClickUpTaskUpdate,
            etag: String?
        ): Response<ClickUpTaskDto> {
            return updateQueue.removeFirst()
        }
    }

    private class InstrumentedFakeStatusRepository : SyncStatusRepository {
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
                val badge = when (report) {
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
                    lastResult = badge,
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
}
