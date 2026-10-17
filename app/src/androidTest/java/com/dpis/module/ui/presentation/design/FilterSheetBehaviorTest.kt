package com.dpis.module.ui.presentation.design

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class FilterSheetBehaviorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun closeRequestRunsAfterTheSheetExitAnimation() {
        val dismissed = AtomicBoolean(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                FilterSheetScaffold(
                    onDismissRequest = { dismissed.set(true) },
                    title = { Text("Filters") },
                    content = { Text("Filter options") },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { assertFalse(dismissed.get()) }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.dialog_close)
        ).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle { assertFalse(dismissed.get()) }
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.dialog_close)
        ).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitUntil(timeoutMillis = 3_000) { dismissed.get() }
        composeRule.mainClock.autoAdvance = true
        assertTrue(dismissed.get())
    }
}
