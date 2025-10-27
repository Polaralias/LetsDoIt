package com.polaralias.letsdoit.ai.speech

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.MainActivity
import com.polaralias.letsdoit.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SpeechSettingsInstrumentationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var speechSettingsRepository: SpeechSettingsRepository

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            speechSettingsRepository.updateCloudConsent(false)
            speechSettingsRepository.updateEngine(SpeechEngineId.Local)
        }
    }

    @Test
    fun cloudEngineRequiresConsent() {
        openSettings()
        val activity = composeRule.activity
        val localLabel = activity.getString(R.string.settings_speech_engine_local)
        val cloudLabel = activity.getString(R.string.settings_speech_engine_google_cloud)
        val consentDescription = activity.getString(R.string.accessibility_toggle_cloud_transcription)

        composeRule.onNodeWithContentDescription(localLabel).assertIsSelected()
        composeRule.onNodeWithContentDescription(consentDescription).assertIsOff()

        composeRule.onNodeWithContentDescription(cloudLabel).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.settings_speech_consent_allow)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.settings_speech_consent_cancel)).performClick()

        composeRule.onNodeWithContentDescription(localLabel).assertIsSelected()
        composeRule.onNodeWithContentDescription(consentDescription).assertIsOff()

        composeRule.onNodeWithContentDescription(cloudLabel).performClick()
        composeRule.onNodeWithText(activity.getString(R.string.settings_speech_consent_allow)).performClick()

        composeRule.onNodeWithContentDescription(cloudLabel).assertIsSelected()
        composeRule.onNodeWithContentDescription(consentDescription).assertIsOn()
    }

    private fun openSettings() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.nav_settings)).performClick()
        composeRule.waitForIdle()
    }
}
