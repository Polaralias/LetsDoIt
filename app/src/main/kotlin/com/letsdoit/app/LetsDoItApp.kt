package com.letsdoit.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.letsdoit.app.backup.BackupAutoController
import com.letsdoit.app.reminders.ReminderMaintenanceScheduler
import com.letsdoit.app.diagnostics.DiagnosticsController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LetsDoItApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderMaintenanceScheduler: ReminderMaintenanceScheduler

    @Inject
    lateinit var backupAutoController: BackupAutoController

    @Inject
    lateinit var diagnosticsController: DiagnosticsController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        reminderMaintenanceScheduler.scheduleDaily()
        backupAutoController
        diagnosticsController
    }
}
