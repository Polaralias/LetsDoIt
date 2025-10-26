package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shared_lists",
    indices = [Index(value = ["listId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SharedListEntity(
    val listId: Long,
    @PrimaryKey val shareId: String,
    val encKey: ByteArray,
    val transport: String,
    val createdAt: Long
)
