package com.dpis.module.updates

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dpis.module.R
import com.dpis.module.ui.compose.DpisTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UpdateAvailableComposeDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun darkDialogExpandsReleaseNotesAndDispatchesActions() {
        val actions = mutableListOf<String>()
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                UpdateDialogContent("Update available", "1.0 -> 2.0",
                    UpdateDialogState(releaseNotes = androidx.compose.ui.text.AnnotatedString("Changes"),
                        primaryLabel = "Download", cancelLabel = "Cancel"),
                    { actions += "primary" }, { actions += "cancel" })
            }
        }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.about_update_release_notes_title)).performClick()
        composeRule.onNodeWithText("Changes").assertIsDisplayed()
        composeRule.onNodeWithText("Download").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("primary", "cancel"), actions)
    }

    @Test
    fun determinateProgressShowsTextAndDisablesPrimaryAction() {
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                UpdateDialogContent("Update", "Message",
                    UpdateDialogState(primaryLabel = "Download", cancelLabel = "Cancel",
                        primaryEnabled = false, progressVisible = true,
                        progressIndeterminate = false, progress = 42, progressText = "42%"), {}, {})
            }
        }
        composeRule.onNodeWithText("42%").assertIsDisplayed()
    }
}
