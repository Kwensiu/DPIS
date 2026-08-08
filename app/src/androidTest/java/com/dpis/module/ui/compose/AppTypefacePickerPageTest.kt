package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.R
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppTypefacePickerPageTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pagerSwipesBetweenTypefaceCataloguesAndKeepsOnlyManageAction() {
        val selectedTypeface = mutableStateOf<String?>(null)
        composeRule.setContent {
            DpisTheme(darkTheme = false, dynamicColor = false) {
                AppTypefacePickerPage(
                    selectedTypefaceId = selectedTypeface.value,
                    onTypefaceSelected = { selectedTypeface.value = it },
                    onBack = {}
                )
            }
        }

        val importedTab = composeRule.activity.getString(R.string.dialog_typeface_tab_imported)
        val tabBounds = composeRule.onNodeWithTag(TypefacePickerTabRowTestTag)
            .fetchSemanticsNode().boundsInRoot
        val listBounds = composeRule.onNodeWithTag(TypefacePickerSystemListTestTag)
            .fetchSemanticsNode().boundsInRoot
        val manageBounds = composeRule.onNodeWithTag(TypefacePickerManageTestTag)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(tabBounds.left < listBounds.left)
        assertTrue(tabBounds.right > listBounds.right)
        assertTrue(tabBounds.left < manageBounds.left)
        assertTrue(tabBounds.right > manageBounds.right)
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.dialog_typeface_done_action)
        ).assertDoesNotExist()

        composeRule.onNodeWithTag(TypefacePickerPagerTestTag).performTouchInput { swipeLeft() }
        composeRule.onNodeWithText(importedTab).assertIsSelected()
    }
}
