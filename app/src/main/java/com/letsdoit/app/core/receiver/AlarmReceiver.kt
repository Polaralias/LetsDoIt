package com.letsdoit.app.core.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.letsdoit.app.MainActivity
import com.letsdoit.app.R
import com.letsdoit.app.core.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Task Due")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}
