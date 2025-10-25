package com.letsdoit.app.backup

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryDriveBackupClientTest {
    @Test
    fun uploadDownloadAndList() = runTest {
        val client = InMemoryDriveBackupClient()
        val payload = byteArrayOf(1, 2, 3)
        val info = client.upload("file.bin", payload)
        assertEquals("file.bin", info.name)
        assertEquals(3L, info.sizeBytes)
        val listed = client.listBackups()
        assertEquals(1, listed.size)
        assertEquals(info.id, listed.first().id)
        val downloaded = client.download(info.id)
        assertArrayEquals(payload, downloaded)
    }

    @Test
    fun pruneRemovesOldest() = runTest {
        val client = InMemoryDriveBackupClient()
        repeat(12) { index ->
            client.upload("file$index.bin", ByteArray(index + 1))
        }
        prune(client, 10)
        val remaining = client.listBackups()
        assertEquals(10, remaining.size)
        val names = remaining.map { it.name }
        assertFalse(names.contains("file0.bin"))
        assertFalse(names.contains("file1.bin"))
        assertTrue(names.contains("file11.bin"))
    }

    private suspend fun prune(client: DriveBackupClient, limit: Int) {
        client.listBackups().sortedByDescending { it.createdAt }.drop(limit).forEach { backup ->
            client.delete(backup.id)
        }
    }

    private class InMemoryDriveBackupClient : DriveBackupClient {
        private var counter = 0
        private val entries = mutableListOf<BackupInfo>()
        private val payloads = mutableMapOf<String, ByteArray>()

        override suspend fun listBackups(): List<BackupInfo> {
            return entries.sortedByDescending { it.createdAt }
        }

        override suspend fun download(id: String): ByteArray {
            return payloads[id]?.clone() ?: error("Missing backup")
        }

        override suspend fun upload(name: String, payload: ByteArray): BackupInfo {
            val id = "id-${counter++}"
            val info = BackupInfo(id = id, name = name, createdAt = Instant.now().plusMillis(counter.toLong()), sizeBytes = payload.size.toLong())
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
