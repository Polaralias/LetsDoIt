package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.accent.AccentImageProvider
import com.polaralias.letsdoit.accent.OpenAiImagesProvider
import com.polaralias.letsdoit.ui.theme.AccentManager
import com.polaralias.letsdoit.ui.theme.LocalAccentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {
    @Binds
    @Singleton
    abstract fun bindAccentManager(impl: LocalAccentManager): AccentManager

    @Binds
    @IntoSet
    abstract fun bindOpenAiImagesProvider(provider: OpenAiImagesProvider): AccentImageProvider
}
