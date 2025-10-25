package com.letsdoit.app.accent

import com.letsdoit.app.ui.theme.AccentManager
import com.letsdoit.app.ui.theme.LocalAccentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.letsdoit.app.di.ThemeModule::class]
)
abstract class TestAccentModule {
    @Binds
    @Singleton
    abstract fun bindAccentManager(impl: LocalAccentManager): AccentManager

    @Binds
    @IntoSet
    abstract fun bindFakeProvider(provider: FakeAccentImageProvider): AccentImageProvider
}
