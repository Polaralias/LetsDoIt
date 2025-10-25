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
        } catch (error: IllegalStateException) {
            statusRepository.recordError(BackupError.Snapshot, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Snapshot, error.message)
        } catch (error: IOException) {
            statusRepository.recordError(BackupError.Remote, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Remote, error.message)
        } catch (error: Exception) {
            statusRepository.recordError(BackupError.Unknown, error.message, Instant.now(clock).toEpochMilli())
            RestoreResult.Failure(BackupError.Unknown, error.message)
        }
    }

    override suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        driveClient.listBackups().sortedByDescending { it.createdAt }
    }

    private suspend fun createSnapshot(): BackupSnapshot {
        val spaces = spaceDao.listAll().map { it.toRecord() }
        val folders = folderDao.listAll().map { it.toRecord() }
        val lists = listDao.listAll().map { it.toRecord() }
        val tasks = taskDao.listAll().map { it.toRecord() }
        val subtasks = subtaskDao.listAll().map { it.toRecord() }
        val orders = taskOrderDao.listAll().map { it.toRecord() }
        val alarms = alarmIndexDao.listAll().map { it.toRecord() }
        val syncMeta = taskSyncMetaDao.listAll().map { it.toRecord() }
        val preferences = readPreferences()
        return BackupSnapshot(
            database = DatabaseSnapshot(
                spaces = spaces,
                folders = folders,
                lists = lists,
                tasks = tasks,
                subtasks = subtasks,
                orders = orders,
                alarms = alarms,
                syncMeta = syncMeta
            ),
            preferences = preferences
        )
    }

    private suspend fun readPreferences(): PreferencesSnapshot {
        val preferences = dataStore.data.first()
        val entries = preferences.asMap().mapNotNull { (key, value) ->
            when (value) {
                is String -> PreferenceEntry(key.name, PreferenceValueType.String, stringValue = value)
                is Set<*> -> {
                    val values = value.filterIsInstance<String>()
                    PreferenceEntry(key.name, PreferenceValueType.StringSet, stringSetValue = values)
                }
                is Int -> PreferenceEntry(key.name, PreferenceValueType.Int, intValue = value)
                is Long -> PreferenceEntry(key.name, PreferenceValueType.Long, longValue = value)
                is Boolean -> PreferenceEntry(key.name, PreferenceValueType.Boolean, booleanValue = value)
                is Double -> PreferenceEntry(key.name, PreferenceValueType.Float, floatValue = value)
                is Float -> PreferenceEntry(key.name, PreferenceValueType.Float, floatValue = value.toDouble())
                else -> null
            }
        }
        return PreferencesSnapshot(entries)
    }

    private suspend fun applySnapshot(snapshot: BackupSnapshot) {
        database.withTransaction {
            database.clearAllTables()
            snapshot.database.spaces.forEach { spaceDao.upsert(it.toEntity()) }
            snapshot.database.folders.forEach { folderDao.upsert(it.toEntity()) }
            snapshot.database.lists.forEach { listDao.upsert(it.toEntity()) }
            snapshot.database.tasks.forEach { taskDao.upsert(it.toEntity()) }
            snapshot.database.subtasks.forEach { subtaskDao.upsert(it.toEntity()) }
            snapshot.database.orders.forEach { taskOrderDao.upsert(it.toEntity()) }
            snapshot.database.alarms.forEach { alarmIndexDao.upsert(it.toEntity()) }
            snapshot.database.syncMeta.forEach { taskSyncMetaDao.upsert(it.toEntity()) }
        }
        dataStore.edit { preferences ->
            preferences.clear()
            snapshot.preferences.entries.forEach { entry ->
                when (entry.type) {
                    PreferenceValueType.String -> entry.stringValue?.let { value ->
                        preferences[stringPreferencesKey(entry.key)] = value
                    }
                    PreferenceValueType.StringSet -> entry.stringSetValue?.let { value ->
                        preferences[stringSetPreferencesKey(entry.key)] = value.toSet()
                    }
                    PreferenceValueType.Int -> entry.intValue?.let { value ->
                        preferences[intPreferencesKey(entry.key)] = value
                    }
                    PreferenceValueType.Long -> entry.longValue?.let { value ->
                        preferences[longPreferencesKey(entry.key)] = value
                    }
                    PreferenceValueType.Boolean -> entry.booleanValue?.let { value ->
                        preferences[booleanPreferencesKey(entry.key)] = value
                    }
                    PreferenceValueType.Float -> entry.floatValue?.let { value ->
                        preferences[doublePreferencesKey(entry.key)] = value
                    }
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
        input.use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val content = zip.readBytes()
                when (entry.name) {
                    "manifest.json" -> manifest = manifestAdapter.fromJson(content.toString(Charsets.UTF_8))
                    "snapshot.json" -> snapshot = snapshotAdapter.fromJson(content.toString(Charsets.UTF_8))
                }
                entry = zip.nextEntry
            }
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

private fun SpaceEntity.toRecord(): SpaceRecord = SpaceRecord(id = id, remoteId = remoteId, name = name)

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

private fun SpaceRecord.toEntity(): SpaceEntity = SpaceEntity(id = id, remoteId = remoteId, name = name)

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

private fun TaskOrderRecord.toEntity(): TaskOrderEntity = TaskOrderEntity(id = id, taskId = taskId, column = column, orderInColumn = orderInColumn)

private fun AlarmIndexRecord.toEntity(): AlarmIndexEntity = AlarmIndexEntity(id = id, taskId = taskId, nextFireAt = nextFireAt, rruleHash = rruleHash)

private fun TaskSyncMetaRecord.toEntity(): TaskSyncMetaEntity = TaskSyncMetaEntity(
    taskId = taskId,
    remoteId = remoteId,
    etag = etag,
    remoteUpdatedAt = remoteUpdatedAt?.let { Instant.ofEpochMilli(it) },
    needsPush = needsPush,
    lastSyncedAt = lastSyncedAt?.let { Instant.ofEpochMilli(it) },
    lastPulledAt = lastPulledAt?.let { Instant.ofEpochMilli(it) },
    lastPushedAt = lastPushedAt?.let { Instant.ofEpochMilli(it) }
)
