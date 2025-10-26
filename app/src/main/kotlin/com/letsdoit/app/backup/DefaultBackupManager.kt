package com.letsdoit.app.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room.withTransaction
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.FolderDao
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.SubtaskDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.dao.TaskSyncMetaDao
import com.letsdoit.app.data.db.entities.AlarmIndexEntity
import com.letsdoit.app.data.db.entities.FolderEntity
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.SubtaskEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskOrderEntity
import com.letsdoit.app.data.db.entities.TaskSyncMetaEntity
import com.squareup.moshi.Moshi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class DefaultBackupManager @Inject constructor(
    private val database: AppDatabase,
    private val spaceDao: SpaceDao,
    private val folderDao: FolderDao,
    private val listDao: ListDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val taskOrderDao: TaskOrderDao,
    private val alarmIndexDao: AlarmIndexDao,
    private val taskSyncMetaDao: TaskSyncMetaDao,
    private val dataStore: DataStore<Preferences>,
    private val driveClient: DriveBackupClient,
    private val crypto: BackupCrypto,
    private val statusRepository: BackupStatusRepository,
    moshi: Moshi,
    private val clock: Clock
) : BackupManager {
    private val manifestAdapter = moshi.adapter(BackupManifest::class.java)
    private val snapshotAdapter = moshi.adapter(BackupSnapshot::class.java)
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        .withLocale(Locale.UK)
        .withZone(clock.zone)

    override suspend fun backupNow(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now(clock)
            val snapshot = createSnapshot()
            val manifest = BackupManifest(schemaVersion = 1, createdAt = now.toEpochMilli())
            val zip = pack(manifest, snapshot)
            val encrypted = crypto.encrypt(zip)
            val name = "letsdoit/backups/backup_${formatter.format(now)}.bin"
            val info = driveClient.upload(name, encrypted)
            prune()
            statusRepository.recordSuccess(now.toEpochMilli())
            BackupResult.Success(info)
        } catch (error: DriveAuthException) {
            statusRepository.recordError(BackupError.AuthRequired, error.message, Instant.now(clock).toEpochMilli())
            BackupResult.Failure(BackupError.AuthRequired, error.message)
        } catch (error: DriveClientException) {
            statusRepository.recordError(BackupError.Remote, error.message, Instant.now(clock).toEpochMilli())
            BackupResult.Failure(BackupError.Remote, error.message)
        } catch (error: GeneralSecurityException) {
            statusRepository.recordError(BackupError.Crypto, error.message, Instant.now(clock).toEpochMilli())
            BackupResult.Failure(BackupError.Crypto, error.message)
        } catch (error: IllegalStateException) {
            statusRepository.recordError(BackupError.Snapshot, error.message, Instant.now(clock).toEpochMilli())
            BackupResult.Failure(BackupError.Snapshot, error.message)
        } catch (error: IOException) {
            statusRepository.recordError(BackupError.Remote, error.message, Instant.now(clock).toEpochMilli())
            BackupResult.Failure(BackupError.Remote, error.message)
        } catch (error: Exception) {
            statusRepository.recordError(BackupError.Unknown, error.message, Instant.now(clock).toEpochMilli())
            BackupResult.Failure(BackupError.Unknown, error.message)
        }
    }

    override suspend fun restoreLatest(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val backups = driveClient.listBackups().sortedByDescending { it.createdAt }
            val latest = backups.firstOrNull() ?: return@withContext RestoreResult.Failure(BackupError.NotFound, null)
            val payload = driveClient.download(latest.id)
            val plain = crypto.decrypt(payload)
            val snapshot = unpack(plain)
            applySnapshot(snapshot)
            statusRepository.recordSuccess(Instant.now(clock).toEpochMilli())
            RestoreResult.Success
        } catch (error: DriveAuthException) {
            statusRepository.recordError(BackupError.AuthRequired, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.AuthRequired, error.message)
        } catch (error: DriveClientException) {
            statusRepository.recordError(BackupError.Remote, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Remote, error.message)
        } catch (error: GeneralSecurityException) {
            statusRepository.recordError(BackupError.Crypto, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Crypto, error.message)
        } catch (error: IOException) {
            statusRepository.recordError(BackupError.Remote, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Remote, error.message)
        } catch (error: IllegalStateException) {
            statusRepository.recordError(BackupError.Snapshot, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Snapshot, error.message)
        } catch (error: Exception) {
            statusRepository.recordError(BackupError.Unknown, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Unknown, error.message)
        }
    }

    override suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        driveClient.listBackups().sortedByDescending { it.createdAt }
    }

    private suspend fun createSnapshot(): BackupSnapshot {
        val spaces = spaceDao.listAll()
        val folders = folderDao.listAll()
        val lists = listDao.listAll()
        val tasks = taskDao.listAll()
        val subtasks = subtaskDao.listAll()
        val orders = taskOrderDao.listAll()
        val alarms = alarmIndexDao.listAll()
        val syncMeta = taskSyncMetaDao.listAll()
        val preferencesSnapshot = readPreferences()
        val databaseSnapshot = DatabaseSnapshot(
            spaces = spaces.map { it.toRecord() },
            folders = folders.map { it.toRecord() },
            lists = lists.map { it.toRecord() },
            tasks = tasks.map { it.toRecord() },
            subtasks = subtasks.map { it.toRecord() },
            orders = orders.map { it.toRecord() },
            alarms = alarms.map { it.toRecord() },
            syncMeta = syncMeta.map { it.toRecord() }
        )
        return BackupSnapshot(database = databaseSnapshot, preferences = preferencesSnapshot)
    }

    private suspend fun applySnapshot(snapshot: BackupSnapshot) {
        database.withTransaction {
            spaceDao.clear()
            folderDao.clear()
            listDao.clear()
            taskDao.clear()
            subtaskDao.clear()
            taskOrderDao.clear()
            alarmIndexDao.clear()
            taskSyncMetaDao.clear()
            spaceDao.insert(snapshot.database.spaces.map { it.toEntity() })
            folderDao.insert(snapshot.database.folders.map { it.toEntity() })
            listDao.insert(snapshot.database.lists.map { it.toEntity() })
            taskDao.insert(snapshot.database.tasks.map { it.toEntity() })
            subtaskDao.insert(snapshot.database.subtasks.map { it.toEntity() })
            taskOrderDao.insert(snapshot.database.orders.map { it.toEntity() })
            alarmIndexDao.insert(snapshot.database.alarms.map { it.toEntity() })
            taskSyncMetaDao.insert(snapshot.database.syncMeta.map { it.toEntity() })
        }
        writePreferences(snapshot.preferences)
    }

    private suspend fun readPreferences(): PreferencesSnapshot {
        val preferences = dataStore.data.first()
        val entries = preferences.asMap().map { (key, value) ->
            val name = key.name
            when (value) {
                is String -> PreferenceEntry(name, PreferenceValueType.String, value)
                is Set<*> -> PreferenceEntry(name, PreferenceValueType.StringSet, value)
                is Int -> PreferenceEntry(name, PreferenceValueType.Int, value)
                is Long -> PreferenceEntry(name, PreferenceValueType.Long, value)
                is Boolean -> PreferenceEntry(name, PreferenceValueType.Boolean, value)
                is Float -> PreferenceEntry(name, PreferenceValueType.Float, value)
                else -> null
            }
        }.filterNotNull()
        return PreferencesSnapshot(entries)
    }

    private suspend fun writePreferences(snapshot: PreferencesSnapshot) {
        dataStore.edit { preferences ->
            preferences.clear()
            snapshot.entries.forEach { entry ->
                when (entry.type) {
                    PreferenceValueType.String -> preferences[stringPreferencesKey(entry.name)] = entry.value as String
                    PreferenceValueType.StringSet -> preferences[stringSetPreferencesKey(entry.name)] = entry.value as Set<String>
                    PreferenceValueType.Int -> preferences[intPreferencesKey(entry.name)] = entry.value as Int
                    PreferenceValueType.Long -> preferences[longPreferencesKey(entry.name)] = entry.value as Long
                    PreferenceValueType.Boolean -> preferences[booleanPreferencesKey(entry.name)] = entry.value as Boolean
                    PreferenceValueType.Float -> preferences[doublePreferencesKey(entry.name)] = (entry.value as Double)
                }
            }
        }
    }

    private fun pack(manifest: BackupManifest, snapshot: BackupSnapshot): ByteArray {
        val manifestJson = manifestAdapter.toJson(manifest)
        val snapshotJson = snapshotAdapter.toJson(snapshot)
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("snapshot.json"))
            zip.write(snapshotJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return buffer.toByteArray()
    }

    private fun unpack(payload: ByteArray): BackupSnapshot {
        val input = ZipInputStream(ByteArrayInputStream(payload))
        var manifest: BackupManifest? = null
        var snapshot: BackupSnapshot? = null
        var entry = input.nextEntry
        while (entry != null) {
            val content = input.readBytes()
            when (entry.name) {
                "manifest.json" -> manifest = manifestAdapter.fromJson(content.toString(Charsets.UTF_8))
                "snapshot.json" -> snapshot = snapshotAdapter.fromJson(content.toString(Charsets.UTF_8))
            }
            entry = input.nextEntry
        }
        val parsedManifest = manifest ?: throw IllegalStateException("Missing manifest")
        if (parsedManifest.schemaVersion != 1) {
            throw IllegalStateException("Unsupported schema")
        }
        val parsedSnapshot = snapshot ?: throw IllegalStateException("Missing snapshot")
        return parsedSnapshot
    }

    private suspend fun prune() {
        val backups = driveClient.listBackups().sortedByDescending { it.createdAt }
        if (backups.size <= 10) {
            return
        }
        backups.drop(10).forEach { backup ->
            runCatching { driveClient.delete(backup.id) }
        }
    }
}

private fun SpaceEntity.toRecord(): SpaceRecord = SpaceRecord(
    id = id,
    remoteId = remoteId,
    name = name,
    isShared = isShared,
    shareId = shareId,
    ownerDeviceId = ownerDeviceId,
    encKeySpace = encKeySpace?.let { Base64.getEncoder().encodeToString(it) }
)

private fun FolderEntity.toRecord(): FolderRecord = FolderRecord(id = id, spaceId = spaceId, remoteId = remoteId, name = name)

private fun ListEntity.toRecord(): ListRecord = ListRecord(id = id, spaceId = spaceId, folderId = folderId, remoteId = remoteId, name = name)

private fun TaskEntity.toRecord(): TaskRecord = TaskRecord(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    dueAt = dueAt?.toEpochMilli(),
    repeatRule = repeatRule,
    remindOffsetMinutes = remindOffsetMinutes,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    completed = completed,
    priority = priority,
    orderInList = orderInList,
    startAt = startAt,
    durationMinutes = durationMinutes,
    calendarEventId = calendarEventId,
    column = column
)

private fun SubtaskEntity.toRecord(): SubtaskRecord = SubtaskRecord(
    id = id,
    parentTaskId = parentTaskId,
    title = title,
    done = done,
    dueAt = dueAt,
    orderInParent = orderInParent,
    startAt = startAt,
    durationMinutes = durationMinutes
)

private fun TaskOrderEntity.toRecord(): TaskOrderRecord = TaskOrderRecord(id = id, taskId = taskId, column = column, orderInColumn = orderInColumn)

private fun AlarmIndexEntity.toRecord(): AlarmIndexRecord = AlarmIndexRecord(id = id, taskId = taskId, nextFireAt = nextFireAt, rruleHash = rruleHash)

private fun TaskSyncMetaEntity.toRecord(): TaskSyncMetaRecord = TaskSyncMetaRecord(
    taskId = taskId,
    remoteId = remoteId,
    etag = etag,
    remoteUpdatedAt = remoteUpdatedAt?.toEpochMilli(),
    needsPush = needsPush,
    lastSyncedAt = lastSyncedAt?.toEpochMilli(),
    lastPulledAt = lastPulledAt?.toEpochMilli(),
    lastPushedAt = lastPushedAt?.toEpochMilli()
)

private fun SpaceRecord.toEntity(): SpaceEntity = SpaceEntity(
    id = id,
    remoteId = remoteId,
    name = name,
    isShared = isShared,
    shareId = shareId,
    ownerDeviceId = ownerDeviceId,
    encKeySpace = encKeySpace?.let { Base64.getDecoder().decode(it) }
)

private fun FolderRecord.toEntity(): FolderEntity = FolderEntity(id = id, spaceId = spaceId, remoteId = remoteId, name = name)

private fun ListRecord.toEntity(): ListEntity = ListEntity(id = id, spaceId = spaceId, folderId = folderId, remoteId = remoteId, name = name)

private fun TaskRecord.toEntity(): TaskEntity = TaskEntity(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    dueAt = dueAt?.let { Instant.ofEpochMilli(it) },
    repeatRule = repeatRule,
    remindOffsetMinutes = remindOffsetMinutes,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    completed = completed,
    priority = priority,
    orderInList = orderInList,
    startAt = startAt,
    durationMinutes = durationMinutes,
    calendarEventId = calendarEventId,
    column = column
)

private fun SubtaskRecord.toEntity(): SubtaskEntity = SubtaskEntity(
    id = id,
    parentTaskId = parentTaskId,
    title = title,
    done = done,
    dueAt = dueAt,
    orderInParent = orderInParent,
    startAt = startAt,
    durationMinutes = durationMinutes
)
