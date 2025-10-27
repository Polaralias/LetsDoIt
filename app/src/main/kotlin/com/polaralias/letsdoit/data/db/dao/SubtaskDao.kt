package com.polaralias.letsdoit.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.polaralias.letsdoit.data.db.entities.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY orderInParent")
    fun observeByParent(parentTaskId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY orderInParent")
    suspend fun listByParent(parentTaskId: Long): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun listAll(): List<SubtaskEntity>

    @Upsert
    suspend fun upsert(subtask: SubtaskEntity): Long

    @Query("UPDATE subtasks SET done = :done WHERE id = :subtaskId")
    suspend fun updateDone(subtaskId: Long, done: Boolean)

    @Query("UPDATE subtasks SET orderInParent = :orderInParent WHERE id = :subtaskId")
    suspend fun updateOrder(subtaskId: Long, orderInParent: Int)

    @Query("UPDATE subtasks SET startAt = :startAt, durationMinutes = :durationMinutes, dueAt = :dueAt WHERE id = :subtaskId")
    suspend fun updateSchedule(subtaskId: Long, startAt: Long?, durationMinutes: Int?, dueAt: Long?)

    @Query("DELETE FROM subtasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun deleteByParent(parentTaskId: Long)
}
