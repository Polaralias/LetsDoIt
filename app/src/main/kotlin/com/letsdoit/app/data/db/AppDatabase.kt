package com.letsdoit.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.CrdtEventDao
import com.letsdoit.app.data.db.dao.EventAckDao
import com.letsdoit.app.data.db.dao.FolderDao
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.SubtaskDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.dao.TaskSyncMetaDao
import com.letsdoit.app.data.db.dao.SharedListDao
import com.letsdoit.app.data.db.dao.TranscriptSessionDao
import com.letsdoit.app.data.db.entities.FolderEntity
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.SubtaskEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskOrderEntity
import com.letsdoit.app.data.db.entities.TaskSyncMetaEntity
import com.letsdoit.app.data.db.entities.AlarmIndexEntity
import com.letsdoit.app.data.db.entities.CrdtEventEntity
import com.letsdoit.app.data.db.entities.EventAckEntity
import com.letsdoit.app.data.db.entities.SubtaskFtsEntity
import com.letsdoit.app.data.db.entities.TaskFtsEntity
import com.letsdoit.app.data.db.entities.TranscriptSessionEntity
import com.letsdoit.app.data.db.entities.SharedListEntity

@Database(
    entities = [
        SpaceEntity::class,
        FolderEntity::class,
        ListEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        TaskOrderEntity::class,
        AlarmIndexEntity::class,
        TaskSyncMetaEntity::class,
        TaskFtsEntity::class,
        SubtaskFtsEntity::class,
        TranscriptSessionEntity::class,
        SharedListEntity::class,
        CrdtEventEntity::class,
        EventAckEntity::class
    ],
    version = 9,
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
    abstract fun transcriptSessionDao(): TranscriptSessionDao
    abstract fun sharedListDao(): SharedListDao
    abstract fun crdtEventDao(): CrdtEventDao
    abstract fun eventAckDao(): EventAckDao
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
            "CREATE TABLE IF NOT EXISTS task_sync_meta (taskId INTEGER NOT NULL PRIMARY KEY, remoteId TEXT, etag TEXT, remoteUpdatedAt INTEGER, needsPush INTEGER NOT NULL DEFAULT 0, lastSyncedAt INTEGER, lastPulledAt INTEGER, lastPushedAt INTEGER, FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_sync_meta_remoteId ON task_sync_meta(remoteId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS tasks_fts USING FTS4(title, notes, content='tasks', content_rowid='id')"
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS tasks_fts_insert AFTER INSERT ON tasks BEGIN INSERT INTO tasks_fts(rowid, title, notes) VALUES (new.id, new.title, new.notes); END"
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS tasks_fts_delete AFTER DELETE ON tasks BEGIN INSERT INTO tasks_fts(tasks_fts, rowid, title, notes) VALUES('delete', old.id, old.title, old.notes); END"
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS tasks_fts_update AFTER UPDATE ON tasks BEGIN INSERT INTO tasks_fts(tasks_fts, rowid, title, notes) VALUES('delete', old.id, old.title, old.notes); INSERT INTO tasks_fts(rowid, title, notes) VALUES (new.id, new.title, new.notes); END"
        )
        db.execSQL("INSERT INTO tasks_fts(rowid, title, notes) SELECT id, title, notes FROM tasks")
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS subtasks_fts USING FTS4(title, content='subtasks', content_rowid='id')"
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS subtasks_fts_insert AFTER INSERT ON subtasks BEGIN INSERT INTO subtasks_fts(rowid, title) VALUES (new.id, new.title); END"
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS subtasks_fts_delete AFTER DELETE ON subtasks BEGIN INSERT INTO subtasks_fts(subtasks_fts, rowid, title) VALUES('delete', old.id, old.title); END"
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS subtasks_fts_update AFTER UPDATE ON subtasks BEGIN INSERT INTO subtasks_fts(subtasks_fts, rowid, title) VALUES('delete', old.id, old.title); INSERT INTO subtasks_fts(rowid, title) VALUES (new.id, new.title); END"
        )
        db.execSQL("INSERT INTO subtasks_fts(rowid, title) SELECT id, title FROM subtasks")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS transcript_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, source TEXT NOT NULL, engine TEXT NOT NULL, langTag TEXT NOT NULL, audioPath TEXT NOT NULL, textPath TEXT, durationMs INTEGER)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transcript_sessions_createdAt ON transcript_sessions(createdAt DESC)"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task_sync_meta ADD COLUMN remoteUpdatedAt INTEGER")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS shared_lists (shareId TEXT NOT NULL PRIMARY KEY, listId INTEGER NOT NULL, encKey BLOB NOT NULL, transport TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(listId) REFERENCES lists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shared_lists_listId ON shared_lists(listId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS crdt_events (id TEXT NOT NULL PRIMARY KEY, listId INTEGER NOT NULL, authorDeviceId TEXT NOT NULL, lamport INTEGER NOT NULL, timestamp INTEGER NOT NULL, type TEXT NOT NULL, payloadJson TEXT NOT NULL, applied INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(listId) REFERENCES lists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crdt_events_listId_lamport ON crdt_events(listId, lamport, authorDeviceId, id)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS event_acks (id TEXT NOT NULL PRIMARY KEY, listId INTEGER NOT NULL, acknowledgedAt INTEGER NOT NULL, FOREIGN KEY(listId) REFERENCES lists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_event_acks_listId ON event_acks(listId)")
    }
}
