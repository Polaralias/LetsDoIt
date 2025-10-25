package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskWithSubtasksRelation
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

    @Query("SELECT MAX(orderInList) FROM tasks WHERE listId = :listId")
    suspend fun maxOrderInList(listId: Long): Int?

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY orderInList")
    suspend fun listByOrder(listId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun listAll(): List<TaskEntity>

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateCompletion(taskId: Long, completed: Boolean, updatedAt: Instant)

    @Query("UPDATE tasks SET orderInList = :orderInList, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateOrderInList(taskId: Long, orderInList: Int, updatedAt: Instant)

    @Query("UPDATE tasks SET priority = :priority, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updatePriority(taskId: Long, priority: Int, updatedAt: Instant)

    @Query("UPDATE tasks SET dueAt = :dueAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateDueDate(taskId: Long, dueAt: Instant?, updatedAt: Instant)

    @Query("UPDATE tasks SET startAt = :startAt, durationMinutes = :durationMinutes, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTimeline(taskId: Long, startAt: Long?, durationMinutes: Int?, updatedAt: Instant)

    @Query("UPDATE tasks SET calendarEventId = :calendarEventId, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateCalendarEvent(taskId: Long, calendarEventId: Long?, updatedAt: Instant)

    @Query("UPDATE tasks SET column = :column, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateColumn(taskId: Long, column: String, updatedAt: Instant)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun delete(taskId: Long)

    @Transaction
    @Query(
        "SELECT * FROM tasks WHERE id IN (" +
            "SELECT rowid FROM tasks_fts WHERE tasks_fts MATCH :query " +
            "UNION SELECT parentTaskId FROM subtasks_fts WHERE subtasks_fts MATCH :query" +
            ") ORDER BY completed ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt"
    )
    fun searchWithSubtasks(query: String): Flow<List<TaskWithSubtasksRelation>>

    @Transaction
    @Query(
        "SELECT * FROM tasks WHERE completed = 0 AND dueAt IS NOT NULL AND dueAt BETWEEN :startInclusive AND :endExclusive " +
            "ORDER BY dueAt, createdAt"
    )
    fun filterDueToday(startInclusive: Instant, endExclusive: Instant): Flow<List<TaskWithSubtasksRelation>>

    @Transaction
    @Query(
        "SELECT * FROM tasks WHERE completed = 0 AND dueAt IS NOT NULL AND dueAt < :reference ORDER BY dueAt"
    )
    fun filterOverdue(reference: Instant): Flow<List<TaskWithSubtasksRelation>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE completed = 0 AND dueAt IS NULL ORDER BY createdAt DESC")
    fun filterNoDueDate(): Flow<List<TaskWithSubtasksRelation>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE completed = 0 AND priority = 0 ORDER BY updatedAt DESC")
    fun filterHighPriority(): Flow<List<TaskWithSubtasksRelation>>

    @Transaction
    @Query(
        "SELECT tasks.* FROM tasks INNER JOIN task_sync_meta ON task_sync_meta.taskId = tasks.id " +
            "WHERE task_sync_meta.remoteId IS NOT NULL ORDER BY tasks.updatedAt DESC"
    )
    fun filterLinkedToClickUp(): Flow<List<TaskWithSubtasksRelation>>

    @Transaction
    @Query(
        "SELECT tasks.* FROM tasks INNER JOIN lists ON lists.id = tasks.listId " +
            "LEFT JOIN task_sync_meta ON task_sync_meta.taskId = tasks.id " +
            "WHERE lists.remoteId IS NOT NULL AND (task_sync_meta.remoteId IS NULL OR task_sync_meta.remoteId = '') " +
            "ORDER BY tasks.updatedAt DESC"
    )
    fun filterShared(): Flow<List<TaskWithSubtasksRelation>>
}
