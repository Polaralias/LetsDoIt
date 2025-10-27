package com.polaralias.letsdoit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index(value = ["spaceId"])],
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val remoteId: String? = null,
    val name: String
)
