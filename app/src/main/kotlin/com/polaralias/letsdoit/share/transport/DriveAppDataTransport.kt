package com.polaralias.letsdoit.share.transport

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DriveAppDataTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : DriveTransport {
    override suspend fun send(shareId: String, payload: ByteArray) {
        withContext(Dispatchers.IO) {
            val dir = ensureDirectory(shareId)
            val file = File(dir, "${System.currentTimeMillis()}-${UUID.randomUUID()}.bin")
            file.writeBytes(payload)
        }
    }

    override suspend fun receive(shareId: String): List<ByteArray> {
        return withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, basePath(shareId))
            if (!dir.exists()) return@withContext emptyList()
            dir.listFiles()?.sortedBy { it.name }?.map { it.readBytes() } ?: emptyList()
        }
    }

    private fun ensureDirectory(shareId: String): File {
        val dir = File(context.filesDir, basePath(shareId))
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun basePath(shareId: String): String = "drive_appdata/$shareId"
}
