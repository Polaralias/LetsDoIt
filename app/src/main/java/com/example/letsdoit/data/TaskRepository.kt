package com.example.letsdoit.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getTasksForList(listId: Long): Flow<List<TaskEntity>> = taskDao.getTasksForList(listId)

    fun getTask(id: Long): Flow<TaskEntity?> = taskDao.getTask(id)

    suspend fun createTask(
        listId: Long,
        title: String,
        notes: String?,
        dueAt: Long?,
        priority: TaskPriority
    ): Long {
        require(title.isNotBlank())
        val now = System.currentTimeMillis()
        return taskDao.insertTask(
            TaskEntity(
                listId = listId,
                title = title,
                notes = notes,
                dueAt = dueAt,
                priority = priority,
                isCompleted = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateTask(
        id: Long,
        listId: Long,
        title: String,
        notes: String?,
        dueAt: Long?,
        priority: TaskPriority
    ) {
        require(title.isNotBlank())
        val existing = taskDao.getTaskById(id) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            listId = listId,
            title = title,
            notes = notes,
            dueAt = dueAt,
            priority = priority,
            updatedAt = now
        )
        taskDao.updateTask(updated)
    }

    suspend fun toggleTaskCompletion(id: Long) {
        val existing = taskDao.getTaskById(id) ?: return
        val now = System.currentTimeMillis()
        taskDao.updateCompletion(id, !existing.isCompleted, now)
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteTask(id)
    }
}
