package com.polaralias.letsdoit.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return when (val result = backupManager.backupNow()) {
            is BackupResult.Success -> Result.success()
            is BackupResult.Failure -> {
                when (result.error) {
                    BackupError.Remote, BackupError.Unknown -> Result.retry()
                    BackupError.AuthRequired, BackupError.Snapshot, BackupError.Crypto, BackupError.NotFound -> Result.failure()
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "cloud_backup"
    }
}
