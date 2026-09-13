package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dpis.module.R
import com.dpis.module.tools.presentation.RuntimeReloadNoticeContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocalToolDialogsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun runtimeNoticeAcknowledgesFromSingleAction() {
        var acknowledged = false
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                RuntimeReloadNoticeContent { acknowledged = true }
            }
        }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.module_runtime_reload_ack_button)).performClick()
        assertTrue(acknowledged)
    }
}
