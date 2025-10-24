package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_index",
    indices = [Index(value = ["taskId"], unique = true)]
)
data class AlarmIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val nextFireAt: Long,
    val rruleHash: String
)
