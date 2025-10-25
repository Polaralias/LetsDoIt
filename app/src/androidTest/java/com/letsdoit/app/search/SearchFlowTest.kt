package com.letsdoit.app.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.MainActivity
import com.letsdoit.app.R
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.task.NewTask
import com.letsdoit.app.data.task.TaskRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SearchFlowTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var database: AppDatabase

    private var taskId: Long = 0

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            database.clearAllTables()
            val listId = taskRepository.ensureDefaultList()
            val zone = ZoneId.systemDefault()
            val dueToday = LocalDate.now(zone).atStartOfDay(zone).plusHours(12).toInstant()
            taskId = taskRepository.addTask(
                NewTask(
                    listId = listId,
                    title = "Strategy review",
                    notes = "Plan release",
                    dueAt = dueToday
                )
            )
        }
    }

    @Test
    fun searchDisplaysResults() {
        openSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Strategy")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Strategy review").assertIsDisplayed()
    }

    @Test
    fun smartFilterShowsDueToday() {
        openSearch()
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.search_filter_due_today)).performClick()
        composeRule.onNodeWithText("Strategy review").assertIsDisplayed()
    }

    @Test
    fun quickActionsSupportUndo() {
        openSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Strategy")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.search_overflow_actions)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.search_menu_priority_high)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_undo)).performClick()
        composeRule.waitForIdle()
        runBlocking {
            val task = taskRepository.getTask(taskId)
            kotlin.test.assertNotNull(task)
            kotlin.test.assertEquals(2, task.priority)
        }
    }

    private fun openSearch() {
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.search_placeholder)).performClick()
    }
}
