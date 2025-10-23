package com.letsdoit.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.letsdoit.app.R
import com.letsdoit.app.integrations.alarm.EXTRA_TASK_ID
import com.letsdoit.app.integrations.alarm.EXTRA_TASK_TITLE
import com.letsdoit.app.integrations.alarm.REMINDER_CHANNEL_ID

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: context.getString(R.string.app_name)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.label_due))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }
}
