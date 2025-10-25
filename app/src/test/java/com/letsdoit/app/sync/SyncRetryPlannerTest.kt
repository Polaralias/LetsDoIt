package com.letsdoit.app.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncRetryPlannerTest {
    private val scheduler = FakeScheduler()
    private val planner = SyncRetryPlanner(scheduler)

    @Test
    fun schedulesWhenDelayProvided() {
        val plan = planner.plan(30)
        assertEquals(SyncRetryPlan.Scheduled, plan)
        assertEquals(30L, scheduler.delay)
    }

    @Test
    fun backoffWhenDelayMissing() {
        val plan = planner.plan(null)
        assertEquals(SyncRetryPlan.Backoff, plan)
        assertNull(scheduler.delay)
    }

    private class FakeScheduler : SyncRetryScheduler {
        var delay: Long? = null
        override fun scheduleRetry(delaySeconds: Long) {
            delay = delaySeconds
        }
    }
}
