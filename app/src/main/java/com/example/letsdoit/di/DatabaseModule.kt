package com.example.letsdoit.di

import android.content.Context
import androidx.room.Room
import com.example.letsdoit.data.AppDatabase
import com.example.letsdoit.data.ListDao
import com.example.letsdoit.data.ListRepository
import com.example.letsdoit.data.TaskDao
import com.example.letsdoit.data.TaskRepository
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
        return Room.databaseBuilder(context, AppDatabase::class.java, "lets_do_it_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideListDao(appDatabase: AppDatabase): ListDao {
        return appDatabase.listDao()
    }

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    @Singleton
    fun provideListRepository(listDao: ListDao): ListRepository {
        return ListRepository(listDao)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }
}
