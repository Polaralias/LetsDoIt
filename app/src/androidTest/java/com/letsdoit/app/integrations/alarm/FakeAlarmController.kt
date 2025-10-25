package com.letsdoit.app.integrations.alarm

import android.app.PendingIntent
import javax.inject.Inject
import javax.inject.Singleton

enum class AlarmRequestType { Exact, Inexact }

data class RecordedAlarm(
    val type: AlarmRequestType,
    val triggerAtMillis: Long
)

@Singleton
class FakeAlarmController @Inject constructor() : AlarmController {
    var lastRequest: RecordedAlarm? = null

    override fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent) {
        lastRequest = RecordedAlarm(AlarmRequestType.Exact, triggerAtMillis)
    }

    override fun setInexact(type: Int, triggerAtMillis: Long, operation: PendingIntent) {
        lastRequest = RecordedAlarm(AlarmRequestType.Inexact, triggerAtMillis)
    }

    override fun cancel(operation: PendingIntent) {
        lastRequest = null
    }

    fun reset() {
        lastRequest = null
    }
}
