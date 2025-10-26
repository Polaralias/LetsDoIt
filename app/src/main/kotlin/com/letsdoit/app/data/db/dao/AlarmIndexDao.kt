package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.letsdoit.app.data.db.entities.AlarmIndexEntity

@Dao
interface AlarmIndexDao {
    @Upsert
    suspend fun upsert(entity: AlarmIndexEntity): Long

    @Query("SELECT * FROM alarm_index WHERE taskId = :taskId")
    suspend fun findByTaskId(taskId: Long): AlarmIndexEntity?

    @Query("DELETE FROM alarm_index WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    @Query("DELETE FROM alarm_index")
    suspend fun clear()

    @Query("SELECT * FROM alarm_index")
    suspend fun listAll(): List<AlarmIndexEntity>

    @Query("DELETE FROM alarm_index WHERE nextFireAt < :threshold")
    suspend fun deleteBefore(threshold: Long)
}
