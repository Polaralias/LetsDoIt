package com.letsdoit.app.di

import com.letsdoit.app.ai.diagnostics.AiDiagnosticsSink
import com.letsdoit.app.ai.diagnostics.DiagnosticsManagerSink
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiDiagnosticsModule {
    @Binds
    @Singleton
    abstract fun bindAiDiagnosticsSink(impl: DiagnosticsManagerSink): AiDiagnosticsSink
}
