package com.letsdoit.app.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager
) : SyncRetryScheduler {
    fun schedule() {
        enqueuePeriodicWork(initialDelaySeconds = null)
    }

    override fun scheduleRetry(delaySeconds: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            SyncWorker.RETRY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun delayPeriodicSync(delaySeconds: Long) {
        enqueuePeriodicWork(initialDelaySeconds = delaySeconds)
    }

    private fun enqueuePeriodicWork(initialDelaySeconds: Long?) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val builder = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
        if (initialDelaySeconds != null) {
            builder.setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
        }
        val request = builder.build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
