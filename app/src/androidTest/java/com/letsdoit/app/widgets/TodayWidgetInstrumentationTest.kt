package com.letsdoit.app.widgets

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.data.task.NewTask
import com.letsdoit.app.data.task.TaskRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TodayWidgetInstrumentationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var taskRepository: TaskRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = runBlocking {
        hiltRule.inject()
        clearTasks()
    }

    @After
    fun tearDown() = runBlocking {
        clearTasks()
    }

    @Test
    fun loadsTodayAndOverdueTasks() = runBlocking {
        val listId = taskRepository.ensureDefaultList()
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val overdue = startOfDay.minusSeconds(1800)
        val dueSoon = startOfDay.plusSeconds(3600)
        taskRepository.addTask(NewTask(listId = listId, title = "Overdue", dueAt = overdue))
        taskRepository.addTask(NewTask(listId = listId, title = "Today", dueAt = dueSoon))

        val tasks = taskRepository.listTodayTasks()
        assertEquals(listOf("Overdue", "Today"), tasks.map { it.title })
    }

    @Test
    fun toggleReceiverMarksTaskComplete() = runBlocking {
        val listId = taskRepository.ensureDefaultList()
        val taskId = taskRepository.addTask(NewTask(listId = listId, title = "Widget toggle"))
        val intent = Intent(context, TodayWidgetToggleReceiver::class.java).apply {
            action = TodayWidgetToggleReceiver.ACTION_TOGGLE
            putExtra(EXTRA_WIDGET_TASK_ID, taskId)
        }
        context.sendBroadcast(intent)

        withTimeout(2000) {
            while (taskRepository.getTask(taskId)?.completed != true) {
                delay(50)
            }
        }
        assertTrue(taskRepository.getTask(taskId)?.completed == true)
    }

    private suspend fun clearTasks() {
        val existing = taskRepository.listAllTasks()
        existing.forEach { task ->
            taskRepository.deleteTask(task.id)
        }
    }
}
