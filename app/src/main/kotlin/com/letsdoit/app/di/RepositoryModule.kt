package com.letsdoit.app.di

import com.letsdoit.app.data.search.SearchRepository
import com.letsdoit.app.data.search.SearchRepositoryImpl
import com.letsdoit.app.data.subtask.SubtaskRepository
import com.letsdoit.app.data.subtask.SubtaskRepositoryImpl
import com.letsdoit.app.data.task.TaskRepository
import com.letsdoit.app.data.task.TaskRepositoryImpl
import com.letsdoit.app.nlp.NaturalLanguageParser
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindSubtaskRepository(impl: SubtaskRepositoryImpl): SubtaskRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Singleton
        fun provideNaturalLanguageParser(clock: Clock): NaturalLanguageParser = NaturalLanguageParser(clock)
    }
}
