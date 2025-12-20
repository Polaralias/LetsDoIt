package com.polaralias.letsdoit.core.di

import com.polaralias.letsdoit.data.repository.CalendarRepositoryImpl
import com.polaralias.letsdoit.data.repository.PreferencesRepositoryImpl
import com.polaralias.letsdoit.data.repository.ProjectRepositoryImpl
import com.polaralias.letsdoit.data.repository.TaskRepositoryImpl
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import com.polaralias.letsdoit.domain.repository.ProjectRepository
import com.polaralias.letsdoit.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        calendarRepositoryImpl: CalendarRepositoryImpl
    ): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        projectRepositoryImpl: ProjectRepositoryImpl
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindInsightsRepository(
        insightsRepositoryImpl: com.polaralias.letsdoit.data.repository.InsightsRepositoryImpl
    ): com.polaralias.letsdoit.domain.repository.InsightsRepository
}
