package com.dpis.module.ui.presentation.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.Dp

/** Shared motion durations used by more than one Compose surface. */
internal object ComposeMotionTokens {
    const val SHEET_SETTLE_DURATION_MILLIS = 220
    const val CONTENT_TRANSITION_DURATION_MILLIS = 180
    const val CONTENT_EXIT_DURATION_MILLIS = 140
    const val PAGE_EXIT_DURATION_MILLIS = 120
    const val MODE_TRANSITION_DURATION_MILLIS = 200
    const val FOCUS_PAN_DURATION_MILLIS = 220
    const val SHEET_DESTINATION_DURATION_MILLIS = 220
    const val SHEET_DESTINATION_FADE_DURATION_MILLIS = 140
    const val SHEET_DESTINATION_HEIGHT_DURATION_MILLIS = 180

    // Editor sheets resize around measured content anchors. A shared spring keeps app and
    // template editors equally calm without changing their feature-owned anchor state machines.
    val EDITOR_SHEET_RESIZE_SPEC = spring<Dp>(
        dampingRatio = 0.82f,
        stiffness = 450f,
    )

    val EditorSheetMotionScheme = object : MotionScheme {
        private fun <T> animationSpec(durationMillis: Int): FiniteAnimationSpec<T> = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing,
        )

        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = animationSpec(360)

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = animationSpec(300)

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = animationSpec(440)

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = animationSpec(300)

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = animationSpec(240)

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = animationSpec(380)
    }
}
