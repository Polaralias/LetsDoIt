package com.polaralias.letsdoit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.polaralias.letsdoit.backup.BackupAutoController
import com.polaralias.letsdoit.reminders.ReminderMaintenanceScheduler
import com.polaralias.letsdoit.diagnostics.DiagnosticsController
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
