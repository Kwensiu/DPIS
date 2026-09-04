package com.dpis.module.ui.compose

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorControlsLayoutTest {
    @Test
    fun secondaryControlShrinksWithAvailableWidth() {
        assertEquals(140.8f, editorSecondaryControlWidth(360.dp, 8.dp, false).value, 0.01f)
        assertEquals(116.8f, editorSecondaryControlWidth(300.dp, 8.dp, false).value, 0.01f)
    }

    @Test
    fun secondaryControlNeverExceedsInputWidth() {
        val modeWidth = editorSecondaryControlWidth(250.dp, 8.dp, false)
        assertTrue(modeWidth <= (250.dp - 8.dp) / 2)
    }

    @Test
    fun landscapeSecondaryControlUsesAnEvenSplitBeforeTheCap() {
        assertEquals(146.dp, editorSecondaryControlWidth(300.dp, 8.dp, true))
        assertEquals(180.dp, editorSecondaryControlWidth(500.dp, 8.dp, true))
    }
}
