package com.letsdoit.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.letsdoit.app.integrations.alarm.AlarmScheduler
import com.letsdoit.app.data.task.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.Instant

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskRepository.observeTasks().first()
                val now = Instant.now()
                tasks.filter { it.dueAt != null && it.dueAt.isAfter(now) }.forEach { task ->
                    alarmScheduler.schedule(task.id, task.dueAt!!, task.title)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
