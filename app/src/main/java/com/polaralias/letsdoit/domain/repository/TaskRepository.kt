package com.polaralias.letsdoit.domain.repository

import com.polaralias.letsdoit.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksFlow(listId: String?): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun createTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun refreshTasks(listId: String)
    suspend fun refreshTask(taskId: String)
    suspend fun syncUnsyncedTasks()
    fun searchTasks(query: String): Flow<List<Task>>
}
