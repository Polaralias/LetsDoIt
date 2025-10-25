package com.letsdoit.app.accent

import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAccentImageProvider @Inject constructor() : AccentImageProvider {
    override val id: String = OpenAiImagesProvider.ID

    override suspend fun generate(prompt: String, variants: Int, size: String): List<ByteArray> {
        val image = Base64.getDecoder().decode(SAMPLE_PNG)
        return List(variants) { image }
    }

    companion object {
        private const val SAMPLE_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII="
    }
}
