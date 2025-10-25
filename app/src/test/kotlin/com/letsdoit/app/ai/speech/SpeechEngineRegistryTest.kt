package com.letsdoit.app.ai.speech

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeEngine(private val langs: List<Locale>) : SttEngine {
    override fun supported(): Boolean = true
    override fun languages(): List<Locale> = langs
    override suspend fun transcribeLive(
        mic: AudioSource,
        lang: Locale,
        offlinePreferred: Boolean,
        diarise: Boolean,
        onPartial: (TranscriptChunk) -> Unit
    ): TranscriptResult = TranscriptResult("", emptyList(), null, "fake", lang.toLanguageTag())

    override suspend fun transcribeFile(
        uri: android.net.Uri,
        lang: Locale,
        diarise: Boolean,
        onPartial: (TranscriptChunk) -> Unit
    ): TranscriptResult = TranscriptResult("", emptyList(), null, "fake", lang.toLanguageTag())
}

class SpeechEngineRegistryTest {
    @Test
    fun returnsLanguagesForEngine() {
        val locale = Locale.UK
        val registry = SpeechEngineRegistry(mapOf(SpeechEngineId.Local to FakeEngine(listOf(locale))))
        assertEquals(listOf(locale), registry.languages(SpeechEngineId.Local))
        assertNull(registry.engine(SpeechEngineId.GoogleCloud))
    }
}
