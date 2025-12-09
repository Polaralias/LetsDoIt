package com.letsdoit.app.core.di

import com.letsdoit.app.domain.ai.SuggestionEngine
import com.letsdoit.app.domain.ai.SuggestionEngineImpl
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
