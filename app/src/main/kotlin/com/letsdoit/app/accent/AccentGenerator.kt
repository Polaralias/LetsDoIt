package com.letsdoit.app.accent

import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AccentGenerator @Inject constructor(
    private val storage: AccentStorage,
    private val providers: Set<AccentImageProvider>,
    private val clock: Clock
) {
    suspend fun generatePack(prompt: String, variants: Int = 6, size: String = "512x512"): AccentPackInfo {
        val provider = providers.firstOrNull() ?: throw AccentGenerationException.ProviderUnavailable
        val cleanedPrompt = prompt.trim()
        if (cleanedPrompt.isEmpty()) {
            throw AccentGenerationException.EmptyPrompt
        }
        val packId = packIdFor(cleanedPrompt, provider.id, size)
        val existing = storage.loadPack(packId)
        if (existing != null) {
            return existing
        }
        val images = withContext(Dispatchers.IO) {
            provider.generate(cleanedPrompt, variants, size)
        }
        if (images.isEmpty()) {
            throw AccentGenerationException.InvalidResponse
        }
        val timestamp = Instant.now(clock)
        val info = AccentPackInfo(
            id = packId,
            name = formatName(cleanedPrompt),
            createdAt = timestamp,
            provider = provider.id,
            prompt = cleanedPrompt,
            count = images.size
        )
        storage.savePack(info, images)
        return info
    }

    companion object {
        fun packIdFor(prompt: String, provider: String, size: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = "$prompt|$provider|$size"
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString(separator = "") { byte ->
                val value = byte.toInt() and 0xFF
                value.toString(16).padStart(2, '0')
            }.take(32)
        }

        private fun formatName(prompt: String): String {
            val normalised = prompt.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
            val capitalised = if (normalised.isNotEmpty()) {
                normalised.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(Locale.UK)
                    } else {
                        char.toString()
                    }
                }
            } else {
                "Accents"
            }
            return if (capitalised.length > 42) {
                capitalised.take(41) + "…"
            } else {
                capitalised
            }
        }
    }
}
