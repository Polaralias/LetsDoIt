package com.letsdoit.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.R
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.ui.components.TaskCard
import com.letsdoit.app.ui.screens.BucketCard
import com.letsdoit.app.ui.screens.TimelineItem
import com.letsdoit.app.ui.screens.ThemeToggleRow
import com.letsdoit.app.ui.theme.AppTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FontScaleSnapshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun taskCardRendersAtLargeFont() {
        val task = sampleTask(completed = false)
        composeRule.setContentWithFontScale {
            TaskCard(
                task = task,
                onToggle = {},
                onRemove = {},
                onClick = {},
                onSplit = {},
                onPlan = {}
            )
        }
        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0 && image.height > 0)
    }

    @Test
    fun bucketCardRendersAtLargeFont() {
        val list = ListEntity(id = 10L, spaceId = 3L, name = "Product updates")
        composeRule.setContentWithFontScale {
            BucketCard(entity = list)
        }
        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0 && image.height > 0)
    }

    @Test
    fun timelineItemRendersAtLargeFont() {
        val task = sampleTask(completed = true)
        composeRule.setContentWithFontScale {
            TimelineItem(task = task, highlight = true)
        }
        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0 && image.height > 0)
    }

    @Test
    fun themeToggleRowRendersAtLargeFont() {
        val label = composeRule.activity.getString(R.string.label_high_contrast)
        val description = composeRule.activity.getString(R.string.accessibility_toggle_high_contrast)
        composeRule.setContentWithFontScale {
            ThemeToggleRow(
                text = label,
                checked = true,
                onToggle = {},
                description = description
            )
        }
        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0 && image.height > 0)
    }

    private fun sampleTask(completed: Boolean): Task {
        return Task(
            id = if (completed) 2L else 1L,
            listId = 4L,
            title = "Schedule sprint review",
            notes = "Confirm agenda",
            dueAt = Instant.parse("2024-12-15T15:30:00Z"),
            repeatRule = null,
            remindOffsetMinutes = null,
            createdAt = Instant.parse("2024-10-01T09:00:00Z"),
            updatedAt = Instant.parse("2024-10-01T09:00:00Z"),
            completed = completed,
            priority = 0,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = "To do"
        )
    }

    private fun ComposeContentTestRule.setContentWithFontScale(content: @Composable () -> Unit) {
        setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 2f)) {
                AppTheme { content() }
            }
        }
    }
}
