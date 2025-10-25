package com.letsdoit.app.di

import com.letsdoit.app.share.DefaultDriveAccountProvider
import com.letsdoit.app.share.DefaultDriveService
import com.letsdoit.app.share.DriveAccountProvider
import com.letsdoit.app.share.DriveService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {
    @Binds
    abstract fun bindDriveService(service: DefaultDriveService): DriveService

    @Binds
    abstract fun bindDriveAccountProvider(provider: DefaultDriveAccountProvider): DriveAccountProvider
}
