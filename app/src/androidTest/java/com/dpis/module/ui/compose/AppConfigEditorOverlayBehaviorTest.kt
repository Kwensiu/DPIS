package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.viewport.ViewportApplyMode
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppConfigEditorOverlayBehaviorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hookTitleBackReturnsToMainWhenPortraitMainAnchorWasNeverMeasured() {
        val destination = showOverlayStartingInHook(ConfigEditorDestination.HOOK_CHAIN_INTERFACE)

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.system_settings_back)
        ).performClick()

        composeRule.onNodeWithTag(MainEditorTag).assertExists()
        composeRule.runOnIdle {
            assertEquals(ConfigEditorDestination.MAIN, destination.value)
        }
    }

    @Test
    fun directHookBackRestoresCollapsedMainWithoutAdvancedContent() {
        val destination = showOverlayStartingInHook(ConfigEditorDestination.MAIN)
        composeRule.onNodeWithTag(MainEditorTag).assertExists()

        composeRule.runOnIdle {
            destination.value = ConfigEditorDestination.HOOK_CHAIN_INTERFACE
        }
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.system_settings_back)
        ).performClick()

        composeRule.onNodeWithTag(MainEditorTag).assertExists()
        composeRule.onNodeWithTag(AdvancedEditorTag).assertIsNotDisplayed()
    }

    @Test
    fun systemBackReturnsFromHookToMainEditorContent() {
        val destination = showOverlayStartingInHook(ConfigEditorDestination.HOOK_CHAIN_FONT)

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithTag(MainEditorTag).assertExists()
        composeRule.runOnIdle {
            assertEquals(ConfigEditorDestination.MAIN, destination.value)
        }
    }

    @Test
    fun systemBackReturnsFromTypefaceToMainEditorContent() {
        val destination = showOverlayStartingInHook(ConfigEditorDestination.TYPEFACE)

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithTag(MainEditorTag).assertExists()
        composeRule.runOnIdle {
            assertEquals(ConfigEditorDestination.MAIN, destination.value)
        }
    }

    @Test
    fun enteringTypefaceExpandsTheSheet() {
        val expandedContentOwnsHeight = AtomicBoolean(false)
        val destination = showOverlayStartingInHook(
            ConfigEditorDestination.MAIN,
            expandedContentOwnsHeight = expandedContentOwnsHeight
        )
        composeRule.onNodeWithTag(MainEditorTag).assertExists()

        composeRule.runOnIdle {
            destination.value = ConfigEditorDestination.TYPEFACE
        }

        composeRule.waitUntil(timeoutMillis = 3_000) { expandedContentOwnsHeight.get() }
    }

    @Test
    fun scrimDismissClosesTheWholeEditorSession() {
        val dismissed = AtomicBoolean(false)
        showOverlayStartingInHook(ConfigEditorDestination.MAIN, dismissed)

        composeRule.onNodeWithTag(AppConfigSheetScrimTestTag).performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) { dismissed.get() }
    }

    private fun showOverlayStartingInHook(
        initialDestination: ConfigEditorDestination,
        dismissed: AtomicBoolean = AtomicBoolean(false),
        expandedContentOwnsHeight: AtomicBoolean? = null
    ): androidx.compose.runtime.MutableState<ConfigEditorDestination> {
        val destination = mutableStateOf(initialDestination)
        composeRule.setContent {
            var currentDestination by remember { destination }
            DpisTheme(darkTheme = false, dynamicColor = false) {
                AppConfigEditorOverlay(
                    onDismissRequest = { dismissed.set(true) },
                    destination = currentDestination,
                    onReturnToMain = {
                        currentDestination = currentDestination.backDestination()
                    },
                    topChrome = { Text("Editor") }
                ) { reportAdvancedAnchor, contentOwnsHeight, returnToMain ->
                    expandedContentOwnsHeight?.set(contentOwnsHeight)
                    if (currentDestination.isHookChain) {
                        HookChainEditorPage(
                            destination = currentDestination,
                            rawDomains = null,
                            fontDomainsResetRequested = true,
                            automaticDomains = emptySet(),
                            fontDomainsEditable = true,
                            viewportApplyMode = ViewportApplyMode.OFF,
                            onHookChainChanged = { _, _, _, _ -> },
                            onDestinationChanged = { currentDestination = it },
                            onBack = returnToMain,
                            animateTabSize = false
                        )
                    } else if (currentDestination.isChildPage) {
                        BackHandler(onBack = returnToMain)
                        Text("Typeface editor", Modifier.testTag(TypefaceEditorTag))
                    } else {
                        LaunchedEffect(Unit) { reportAdvancedAnchor(320.dp) }
                        Column {
                            Text("Main editor", Modifier.testTag(MainEditorTag))
                            Spacer(Modifier.height(400.dp))
                            Text("Advanced editor", Modifier.testTag(AdvancedEditorTag))
                        }
                    }
                }
            }
        }
        return destination
    }

    private companion object {
        const val MainEditorTag = "app-editor-main"
        const val TypefaceEditorTag = "app-editor-typeface"
        const val AdvancedEditorTag = "app-editor-advanced"
    }
}
