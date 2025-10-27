package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["listId"]),
        Index(value = ["dueAt"]),
        Index(value = ["completed"]),
        Index(value = ["column"]),
        Index(value = ["priority"])
    ],
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
    val remindOffsetMinutes: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completed: Boolean = false,
    val priority: Int = 2,
    val orderInList: Int = 0,
    val startAt: Long? = null,
    val durationMinutes: Int? = null,
    val calendarEventId: Long? = null,
    val column: String = "To do"
)
