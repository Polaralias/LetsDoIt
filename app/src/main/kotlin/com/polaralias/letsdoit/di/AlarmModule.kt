package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.integrations.alarm.AlarmController
import com.polaralias.letsdoit.integrations.alarm.AlarmScheduler
import com.polaralias.letsdoit.integrations.alarm.DefaultAlarmScheduler
import com.polaralias.letsdoit.integrations.alarm.DefaultExactAlarmPermissionRepository
import com.polaralias.letsdoit.integrations.alarm.ExactAlarmPermissionRepository
import com.polaralias.letsdoit.integrations.alarm.SystemAlarmController
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
