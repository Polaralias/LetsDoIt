package com.polaralias.letsdoit.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.accent.AccentPackInfo
import com.polaralias.letsdoit.accent.AccentStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AccentPackDescriptor(val id: String, val label: String, val isCustom: Boolean)

interface AccentManager {
    suspend fun availablePacks(): List<AccentPackDescriptor>
    suspend fun stickerPainter(packId: String, slot: Int): Painter?
    suspend fun overlayPainter(packId: String): Painter?
    suspend fun packInfo(packId: String): AccentPackInfo?
    suspend fun deletePack(packId: String)
}

@Singleton
class LocalAccentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accentStorage: AccentStorage
) : AccentManager {
    private val storageDir: File get() = accentStorage.rootDirectory
    private val stickerCache = LruCache<String, ImageBitmap>(24)
    private val overlayCache = LruCache<String, ImageBitmap>(8)

    override suspend fun availablePacks(): List<AccentPackDescriptor> {
        val resourcePacks = discoverResourcePacks()
        val stored = accentStorage.listPacks().map { info ->
            AccentPackDescriptor(id = info.id, label = info.name, isCustom = true)
        }
        val builtIn = resourcePacks.map { id ->
            val label = id.replace('_', ' ').replaceFirstChar { it.uppercase() }
            AccentPackDescriptor(id = id, label = label, isCustom = false)
        }
        return (builtIn + stored)
            .distinctBy { it.id }
            .sortedWith(compareBy<AccentPackDescriptor> { it.isCustom }.thenBy { it.label.lowercase(Locale.UK) })
    }

    override suspend fun stickerPainter(packId: String, slot: Int): Painter? = withContext(Dispatchers.IO) {
        val key = "sticker:$packId:$slot"
        val cached = stickerCache[key]
        val bitmap = cached ?: loadSticker(packId, slot)?.also { stickerCache.put(key, it) }
        bitmap?.let { BitmapPainter(it) }
    }

    override suspend fun overlayPainter(packId: String): Painter? = withContext(Dispatchers.IO) {
        val key = "overlay:$packId"
        val cached = overlayCache[key]
        val bitmap = cached ?: loadOverlay(packId)?.also { overlayCache.put(key, it) }
        bitmap?.let { BitmapPainter(it) }
    }

    override suspend fun packInfo(packId: String): AccentPackInfo? = accentStorage.loadPack(packId)

    override suspend fun deletePack(packId: String) {
        accentStorage.deletePack(packId)
    }

    private fun discoverResourcePacks(): List<String> {
        val fields = R.drawable::class.java.fields
        return fields.mapNotNull { field ->
            val name = field.name
            if (name.startsWith("accent_")) {
                name.removePrefix("accent_").substringBefore('_')
            } else {
                null
            }
        }.distinct()
    }

    private fun findStickerResource(packId: String, slot: Int): Int? {
        val names = listOf(
            "accent_${packId}_sticker_$slot",
            "accent_${packId}_$slot"
        )
        for (name in names) {
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) return id
        }
        return null
    }

    private fun findOverlayResource(packId: String): Int? {
        val names = listOf(
            "accent_${packId}_overlay",
            "accent_${packId}_card"
        )
        for (name in names) {
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) return id
        }
        return null
    }

    private fun loadSticker(packId: String, slot: Int): ImageBitmap? {
        val resource = findStickerResource(packId, slot)
        if (resource != null) {
            val drawable = ContextCompat.getDrawable(context, resource) ?: return null
            val bitmap = drawable.toBitmap()
            return bitmap.asImageBitmap()
        }
        val file = File(File(storageDir, packId), "sticker_$slot.png")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.path) ?: return null
            return bitmap.asImageBitmap()
        }
        return null
    }

    private fun loadOverlay(packId: String): ImageBitmap? {
        val resource = findOverlayResource(packId)
        if (resource != null) {
            val drawable = ContextCompat.getDrawable(context, resource) ?: return null
            val bitmap = drawable.toBitmap()
            return bitmap.asImageBitmap()
        }
        val file = File(File(storageDir, packId), "overlay.png")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.path) ?: return null
            return bitmap.asImageBitmap()
        }
        return null
    }
}
