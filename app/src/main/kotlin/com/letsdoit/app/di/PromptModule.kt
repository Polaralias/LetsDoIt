package com.letsdoit.app.di

import com.letsdoit.app.ai.prompts.AssetPromptLoader
import com.letsdoit.app.ai.prompts.PromptLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PromptModule {
    @Binds
    @Singleton
    abstract fun bindPromptLoader(loader: AssetPromptLoader): PromptLoader
}
