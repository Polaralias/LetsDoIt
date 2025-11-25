package com.example.letsdoit.di

import android.content.Context
import androidx.room.Room
import com.example.letsdoit.data.AppDatabase
import com.example.letsdoit.data.TaskListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "lets_do_it_db").build()
    }

    @Provides
    fun provideTaskListDao(appDatabase: AppDatabase): TaskListDao {
        return appDatabase.taskListDao()
    }
}
