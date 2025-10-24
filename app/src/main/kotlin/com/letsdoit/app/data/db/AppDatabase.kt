package com.letsdoit.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.letsdoit.app.data.db.dao.FolderDao
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.entities.FolderEntity
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskOrderEntity

@Database(
    entities = [
        SpaceEntity::class,
        FolderEntity::class,
        ListEntity::class,
        TaskEntity::class,
        TaskOrderEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun folderDao(): FolderDao
    abstract fun listDao(): ListDao
    abstract fun taskDao(): TaskDao
    abstract fun taskOrderDao(): TaskOrderDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 2")
        db.execSQL("ALTER TABLE tasks ADD COLUMN orderInList INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN startAt INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN calendarEventId INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN column TEXT NOT NULL DEFAULT 'To do'")
        db.execSQL("CREATE TABLE IF NOT EXISTS task_order (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskId INTEGER NOT NULL, column TEXT NOT NULL, orderInColumn INTEGER NOT NULL, FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_order_taskId ON task_order(taskId)")
    }
}
