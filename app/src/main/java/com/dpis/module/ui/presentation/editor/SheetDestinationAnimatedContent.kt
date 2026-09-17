package com.dpis.module.ui.presentation.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import com.dpis.module.ui.presentation.design.ComposeMotionTokens

/**
 * Horizontal sheet-page transition used by the editor and other wrap-content sheets.
 *
 * SizeTransform never clips; [clipContentToAnimatedBounds] only clips the shared viewport.
 * Outgoing content must stay drawable until its horizontal exit finishes.
 */
@Composable
internal fun <T> SheetDestinationAnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    animateSize: Boolean = true,
    clipContentToAnimatedBounds: Boolean = true,
    contentKey: (T) -> Any? = { it },
    towardChild: (T) -> Boolean,
    label: String,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier
            .fillMaxWidth()
            .then(if (clipContentToAnimatedBounds) Modifier.clipToBounds() else Modifier),
        transitionSpec = {
            val direction = if (towardChild(targetState)) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(ComposeMotionTokens.SHEET_DESTINATION_DURATION_MILLIS),
                initialOffsetX = { direction * it },
            ) + fadeIn(tween(ComposeMotionTokens.SHEET_DESTINATION_FADE_DURATION_MILLIS))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(ComposeMotionTokens.SHEET_DESTINATION_DURATION_MILLIS),
                        targetOffsetX = { -direction * it },
                    ) + fadeOut(tween(ComposeMotionTokens.SHEET_DESTINATION_FADE_DURATION_MILLIS))) using
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ ->
                            if (animateSize) {
                                tween(ComposeMotionTokens.SHEET_DESTINATION_HEIGHT_DURATION_MILLIS)
                            } else {
                                snap()
                            }
                        },
                    )
        },
        contentKey = contentKey,
        label = label,
    ) { page ->
        Box(
            Modifier.zIndex(
                if (contentKey(page) == contentKey(targetState)) 1f else 0f,
            ),
        ) {
            content(page)
        }
    }
}
