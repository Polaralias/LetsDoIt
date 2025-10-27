package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "space_events",
    indices = [Index(value = ["shareId", "lamport", "authorDeviceId", "id"])]
)
data class SpaceEventEntity(
    @PrimaryKey val id: String,
    val shareId: String,
    val authorDeviceId: String,
    val lamport: Long,
    val timestamp: Long,
    val type: String,
    val payloadJson: String,
    val applied: Boolean = false
)
