package com.letsdoit.app.integrations.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.letsdoit.app.reminders.ReminderAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

const val REMINDER_CHANNEL_ID = "reminders"
const val ACTION_REMINDER = "com.letsdoit.app.REMINDER"
const val EXTRA_TASK_ID = "extra_task_id"
const val EXTRA_TASK_TITLE = "extra_task_title"

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {
    init {
        ensureChannel()
    }

    fun schedule(taskId: Long, triggerAt: Instant, title: String) {
        ensureChannel()
        val pendingIntent = buildPendingIntent(taskId, title)
        val triggerMillis = triggerAt.toEpochMilli()
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun cancel(taskId: Long) {
        val pendingIntent = buildPendingIntent(taskId, null)
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(taskId: Long, title: String?): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            if (title != null) {
                putExtra(EXTRA_TASK_TITLE, title)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags)
    }

    private fun ensureChannel() {
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(REMINDER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Reminders")
                .setDescription("Task reminders")
                .build()
            manager.createNotificationChannel(channel)
        }
    }
}
