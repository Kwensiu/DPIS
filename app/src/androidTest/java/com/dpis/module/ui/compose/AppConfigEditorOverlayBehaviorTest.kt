package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.viewport.ViewportApplyMode
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        composeRule.onNodeWithTag(TypefacePickerSystemListTestTag).performTouchInput { swipeDown() }
        composeRule.runOnIdle {
            assertTrue(expandedContentOwnsHeight.get())
        }
    }

    @Test
    fun typefacePageControlsRemainClickableAfterReturningAndReenteringSheet() {
        val destination = showOverlayStartingInHook(ConfigEditorDestination.MAIN)

        composeRule.runOnIdle {
            destination.value = ConfigEditorDestination.TYPEFACE
        }
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.system_settings_back)
        ).performClick()
        composeRule.onNodeWithTag(MainEditorTag).assertExists()

        composeRule.runOnIdle {
            destination.value = ConfigEditorDestination.TYPEFACE
        }
        val importedTab = composeRule.activity.getString(R.string.dialog_typeface_tab_imported)
        composeRule.onNodeWithText(importedTab).performClick()
        composeRule.onNodeWithText(importedTab).assertIsSelected()
        composeRule.onNodeWithTag(TypefacePickerManageTestTag).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.system_settings_back)
        ).performClick()
        composeRule.onNodeWithTag(MainEditorTag).assertExists()
    }

    @Test
    fun animatedTypefacePageControlsRemainClickableInsideTheProductionSheetPath() {
        val destination = showAnimatedOverlay()
        val defaultTypeface = composeRule.activity.getString(R.string.dialog_typeface_default)

        repeat(2) {
            composeRule.runOnIdle {
                destination.value = ConfigEditorDestination.TYPEFACE
            }
            composeRule.onNodeWithText(defaultTypeface).performClick()
            composeRule.onNodeWithTag(MainEditorTag).assertExists()
        }

        composeRule.runOnIdle {
            destination.value = ConfigEditorDestination.TYPEFACE
        }
        val importedTab = composeRule.activity.getString(R.string.dialog_typeface_tab_imported)
        composeRule.onNodeWithText(importedTab).performClick()
        composeRule.onNodeWithText(importedTab).assertIsSelected()
        composeRule.onNodeWithTag(TypefacePickerManageTestTag).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.system_settings_back)
        ).performClick()
        composeRule.onNodeWithTag(MainEditorTag).assertExists()
    }

    @Test
    fun typefaceControlsRemainClickableWhenReenteredDuringReturnAnimation() {
        val destination = showAnimatedOverlay()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread {
            destination.value = ConfigEditorDestination.TYPEFACE
        }
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.runOnUiThread {
            destination.value = ConfigEditorDestination.MAIN
        }
        // Let MAIN publish the return target and start partialExpand(), but do not finish it.
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.runOnUiThread {
            destination.value = ConfigEditorDestination.TYPEFACE
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.mainClock.autoAdvance = true

        val importedTab = composeRule.activity.getString(R.string.dialog_typeface_tab_imported)
        composeRule.onNodeWithText(importedTab).performClick()
        composeRule.onNodeWithText(importedTab).assertIsSelected()
    }

    @Test
    fun destinationPageOwnsClicksAfterReturningAndReenteringDuringAnimatedTransition() {
        val destination = mutableStateOf(ConfigEditorDestination.MAIN)
        val targetClicked = AtomicBoolean(false)
        composeRule.setContent {
            var currentDestination by remember { destination }
            DpisTheme(darkTheme = false, dynamicColor = false) {
                ConfigEditorAnimatedContent(
                    destination = currentDestination,
                    modifier = Modifier.fillMaxSize(),
                    mainContent = {
                        Box(Modifier.fillMaxSize().clickable { })
                    },
                    hookContent = { Text("Hook") },
                    typefaceContent = {
                        Button(
                            onClick = { targetClicked.set(true) },
                            modifier = Modifier.testTag(TypefaceTargetButtonTag)
                        ) {
                            Text("Typeface")
                        }
                    }
                )
            }
        }

        repeat(2) {
            composeRule.runOnIdle {
                destination.value = ConfigEditorDestination.TYPEFACE
            }
            composeRule.onNodeWithTag(TypefaceTargetButtonTag).performClick()
            composeRule.runOnIdle {
                assertTrue(targetClicked.get())
                targetClicked.set(false)
                destination.value = ConfigEditorDestination.MAIN
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun draggingOutsideTypefaceListKeepsTheExpandedTypefacePageInteractive() {
        val dismissed = AtomicBoolean(false)
        val expandedContentOwnsHeight = AtomicBoolean(false)
        val destination = showOverlayStartingInHook(
            ConfigEditorDestination.MAIN,
            dismissed,
            expandedContentOwnsHeight
        )
        composeRule.onNodeWithTag(MainEditorTag).assertExists()

        composeRule.runOnIdle {
            destination.value = ConfigEditorDestination.TYPEFACE
        }

        composeRule.waitUntil(timeoutMillis = 3_000) { expandedContentOwnsHeight.get() }
        composeRule.onNodeWithTag(SheetChromeTag).performTouchInput {
            swipe(
                start = center,
                end = center.copy(y = center.y + 1_000f),
                durationMillis = 500
            )
        }
        composeRule.runOnIdle {
            assertTrue(!dismissed.get())
        }
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.system_settings_back)
        ).performClick()
        composeRule.onNodeWithTag(MainEditorTag).assertExists()
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
                    topChrome = { Text("Editor", Modifier.testTag(SheetChromeTag)) },
                    content = { reportAdvancedAnchor, contentOwnsHeight, returnToMain ->
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
                        AppTypefacePickerPage(
                            selectedTypefaceId = null,
                            onTypefaceSelected = { },
                            onBack = returnToMain
                        )
                    } else {
                        LaunchedEffect(Unit) { reportAdvancedAnchor(320.dp) }
                        Column {
                            Text("Main editor", Modifier.testTag(MainEditorTag))
                            Spacer(Modifier.height(400.dp))
                            Text("Advanced editor", Modifier.testTag(AdvancedEditorTag))
                        }
                    }
                    }
                )
            }
        }
        return destination
    }

    private fun showAnimatedOverlay():
        androidx.compose.runtime.MutableState<ConfigEditorDestination> {
        val destination = mutableStateOf(ConfigEditorDestination.MAIN)
        composeRule.setContent {
            var currentDestination by remember { destination }
            DpisTheme(darkTheme = false, dynamicColor = false) {
                AppConfigEditorOverlay(
                    onDismissRequest = {},
                    destination = currentDestination,
                    onReturnToMain = {
                        currentDestination = currentDestination.backDestination()
                    },
                    topChrome = { Text("Editor", Modifier.testTag(SheetChromeTag)) },
                    content = { reportAdvancedAnchor, _, returnToMain ->
                        LaunchedEffect(Unit) { reportAdvancedAnchor(320.dp) }
                        ConfigEditorAnimatedContent(
                            destination = currentDestination,
                            modifier = Modifier.fillMaxSize(),
                            mainContent = {
                                Column {
                                    Text("Main editor", Modifier.testTag(MainEditorTag))
                                    Spacer(Modifier.height(400.dp))
                                }
                            },
                            hookContent = { Text("Hook") },
                            typefaceContent = {
                                AppTypefacePickerPage(
                                    selectedTypefaceId = null,
                                    onTypefaceSelected = { returnToMain() },
                                    onBack = returnToMain
                                )
                            }
                        )
                    }
                )
            }
        }
        return destination
    }

    private companion object {
        const val MainEditorTag = "app-editor-main"
        const val SheetChromeTag = "app-editor-sheet-chrome"
        const val AdvancedEditorTag = "app-editor-advanced"
        const val TypefaceTargetButtonTag = "typeface-target-button"
    }
}
