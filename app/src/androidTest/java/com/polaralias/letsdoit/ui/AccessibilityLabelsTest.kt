package com.polaralias.letsdoit.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.model.Subtask
import com.polaralias.letsdoit.data.model.Task
import com.polaralias.letsdoit.ui.components.TaskCard
import com.polaralias.letsdoit.ui.components.TaskDetailSheet
import com.polaralias.letsdoit.ui.screens.BucketCard
import com.polaralias.letsdoit.ui.theme.AppTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.test.assertTrue
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

    @Test
    fun subtaskRowProvidesReorderAccessibility() {
        val task = Task(
            id = 5L,
            listId = 3L,
            title = "Plan launch",
            notes = "",
            dueAt = Instant.parse("2024-11-15T10:00:00Z"),
            repeatRule = null,
            remindOffsetMinutes = null,
            createdAt = Instant.parse("2024-10-01T09:00:00Z"),
            updatedAt = Instant.parse("2024-10-01T09:00:00Z"),
            completed = false,
            priority = 1,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = "To do"
        )
        val subtask = Subtask(
            id = 7L,
            parentTaskId = task.id,
            title = "Draft announcement",
            done = false,
            dueAt = null,
            orderInParent = 0,
            startAt = null,
            durationMinutes = null
        )
        composeRule.setContent {
            AppTheme {
                TaskDetailSheet(
                    task = task,
                    onDismiss = {},
                    onSave = { _, _ -> },
                    subtasks = listOf(subtask),
                    onToggleSubtask = {},
                    onMoveSubtask = { _, _ -> },
                    onRemoveFromCalendar = {}
                )
            }
        }
        val handleDescription = composeRule.activity.getString(
            R.string.accessibility_subtask_reorder_handle,
            subtask.title
        )
        composeRule.onNodeWithContentDescription(handleDescription, useUnmergedTree = false)
            .assertExists()
            .assertMinTouchTarget()
        val moveLabel = composeRule.activity.getString(R.string.action_move_to)
        val matcher = SemanticsMatcher.expectValue(SemanticsProperties.CustomActions) { actions ->
            actions.any { it.label == moveLabel }
        }
        composeRule.onNode(
            hasText(subtask.title) and SemanticsMatcher.keyIsDefined(SemanticsProperties.CustomActions),
            useUnmergedTree = false
        ).assert(matcher)
    }

    private fun SemanticsNodeInteraction.assertMinTouchTarget(): SemanticsNodeInteraction {
        val minPx = with(composeRule.density) { 48.dp.toPx() }
        val bounds = fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width >= minPx && bounds.height >= minPx)
        return this
    }
}
