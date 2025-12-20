package com.polaralias.letsdoit.core.di

import com.polaralias.letsdoit.domain.ai.SuggestionEngine
import com.polaralias.letsdoit.domain.ai.SuggestionEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindSuggestionEngine(
        suggestionEngineImpl: SuggestionEngineImpl
    ): SuggestionEngine
}
