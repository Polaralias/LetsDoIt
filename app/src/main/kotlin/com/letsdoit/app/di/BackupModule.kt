package com.letsdoit.app.di

import com.letsdoit.app.backup.BackupKeyStore
import com.letsdoit.app.backup.BackupManager
import com.letsdoit.app.backup.BackupStatusRepository
import com.letsdoit.app.backup.DefaultBackupManager
import com.letsdoit.app.backup.DefaultBackupStatusRepository
import com.letsdoit.app.backup.DefaultDriveBackupClient
import com.letsdoit.app.backup.DriveBackupClient
import com.letsdoit.app.backup.SecureBackupKeyStore
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
