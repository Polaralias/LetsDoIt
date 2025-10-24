package com.letsdoit.app.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: ReminderCoordinator
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        coordinator.performMaintenance()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "reminder_maintenance"
    }
}
