package com.letsdoit.app.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.letsdoit.app.MainActivity
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.TaskRepository
import com.letsdoit.app.integrations.alarm.EXTRA_TASK_ID
import com.letsdoit.app.integrations.alarm.REMINDER_CHANNEL_ID
import com.letsdoit.app.navigation.AppIntentExtras
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val REMINDER_GROUP_KEY = "reminders_group"
private const val SUMMARY_NOTIFICATION_ID = 0
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderReceiverEntryPoint {
    fun taskRepository(): TaskRepository
    fun reminderCoordinator(): ReminderCoordinator
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        if (taskId == 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, ReminderReceiverEntryPoint::class.java)
                val repository = entryPoint.taskRepository()
                val task = repository.getTask(taskId)
                if (task == null) {
                    NotificationManagerCompat.from(context).cancel(taskId.toInt())
                    return@launch
                }
                val notification = buildNotification(context, task)
                val manager = NotificationManagerCompat.from(context)
                manager.notify(taskId.toInt(), notification)
                manager.notify(SUMMARY_NOTIFICATION_ID, buildSummary(context))
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildNotification(context: Context, task: Task): android.app.Notification {
        val title = task.title.ifBlank { context.getString(R.string.app_name) }
        val dueText = task.dueAt?.let { due ->
            context.getString(R.string.reminder_due_time, timeFormatter.format(due))
        } ?: context.getString(R.string.label_due)
        val completeIntent = ReminderActionReceiver.buildCompleteIntent(context, task.id)
        val snoozeTenIntent = ReminderActionReceiver.buildSnoozeIntent(context, task.id, 10)
        val snoozeHourIntent = ReminderActionReceiver.buildSnoozeIntent(context, task.id, 60)
        val timelineIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppIntentExtras.TIMELINE_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(dueText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(dueText))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(timelineIntent)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.reminder_action_complete), completeIntent)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.reminder_action_snooze_ten), snoozeTenIntent)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.reminder_action_snooze_hour), snoozeHourIntent)
            .setGroup(REMINDER_GROUP_KEY)
            .build()
    }

    private fun buildSummary(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_summary_title))
            .setContentText(context.getString(R.string.reminder_summary_text))
            .setGroup(REMINDER_GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        if (taskId == 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, ReminderReceiverEntryPoint::class.java)
                val repository = entryPoint.taskRepository()
                val coordinator = entryPoint.reminderCoordinator()
                when (intent.action) {
                    ACTION_COMPLETE -> repository.updateCompletion(taskId, true)
                    ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        coordinator.snooze(taskId, minutes.toLong())
                    }
                }
                NotificationManagerCompat.from(context).cancel(taskId.toInt())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.letsdoit.app.reminders.COMPLETE"
        const val ACTION_SNOOZE = "com.letsdoit.app.reminders.SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

        fun buildCompleteIntent(context: Context, taskId: Long): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            return PendingIntent.getBroadcast(
                context,
                (taskId shl 3).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun buildSnoozeIntent(context: Context, taskId: Long, minutes: Int): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_SNOOZE_MINUTES, minutes)
            }
            val requestCode = (taskId shl 3).toInt() + minutes
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
