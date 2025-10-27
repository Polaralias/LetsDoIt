package com.polaralias.letsdoit.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.db.entities.AlarmIndexEntity
import com.polaralias.letsdoit.data.db.entities.FolderEntity
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import com.polaralias.letsdoit.data.db.entities.SubtaskEntity
import com.polaralias.letsdoit.data.db.entities.TaskEntity
import com.polaralias.letsdoit.data.db.entities.TaskOrderEntity
import com.polaralias.letsdoit.data.db.entities.TaskSyncMetaEntity
import com.squareup.moshi.Moshi
import java.io.File
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BackupManagerInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: TestScope
    private lateinit var driveClient: TestDriveBackupClient
    private lateinit var backupManager: DefaultBackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = TestScope(StandardTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            context.preferencesDataStoreFile("backup-test")
        }
        driveClient = TestDriveBackupClient()
        val statusRepository = DefaultBackupStatusRepository(dataStore)
        val crypto = BackupCrypto(InMemoryBackupKeyStore())
        val moshi = Moshi.Builder().build()
        backupManager = DefaultBackupManager(
            database = database,
            spaceDao = database.spaceDao(),
            folderDao = database.folderDao(),
            listDao = database.listDao(),
            taskDao = database.taskDao(),
            subtaskDao = database.subtaskDao(),
            taskOrderDao = database.taskOrderDao(),
            alarmIndexDao = database.alarmIndexDao(),
            taskSyncMetaDao = database.taskSyncMetaDao(),
            dataStore = dataStore,
            driveClient = driveClient,
            crypto = crypto,
            statusRepository = statusRepository,
            moshi = moshi,
            clock = Clock.systemUTC()
        )
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
        File(context.filesDir, "datastore/backup-test.preferences_pb").delete()
    }

    @Test
    fun backupAndRestoreRoundTrip() = scope.runTest {
        val space = SpaceEntity(id = 1, remoteId = "remote-space", name = "Home")
        val folder = FolderEntity(id = 1, spaceId = 1, remoteId = "remote-folder", name = "Focus")
        val list = ListEntity(id = 1, spaceId = 1, folderId = 1, remoteId = "remote-list", name = "Today")
        val task = TaskEntity(
            id = 1,
            listId = 1,
            title = "Plan",
            notes = "Notes",
            dueAt = Instant.ofEpochMilli(5000),
            repeatRule = null,
            remindOffsetMinutes = 10,
            createdAt = Instant.ofEpochMilli(1000),
            updatedAt = Instant.ofEpochMilli(2000),
            completed = false,
            priority = 1,
            orderInList = 0,
            startAt = 6000,
            durationMinutes = 45,
            calendarEventId = 7,
            column = "To do"
        )
        val subtask = SubtaskEntity(id = 1, parentTaskId = 1, title = "Outline", done = false, dueAt = 5500, orderInParent = 0, startAt = 5200, durationMinutes = 20)
        val order = TaskOrderEntity(id = 1, taskId = 1, column = "To do", orderInColumn = 0)
        val alarm = AlarmIndexEntity(id = 1, taskId = 1, nextFireAt = 4800, rruleHash = "hash")
        val syncMeta = TaskSyncMetaEntity(
            taskId = 1,
            remoteId = "remote-task",
            etag = "etag",
            needsPush = false,
            lastSyncedAt = Instant.ofEpochMilli(2500),
            lastPulledAt = Instant.ofEpochMilli(2600),
            lastPushedAt = Instant.ofEpochMilli(2700)
        )

        database.spaceDao().upsert(space)
        database.folderDao().upsert(folder)
        database.listDao().upsert(list)
        database.taskDao().upsert(task)
        database.subtaskDao().upsert(subtask)
        database.taskOrderDao().upsert(order)
        database.alarmIndexDao().upsert(alarm)
        database.taskSyncMetaDao().upsert(syncMeta)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("view_theme_preset")] = "sunrise"
            preferences[booleanPreferencesKey("view_show_completed")] = true
        }

        val backupResult = backupManager.backupNow()
        assertTrue(backupResult is BackupResult.Success)
        assertEquals(1, driveClient.listBackups().size)

        database.clearAllTables()
        dataStore.edit { it.clear() }

        val restoreResult = backupManager.restoreLatest()
        assertTrue(restoreResult is RestoreResult.Success)

        assertEquals(listOf(space), database.spaceDao().listAll())
        assertEquals(listOf(folder), database.folderDao().listAll())
        assertEquals(listOf(list), database.listDao().listAll())
        assertEquals(listOf(task), database.taskDao().listAll())
        assertEquals(listOf(subtask), database.subtaskDao().listAll())
        assertEquals(listOf(order), database.taskOrderDao().listAll())
        assertEquals(listOf(alarm), database.alarmIndexDao().listAll())
        assertEquals(listOf(syncMeta), database.taskSyncMetaDao().listAll())

        val restoredPreferences = dataStore.data.first()
        assertEquals("sunrise", restoredPreferences[stringPreferencesKey("view_theme_preset")])
        assertEquals(true, restoredPreferences[booleanPreferencesKey("view_show_completed")])
    }

    private class InMemoryBackupKeyStore : BackupKeyStore {
        private var value: String? = null
        override fun readKey(): String? = value
        override fun writeKey(value: String) {
            this.value = value
        }
    }

    private class TestDriveBackupClient : DriveBackupClient {
        private var counter = 0
        private val entries = mutableListOf<BackupInfo>()
        private val payloads = mutableMapOf<String, ByteArray>()

        override suspend fun listBackups(): List<BackupInfo> = entries.sortedByDescending { it.createdAt }

        override suspend fun download(id: String): ByteArray = payloads[id]?.clone() ?: error("Missing backup")

        override suspend fun upload(name: String, payload: ByteArray): BackupInfo {
            val id = "id-${counter++}"
            val info = BackupInfo(id = id, name = name, createdAt = Instant.ofEpochMilli(1000L + counter), sizeBytes = payload.size.toLong())
            entries += info
            payloads[id] = payload.clone()
            return info
        }

        override suspend fun delete(id: String) {
            entries.removeAll { it.id == id }
            payloads.remove(id)
        }
    }
}
