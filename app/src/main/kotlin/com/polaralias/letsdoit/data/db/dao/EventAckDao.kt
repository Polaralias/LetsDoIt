package com.polaralias.letsdoit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.polaralias.letsdoit.data.db.entities.EventAckEntity

@Dao
interface EventAckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ack: EventAckEntity)

    @Query("SELECT * FROM event_acks WHERE listId = :listId")
    suspend fun listForList(listId: Long): List<EventAckEntity>

    @Query("DELETE FROM event_acks WHERE id = :eventId")
    suspend fun delete(eventId: String)

    @Query("DELETE FROM event_acks")
    suspend fun deleteAll()
}
