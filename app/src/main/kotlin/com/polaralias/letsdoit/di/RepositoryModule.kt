package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.data.search.SearchRepository
import com.polaralias.letsdoit.data.search.SearchRepositoryImpl
import com.polaralias.letsdoit.data.subtask.SubtaskRepository
import com.polaralias.letsdoit.data.subtask.SubtaskRepositoryImpl
import com.polaralias.letsdoit.data.task.TaskRepository
import com.polaralias.letsdoit.data.task.TaskRepositoryImpl
import com.polaralias.letsdoit.nlp.NaturalLanguageParser
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
