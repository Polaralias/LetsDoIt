package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lists",
    indices = [Index(value = ["spaceId"]), Index(value = ["folderId"])],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val folderId: Long? = null,
    val remoteId: String? = null,
    val name: String
)
