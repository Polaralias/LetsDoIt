package com.polaralias.letsdoit.share

import dagger.Binds
import dagger.Module
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.components.SingletonComponent

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.polaralias.letsdoit.di.ShareModule::class]
)
abstract class TestShareModule {
    @Binds
    abstract fun bindDriveService(service: FakeDriveService): DriveService

    @Binds
    abstract fun bindDriveAccountProvider(provider: FakeDriveAccountProvider): DriveAccountProvider
}
