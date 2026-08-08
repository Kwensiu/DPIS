package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dpis.module.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FontDebugComposeSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun darkSheetShowsStateAndDispatchesAllActions() {
        val actions = mutableListOf<String>()
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                FontDebugSheetContent(
                    FontDebugSheetState("Chain", "30 seconds", "Updated now", "stats body",
                        "Disable overlay", true),
                    { actions += "mode" }, { actions += "window" },
                    { actions += "overlay" }, { actions += "clear" }, { actions += "close" })
            }
        }
        composeRule.onNodeWithText("stats body").assertIsDisplayed()
        composeRule.onNodeWithText("Chain").performClick()
        composeRule.onNodeWithText("30 seconds").performClick()
        composeRule.onNodeWithText("Disable overlay").performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.font_debug_clear_button))
            .performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.dialog_cancel_button))
            .performClick()
        assertEquals(listOf("mode", "window", "overlay", "clear", "close"), actions)
    }
}
