package com.letsdoit.app.accent

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.MainActivity
import com.letsdoit.app.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AccentGenerationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun generatePackAppearsAndCanBeSelected() {
        val activity = composeRule.activity
        val settingsLabel = activity.getString(R.string.nav_settings)
        composeRule.onNodeWithText(settingsLabel).performClick()
        val generateTitle = activity.getString(R.string.accent_generate_title)
        composeRule.onNodeWithText(generateTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Trees and nature").performClick()
        val generateAction = activity.getString(R.string.accent_generate_action)
        composeRule.onNodeWithText(generateAction).performClick()
        val saveAction = activity.getString(R.string.accent_save_action)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(saveAction, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(saveAction).performClick()
        val packLabel = "Trees and nature"
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(packLabel, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .any { node -> node.config.getOrNull(SemanticsProperties.Selected) == true }
        }
        val selectedMatcher = hasText(packLabel) and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
        composeRule.onNode(selectedMatcher, useUnmergedTree = true).assertExists()
    }
}
