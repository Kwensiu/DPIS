package com.dpis.module.templates.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateEditorSheetBehaviorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun globalPrefillOpensWithLessOfTheSheetVisibleThanTemplateEditing() {
        lateinit var updatePeekFraction: (Float) -> Unit
        composeRule.setContent {
            var peekFraction by remember {
                mutableStateOf(TemplateSheetUiTokens.GlobalPrefillCollapsedPeekFraction)
            }
            updatePeekFraction = { peekFraction = it }
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                TemplateEditorSheet(
                    destination = ConfigEditorDestination.MAIN,
                    onDismissRequest = {},
                    onReturnToMain = {},
                    topChrome = { Box(Modifier
                        .fillMaxSize()
                        .height(56.dp)
                        .testTag(ChromeTag)) },
                    content = { reportContentBottom, _, _ ->
                        SideEffect { reportContentBottom(320.dp) }
                        Text("Template controls")
                    },
                    mainCollapsedPeekFraction = peekFraction,
                )
            }
        }
        composeRule.waitForIdle()
        val prefillVisibleHeight = visibleSheetHeight()

        composeRule.runOnIdle {
            updatePeekFraction(TemplateSheetUiTokens.TemplateCollapsedPeekFraction)
        }
        composeRule.waitForIdle()
        val templateVisibleHeight = visibleSheetHeight()

        assertTrue(prefillVisibleHeight < templateVisibleHeight)
    }

    @Test
    fun mainTemplateSheetCanBeDraggedToRevealAdditionalContent() {
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                TemplateEditorSheet(
                    destination = ConfigEditorDestination.MAIN,
                    onDismissRequest = {},
                    onReturnToMain = {},
                    topChrome = { Box(Modifier
                        .fillMaxSize()
                        .height(56.dp)
                        .testTag(ChromeTag)) },
                    content = { reportContentBottom, expanded, _ ->
                        SideEffect { reportContentBottom(320.dp) }
                        if (expanded) Text(
                            "Expanded controls",
                            Modifier.testTag(ExpandedContentTag)
                        )
                    },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ChromeTag).performTouchInput {
            swipe(
                start = center.copy(y = center.y + 20f),
                end = center.copy(y = center.y - 100f),
                durationMillis = 400
            )
        }

        composeRule.onNodeWithTag(ExpandedContentTag).assertIsDisplayed()
    }

    private fun visibleSheetHeight(): Float {
        val root = composeRule.onRoot().fetchSemanticsNode()
        val chrome = composeRule.onNodeWithTag(ChromeTag).fetchSemanticsNode()
        return root.boundsInRoot.bottom - chrome.boundsInRoot.top
    }

    private companion object {
        const val ChromeTag = "template-editor-sheet-chrome-test"
        const val ExpandedContentTag = "template-editor-sheet-expanded-content-test"
    }
}
