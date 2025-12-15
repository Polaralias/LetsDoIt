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

    override fun getTasksFlow(listId: String?): Flow<List<Task>> {
        val flow = if (listId != null) {
            taskDao.getTasksByListId(listId)
        } else {
            taskDao.getAllTasks()
        }
        return flow.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTask(id: String): Task? {
        return taskDao.getTaskById(id).firstOrNull()?.toDomain()
    }

    override suspend fun createTask(task: Task) {
        // Save locally first
        val localEntity = task.toEntity().copy(isSynced = false)
        taskDao.insertTask(localEntity)

        try {
            val request = ClickUpCreateTaskRequest(
                name = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                dueDate = task.dueDate?.toEpochMilli()
            )
            val dto = api.createTask(task.listId, request)
            // Remove local temp task and insert real one
            taskDao.deleteTask(localEntity)
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

    override suspend fun refreshTask(taskId: String) {
        try {
            val taskDto = api.getTask(taskId)
            val listId = taskDto.list.id
            val entity = taskDto.toEntity(listId)
            taskDao.insertTask(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun syncUnsyncedTasks() {
        val unsynced = taskDao.getUnsyncedTasks()
        for (entity in unsynced) {
            try {
                // Heuristic: ClickUp IDs are short, UUIDs are long
                if (entity.id.length > 20) {
                    // Create
                    val request = ClickUpCreateTaskRequest(
                        name = entity.title,
                        description = entity.description,
                        status = entity.status,
                        priority = entity.priority,
                        dueDate = entity.dueDate
                    )
                    val dto = api.createTask(entity.listId, request)
                    taskDao.deleteTask(entity)
                    taskDao.insertTask(dto.toEntity(entity.listId))
                } else {
                    // Update
                    val request = ClickUpUpdateTaskRequest(
                        name = entity.title,
                        description = entity.description,
                        status = entity.status,
                        priority = entity.priority,
                        dueDate = entity.dueDate
                    )
                    val dto = api.updateTask(entity.id, request)
                    taskDao.insertTask(dto.toEntity(entity.listId))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
