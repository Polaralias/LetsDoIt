package com.letsdoit.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.letsdoit.app.data.sync.SyncReport
import com.letsdoit.app.security.SecurePrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import kotlin.math.max
import kotlinx.coroutines.delay

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val securePrefs: SecurePrefs,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val token = securePrefs.read("clickup_token") ?: return Result.success()
        return try {
            when (val report = syncManager.runFullSync()) {
                is SyncReport.Success -> Result.success()
                is SyncReport.RateLimited -> {
                    val waitSeconds = max(1L, report.retryAfterSeconds)
                    delay(waitSeconds * 1000)
                    Result.retry()
                }
                is SyncReport.Failure -> if (report.retryable) Result.retry() else Result.success()
            }
        } catch (exception: Exception) {
            if (exception is HttpException && exception.code() in 400..499) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val WORK_NAME = "clickup_sync"
    }
}
