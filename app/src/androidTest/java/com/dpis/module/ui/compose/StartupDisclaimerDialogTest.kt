package com.dpis.module.ui.compose

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.function.BooleanSupplier

@RunWith(AndroidJUnit4::class)
class StartupDisclaimerDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contentRequiresAgreementBeforeAccepting() {
        var accepted = false
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                StartupDisclaimerContent(onAccept = { accepted = true })
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
    fun dialogHostPersistsAcceptanceAndDispatchesCompletion() {
        var persisted = false
        var accepted = false
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setTheme(R.style.Theme_Dpis)
            StartupDisclaimerDialog.show(
                activity = activity,
                markAccepted = BooleanSupplier {
                    persisted = true
                    true
                },
                onSaveFailed = Runnable {},
                onAccepted = Runnable { accepted = true },
                onBack = Runnable {}
            )
        }

        composeRule.onNodeWithTag("startup-disclaimer-agreement").performClick()
        composeRule.onNodeWithTag("startup-disclaimer-accept").performClick()
        composeRule.runOnIdle {
            assertTrue(persisted)
            assertTrue(accepted)
        }
    }
}
