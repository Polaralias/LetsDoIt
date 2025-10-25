package com.letsdoit.app.di

import com.letsdoit.app.integrations.alarm.AlarmController
import com.letsdoit.app.integrations.alarm.AlarmScheduler
import com.letsdoit.app.integrations.alarm.DefaultAlarmScheduler
import com.letsdoit.app.integrations.alarm.DefaultExactAlarmPermissionRepository
import com.letsdoit.app.integrations.alarm.ExactAlarmPermissionRepository
import com.letsdoit.app.integrations.alarm.SystemAlarmController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {
    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: DefaultAlarmScheduler): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindAlarmController(impl: SystemAlarmController): AlarmController

    @Binds
    @Singleton
    abstract fun bindExactAlarmPermissionRepository(impl: DefaultExactAlarmPermissionRepository): ExactAlarmPermissionRepository
}
