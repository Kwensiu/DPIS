package com.dpis.module.ui.presentation.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeDismissFocusPolicyTest {
    @Test
    fun hidingImeAfterItWasShownReleasesFocus() {
        assertTrue(
            shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = true,
            ),
        )
    }

    @Test
    fun showingImeDoesNotReleaseFocus() {
        assertFalse(
            shouldClearFocus(
                inputFocused = true,
                imeVisible = true,
                previousImeVisible = false,
            ),
        )
    }

    @Test
    fun stayingHiddenDoesNotReleaseFocus() {
        assertFalse(
            shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = false,
            ),
        )
    }

    @Test
    fun unfocusedInputDoesNotReleaseFocusForImeTransition() {
        assertFalse(
            shouldClearFocus(
                inputFocused = false,
                imeVisible = false,
                previousImeVisible = true,
            ),
        )
    }

    @Test
    fun windowFocusIsNotPartOfImeDismissalPolicy() {
        assertTrue(
            shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = true,
            ),
        )
        assertFalse(
            shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = false,
            ),
        )
    }

    private fun shouldClearFocus(
        inputFocused: Boolean,
        imeVisible: Boolean,
        previousImeVisible: Boolean,
    ): Boolean = ImeDismissFocusPolicy.shouldClearFocus(
        inputFocused = inputFocused,
        imeVisible = imeVisible,
        previousImeVisible = previousImeVisible,
    )
}
