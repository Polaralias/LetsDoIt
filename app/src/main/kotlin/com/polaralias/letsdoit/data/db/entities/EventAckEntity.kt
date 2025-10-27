package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_acks",
    indices = [Index(value = ["listId"])],
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EventAckEntity(
    @PrimaryKey val id: String,
    val listId: Long,
    val acknowledgedAt: Long
)
