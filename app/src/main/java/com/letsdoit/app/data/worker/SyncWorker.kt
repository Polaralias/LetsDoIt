package com.letsdoit.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.letsdoit.app.core.util.Constants
import com.letsdoit.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            taskRepository.syncUnsyncedTasks()
            // We need a listId for refreshTasks.
            // For now, we use the hardcoded DEMO_LIST_ID from Constants.
            // In a real app, we might iterate all active lists.
            taskRepository.refreshTasks(Constants.DEMO_LIST_ID)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
