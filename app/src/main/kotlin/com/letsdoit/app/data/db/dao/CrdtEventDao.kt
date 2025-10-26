package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.letsdoit.app.data.db.entities.CrdtEventEntity

@Dao
interface CrdtEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: CrdtEventEntity): Long

    @Update
    suspend fun update(event: CrdtEventEntity)

    @Query("SELECT * FROM crdt_events WHERE listId = :listId ORDER BY lamport ASC, authorDeviceId ASC, id ASC")
    suspend fun listForList(listId: Long): List<CrdtEventEntity>

    @Query("SELECT * FROM crdt_events WHERE listId = :listId AND applied = 0 ORDER BY lamport ASC, authorDeviceId ASC, id ASC")
    suspend fun pendingForList(listId: Long): List<CrdtEventEntity>

    @Query(
        "SELECT * FROM crdt_events WHERE listId = :listId AND id NOT IN (SELECT id FROM event_acks WHERE listId = :listId) ORDER BY lamport ASC, authorDeviceId ASC, id ASC"
    )
    suspend fun pendingForRemote(listId: Long): List<CrdtEventEntity>

    @Query("SELECT id FROM crdt_events WHERE listId = :listId AND applied = 1 AND authorDeviceId != :deviceId")
    suspend fun acknowledgements(listId: Long, deviceId: String): List<String>

    @Query("UPDATE crdt_events SET applied = 1 WHERE id = :eventId")
    suspend fun markApplied(eventId: String)

    @Query("SELECT * FROM crdt_events WHERE id = :eventId")
    suspend fun findById(eventId: String): CrdtEventEntity?

    @Query("DELETE FROM crdt_events")
    suspend fun deleteAll()
}
