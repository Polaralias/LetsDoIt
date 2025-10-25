package com.letsdoit.app.ai.provider

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiImagesProvider @Inject constructor() : AiImageProvider {
    override suspend fun generateStickers(prompt: String, variants: Int, size: String): List<ByteArray> {
        throw UnsupportedOperationException("Gemini image generation is not yet supported")
    }
}
