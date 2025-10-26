package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_list_migrations")
data class SharedListMigrationEntity(
    @PrimaryKey val legacyShareId: String,
    val spaceShareId: String
)
