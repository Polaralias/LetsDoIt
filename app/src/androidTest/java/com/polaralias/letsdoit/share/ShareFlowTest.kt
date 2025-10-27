package com.polaralias.letsdoit.share

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.letsdoit.MainActivity
import com.polaralias.letsdoit.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShareFlowTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var shareRepository: ShareRepository

    @Inject
    lateinit var inviteLinkBuilder: InviteLinkBuilder

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
        runBlocking { shareRepository.clearAll() }
    }

    @Test
    fun driveSignInShowsStatus() {
        openSettings()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_title)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_drive_sign_in)).performClick()
        composeRule.onNodeWithText("share@test.local").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("share@test.local").assertIsDisplayed()
        val context = composeRule.activity
        val quota = Formatter.formatFileSize(context, 10L * 1024L * 1024L)
        val total = Formatter.formatFileSize(context, 20L * 1024L * 1024L)
        val quotaText = context.getString(R.string.share_drive_quota, quota, total)
        composeRule.onNodeWithText(quotaText).assertIsDisplayed()
        val date = java.util.Date(FakeDriveService.FIXED_TIME)
        val formatted = DateFormat.getMediumDateFormat(context).format(date) + " " + DateFormat.getTimeFormat(context).format(date)
        val syncText = context.getString(R.string.share_drive_last_sync, formatted)
        composeRule.onNodeWithText(syncText).assertIsDisplayed()
    }

    @Test
    fun inviteGeneratesAndJoinStoresSharedList() {
        openSettings()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_title)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_create_share)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_show_invite)).performClick()
        composeRule.waitForIdle()
        val inviteState = runBlocking { shareRepository.shareState.first { it.lastInvite != null } }
        val invite = requireNotNull(inviteState.lastInvite)
        val link = inviteLinkBuilder.build(invite)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.share_invite_join)).performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(link)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.join_action)).performClick()
        composeRule.waitForIdle()
        val sharedLists = runBlocking { shareRepository.shareState.first { it.sharedLists.isNotEmpty() }.sharedLists }
        kotlin.test.assertTrue(sharedLists.any { it.shareId == invite.shareId })
    }

    private fun openSettings() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.nav_settings)).performClick()
    }
}
