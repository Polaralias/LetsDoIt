package com.example.letsdoit.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskListEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskListDao(): TaskListDao
}
