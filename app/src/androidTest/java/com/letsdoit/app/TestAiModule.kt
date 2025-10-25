package com.letsdoit.app

import com.letsdoit.app.ai.AiService
import dagger.Binds
import dagger.Module
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.components.SingletonComponent

@Module
@TestInstallIn(
    components = [SingletonComponent::class]
)
abstract class TestAiModule {
    @Binds
    abstract fun bindAiService(service: FakeAiService): AiService
}
