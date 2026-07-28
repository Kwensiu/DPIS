package com.dpis.module.templates

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.dpis.module.R
import com.dpis.module.ui.compose.DpisTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickTemplateSortDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longPressDragReordersItemsBeforeSave() {
        var savedIds: List<String> = emptyList()
        composeRule.setContent {
            DpisTheme(darkTheme = true, dynamicColor = false) {
                QuickTemplateSortContent(
                    initialItems = listOf(
                        QuickTemplateSortItem("one", "First"),
                        QuickTemplateSortItem("two", "Second")
                    ),
                    onCancel = {},
                    onSave = { savedIds = it }
                )
            }
        }

        val dragDescription = composeRule.activity.getString(
            R.string.quick_template_sort_drag_handle
        )
        composeRule.onAllNodesWithContentDescription(dragDescription)[1]
            .performTouchInput {
                down(center)
                advanceEventTime(650)
                moveBy(Offset(0f, -220f), delayMillis = 120)
                up()
            }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.quick_template_sort_save)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("two", "one"), savedIds)
        }
    }
}
