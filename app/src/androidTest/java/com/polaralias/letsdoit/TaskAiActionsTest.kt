package com.polaralias.letsdoit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TaskAiActionsTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun splitAndUndoFromMenu() {
        composeRule.onNode(hasSetTextAction()).performTextInput("AI Task")
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_add)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.action_more)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_split_subtasks)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_add_items)).performClick()
        composeRule.onNodeWithText("AI Task").performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.title_subtasks)).assertIsDisplayed()
        composeRule.onNodeWithText("First generated subtask").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_undo)).performClick()
        composeRule.onNodeWithText("AI Task").performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.message_no_subtasks)).assertIsDisplayed()
    }
}
