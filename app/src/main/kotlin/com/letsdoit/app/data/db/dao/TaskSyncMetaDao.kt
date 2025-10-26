package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.letsdoit.app.data.db.entities.TaskSyncMetaEntity
import java.time.Instant

@Dao
interface TaskSyncMetaDao {
    @Query("SELECT * FROM task_sync_meta")
    suspend fun listAll(): List<TaskSyncMetaEntity>

    @Query("SELECT * FROM task_sync_meta WHERE needsPush = 1")
    suspend fun listPendingPushes(): List<TaskSyncMetaEntity>

    @Query("SELECT * FROM task_sync_meta WHERE taskId = :taskId")
    suspend fun get(taskId: Long): TaskSyncMetaEntity?

    @Upsert
    suspend fun upsert(entity: TaskSyncMetaEntity)

    @Query("DELETE FROM task_sync_meta")
    suspend fun clear()

    @Query("DELETE FROM task_sync_meta WHERE taskId = :taskId")
    suspend fun delete(taskId: Long)

    @Query("UPDATE task_sync_meta SET needsPush = :needsPush WHERE taskId = :taskId")
    suspend fun setNeedsPush(taskId: Long, needsPush: Boolean)

    @Query(
        "UPDATE task_sync_meta SET etag = :etag, remoteUpdatedAt = :remoteUpdatedAt, needsPush = 0, lastSyncedAt = :timestamp, lastPushedAt = :timestamp WHERE taskId = :taskId"
    )
    suspend fun markPushed(taskId: Long, etag: String?, remoteUpdatedAt: Instant?, timestamp: Instant)

    @Query(
        "UPDATE task_sync_meta SET etag = :etag, remoteUpdatedAt = :remoteUpdatedAt, needsPush = 0, lastSyncedAt = :timestamp, lastPulledAt = :timestamp WHERE taskId = :taskId"
    )
    suspend fun markPulled(taskId: Long, etag: String?, remoteUpdatedAt: Instant?, timestamp: Instant)
}
