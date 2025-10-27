package com.polaralias.letsdoit.integrations.alarm

import android.content.Context
import com.polaralias.letsdoit.diagnostics.DiagnosticsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.polaralias.letsdoit.di.AlarmModule::class]
)
object TestAlarmModule {
    @Provides
    @Singleton
    fun provideAlarmController(): FakeAlarmController = FakeAlarmController()

    @Provides
    @Singleton
    fun provideExactAlarmPermissionRepository(): FakeExactAlarmPermissionRepository = FakeExactAlarmPermissionRepository()

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        controller: FakeAlarmController,
        repository: FakeExactAlarmPermissionRepository,
        diagnosticsManager: DiagnosticsManager
    ): AlarmScheduler {
        return DefaultAlarmScheduler(context, controller, repository, diagnosticsManager)
    }
}
