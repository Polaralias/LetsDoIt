package com.letsdoit.app.ui

import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityScenarioRule
import com.letsdoit.app.MainActivity
import com.letsdoit.app.R
import com.letsdoit.app.navigation.AppIntentExtras
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.BeforeTest
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FontScaleNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    private val launchIntent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
        putExtra(AppIntentExtras.FONT_SCALE_OVERRIDE, 2f)
    }

    @get:Rule(order = 1)
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule(ActivityScenarioRule(launchIntent))

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun listScreenEssentialControlsAccessible() {
        val searchLabel = composeRule.activity.getString(R.string.search_placeholder)
        composeRule.onNodeWithContentDescription(searchLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
        val addLabel = composeRule.activity.getString(R.string.action_add)
        composeRule.onNodeWithText(addLabel)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
        val timelineLabel = composeRule.activity.getString(R.string.nav_timeline)
        composeRule.onNodeWithContentDescription(timelineLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
    }

    @Test
    fun boardScreenEssentialControlsAccessible() {
        openDestination(R.string.nav_buckets)
        composeRule.waitForIdle()
        val bulkLabel = composeRule.activity.getString(R.string.bulk_title)
        composeRule.onNodeWithText(bulkLabel)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
        val searchLabel = composeRule.activity.getString(R.string.search_placeholder)
        composeRule.onNodeWithContentDescription(searchLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
    }

    @Test
    fun timelineScreenEssentialControlsAccessible() {
        openDestination(R.string.nav_timeline)
        composeRule.waitForIdle()
        val emptyLabel = composeRule.activity.getString(R.string.message_empty_tasks)
        composeRule.onNode(hasText(emptyLabel), useUnmergedTree = false).assertIsDisplayed()
        val searchLabel = composeRule.activity.getString(R.string.search_placeholder)
        composeRule.onNodeWithContentDescription(searchLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
    }

    @Test
    fun settingsScreenEssentialControlsAccessible() {
        openDestination(R.string.nav_settings)
        composeRule.waitForIdle()
        val saveLabel = composeRule.activity.getString(R.string.action_save)
        composeRule.onAllNodesWithText(saveLabel).onFirst()
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
        val searchLabel = composeRule.activity.getString(R.string.search_placeholder)
        composeRule.onNodeWithContentDescription(searchLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertMinTouchTarget()
    }

    private fun openDestination(labelRes: Int) {
        val label = composeRule.activity.getString(labelRes)
        composeRule.onNodeWithContentDescription(label, useUnmergedTree = true).performClick()
    }

    private fun SemanticsNodeInteraction.assertMinTouchTarget(): SemanticsNodeInteraction {
        val minPx = with(composeRule.density) { 48.dp.toPx() }
        val bounds = fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width >= minPx && bounds.height >= minPx)
        return this
    }
}
