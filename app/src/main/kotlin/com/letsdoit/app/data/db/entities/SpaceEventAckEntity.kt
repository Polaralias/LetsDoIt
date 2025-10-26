package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "space_event_acks",
    indices = [Index(value = ["shareId", "acknowledgedAt"])]
)
data class SpaceEventAckEntity(
    @PrimaryKey val id: String,
    val shareId: String,
    val acknowledgedAt: Long
)
