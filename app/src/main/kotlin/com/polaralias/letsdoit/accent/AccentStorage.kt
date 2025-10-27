package com.polaralias.letsdoit.accent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class AccentStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    val rootDirectory: File = File(context.filesDir, "accents")

    suspend fun listPacks(): List<AccentPackInfo> = withContext(Dispatchers.IO) {
        if (!rootDirectory.exists()) {
            emptyList()
        } else {
            rootDirectory.listFiles()?.mapNotNull { readMetadata(it) }?.sortedBy { it.name.lowercase(Locale.UK) } ?: emptyList()
        }
    }

    suspend fun loadPack(id: String): AccentPackInfo? = withContext(Dispatchers.IO) {
        val dir = File(rootDirectory, id)
        readMetadata(dir)
    }

    suspend fun savePack(info: AccentPackInfo, images: List<ByteArray>) {
        withContext(Dispatchers.IO) {
            val dir = File(rootDirectory, info.id)
            try {
                if (!rootDirectory.exists()) {
                    rootDirectory.mkdirs()
                }
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
                dir.mkdirs()
                images.forEachIndexed { index, bytes ->
                    val file = File(dir, "sticker_${index + 1}.png")
                    FileOutputStream(file).use { stream ->
                        stream.write(bytes)
                    }
                }
                val metadata = JSONObject()
                metadata.put("id", info.id)
                metadata.put("name", info.name)
                metadata.put("createdAt", info.createdAt.toString())
                metadata.put("provider", info.provider)
                metadata.put("prompt", info.prompt)
                metadata.put("count", info.count)
                val file = File(dir, "pack.json")
                file.writeText(metadata.toString())
            } catch (e: Exception) {
                dir.deleteRecursively()
                throw AccentGenerationException.StorageError(e)
            }
        }
    }

    suspend fun deletePack(id: String) {
        withContext(Dispatchers.IO) {
            val dir = File(rootDirectory, id)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    private fun readMetadata(dir: File): AccentPackInfo? {
        if (!dir.isDirectory) {
            return null
        }
        val file = File(dir, "pack.json")
        if (!file.exists()) {
            return null
        }
        return runCatching {
            val json = JSONObject(file.readText())
            val id = json.optString("id", dir.name)
            val name = json.optString("name", id)
            val createdAt = Instant.parse(json.optString("createdAt"))
            val provider = json.optString("provider")
            val prompt = json.optString("prompt")
            val count = json.optInt("count", dir.listFiles { child -> child.name.startsWith("sticker_") }?.size ?: 0)
            AccentPackInfo(
                id = id,
                name = name,
                createdAt = createdAt,
                provider = provider,
                prompt = prompt,
                count = count
            )
        }.getOrNull()
    }
}
