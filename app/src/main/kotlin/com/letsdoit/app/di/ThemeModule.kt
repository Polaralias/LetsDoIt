package com.letsdoit.app.di

import com.letsdoit.app.ui.theme.AccentManager
import com.letsdoit.app.ui.theme.LocalAccentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {
    @Binds
    @Singleton
    abstract fun bindAccentManager(impl: LocalAccentManager): AccentManager
}
