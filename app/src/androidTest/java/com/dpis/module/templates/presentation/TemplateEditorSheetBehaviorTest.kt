package com.dpis.module.templates.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateEditorSheetBehaviorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainTemplateSheetCanBeDraggedToRevealAdditionalContent() {
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                TemplateEditorSheet(
                    destination = ConfigEditorDestination.MAIN,
                    onDismissRequest = {},
                    onReturnToMain = {},
                    topChrome = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .height(56.dp)
                                .testTag(ChromeTag),
                        )
                    },
                    content = { reportContentBottom, expanded, _ ->
                        val density = LocalDensity.current
                        Text(
                            "Save template",
                            Modifier
                                .testTag(SaveActionTag)
                                .onGloballyPositioned { coordinates ->
                                    with(density) {
                                        reportContentBottom(
                                            (coordinates.positionInParent().y + coordinates.size.height)
                                                .toDp()
                                        )
                                    }
                                },
                        )
                        if (expanded) Text(
                            "Expanded controls",
                            Modifier.testTag(ExpandedContentTag)
                        )
                    },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SaveActionTag).assertIsDisplayed()

        composeRule.onNodeWithTag(ChromeTag).performTouchInput {
            swipe(
                start = center.copy(y = center.y + 20f),
                end = center.copy(y = center.y - 100f),
                durationMillis = 400,
            )
        }

        composeRule.onNodeWithTag(ExpandedContentTag).assertIsDisplayed()
    }

    private companion object {
        const val ChromeTag = "template-editor-sheet-chrome-test"
        const val SaveActionTag = "template-editor-sheet-save-action-test"
        const val ExpandedContentTag = "template-editor-sheet-expanded-content-test"
    }
}
