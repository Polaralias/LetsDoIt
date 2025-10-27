package com.polaralias.letsdoit.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface SyncRetryScheduler {
    fun scheduleRetry(delaySeconds: Long)
}

@Singleton
class SyncRetryPlanner @Inject constructor(
    private val syncScheduler: SyncRetryScheduler
) {
    fun plan(retryAfterSeconds: Long?): SyncRetryPlan {
        val delay = retryAfterSeconds?.let { max(1L, it) }
        return if (delay != null) {
            syncScheduler.scheduleRetry(delay)
            SyncRetryPlan.Scheduled
        } else {
            SyncRetryPlan.Backoff
        }
    }
}

enum class SyncRetryPlan {
    Scheduled,
    Backoff
}
