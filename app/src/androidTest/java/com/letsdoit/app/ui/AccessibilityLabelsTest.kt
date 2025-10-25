package com.letsdoit.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.R
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.components.TaskCard
import com.letsdoit.app.ui.screens.BucketCard
import com.letsdoit.app.ui.theme.AppTheme
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityLabelsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun taskCardProvidesTalkBackLabels() {
        val task = Task(
            id = 1L,
            listId = 1L,
            title = "Review quarterly plan",
            notes = "Prepare slides",
            dueAt = Instant.parse("2024-12-01T10:00:00Z"),
            repeatRule = null,
            remindOffsetMinutes = null,
            createdAt = Instant.parse("2024-11-01T09:00:00Z"),
            updatedAt = Instant.parse("2024-11-01T09:00:00Z"),
            completed = false,
            priority = 0,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = "To do"
        )
        composeRule.setContent {
            AppTheme {
                TaskCard(
                    task = task,
                    onToggle = {},
                    onRemove = {},
                    onClick = {},
                    onSplit = {},
                    onPlan = {}
                )
            }
        }
        val toggleLabel = composeRule.activity.getString(R.string.accessibility_task_toggle_complete, task.title)
        composeRule.onNodeWithContentDescription(toggleLabel).assertExists()
        val deleteLabel = composeRule.activity.getString(R.string.action_delete_task, task.title)
        composeRule.onNodeWithContentDescription(deleteLabel).assertExists()
        val moreLabel = composeRule.activity.getString(R.string.action_show_more_for_task, task.title)
        composeRule.onNodeWithContentDescription(moreLabel).assertExists()
    }

    @Test
    fun bucketCardAnnouncesSummary() {
        val list = ListEntity(id = 1L, spaceId = 2L, name = "Team goals")
        composeRule.setContent {
            AppTheme {
                BucketCard(entity = list)
            }
        }
        val spaceLabel = composeRule.activity.getString(R.string.bucket_space_label, list.spaceId)
        val summary = composeRule.activity.getString(R.string.accessibility_bucket_summary, list.name, spaceLabel)
        composeRule.onNodeWithContentDescription(summary).assertExists()
    }
}
