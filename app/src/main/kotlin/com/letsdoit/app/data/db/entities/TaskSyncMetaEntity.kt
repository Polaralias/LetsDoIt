package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "task_sync_meta",
    indices = [Index(value = ["remoteId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskSyncMetaEntity(
    @PrimaryKey val taskId: Long,
    val remoteId: String? = null,
    val etag: String? = null,
    val needsPush: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val lastPulledAt: Instant? = null,
    val lastPushedAt: Instant? = null
)
