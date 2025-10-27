package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.ai.provider.AiImageProvider
import com.polaralias.letsdoit.ai.provider.AiTextProvider
import com.polaralias.letsdoit.ai.provider.GeminiImagesProvider
import com.polaralias.letsdoit.ai.provider.GeminiTextProvider
import com.polaralias.letsdoit.ai.provider.OpenAiImagesAiProvider
import com.polaralias.letsdoit.ai.provider.OpenAiTextProvider
import com.polaralias.letsdoit.ai.settings.AiImageProviderId
import com.polaralias.letsdoit.ai.settings.AiTextProviderId
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
