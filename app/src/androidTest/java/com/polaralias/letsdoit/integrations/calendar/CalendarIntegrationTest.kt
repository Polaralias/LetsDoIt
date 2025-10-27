package com.polaralias.letsdoit.integrations.calendar

import android.content.ContentResolver
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.provider.ProviderTestRule
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.task.NewTask
import com.polaralias.letsdoit.data.task.TaskRepository
import com.polaralias.letsdoit.data.task.TaskRepositoryImpl
import com.polaralias.letsdoit.data.subtask.SubtaskRepositoryImpl
import com.polaralias.letsdoit.integrations.alarm.AlarmScheduler
import com.polaralias.letsdoit.reminders.ReminderCoordinator
import com.polaralias.letsdoit.testing.FakeCalendarProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

@RunWith(AndroidJUnit4::class)
class CalendarIntegrationTest {
    @get:Rule
    val providerRule: ProviderTestRule = ProviderTestRule.Builder(
        FakeCalendarProvider::class.java,
        android.provider.CalendarContract.AUTHORITY
    ).build()

    private lateinit var repository: TaskRepository
    private lateinit var bridge: CalendarBridge
    private lateinit var database: AppDatabase
    private val zoneId = ZoneId.of("Europe/London")
    private val clock: Clock = Clock.fixed(Instant.parse("2024-03-01T09:00:00Z"), zoneId)

    @Before
    fun setUp() {
        FakeCalendarProvider.reset()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseContext = instrumentation.targetContext
        val testContext = object : ContextWrapper(baseContext) {
            override fun getContentResolver(): ContentResolver = providerRule.mockContentResolver
        }
        bridge = CalendarBridge(testContext)
        database = Room.inMemoryDatabaseBuilder(baseContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val reminderCoordinator = ReminderCoordinator(
            object : AlarmScheduler {
                override fun schedule(taskId: Long, triggerAt: Instant, title: String) = Unit
                override fun cancel(taskId: Long) = Unit
            },
            database.alarmIndexDao(),
            database.taskDao(),
            clock
        )
        val subtaskRepository = SubtaskRepositoryImpl(database, database.subtaskDao())
        repository = TaskRepositoryImpl(
            database,
            database.taskDao(),
            database.taskOrderDao(),
            database.listDao(),
            database.spaceDao(),
            clock,
            reminderCoordinator,
            subtaskRepository,
            bridge
        )
    }

    @After
    fun tearDown() {
        database.close()
        FakeCalendarProvider.reset()
    }

    @Test
    fun calendarEventLifecycle() = runBlocking {
        val listId = repository.ensureDefaultList()
        val initialDue = Instant.parse("2024-03-15T09:00:00Z")
        val taskId = repository.addTask(NewTask(listId = listId, title = "Morning briefing", dueAt = initialDue))
        val eventId = assertNotNull(bridge.insertEvent("Morning briefing", initialDue))
        repository.linkCalendarEvent(taskId, eventId)

        val stored = repository.getTask(taskId)
        assertEquals(eventId, stored?.calendarEventId)

        val updatedTask = stored!!.copy(title = "Updated briefing", dueAt = initialDue.plus(2, ChronoUnit.HOURS))
        repository.updateTask(updatedTask)

        val eventValues = FakeCalendarProvider.events[eventId]
        assertNotNull(eventValues)
        assertEquals("Updated briefing", eventValues.getAsString(android.provider.CalendarContract.Events.TITLE))
        assertEquals(updatedTask.dueAt!!.toEpochMilli(), eventValues.getAsLong(android.provider.CalendarContract.Events.DTSTART))

        repository.removeFromCalendar(taskId)
        val cleared = repository.getTask(taskId)
        assertNull(cleared?.calendarEventId)
        assertFalse(FakeCalendarProvider.events.containsKey(eventId))
    }

    @Test
    fun updateUsesDeviceTimezoneAcrossDst() = runBlocking {
        val originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        try {
            val listId = repository.ensureDefaultList()
            val due = Instant.parse("2024-03-30T09:00:00Z")
            val taskId = repository.addTask(NewTask(listId = listId, title = "Plan session", dueAt = due))
            val eventId = assertNotNull(bridge.insertEvent("Plan session", due))
            repository.linkCalendarEvent(taskId, eventId)

            val newDue = Instant.parse("2024-03-31T09:00:00Z")
            repository.setDueDate(taskId, newDue)

            val values = FakeCalendarProvider.events[eventId]
            assertNotNull(values)
            assertEquals(TimeZone.getDefault().id, values.getAsString(android.provider.CalendarContract.Events.EVENT_TIMEZONE))
            assertEquals(newDue.toEpochMilli(), values.getAsLong(android.provider.CalendarContract.Events.DTSTART))
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }
}
