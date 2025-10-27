package com.polaralias.letsdoit.backup

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupSnapshot(
    val database: DatabaseSnapshot,
    val preferences: PreferencesSnapshot
)

@JsonClass(generateAdapter = true)
data class DatabaseSnapshot(
    val spaces: List<SpaceRecord>,
    val folders: List<FolderRecord>,
    val lists: List<ListRecord>,
    val tasks: List<TaskRecord>,
    val subtasks: List<SubtaskRecord>,
    val orders: List<TaskOrderRecord>,
    val alarms: List<AlarmIndexRecord>,
    val syncMeta: List<TaskSyncMetaRecord>
)

@JsonClass(generateAdapter = true)
data class SpaceRecord(
    val id: Long,
    val remoteId: String?,
    val name: String,
    val isShared: Boolean,
    val shareId: String?,
    val ownerDeviceId: String?,
    val encKeySpace: String?
)

@JsonClass(generateAdapter = true)
data class FolderRecord(
    val id: Long,
    val spaceId: Long,
    val remoteId: String?,
    val name: String
)

@JsonClass(generateAdapter = true)
data class ListRecord(
    val id: Long,
    val spaceId: Long,
    val folderId: Long?,
    val remoteId: String?,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TaskRecord(
    val id: Long,
    val listId: Long,
    val title: String,
    val notes: String?,
    val dueAt: Long?,
    val repeatRule: String?,
    val remindOffsetMinutes: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val completed: Boolean,
    val priority: Int,
    val orderInList: Int,
    val startAt: Long?,
    val durationMinutes: Int?,
    val calendarEventId: Long?,
    val column: String
)

@JsonClass(generateAdapter = true)
data class SubtaskRecord(
    val id: Long,
    val parentTaskId: Long,
    val title: String,
    val done: Boolean,
    val dueAt: Long?,
    val orderInParent: Int,
    val startAt: Long?,
    val durationMinutes: Int?
)

@JsonClass(generateAdapter = true)
data class TaskOrderRecord(
    val id: Long,
    val taskId: Long,
    val column: String,
    val orderInColumn: Int
)

@JsonClass(generateAdapter = true)
data class AlarmIndexRecord(
    val id: Long,
    val taskId: Long,
    val nextFireAt: Long,
    val rruleHash: String
)

@JsonClass(generateAdapter = true)
data class TaskSyncMetaRecord(
    val taskId: Long,
    val remoteId: String?,
    val etag: String?,
    val remoteUpdatedAt: Long?,
    val needsPush: Boolean,
    val lastSyncedAt: Long?,
    val lastPulledAt: Long?,
    val lastPushedAt: Long?
)

@JsonClass(generateAdapter = true)
data class PreferencesSnapshot(
    val entries: List<PreferenceEntry>
)

@JsonClass(generateAdapter = true)
data class PreferenceEntry(
    val key: String,
    val type: PreferenceValueType,
    val stringValue: String? = null,
    val stringSetValue: List<String>? = null,
    val intValue: Int? = null,
    val longValue: Long? = null,
    val booleanValue: Boolean? = null,
    val floatValue: Double? = null
)

enum class PreferenceValueType {
    String,
    StringSet,
    Int,
    Long,
    Boolean,
    Float
}
