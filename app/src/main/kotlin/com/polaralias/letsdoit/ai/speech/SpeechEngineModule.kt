package com.polaralias.letsdoit.ai.speech

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SpeechEngineModule {
    @Provides
    @Singleton
    fun provideSpeechEngines(
        localEngine: LocalSpeechRecognizerEngine
    ): Map<SpeechEngineId, @JvmSuppressWildcards SttEngine> {
        return mapOf(SpeechEngineId.Local to localEngine)
    }
}
