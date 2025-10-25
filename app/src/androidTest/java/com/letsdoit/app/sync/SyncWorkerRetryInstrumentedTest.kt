package com.letsdoit.app.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.sync.SyncReport
import com.letsdoit.app.data.sync.SyncResultBadge
import com.letsdoit.app.data.sync.SyncStatus
import com.letsdoit.app.data.sync.SyncStatusRepository
import com.letsdoit.app.data.sync.TaskSyncMeta
import com.letsdoit.app.data.sync.TaskSyncStateManager
import com.letsdoit.app.network.ClickUpService
import com.letsdoit.app.network.ClickUpTaskDto
import com.letsdoit.app.network.ClickUpTaskUpdate
import com.letsdoit.app.security.SecurePrefs
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ExecutionException
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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class SyncWorkerRetryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var syncManager: SyncManager
    private lateinit var stateManager: TaskSyncStateManager
    private lateinit var service: RateLimitedService
    private lateinit var statusRepository: RecordingStatusRepository
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: SyncScheduler
    private lateinit var retryPlanner: SyncRetryPlanner
    private lateinit var securePrefs: SecurePrefs
    private val clock: Clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))

    @BeforeTest
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        stateManager = TaskSyncStateManager(database.taskSyncMetaDao())
        service = RateLimitedService()
        statusRepository = RecordingStatusRepository()
        syncManager = SyncManager(database.taskDao(), stateManager, service, statusRepository, clock)
        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
        scheduler = SyncScheduler(workManager)
        retryPlanner = SyncRetryPlanner(scheduler)
        securePrefs = SecurePrefs(context)
        securePrefs.write("clickup_token", "token")
        seedTask()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        try {
            workManager.cancelAllWork().result.get()
        } catch (_: ExecutionException) {
        } catch (_: InterruptedException) {
        }
    }

    @Test
    fun schedulesRetryWorkWithDelayFromHeader() = runTest {
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    params: WorkerParameters
                ) = SyncWorker(appContext, params, securePrefs, syncManager, retryPlanner)
            })
            .build()

        val result = worker.doWork()

        assertIs<androidx.work.ListenableWorker.Result.Success>(result)
        val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.RETRY_WORK_NAME).get()
        assertEquals(1, workInfos.size)
        val workInfo = workInfos.first()
        val workManagerImpl = workManager as WorkManagerImpl
        val workSpec = workManagerImpl.workDatabase.workSpecDao().getWorkSpec(workInfo.id.toString())
        assertIs<SyncReport.RateLimited>(statusRepository.lastReport)
        assertEquals(45_000L, workSpec.initialDelay)

        val periodicInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get()
        assertEquals(1, periodicInfos.size)
        val periodicSpec = workManagerImpl.workDatabase.workSpecDao()
            .getWorkSpec(periodicInfos.first().id.toString())
        assertEquals(45_000L, periodicSpec.initialDelay)
    }

    private suspend fun seedTask() {
        val spaceDao = database.spaceDao()
        val listDao = database.listDao()
        spaceDao.upsert(SpaceEntity(name = "Space"))
        val spaceId = spaceDao.listAll().first().id
        listDao.upsert(ListEntity(spaceId = spaceId, name = "List"))
        val listId = listDao.listAll().first().id
        val taskId = database.taskDao().upsert(
            TaskEntity(
                listId = listId,
                title = "Local",
                createdAt = clock.instant(),
                updatedAt = clock.instant()
            )
        )
        stateManager.save(
            TaskSyncMeta(
                taskId = taskId,
                remoteId = "remote-rate",
                etag = null,
                remoteUpdatedAt = null,
                needsPush = true,
                lastSyncedAt = null,
                lastPulledAt = null,
                lastPushedAt = null
            )
        )
    }

    private class RateLimitedService : ClickUpService {
        override suspend fun getTeams(): Response<Unit> = Response.success(Unit)
        override suspend fun getTasks(listId: String): Response<Unit> = Response.success(Unit)
        override suspend fun getTask(taskId: String): Response<ClickUpTaskDto> {
            throw UnsupportedOperationException()
        }

        override suspend fun updateTask(
            taskId: String,
            payload: ClickUpTaskUpdate,
            etag: String?
        ): Response<ClickUpTaskDto> {
            return errorResponse(429, "limited", Headers.headersOf("Retry-After", "45"))
        }

        private fun errorResponse(
            code: Int,
            message: String,
            headers: Headers
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

    private class RecordingStatusRepository : SyncStatusRepository {
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
        var lastReport: SyncReport? = null

        override suspend fun record(report: SyncReport, completedAt: Instant) {
            lastReport = report
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
                    lastRetryAfterSeconds = if (report is SyncReport.RateLimited) report.retryAfterSeconds else null
                )
            }
        }
    }
}
