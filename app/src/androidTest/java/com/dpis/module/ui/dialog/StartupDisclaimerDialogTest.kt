package com.dpis.module.ui.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupDisclaimerDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contentRequiresAgreementBeforeAccepting() {
        var accepted = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                StartupDisclaimerDialog(onAccept = { accepted = true }, onBack = {})
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.startup_disclaimer_title)
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("startup-disclaimer-accept").assertIsNotEnabled()
        composeRule.onNodeWithTag("startup-disclaimer-agreement").performClick()
        composeRule.onNodeWithTag("startup-disclaimer-accept").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertTrue(accepted) }
    }

    @Test
    fun backLeavesWithoutAccepting() {
        var accepted = false
        var backed = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                StartupDisclaimerDialog(
                    onAccept = { accepted = true },
                    onBack = { backed = true },
                )
            }
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.runOnIdle {
            assertTrue(backed)
            assertFalse(accepted)
        }
    }
}
