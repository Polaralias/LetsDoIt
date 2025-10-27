package com.polaralias.letsdoit.widgets

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.glance.testing.GlanceAppWidgetTestRule
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.data.model.Task
import com.polaralias.letsdoit.reminders.ReminderCoordinator
import com.polaralias.letsdoit.test.FakeTaskRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TodayTasksWidgetTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val glanceRule = GlanceAppWidgetTestRule()

    @BindValue
    @JvmField
    val taskRepository: FakeTaskRepository = FakeTaskRepository()

    @BindValue
    @JvmField
    val reminderCoordinator: ReminderCoordinator = mock(ReminderCoordinator::class.java)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
        reset(reminderCoordinator)
        taskRepository.setTasks(emptyList())
    }

    @Test
    fun showsEmptyStateWhenNoTasks() {
        glanceRule.setAppWidget(TodayTasksWidget)
        glanceRule.waitForIdle()
        val emptyText = context.getString(R.string.widget_empty_today)
        glanceRule.onNode(hasText(emptyText)).assertExists()
    }

    @Test
    fun toggleMarksTaskCompleteAndRefreshesWidget() = runBlocking {
        val now = Instant.now()
        val task = Task(
            id = 7L,
            listId = 1L,
            title = "Morning stretch",
            notes = null,
            dueAt = now,
            repeatRule = null,
            remindOffsetMinutes = null,
            createdAt = now,
            updatedAt = now,
            completed = false,
            priority = 2,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = "To do"
        )
        taskRepository.setTasks(listOf(task))

        glanceRule.setAppWidget(TodayTasksWidget)
        glanceRule.onNode(hasText("Morning stretch")).assertExists()

        glanceRule.onNode(isToggleable().and(hasText("Morning stretch"))).performClick()
        awaitCondition { taskRepository.completionUpdates.contains(task.id to true) }
        assertTrue(taskRepository.completionUpdates.contains(task.id to true))
        glanceRule.waitForIdle()

        val emptyText = context.getString(R.string.widget_empty_today)
        glanceRule.onNode(hasText(emptyText)).assertExists()
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
