package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.ai.diagnostics.AiDiagnosticsSink
import com.polaralias.letsdoit.ai.diagnostics.DiagnosticsManagerSink
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
