package com.dpis.module.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Matches legacy press feedback for discrete actions without coupling Compose screens to View
 * touch listeners. Continuous controls use their own step feedback instead.
 */
@Composable
fun rememberConfirmAction(action: () -> Unit): () -> Unit =
    rememberConfirmAction(hapticFeedbackEnabled = true, action = action)

/** Allows a future user preference to suppress touch feedback without changing action semantics. */
@Composable
fun rememberConfirmAction(
    hapticFeedbackEnabled: Boolean,
    action: () -> Unit
): () -> Unit {
    val performFeedback = rememberConfirmFeedback(hapticFeedbackEnabled)
    return remember(action, performFeedback) {
        {
            performFeedback()
            action()
        }
    }
}

/** Shared policy-aware feedback hook for callbacks whose parameters must be preserved. */
@Composable
fun rememberConfirmFeedback(hapticFeedbackEnabled: Boolean = true): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val policyEnabled = LocalDpisClickHapticsEnabled.current
    return remember(hapticFeedback, hapticFeedbackEnabled, policyEnabled) {
        {
            if (policyEnabled && hapticFeedbackEnabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
    }
}
