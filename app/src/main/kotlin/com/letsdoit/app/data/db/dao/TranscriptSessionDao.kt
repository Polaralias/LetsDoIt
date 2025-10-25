package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.letsdoit.app.data.db.entities.TranscriptSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSessionDao {
    @Query("SELECT * FROM transcript_sessions ORDER BY createdAt DESC")
    fun sessions(): Flow<List<TranscriptSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranscriptSessionEntity)

    @Query("SELECT * FROM transcript_sessions WHERE id = :id")
    suspend fun get(id: Long): TranscriptSessionEntity?
}
