package com.letsdoit.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.FolderDao
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.SubtaskDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.dao.TaskSyncMetaDao
import com.letsdoit.app.data.db.entities.FolderEntity
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.SubtaskEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskOrderEntity
import com.letsdoit.app.data.db.entities.TaskSyncMetaEntity
import com.letsdoit.app.data.db.entities.AlarmIndexEntity

@Database(
    entities = [
        SpaceEntity::class,
        FolderEntity::class,
        ListEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        TaskOrderEntity::class,
        AlarmIndexEntity::class,
        TaskSyncMetaEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun folderDao(): FolderDao
    abstract fun listDao(): ListDao
    abstract fun taskDao(): TaskDao
    abstract fun taskOrderDao(): TaskOrderDao
    abstract fun taskSyncMetaDao(): TaskSyncMetaDao
    abstract fun alarmIndexDao(): AlarmIndexDao
    abstract fun subtaskDao(): SubtaskDao
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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN remindOffsetMinutes INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS alarm_index (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskId INTEGER NOT NULL, nextFireAt INTEGER NOT NULL, rruleHash TEXT NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_alarm_index_taskId ON alarm_index(taskId)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS subtasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, parentTaskId INTEGER NOT NULL, title TEXT NOT NULL, done INTEGER NOT NULL, dueAt INTEGER, orderInParent INTEGER NOT NULL, startAt INTEGER, durationMinutes INTEGER, FOREIGN KEY(parentTaskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_parentTaskId ON subtasks(parentTaskId)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS task_sync_meta (taskId INTEGER NOT NULL PRIMARY KEY, remoteId TEXT, etag TEXT, needsPush INTEGER NOT NULL DEFAULT 0, lastSyncedAt INTEGER, lastPulledAt INTEGER, lastPushedAt INTEGER, FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_sync_meta_remoteId ON task_sync_meta(remoteId)")
    }
}
