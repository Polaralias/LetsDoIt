package com.polaralias.letsdoit.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.polaralias.letsdoit.data.sync.SyncReport
import com.polaralias.letsdoit.security.SecurePrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val securePrefs: SecurePrefs,
    private val syncManager: SyncManager,
    private val retryPlanner: SyncRetryPlanner
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val token = securePrefs.read("clickup_token") ?: return Result.success()
        return try {
            when (val report = syncManager.runFullSync()) {
                is SyncReport.Success -> Result.success()
                is SyncReport.RateLimited -> when (retryPlanner.plan(report.retryAfterSeconds)) {
                    SyncRetryPlan.Scheduled -> Result.success()
                    SyncRetryPlan.Backoff -> Result.retry()
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
        const val RETRY_WORK_NAME = "clickup_sync_retry"
    }
}
