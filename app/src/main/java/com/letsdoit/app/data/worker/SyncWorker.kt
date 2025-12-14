package com.letsdoit.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.letsdoit.app.domain.repository.PreferencesRepository
import com.letsdoit.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val taskId = inputData.getString("taskId")
            if (taskId != null) {
                taskRepository.refreshTask(taskId)
            } else {
                taskRepository.syncUnsyncedTasks()
                val listId = preferencesRepository.getSelectedListId()
                if (listId != null) {
                    taskRepository.refreshTasks(listId)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
