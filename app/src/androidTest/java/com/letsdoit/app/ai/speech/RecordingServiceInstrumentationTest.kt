package com.letsdoit.app.ai.speech

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ServiceScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.data.transcript.TranscriptRepository
import com.letsdoit.app.data.transcript.TranscriptSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecordingServiceInstrumentationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var transcriptRepository: TranscriptRepository

    @Inject
    lateinit var speechSettingsRepository: SpeechSettingsRepository

    private var scenario: ServiceScenario<RecordingService>? = null

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            speechSettingsRepository.updateCloudConsent(false)
            speechSettingsRepository.updateEngine(SpeechEngineId.Local)
        }
    }

    @AfterTest
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun startPauseStopPersistsSession() {
        val initial = runBlocking { transcriptRepository.sessions().first() }
        val launched = ServiceScenario.launch(RecordingService::class.java)
        scenario = launched
        launched.moveToState(Lifecycle.State.STARTED)
        val binder = launched.bindService() as RecordingService.RecordingBinder

        runBlocking { binder.start() }
        runBlocking { binder.state().filterIsInstance<RecordingState.Recording>().first() }

        runBlocking { binder.pause() }
        runBlocking { binder.state().filterIsInstance<RecordingState.Paused>().first() }

        runBlocking { binder.stop() }
        runBlocking { binder.state().first { it is RecordingState.Idle } }

        val sessions = runBlocking {
            withTimeout(5000) {
                transcriptRepository.sessions().first { it.size == initial.size + 1 }
            }
        }
        val latest = sessions.first()
        assertEquals(TranscriptSource.Record, latest.source)
        assertEquals(SpeechEngineId.Local.name, latest.engine)
        assertNull(latest.textPath)
        val duration = latest.durationMs
        assertNotNull(duration)
        assertTrue(duration >= 0)
        assertTrue(File(latest.audioPath).exists())

        launched.moveToState(Lifecycle.State.DESTROYED)
    }
}
