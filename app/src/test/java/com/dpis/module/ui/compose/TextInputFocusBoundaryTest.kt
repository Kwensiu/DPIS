package com.dpis.module.ui.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextInputFocusBoundaryTest {
    @Test
    fun outsideGestureIsEligibleForDismissalOnlyWhileAnInputIsFocused() {
        val boundary = TextInputFocusBoundary()

        assertFalse(boundary.hasFocusedInput)

        boundary.updateInputFocus("viewport", true)
        assertTrue(boundary.hasFocusedInput)

        boundary.updateInputFocus("viewport", false)
        assertFalse(boundary.hasFocusedInput)
    }

    @Test
    fun removingAnInputAlsoRemovesItsFocusState() {
        val boundary = TextInputFocusBoundary()

        boundary.updateInputFocus("font", true)
        boundary.removeInput("font")

        assertFalse(boundary.hasFocusedInput)
    }

    @Test
    fun animationDirectionUsesThePreviousStableImeHeight() {
        val boundary = TextInputFocusBoundary()

        boundary.beginImeAnimation(900)
        assertTrue(boundary.isImeAnimationOpening)
        boundary.updateAnimatedImeBottom(900)
        boundary.finishImeAnimation()

        boundary.beginImeAnimation(900)
        assertFalse(boundary.isImeAnimationOpening)
    }
}
