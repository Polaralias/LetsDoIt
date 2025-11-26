package com.example.letsdoit.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [ListEntity::class, TaskEntity::class], version = 2, exportSchema = false)
@TypeConverters(TaskPriorityConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
    abstract fun taskDao(): TaskDao
}

class TaskPriorityConverter {
    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)
}
