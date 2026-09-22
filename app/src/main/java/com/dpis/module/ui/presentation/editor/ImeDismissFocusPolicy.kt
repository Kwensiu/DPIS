package com.dpis.module.ui.presentation.editor

/**
 * Decides when hiding the IME should also release text-field focus.
 *
 * Compose keeps field focus when the IME is hidden. This policy only models the visible-to-hidden
 * IME transition; window focus changes are unrelated and must not end text input implicitly.
 */
internal object ImeDismissFocusPolicy {
    fun shouldClearFocus(
        inputFocused: Boolean,
        imeVisible: Boolean,
        previousImeVisible: Boolean,
    ): Boolean = inputFocused && previousImeVisible && !imeVisible
}
