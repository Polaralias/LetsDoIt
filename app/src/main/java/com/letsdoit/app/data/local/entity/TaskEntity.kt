package com.letsdoit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val description: String?,
    val status: String,
    val dueDate: Long?,
    val priority: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = true,
    val calendarEventId: Long? = null,
    val recurrenceRule: String? = null
)
