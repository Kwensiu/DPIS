package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeConfirmDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contentShowsMessageAndDispatchesBothActions() {
        var confirmed = false
        var canceled = false
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                ConfirmDialogContent(
                    title = "High-risk action",
                    message = "This may crash the system.",
                    onConfirm = { confirmed = true },
                    onCancel = { canceled = true }
                )
            }
        }

        composeRule.onNodeWithText("High-risk action").assertIsDisplayed()
        composeRule.onNodeWithText("This may crash the system.").assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.dialog_process_action_confirm_positive)
        ).performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.dialog_process_action_confirm_negative)
        ).performClick()
        composeRule.runOnIdle { assertTrue(canceled) }
    }

    @Test
    fun dialogHostAttachesComposeContentAndDispatchesConfirm() {
        var confirmed = false
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setTheme(R.style.Theme_Dpis)
            ComposeConfirmDialog.show(
                activity = activity,
                title = "Confirm host",
                message = "Hosted Compose content",
                onConfirm = Runnable { confirmed = true },
                onCancel = Runnable {}
            )
        }

        composeRule.onNodeWithText("Confirm host").assertIsDisplayed()
        composeRule.onNodeWithText("Hosted Compose content").assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.dialog_process_action_confirm_positive)
        ).performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
    }
}
