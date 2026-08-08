package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.dpis.module.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsComposeDialogsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun interfaceScaleRejectsOutOfRangeValue() {
        var saved: Int? = null
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                InterfaceScaleDialogContent(100, 60, 120, {}, { saved = it })
            }
        }

        composeRule.onNodeWithText("100").assertIsFocused()
        composeRule.onNodeWithText("100").performTextClearance()
        composeRule.onNodeWithText(composeRule.activity.getString(
            R.string.settings_interface_scale_input_hint)).performTextInput("121")
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.status_save_button))
            .assertIsNotEnabled()
        assertEquals(null, saved)
    }

    @Test
    fun languageActionDispatchesSelectedTag() {
        var selected = ""
        var done = false
        val options = (0..12).map { LanguageDialogOption("tag-$it", "Language $it") }
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                LanguageDialogContent(
                    options,
                    "tag-0",
                    onDone = { done = true },
                    onSelected = { selected = it }
                )
            }
        }
        composeRule.onNodeWithTag(LanguageDialogOptionsTestTag).performScrollToIndex(12)
        composeRule.onNodeWithText("Language 12").assertIsDisplayed()
        composeRule.onNodeWithText("Language 12").performClick()
        assertEquals("tag-12", selected)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.dialog_typeface_done_action))
            .performClick()
        assertTrue(done)
    }

    @Test
    fun backupActionsDispatchTheirOwnCallbacks() {
        var exported = false
        var imported = false
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                BackupActionsDialogContent({ exported = true }, { imported = true }, {})
            }
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.config_backup_export_action))
            .performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.config_backup_import_action))
            .performClick()
        assertTrue(exported)
        assertTrue(imported)
    }
}
