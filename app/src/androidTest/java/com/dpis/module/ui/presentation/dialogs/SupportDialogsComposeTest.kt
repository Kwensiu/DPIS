package com.dpis.module.ui.presentation.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dpis.module.R
import com.dpis.module.about.presentation.LicenseDetailContent
import com.dpis.module.ui.dialog.ConfirmDialogContent
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SupportDialogsComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun textInputSubmitsEditedValue() {
        var submitted = ""
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = true, dynamicColor = false) {
                TextInputDialogContent("Font display name", "Name", "Old", {}, { submitted = it })
            }
        }
        composeRule.onNodeWithText("Old").assertIsFocused().performTextInput("New")
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.dialog_confirm_button)).performClick()
        assertEquals("New", submitted)
    }

    @Test
    fun customConfirmActionIsUsed() {
        var confirmed = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                ConfirmDialogContent("Delete font?", "Delete this font?", { confirmed = true }, {},
                    cancelLabel = "Cancel", confirmLabel = "Delete")
            }
        }
        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun darkLicenseDetailShowsBodyAndWebsiteAction() {
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = true, dynamicColor = false) {
                LicenseDetailContent("Library", "License body", true, {}, {})
            }
        }
        composeRule.onNodeWithText("License body").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.about_link_source_title))
            .assertIsDisplayed()
    }
}
