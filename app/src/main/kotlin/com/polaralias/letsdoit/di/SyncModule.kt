package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.data.sync.SyncStateRepository
import com.polaralias.letsdoit.data.sync.SyncStatusRepository
import com.polaralias.letsdoit.sync.SyncRetryScheduler
import com.polaralias.letsdoit.sync.SyncScheduler
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

    @Binds
    @Singleton
    abstract fun bindSyncRetryScheduler(impl: SyncScheduler): SyncRetryScheduler
}
