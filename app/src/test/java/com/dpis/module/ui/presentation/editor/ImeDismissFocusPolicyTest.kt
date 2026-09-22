package com.dpis.module.ui.presentation.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeDismissFocusPolicyTest {
    @Test
    fun hidingImeAfterItWasShownReleasesFocus() {
        assertTrue(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = true,
            ),
        )
    }

    @Test
    fun showingImeDoesNotReleaseFocus() {
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = true,
                previousImeVisible = false,
            ),
        )
    }

    @Test
    fun stayingHiddenDoesNotReleaseFocus() {
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = false,
            ),
        )
    }

    @Test
    fun unfocusedInputDoesNotReleaseFocusForImeTransition() {
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = false,
                imeVisible = false,
                previousImeVisible = true,
            ),
        )
    }

    @Test
    fun windowFocusIsNotPartOfImeDismissalPolicy() {
        assertTrue(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = true,
            ),
        )
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                previousImeVisible = false,
            ),
        )
    }
}
