package com.letsdoit.app.domain.repository

import com.letsdoit.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksFlow(listId: String?): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun createTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun refreshTasks(listId: String)
    suspend fun syncUnsyncedTasks()
}
