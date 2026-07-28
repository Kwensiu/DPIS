package com.dpis.module.diagnostics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dpis.module.R
import com.dpis.module.ui.compose.DpisTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedbackDiagnosticComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun darkResultContentShowsFilesAndDispatchesBothActions() {
        var saved = false
        var shared = false
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                FeedbackDiagnosticResultContent(
                    title = "Diagnostic ready",
                    packageLine = "Package: example.app",
                    versionLine = "Version: 1.0",
                    entries = listOf(
                        FeedbackDiagnosticEntryUi("diagnostic.txt", "12 lines")
                    ),
                    onSave = { saved = true },
                    onShare = { shared = true }
                )
            }
        }

        composeRule.onNodeWithText("diagnostic.txt").assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.feedback_diagnostic_save_action)
        ).performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.feedback_diagnostic_share_action)
        ).performClick()
        composeRule.runOnIdle {
            assertTrue(saved)
            assertTrue(shared)
        }
    }

    @Test
    fun packagingContentShowsProgressCopy() {
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                Column { FeedbackDiagnosticPackagingContent() }
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.feedback_diagnostic_packaging_title)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.feedback_diagnostic_packaging_message)
        ).assertIsDisplayed()
    }
}
