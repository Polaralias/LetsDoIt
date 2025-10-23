package com.letsdoit.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.letsdoit.app.network.ClickUpService
import com.letsdoit.app.security.SecurePrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val clickUpService: ClickUpService,
    private val securePrefs: SecurePrefs
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val token = securePrefs.read("clickup_token") ?: return Result.success()
        return try {
            val response = clickUpService.getTeams()
            if (response.isSuccessful) {
                Result.success()
            } else if (response.code() in 400..499) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (exception: Exception) {
            if (exception is HttpException && exception.code() in 400..499) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val WORK_NAME = "clickup_sync"
    }
}
