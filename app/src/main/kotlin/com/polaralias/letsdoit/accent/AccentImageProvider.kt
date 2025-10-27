package com.polaralias.letsdoit.accent

interface AccentImageProvider {
    val id: String
    suspend fun generate(prompt: String, variants: Int, size: String): List<ByteArray>
}
