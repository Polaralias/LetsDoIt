package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = SubtaskEntity::class)
@Entity(tableName = "subtasks_fts")
data class SubtaskFtsEntity(
    val title: String
)
