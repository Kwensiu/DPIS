package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                LanguageDialogContent(
                    listOf(LanguageDialogOption("en", "English"), LanguageDialogOption("zh", "Chinese")),
                    "en",
                    {},
                    { selected = it }
                )
            }
        }
        composeRule.onNodeWithText("Chinese").performClick()
        assertEquals("zh", selected)
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
