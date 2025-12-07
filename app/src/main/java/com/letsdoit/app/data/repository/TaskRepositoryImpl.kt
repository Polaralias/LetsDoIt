package com.letsdoit.app.data.repository

import com.letsdoit.app.data.local.dao.TaskDao
import com.letsdoit.app.data.mapper.toDomain
import com.letsdoit.app.data.mapper.toEntity
import com.letsdoit.app.data.mapper.toEpochMilli
import com.letsdoit.app.data.remote.ClickUpApi
import com.letsdoit.app.data.remote.dto.ClickUpCreateTaskRequest
import com.letsdoit.app.data.remote.dto.ClickUpUpdateTaskRequest
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val api: ClickUpApi
) : TaskRepository {

    override fun getTasksFlow(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTask(id: String): Task? {
        return taskDao.getTaskById(id).firstOrNull()?.toDomain()
    }

    override suspend fun createTask(task: Task) {
        // Save locally first
        taskDao.insertTask(task.toEntity().copy(isSynced = false))

        try {
            val request = ClickUpCreateTaskRequest(
                name = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                dueDate = task.dueDate?.toEpochMilli()
            )
            val dto = api.createTask(task.listId, request)
            taskDao.insertTask(dto.toEntity(task.listId))
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep unsynced
        }
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity().copy(isSynced = false))

        try {
            val request = ClickUpUpdateTaskRequest(
                name = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                dueDate = task.dueDate?.toEpochMilli()
            )
            val dto = api.updateTask(task.id, request)
            taskDao.insertTask(dto.toEntity(task.listId))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun refreshTasks(listId: String) {
        try {
            val response = api.getTasks(listId)
            val entities = response.tasks.map { it.toEntity(listId) }
            entities.forEach { taskDao.insertTask(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
