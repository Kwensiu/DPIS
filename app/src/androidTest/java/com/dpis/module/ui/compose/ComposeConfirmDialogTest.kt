package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.R
import org.junit.Assert.assertEquals
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
        val confirm = composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.dialog_process_action_confirm_positive)
        )
        val cancel = composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.dialog_process_action_confirm_negative)
        )
        val confirmBounds = confirm.fetchSemanticsNode().boundsInRoot
        val cancelBounds = cancel.fetchSemanticsNode().boundsInRoot
        assertTrue("Cancel must be left of confirm", cancelBounds.center.x < confirmBounds.center.x)
        assertEquals(cancelBounds.width, confirmBounds.width, 1f)

        confirm.performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
        cancel.performClick()
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

    @Test
    fun messageDialogUsesThemeColorsAndDispatchesClose() {
        var closed = false
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                MessageDialogContent(
                    title = "Release notes",
                    message = AnnotatedString("Changes in this version."),
                    closeLabel = "Close",
                    onClose = { closed = true }
                )
            }
        }

        composeRule.onNodeWithText("Release notes").assertIsDisplayed()
        composeRule.onNodeWithText("Changes in this version.").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.runOnIdle { assertTrue(closed) }
    }

    @Test
    fun messageDialogHandleUpdatesControllerOwnedBody() {
        lateinit var handle: ComposeMessageDialog.Handle
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setTheme(R.style.Theme_Dpis)
            handle = ComposeMessageDialog.showLarge(
                activity,
                "Release notes",
                "Loading",
                "Close"
            )
        }

        composeRule.onNodeWithText("Loading").assertIsDisplayed()
        composeRule.runOnUiThread { handle.setMessage("Loaded changes") }
        composeRule.onNodeWithText("Loaded changes").assertIsDisplayed()
    }
}
