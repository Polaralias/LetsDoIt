package com.polaralias.letsdoit.backup

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock
) {
    fun scheduleDaily() {
        val now = Instant.now(clock)
        val zone = clock.zone
        val today = now.atZone(zone).withHour(3).withMinute(0).withSecond(0).withNano(0)
        val target = if (today.toInstant().isAfter(now)) today else today.plusDays(1)
        val delay = Duration.between(now, target.toInstant()).toMillis()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()
        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(BackupWorker.WORK_NAME)
    }
}
