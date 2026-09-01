package com.dpis.module.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * Shared input focus policy. Text entry itself is continuous, so feedback is emitted only at
 * the unfocused -> focused edge and never when recomposition or keyboard updates repeat focus.
 */
fun Modifier.inputFocusFeedback(
    onFocused: (() -> Unit)? = null,
): Modifier = composed {
    var wasFocused by remember { mutableStateOf(false) }
    val confirmFocus = rememberClickFeedback()
    onFocusChanged { focusState ->
        if (focusState.isFocused && !wasFocused) {
            confirmFocus()
            onFocused?.invoke()
        }
        wasFocused = focusState.isFocused
    }
}

/**
 * Centralizes DPIS feedback policy for discrete Compose interactions. Continuous controls use
 * their own step feedback instead of this confirmation-style response.
 */
@Composable
fun rememberClickAction(action: () -> Unit): () -> Unit =
    rememberClickAction(hapticFeedbackEnabled = true, action = action)

/** Allows a future user preference to suppress touch feedback without changing action semantics. */
@Composable
fun rememberClickAction(
    hapticFeedbackEnabled: Boolean,
    action: () -> Unit
): () -> Unit {
    val performFeedback = rememberClickFeedback(hapticFeedbackEnabled)
    return remember(action, performFeedback) {
        {
            performFeedback()
            action()
        }
    }
}

/** Shared policy-aware feedback hook for callbacks whose parameters must be preserved. */
@Composable
fun rememberClickFeedback(hapticFeedbackEnabled: Boolean = true): () -> Unit {
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

/** Wraps callbacks that preserve a value parameter, such as switches and option selectors. */
@Composable
fun <T> rememberClickValueAction(action: (T) -> Unit): (T) -> Unit {
    val performFeedback = rememberClickFeedback()
    return remember(action, performFeedback) { { value -> performFeedback(); action(value) } }
}

/** Wraps a discrete long-press action, such as choosing a default page. */
@Composable
fun rememberLongPressAction(action: () -> Unit): () -> Unit {
    val performFeedback = rememberLongPressFeedback()
    return remember(action, performFeedback) {
        {
            performFeedback()
            action()
        }
    }
}

/** Shared policy-aware feedback for long-press gestures such as drag handles. */
@Composable
fun rememberLongPressFeedback(hapticFeedbackEnabled: Boolean = true): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val policyEnabled = LocalDpisClickHapticsEnabled.current
    return remember(hapticFeedback, hapticFeedbackEnabled, policyEnabled) {
        {
            if (policyEnabled && hapticFeedbackEnabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}

/** Emits the single long-press acknowledgement owned by a reorderable item. */
@Composable
fun ReorderableDragFeedback(isDragging: Boolean) {
    val feedback = rememberLongPressFeedback()
    LaunchedEffect(isDragging) {
        if (isDragging) feedback()
    }
}

/**
 * Project-standard discrete click surface. Use this instead of [Modifier.clickable] for DPIS
 * product actions so new cards and rows inherit the same feedback policy by construction.
 */
@Composable
fun Modifier.dpisClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = null,
    hapticFeedbackEnabled: Boolean = true,
): Modifier = clickable(
    enabled = enabled,
    role = role,
    onClick = rememberClickAction(hapticFeedbackEnabled, onClick),
)

/**
 * Project-standard click and long-press surface. Long-press feedback is kept separate from
 * drag handles, which own their pointer stream and report feedback when dragging actually starts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.dpisCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role? = null,
    hapticFeedbackEnabled: Boolean = true,
): Modifier = combinedClickable(
    enabled = enabled,
    role = role,
    onClick = rememberClickAction(hapticFeedbackEnabled, onClick),
    onLongClick = if (onLongClick == null) null else rememberLongPressAction(onLongClick),
)

/**
 * Long-press-only surface for content that already has a parent click action. Once the long
 * press wins, the remainder of the pointer stream is consumed so the parent's click cannot run
 * when the finger is released.
 */
@Composable
fun Modifier.dpisLongPress(
    onLongPress: () -> Unit,
    enabled: Boolean = true,
): Modifier {
    val action = rememberLongPressAction(onLongPress)
    return pointerInput(enabled, action) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val longPress = awaitLongPressOrCancellation(down.id)
            if (longPress != null) {
                longPress.consume()
                action()
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }
    }
}
