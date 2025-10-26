package com.letsdoit.app.di

import android.content.Context
import androidx.room.Room
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.CrdtEventDao
import com.letsdoit.app.data.db.dao.EventAckDao
import com.letsdoit.app.data.db.dao.FolderDao
import com.letsdoit.app.data.db.MIGRATION_1_2
import com.letsdoit.app.data.db.MIGRATION_2_3
import com.letsdoit.app.data.db.MIGRATION_3_4
import com.letsdoit.app.data.db.MIGRATION_4_5
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.SubtaskDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.dao.TaskSyncMetaDao
import com.letsdoit.app.data.db.dao.TranscriptSessionDao
import com.letsdoit.app.data.db.dao.SharedListDao
import com.letsdoit.app.data.db.MIGRATION_5_6
import com.letsdoit.app.data.db.MIGRATION_6_7
import com.letsdoit.app.data.db.MIGRATION_7_8
import com.letsdoit.app.data.db.MIGRATION_8_9
import com.letsdoit.app.data.db.MIGRATION_9_10
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "letsdoit.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10
            )
            .build()
    }

    @Provides
    fun provideSpaceDao(database: AppDatabase): SpaceDao = database.spaceDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideListDao(database: AppDatabase): ListDao = database.listDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskOrderDao(database: AppDatabase): TaskOrderDao = database.taskOrderDao()

    @Provides
    fun provideAlarmIndexDao(database: AppDatabase): AlarmIndexDao = database.alarmIndexDao()

    @Provides
    fun provideSubtaskDao(database: AppDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideTaskSyncMetaDao(database: AppDatabase): TaskSyncMetaDao = database.taskSyncMetaDao()

    @Provides
    fun provideTranscriptSessionDao(database: AppDatabase): TranscriptSessionDao = database.transcriptSessionDao()

    @Provides
    fun provideSharedListDao(database: AppDatabase): SharedListDao = database.sharedListDao()

    @Provides
    fun provideCrdtEventDao(database: AppDatabase): CrdtEventDao = database.crdtEventDao()

    @Provides
    fun provideEventAckDao(database: AppDatabase): EventAckDao = database.eventAckDao()
}
