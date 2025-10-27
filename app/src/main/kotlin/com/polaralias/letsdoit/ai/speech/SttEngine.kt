package com.polaralias.letsdoit.ai.speech

import android.net.Uri
import java.util.Locale

data class AudioSource(
    val sampleRate: Int,
    val channelCount: Int
)

data class TranscriptChunk(
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
    val speaker: String?
)

data class TranscriptResult(
    val text: String,
    val chunks: List<TranscriptChunk>,
    val speakers: List<String>?,
    val engine: String,
    val langTag: String
)

interface SttEngine {
    fun supported(): Boolean
    fun languages(): List<Locale>
    suspend fun transcribeLive(
        mic: AudioSource,
        lang: Locale,
        offlinePreferred: Boolean,
        diarise: Boolean,
        onPartial: (TranscriptChunk) -> Unit
    ): TranscriptResult
    suspend fun transcribeFile(
        uri: Uri,
        lang: Locale,
        diarise: Boolean,
        onPartial: (TranscriptChunk) -> Unit
    ): TranscriptResult
}
