package com.polaralias.letsdoit.ai.speech

import dagger.MapKey
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@MapKey
annotation class SpeechEngineKey(val value: SpeechEngineId)

@Singleton
class SpeechEngineRegistry @Inject constructor(
    private val engines: Map<SpeechEngineId, @JvmSuppressWildcards SttEngine>
) {
    fun engine(id: SpeechEngineId): SttEngine? = engines[id]

    fun defaultEngine(): SttEngine? = engines[SpeechEngineId.Local]

    fun languages(id: SpeechEngineId): List<Locale> {
        return engines[id]?.languages().orEmpty()
    }
}
