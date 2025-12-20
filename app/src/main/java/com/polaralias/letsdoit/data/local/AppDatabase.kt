package com.polaralias.letsdoit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.polaralias.letsdoit.data.local.converters.DateConverter
import com.polaralias.letsdoit.data.local.dao.FolderDao
import com.polaralias.letsdoit.data.local.dao.ListDao
import com.polaralias.letsdoit.data.local.dao.SpaceDao
import com.polaralias.letsdoit.data.local.dao.TaskDao
import com.polaralias.letsdoit.data.local.entity.FolderEntity
import com.polaralias.letsdoit.data.local.entity.ListEntity
import com.polaralias.letsdoit.data.local.entity.SpaceEntity
import com.polaralias.letsdoit.data.local.entity.TaskEntity

@Database(
    entities = [
        SpaceEntity::class,
        FolderEntity::class,
        ListEntity::class,
        TaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun folderDao(): FolderDao
    abstract fun listDao(): ListDao
    abstract fun taskDao(): TaskDao
}
