package com.letsdoit.app.reminders

import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.diagnostics.DiagnosticsManager
import com.letsdoit.app.integrations.alarm.AlarmController
import com.letsdoit.app.integrations.alarm.DefaultAlarmScheduler
import com.letsdoit.app.integrations.alarm.ExactAlarmPermissionRepository
import com.letsdoit.app.integrations.alarm.ExactAlarmPermissionStatus
import com.letsdoit.app.integrations.alarm.REMINDER_CHANNEL_ID
import com.letsdoit.app.test.FakeTaskRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReminderNotificationsTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val taskRepository: FakeTaskRepository = FakeTaskRepository()

    @BindValue
    @JvmField
    val reminderCoordinator: ReminderCoordinator = mock(ReminderCoordinator::class.java)

    private val diagnosticsManager: DiagnosticsManager = mock(DiagnosticsManager::class.java)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
        NotificationManagerCompat.from(context).deleteNotificationChannel(REMINDER_CHANNEL_ID)
        taskRepository.setTasks(emptyList())
        reset(reminderCoordinator)
    }

    @After
    fun tearDown() {
        NotificationManagerCompat.from(context).deleteNotificationChannel(REMINDER_CHANNEL_ID)
    }

    @Test
    fun reminderChannelCreatedWithHighImportance() {
        DefaultAlarmScheduler(
            context,
            object : AlarmController {
                override fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent) {}
                override fun setInexact(type: Int, triggerAtMillis: Long, operation: PendingIntent) {}
                override fun cancel(operation: PendingIntent) {}
            },
            object : ExactAlarmPermissionRepository {
                override val status = MutableStateFlow(ExactAlarmPermissionStatus(true, true))
                override fun isExactAlarmAllowed(): Boolean = true
                override suspend fun refresh() {}
                override suspend fun setExactAlarmAllowed(allowed: Boolean) {}
            },
            diagnosticsManager
        )
        val channel = NotificationManagerCompat.from(context).getNotificationChannel(REMINDER_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManagerCompat.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun reminderActionsDispatchReceivers() = runBlocking {
        val task = Task(
            id = 42L,
            listId = 1L,
            title = "Tea time",
            notes = null,
            dueAt = Instant.now(),
            repeatRule = null,
            remindOffsetMinutes = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completed = false,
            priority = 2,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = "To do"
        )
        taskRepository.setTasks(listOf(task))

        ReminderActionReceiver.buildCompleteIntent(context, task.id).send()
        awaitCondition { taskRepository.completionUpdates.contains(task.id to true) }

        ReminderActionReceiver.buildSnoozeIntent(context, task.id, 10).send()
        verify(reminderCoordinator, timeout(1000)).snooze(task.id, 10L)

        ReminderActionReceiver.buildSnoozeIntent(context, task.id, 60).send()
        verify(reminderCoordinator, timeout(1000)).snooze(task.id, 60L)
    }

    private suspend fun awaitCondition(timeoutMillis: Long = 2000, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (predicate()) {
                return
            }
            delay(20)
        }
        throw AssertionError("Condition not satisfied")
    }
}
