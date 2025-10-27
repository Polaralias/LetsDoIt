package com.polaralias.letsdoit.ai.provider

import com.polaralias.letsdoit.accent.OpenAiImagesProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiImagesAiProvider @Inject constructor(
    private val delegate: OpenAiImagesProvider
) : AiImageProvider {
    override suspend fun generateStickers(prompt: String, variants: Int, size: String): List<ByteArray> {
        return delegate.generate(prompt, variants, size)
    }
}
