package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val name: String,
    val isShared: Boolean = false,
    val shareId: String? = null,
    val ownerDeviceId: String? = null,
    val encKeySpace: ByteArray? = null
)
