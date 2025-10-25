package com.letsdoit.app.ai.speech

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@InstallIn(SingletonComponent::class)
@Module
abstract class SpeechEngineModule {
    @Binds
    @IntoMap
    @SpeechEngineKey(SpeechEngineId.Local)
    abstract fun bindLocalEngine(engine: LocalSpeechRecognizerEngine): SttEngine

}
