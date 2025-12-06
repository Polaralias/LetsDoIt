package com.letsdoit.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.letsdoit.app.data.local.converters.DateConverter
import com.letsdoit.app.data.local.dao.FolderDao
import com.letsdoit.app.data.local.dao.ListDao
import com.letsdoit.app.data.local.dao.SpaceDao
import com.letsdoit.app.data.local.dao.TaskDao
import com.letsdoit.app.data.local.entity.FolderEntity
import com.letsdoit.app.data.local.entity.ListEntity
import com.letsdoit.app.data.local.entity.SpaceEntity
import com.letsdoit.app.data.local.entity.TaskEntity

@Database(
    entities = [
        SpaceEntity::class,
        FolderEntity::class,
        ListEntity::class,
        TaskEntity::class
    ],
    version = 1
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun folderDao(): FolderDao
    abstract fun listDao(): ListDao
    abstract fun taskDao(): TaskDao
}
