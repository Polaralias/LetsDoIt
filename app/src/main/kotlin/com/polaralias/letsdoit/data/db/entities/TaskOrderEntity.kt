package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_order",
    indices = [
        Index(value = ["taskId"], unique = true),
        Index(value = ["column"])
    ]
)
data class TaskOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val column: String,
    val orderInColumn: Int
)
