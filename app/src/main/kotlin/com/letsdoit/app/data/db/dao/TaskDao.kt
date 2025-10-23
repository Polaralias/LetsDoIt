package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.letsdoit.app.data.db.entities.TaskEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY completed ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY completed ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt")
    fun observeByList(listId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAt IS NOT NULL ORDER BY dueAt")
    fun observeTimeline(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getById(taskId: Long): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateCompletion(taskId: Long, completed: Boolean, updatedAt: Instant)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun delete(taskId: Long)
}
