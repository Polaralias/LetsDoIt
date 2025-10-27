package com.letsdoit.app.ai.speech

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.letsdoit.app.R
import com.letsdoit.app.data.transcript.TranscriptRepository
import com.letsdoit.app.data.transcript.TranscriptSource
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val CHANNEL_ID = "recording"
private const val NOTIFICATION_ID = 91

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val engine: SpeechEngineId, val languageTag: String, val elapsedMs: Long) : RecordingState
    data class Paused(val engine: SpeechEngineId, val languageTag: String, val elapsedMs: Long) : RecordingState
}

@AndroidEntryPoint
class RecordingService : Service() {
    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var speechSettingsRepository: SpeechSettingsRepository

    @Inject
    lateinit var transcriptRepository: TranscriptRepository

    private val state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    private val binder = RecordingBinder()
    private val mutex = Mutex()
    private var session: ActiveSession? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    inner class RecordingBinder : Binder() {
        fun state(): StateFlow<RecordingState> = state.asStateFlow()

        suspend fun start(): RecordingState = mutex.withLock { startLocked() }

        suspend fun pause(): RecordingState = mutex.withLock { pauseLocked() }

        suspend fun stop(): RecordingState = mutex.withLock { stopLocked() }
    }

    private suspend fun startLocked(): RecordingState {
        val current = session
        if (current != null) {
            if (current.paused) {
                val now = SystemClock.elapsedRealtime()
                current.resume(now)
                updateNotification(paused = false, current.engine)
                val resumed = RecordingState.Recording(current.engine, current.languageTag, current.elapsed(now))
                state.value = resumed
                return resumed
            }
            return state.value
        }
        val settings = speechSettingsRepository.settings.first()
        val engine = settings.engine
        val lang = settings.languageTag
        val audio = createAudioFile()
        val started = SystemClock.elapsedRealtime()
        session = ActiveSession(engine, lang, audio, started, 0L, paused = false)
        val notification = buildNotification(paused = false, engine)
        startForeground(NOTIFICATION_ID, notification)
        val next = RecordingState.Recording(engine, lang, 0L)
        state.value = next
        return next
    }

    private suspend fun pauseLocked(): RecordingState {
        val current = session ?: return state.value
        if (current.paused) {
            return state.value
        }
        val now = SystemClock.elapsedRealtime()
        current.pause(now)
        updateNotification(paused = true, current.engine)
        val paused = RecordingState.Paused(current.engine, current.languageTag, current.elapsed(now))
        state.value = paused
        return paused
    }

    private suspend fun stopLocked(): RecordingState {
        val current = session ?: return state.value
        session = null
        val now = SystemClock.elapsedRealtime()
        val duration = current.elapsed(now)
        stopForeground(STOP_FOREGROUND_REMOVE)
        state.value = RecordingState.Idle
        withContext(Dispatchers.IO) {
            transcriptRepository.recordSession(
                source = TranscriptSource.Record,
                engine = current.engine.name,
                langTag = current.languageTag,
                audioPath = current.audio.absolutePath,
                textPath = null,
                durationMs = duration
            )
        }
        stopSelf()
        return RecordingState.Idle
    }

    private fun ensureChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName(getString(R.string.recording_notification_channel_name))
                .setDescription(getString(R.string.recording_notification_channel_desc))
                .build()
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(paused: Boolean, engine: SpeechEngineId): Notification {
        val title = if (paused) {
            getString(R.string.recording_notification_paused)
        } else {
            getString(R.string.recording_notification_title)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(engineLabel(engine))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(!paused)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(paused: Boolean, engine: SpeechEngineId) {
        val notification = buildNotification(paused, engine)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun engineLabel(engine: SpeechEngineId): String {
        return when (engine) {
            SpeechEngineId.Local -> getString(R.string.settings_speech_engine_local)
            SpeechEngineId.GoogleCloud -> getString(R.string.settings_speech_engine_google_cloud)
            SpeechEngineId.Azure -> getString(R.string.settings_speech_engine_azure)
        }
    }

    private suspend fun createAudioFile(): File {
        return withContext(Dispatchers.IO) {
            val directory = File(filesDir, "recordings")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "recording_${System.currentTimeMillis()}.aac")
            if (!file.exists()) {
                file.createNewFile()
            }
            file
        }
    }

    private data class ActiveSession(
        val engine: SpeechEngineId,
        val languageTag: String,
        val audio: File,
        var startElapsed: Long,
        var accumulatedMs: Long,
        var paused: Boolean
    ) {
        fun pause(now: Long) {
            if (!paused) {
                accumulatedMs += now - startElapsed
                paused = true
            }
        }

        fun resume(now: Long) {
            if (paused) {
                startElapsed = now
                paused = false
            }
        }

        fun elapsed(now: Long): Long {
            return if (paused) accumulatedMs else accumulatedMs + (now - startElapsed)
        }
    }
}
