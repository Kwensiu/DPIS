package com.dpis.module.ui.presentation.editor

/**
 * Decides when hiding the IME should also release text-field focus.
 *
 * Android can hide the keyboard or extract UI (back gesture, Done, overlay) while Compose
 * keeps the field focused. The still-focused field then asks the IME to return, which on
 * small screens is the fullscreen extract panel.
 */
internal object ImeDismissFocusPolicy {
    fun imeShownWhileFocused(
        inputFocused: Boolean,
        imeVisible: Boolean,
        previouslyShownWhileFocused: Boolean,
    ): Boolean {
        if (!inputFocused) {
            return false
        }
        return previouslyShownWhileFocused || imeVisible
    }

    fun lostWindowFocusWhileInputFocused(
        inputFocused: Boolean,
        windowHasFocus: Boolean,
        previouslyLostWindowFocusWhileInputFocused: Boolean,
    ): Boolean {
        if (!inputFocused) {
            return false
        }
        return previouslyLostWindowFocusWhileInputFocused || !windowHasFocus
    }

    fun shouldClearFocus(
        inputFocused: Boolean,
        imeVisible: Boolean,
        windowHasFocus: Boolean,
        imeShownWhileFocused: Boolean,
        lostWindowFocusWhileInputFocused: Boolean,
    ): Boolean {
        if (!inputFocused || imeVisible || !windowHasFocus) {
            return false
        }
        return imeShownWhileFocused || lostWindowFocusWhileInputFocused
    }
}
