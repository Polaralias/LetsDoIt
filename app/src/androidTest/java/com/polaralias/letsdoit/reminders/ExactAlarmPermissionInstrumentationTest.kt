package com.polaralias.letsdoit.reminders

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.MainActivity
import com.polaralias.letsdoit.R
import com.polaralias.letsdoit.integrations.alarm.AlarmRequestType
import com.polaralias.letsdoit.integrations.alarm.AlarmScheduler
import com.polaralias.letsdoit.integrations.alarm.FakeAlarmController
import com.polaralias.letsdoit.integrations.alarm.FakeExactAlarmPermissionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExactAlarmPermissionInstrumentationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: FakeExactAlarmPermissionRepository

    @Inject
    lateinit var scheduler: AlarmScheduler

    @Inject
    lateinit var controller: FakeAlarmController

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
        repository.setState(allowed = false, requestAvailable = true)
    }

    @Test
    fun permissionDeniedShowsCtaAndSchedulesInexact() {
        openSettingsTab()
        val activity = composeRule.activity
        composeRule.onNodeWithText(activity.getString(R.string.allow_exact_alarms)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.exact_alarms_not_allowed)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.open_settings)).assertIsDisplayed()

        controller.reset()
        composeRule.runOnIdle {
            scheduler.schedule(101, Instant.now().plusSeconds(600), "Denied reminder")
        }
        val request = controller.lastRequest
        kotlin.test.assertNotNull(request)
        kotlin.test.assertEquals(AlarmRequestType.Inexact, request.type)
    }

    @Test
    fun schedulesExactAfterPermissionEnabled() {
        openSettingsTab()
        repository.setState(allowed = true, requestAvailable = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.exact_alarms_not_allowed)).assertDoesNotExist()

        controller.reset()
        composeRule.runOnIdle {
            scheduler.schedule(202, Instant.now().plusSeconds(600), "Allowed reminder")
        }
        val request = controller.lastRequest
        kotlin.test.assertNotNull(request)
        kotlin.test.assertEquals(AlarmRequestType.Exact, request.type)
    }

    private fun openSettingsTab() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.nav_settings)).performClick()
        composeRule.waitForIdle()
    }
}
