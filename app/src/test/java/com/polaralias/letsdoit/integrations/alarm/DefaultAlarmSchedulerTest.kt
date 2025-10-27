package com.polaralias.letsdoit.integrations.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.polaralias.letsdoit.diagnostics.DiagnosticsManager
import com.polaralias.letsdoit.diagnostics.DiagnosticsRedactor
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class DefaultAlarmSchedulerTest {
    private lateinit var context: Context
    private lateinit var controller: RecordingAlarmController
    private lateinit var repository: TestExactAlarmPermissionRepository
    private lateinit var diagnosticsManager: DiagnosticsManager
    private lateinit var scheduler: DefaultAlarmScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        controller = RecordingAlarmController()
        repository = TestExactAlarmPermissionRepository()
        diagnosticsManager = DiagnosticsManager(context, DiagnosticsRedactor(), dispatcher = Dispatchers.Unconfined)
        scheduler = DefaultAlarmScheduler(context, controller, repository, diagnosticsManager)
    }

    @Test
    fun rescheduleUsesStoredPermissionState() = runBlocking {
        repository.setAllowed(false)
        scheduler.schedule(1, Instant.now().plusSeconds(600), "Fallback reminder")
        val inexact = controller.lastRequest
        assertNotNull(inexact)
        assertEquals(AlarmRequestType.Inexact, inexact.type)

        repository.setAllowed(true)
        scheduler.schedule(2, Instant.now().plusSeconds(600), "Exact reminder")
        val exact = controller.lastRequest
        assertNotNull(exact)
        assertEquals(AlarmRequestType.Exact, exact.type)
    }

    private class RecordingAlarmController : AlarmController {
        var lastRequest: RecordedRequest? = null

        override fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: android.app.PendingIntent) {
            lastRequest = RecordedRequest(AlarmRequestType.Exact, triggerAtMillis)
        }

        override fun setInexact(type: Int, triggerAtMillis: Long, operation: android.app.PendingIntent) {
            lastRequest = RecordedRequest(AlarmRequestType.Inexact, triggerAtMillis)
        }

        override fun cancel(operation: android.app.PendingIntent) {
            lastRequest = null
        }
    }

    private data class RecordedRequest(val type: AlarmRequestType, val triggerAtMillis: Long)

    private class TestExactAlarmPermissionRepository : ExactAlarmPermissionRepository {
        private val _status = MutableStateFlow(ExactAlarmPermissionStatus(allowed = false, requestAvailable = true))
        override val status: StateFlow<ExactAlarmPermissionStatus> = _status.asStateFlow()

        override fun isExactAlarmAllowed(): Boolean = _status.value.allowed

        override suspend fun refresh() = Unit

        override suspend fun setExactAlarmAllowed(allowed: Boolean) {
            _status.value = _status.value.copy(allowed = allowed)
        }

        fun setAllowed(value: Boolean) {
            _status.value = _status.value.copy(allowed = value)
        }
    }
}
