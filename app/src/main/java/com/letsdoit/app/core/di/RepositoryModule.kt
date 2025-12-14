package com.letsdoit.app.core.di

import com.letsdoit.app.data.repository.CalendarRepositoryImpl
import com.letsdoit.app.data.repository.PreferencesRepositoryImpl
import com.letsdoit.app.data.repository.ProjectRepositoryImpl
import com.letsdoit.app.data.repository.TaskRepositoryImpl
import com.letsdoit.app.domain.repository.CalendarRepository
import com.letsdoit.app.domain.repository.PreferencesRepository
import com.letsdoit.app.domain.repository.ProjectRepository
import com.letsdoit.app.domain.repository.TaskRepository
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
        insightsRepositoryImpl: com.letsdoit.app.data.repository.InsightsRepositoryImpl
    ): com.letsdoit.app.domain.repository.InsightsRepository
}
