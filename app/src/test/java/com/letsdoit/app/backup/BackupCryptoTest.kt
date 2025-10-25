package com.letsdoit.app.backup

import com.squareup.moshi.Moshi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCryptoTest {
    private val keyStore = InMemoryBackupKeyStore()
    private val crypto = BackupCrypto(keyStore)
    private val moshi = Moshi.Builder().build()
    private val manifestAdapter = moshi.adapter(BackupManifest::class.java)
    private val snapshotAdapter = moshi.adapter(BackupSnapshot::class.java)

    @Test
    fun roundTripSnapshot() {
        val manifest = BackupManifest(schemaVersion = 1, createdAt = 1234L)
        val snapshot = BackupSnapshot(
            database = DatabaseSnapshot(
                spaces = listOf(SpaceRecord(id = 1, remoteId = "remote", name = "Home")),
                folders = listOf(FolderRecord(id = 1, spaceId = 1, remoteId = null, name = "Planning")),
                lists = listOf(ListRecord(id = 1, spaceId = 1, folderId = null, remoteId = null, name = "Tasks")),
                tasks = listOf(
                    TaskRecord(
                        id = 1,
                        listId = 1,
                        title = "Task",
                        notes = "Notes",
                        dueAt = 2000L,
                        repeatRule = null,
                        remindOffsetMinutes = 15,
                        createdAt = 1000L,
                        updatedAt = 1500L,
                        completed = false,
                        priority = 2,
                        orderInList = 0,
                        startAt = 4000L,
                        durationMinutes = 60,
                        calendarEventId = 22L,
                        column = "To do"
                    )
                ),
                subtasks = listOf(
                    SubtaskRecord(
                        id = 1,
                        parentTaskId = 1,
                        title = "Sub",
                        done = false,
                        dueAt = 2100L,
                        orderInParent = 0,
                        startAt = 3000L,
                        durationMinutes = 30
                    )
                ),
                orders = listOf(TaskOrderRecord(id = 1, taskId = 1, column = "To do", orderInColumn = 0)),
                alarms = listOf(AlarmIndexRecord(id = 1, taskId = 1, nextFireAt = 2200L, rruleHash = "hash")),
                syncMeta = listOf(
                    TaskSyncMetaRecord(
                        taskId = 1,
                        remoteId = "remote",
                        etag = "etag",
                        remoteUpdatedAt = 1550L,
                        needsPush = false,
                        lastSyncedAt = 1600L,
                        lastPulledAt = 1700L,
                        lastPushedAt = 1800L
                    )
                )
            ),
            preferences = PreferencesSnapshot(
                entries = listOf(
                    PreferenceEntry(key = "theme", type = PreferenceValueType.String, stringValue = "dark"),
                    PreferenceEntry(key = "show_completed", type = PreferenceValueType.Boolean, booleanValue = true),
                    PreferenceEntry(key = "duration", type = PreferenceValueType.Int, intValue = 30)
                )
            )
        )
        val packed = pack(manifest, snapshot)
        val encrypted = crypto.encrypt(packed)
        val decrypted = crypto.decrypt(encrypted)
        val (parsedManifest, parsedSnapshot) = unpack(decrypted)
        assertEquals(manifest, parsedManifest)
        assertEquals(snapshot, parsedSnapshot)
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

    private fun unpack(bytes: ByteArray): Pair<BackupManifest, BackupSnapshot> {
        var manifest: BackupManifest? = null
        var snapshot: BackupSnapshot? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val content = zip.readBytes().toString(Charsets.UTF_8)
                when (entry.name) {
                    "manifest.json" -> manifest = manifestAdapter.fromJson(content)
                    "snapshot.json" -> snapshot = snapshotAdapter.fromJson(content)
                }
                entry = zip.nextEntry
            }
        }
        return Pair(requireNotNull(manifest), requireNotNull(snapshot))
    }

    private class InMemoryBackupKeyStore : BackupKeyStore {
        private var value: String? = null
        override fun readKey(): String? = value
        override fun writeKey(value: String) {
            this.value = value
        }
    }
}
