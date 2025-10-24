package com.letsdoit.app.di

import android.content.Context
import androidx.room.Room
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.FolderDao
import com.letsdoit.app.data.db.MIGRATION_1_2
import com.letsdoit.app.data.db.MIGRATION_2_3
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
}
