package com.example.letsdoit.data

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
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val notes: String?,
    val dueAt: Long?,
    val priority: TaskPriority,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

enum class TaskPriority {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}
