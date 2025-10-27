package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "space_members",
    indices = [Index(value = ["shareId", "deviceId"], unique = true)]
)
data class SpaceMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shareId: String,
    val deviceId: String,
    val displayName: String?,
    val joinedAt: Long,
    val role: String
)
