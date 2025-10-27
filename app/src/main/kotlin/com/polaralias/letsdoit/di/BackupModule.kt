package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.backup.BackupKeyStore
import com.polaralias.letsdoit.backup.BackupManager
import com.polaralias.letsdoit.backup.BackupStatusRepository
import com.polaralias.letsdoit.backup.DefaultBackupManager
import com.polaralias.letsdoit.backup.DefaultBackupStatusRepository
import com.polaralias.letsdoit.backup.DefaultDriveBackupClient
import com.polaralias.letsdoit.backup.DriveBackupClient
import com.polaralias.letsdoit.backup.SecureBackupKeyStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupManager(impl: DefaultBackupManager): BackupManager

    @Binds
    @Singleton
    abstract fun bindBackupStatusRepository(impl: DefaultBackupStatusRepository): BackupStatusRepository

    @Binds
    @Singleton
    abstract fun bindDriveBackupClient(impl: DefaultDriveBackupClient): DriveBackupClient

    @Binds
    @Singleton
    abstract fun bindBackupKeyStore(impl: SecureBackupKeyStore): BackupKeyStore
}
