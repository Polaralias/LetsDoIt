package com.polaralias.letsdoit.data.sync

import com.polaralias.letsdoit.data.db.entities.TaskSyncMetaEntity
import java.time.Instant

data class TaskSyncMeta(
    val taskId: Long,
    val remoteId: String?,
    val etag: String?,
    val remoteUpdatedAt: Instant?,
    val needsPush: Boolean,
    val lastSyncedAt: Instant?,
    val lastPulledAt: Instant?,
    val lastPushedAt: Instant?
)

fun TaskSyncMetaEntity.toModel(): TaskSyncMeta = TaskSyncMeta(
    taskId = taskId,
    remoteId = remoteId,
    etag = etag,
    remoteUpdatedAt = remoteUpdatedAt,
    needsPush = needsPush,
    lastSyncedAt = lastSyncedAt,
    lastPulledAt = lastPulledAt,
    lastPushedAt = lastPushedAt
)

fun TaskSyncMeta.toEntity(): TaskSyncMetaEntity = TaskSyncMetaEntity(
    taskId = taskId,
    remoteId = remoteId,
    etag = etag,
    remoteUpdatedAt = remoteUpdatedAt,
    needsPush = needsPush,
    lastSyncedAt = lastSyncedAt,
    lastPulledAt = lastPulledAt,
    lastPushedAt = lastPushedAt
)
