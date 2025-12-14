package com.letsdoit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["spaceId"])]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val name: String,
    val orderIndex: Int
)
