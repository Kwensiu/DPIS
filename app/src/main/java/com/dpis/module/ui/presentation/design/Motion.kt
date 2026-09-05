package com.dpis.module.ui.compose

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

/** Shared motion durations used by more than one Compose surface. */
internal object ComposeMotionTokens {
    const val SHEET_SETTLE_DURATION_MILLIS = 220
    const val CONTENT_TRANSITION_DURATION_MILLIS = 180
    const val CONTENT_EXIT_DURATION_MILLIS = 140
    const val PAGE_EXIT_DURATION_MILLIS = 120
    const val MODE_TRANSITION_DURATION_MILLIS = 200
    const val FOCUS_PAN_DURATION_MILLIS = 220

    // Editor sheets resize around measured content anchors. A shared spring keeps app and
    // template editors equally calm without changing their feature-owned anchor state machines.
    val EDITOR_SHEET_RESIZE_SPEC = spring<Dp>(
        dampingRatio = 0.82f,
        stiffness = 450f,
    )
}
