package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crdt_events",
    indices = [Index(value = ["listId", "lamport", "authorDeviceId", "id"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CrdtEventEntity(
    @PrimaryKey val id: String,
    val listId: Long,
    val authorDeviceId: String,
    val lamport: Long,
    val timestamp: Long,
    val type: String,
    val payloadJson: String,
    val applied: Boolean
)
