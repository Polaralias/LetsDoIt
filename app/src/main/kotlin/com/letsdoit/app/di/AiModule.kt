package com.letsdoit.app.di

import com.letsdoit.app.ai.provider.AiImageProvider
import com.letsdoit.app.ai.provider.AiTextProvider
import com.letsdoit.app.ai.provider.GeminiImagesProvider
import com.letsdoit.app.ai.provider.GeminiTextProvider
import com.letsdoit.app.ai.provider.OpenAiImagesAiProvider
import com.letsdoit.app.ai.provider.OpenAiTextProvider
import com.letsdoit.app.ai.settings.AiImageProviderId
import com.letsdoit.app.ai.settings.AiTextProviderId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideTextProviders(
        openAi: OpenAiTextProvider,
        gemini: GeminiTextProvider
    ): Map<AiTextProviderId, AiTextProvider> {
        return mapOf(
            AiTextProviderId.openai to openAi,
            AiTextProviderId.gemini to gemini
        )
    }

    @Provides
    @Singleton
    fun provideImageProviders(
        openAi: OpenAiImagesAiProvider,
        gemini: GeminiImagesProvider
    ): Map<AiImageProviderId, AiImageProvider> {
        return mapOf(
            AiImageProviderId.openai to openAi,
            AiImageProviderId.gemini to gemini
        )
    }
}
