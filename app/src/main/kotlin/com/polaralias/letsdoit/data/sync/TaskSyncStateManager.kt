package com.polaralias.letsdoit.data.sync

import com.polaralias.letsdoit.data.db.dao.TaskSyncMetaDao
import com.polaralias.letsdoit.data.db.entities.TaskSyncMetaEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskSyncStateManager @Inject constructor(
    private val taskSyncMetaDao: TaskSyncMetaDao
) {
    suspend fun list(): List<TaskSyncMeta> = taskSyncMetaDao.listAll().map(TaskSyncMetaEntity::toModel)

    suspend fun listPendingPushes(): List<TaskSyncMeta> = taskSyncMetaDao.listPendingPushes().map(TaskSyncMetaEntity::toModel)

    suspend fun find(taskId: Long): TaskSyncMeta? = taskSyncMetaDao.get(taskId)?.toModel()

    suspend fun save(meta: TaskSyncMeta) {
        taskSyncMetaDao.upsert(meta.toEntity())
    }

    suspend fun markNeedsPush(taskId: Long) {
        taskSyncMetaDao.setNeedsPush(taskId, true)
    }

    suspend fun markPulled(taskId: Long, etag: String?, remoteUpdatedAt: Instant?, timestamp: Instant) {
        taskSyncMetaDao.markPulled(taskId, etag, remoteUpdatedAt, timestamp)
    }

    suspend fun markPushed(taskId: Long, etag: String?, remoteUpdatedAt: Instant?, timestamp: Instant) {
        taskSyncMetaDao.markPushed(taskId, etag, remoteUpdatedAt, timestamp)
    }

    suspend fun reset(taskId: Long) {
        taskSyncMetaDao.delete(taskId)
    }
}
