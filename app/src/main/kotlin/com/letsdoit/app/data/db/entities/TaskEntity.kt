package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["listId"]), Index(value = ["dueAt"])],
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val notes: String? = null,
    val dueAt: Instant? = null,
    val repeatRule: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completed: Boolean = false
)
