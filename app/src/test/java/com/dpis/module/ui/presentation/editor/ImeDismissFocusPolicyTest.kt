package com.dpis.module.ui.presentation.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeDismissFocusPolicyTest {
    @Test
    fun hidingImeAfterItWasShownReleasesFocus() {
        val shown = ImeDismissFocusPolicy.imeShownWhileFocused(
            inputFocused = true,
            imeVisible = true,
            previouslyShownWhileFocused = false,
        )
        assertTrue(shown)
        assertTrue(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                windowHasFocus = true,
                imeShownWhileFocused = shown,
                lostWindowFocusWhileInputFocused = false,
            ),
        )
    }

    @Test
    fun waitingForImeToAppearDoesNotReleaseFocus() {
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                windowHasFocus = true,
                imeShownWhileFocused = false,
                lostWindowFocusWhileInputFocused = false,
            ),
        )
    }

    @Test
    fun visibleImeDoesNotReleaseFocus() {
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = true,
                windowHasFocus = true,
                imeShownWhileFocused = true,
                lostWindowFocusWhileInputFocused = false,
            ),
        )
    }

    @Test
    fun extractUiReturningToTheWindowReleasesFocus() {
        val lostWindow = ImeDismissFocusPolicy.lostWindowFocusWhileInputFocused(
            inputFocused = true,
            windowHasFocus = false,
            previouslyLostWindowFocusWhileInputFocused = false,
        )
        assertTrue(lostWindow)
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                windowHasFocus = false,
                imeShownWhileFocused = false,
                lostWindowFocusWhileInputFocused = lostWindow,
            ),
        )
        assertTrue(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = true,
                imeVisible = false,
                windowHasFocus = true,
                imeShownWhileFocused = false,
                lostWindowFocusWhileInputFocused = lostWindow,
            ),
        )
    }

    @Test
    fun unfocusedInputResetsImeAndWindowSession() {
        assertFalse(
            ImeDismissFocusPolicy.imeShownWhileFocused(
                inputFocused = false,
                imeVisible = true,
                previouslyShownWhileFocused = true,
            ),
        )
        assertFalse(
            ImeDismissFocusPolicy.lostWindowFocusWhileInputFocused(
                inputFocused = false,
                windowHasFocus = false,
                previouslyLostWindowFocusWhileInputFocused = true,
            ),
        )
        assertFalse(
            ImeDismissFocusPolicy.shouldClearFocus(
                inputFocused = false,
                imeVisible = false,
                windowHasFocus = true,
                imeShownWhileFocused = true,
                lostWindowFocusWhileInputFocused = true,
            ),
        )
    }
}
