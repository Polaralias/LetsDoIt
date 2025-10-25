package com.letsdoit.app.di

import com.letsdoit.app.data.sync.SyncStateRepository
import com.letsdoit.app.data.sync.SyncStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindSyncStatusRepository(impl: SyncStateRepository): SyncStatusRepository
}
