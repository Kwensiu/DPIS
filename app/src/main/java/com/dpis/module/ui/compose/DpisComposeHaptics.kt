package com.dpis.module.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Matches legacy press feedback for discrete actions without coupling Compose screens to View
 * touch listeners. Continuous controls use their own step feedback instead.
 */
@Composable
fun rememberDpisConfirmAction(action: () -> Unit): () -> Unit =
    rememberDpisConfirmAction(hapticFeedbackEnabled = true, action = action)

/** Allows a future user preference to suppress touch feedback without changing action semantics. */
@Composable
fun rememberDpisConfirmAction(
    hapticFeedbackEnabled: Boolean,
    action: () -> Unit
): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(action, hapticFeedback, hapticFeedbackEnabled) {
        {
            if (hapticFeedbackEnabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            action()
        }
    }
}

fun HapticFeedback.performDpisConfirm() {
    performHapticFeedback(HapticFeedbackType.Confirm)
}
