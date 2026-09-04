package com.dpis.module.ui.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Dp

internal enum class EdgeOcclusionFadeDirection {
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
}

/**
 * Draws a transient surface-colored mask at a content boundary.
 *
 * The mask is deliberately an overlay, rather than layout space, so scrolling
 * content keeps its measured position and only the obscured edge is softened.
 */
internal fun Modifier.edgeOcclusionFade(
    visibility: Float,
    direction: EdgeOcclusionFadeDirection,
    edgePosition: Float? = null,
): Modifier = drawWithContent {
    drawContent()
    drawEdgeOcclusionFade(visibility, direction, edgePosition)
}

internal fun DrawScope.drawEdgeOcclusionFade(
    visibility: Float,
    direction: EdgeOcclusionFadeDirection,
    edgePosition: Float? = null,
) {
    val alpha = visibility.coerceIn(0f, 1f) * EdgeOcclusionFadeTokens.MaxAlpha
    if (alpha == 0f) return
    val height = EdgeOcclusionFadeTokens.Height.toPx()
    val isFadeRegion = size.height <= height * 1.5f
    val startY = if (isFadeRegion) {
        0f
    } else {
        when (direction) {
            EdgeOcclusionFadeDirection.TOP_TO_BOTTOM -> edgePosition ?: (size.height - 1.dp.toPx())
            EdgeOcclusionFadeDirection.BOTTOM_TO_TOP -> -height
        }
    }
    val endY = if (isFadeRegion) {
        size.height
    } else {
        when (direction) {
            EdgeOcclusionFadeDirection.TOP_TO_BOTTOM -> startY + height
            EdgeOcclusionFadeDirection.BOTTOM_TO_TOP -> 0f
        }
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = when (direction) {
                EdgeOcclusionFadeDirection.TOP_TO_BOTTOM -> listOf(
                    EdgeOcclusionFadeTokens.ShadowColor.copy(alpha = alpha),
                    Color.Transparent,
                )
                EdgeOcclusionFadeDirection.BOTTOM_TO_TOP -> listOf(
                    Color.Transparent,
                    EdgeOcclusionFadeTokens.ShadowColor.copy(alpha = alpha),
                )
            },
            startY = startY,
            endY = endY,
        ),
        topLeft = Offset(0f, if (isFadeRegion) 0f else startY),
        size = Size(size.width, if (isFadeRegion) size.height else height),
    )
}

internal object EdgeOcclusionFadeTokens {
    val Height = 4.dp
    // A theme-independent shadow keeps the boundary legible in both light and dark surfaces.
    val ShadowColor = Color.Black
    const val MaxAlpha = 0.12f
}

/** Fades scrollable dialog content into its owning surface, like horizontal chip fades. */
internal fun Modifier.dialogListContentFade(
    state: LazyListState,
    edgeColor: Color,
    edgeHeight: Dp = EdgeFadeTokens.Width,
): Modifier = drawWithContent {
    drawContent()
    val edgePx = edgeHeight.toPx()
    val hasHiddenTop = state.canScrollBackward
    val hasHiddenBottom = state.canScrollForward
    if (hasHiddenTop) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(edgeColor, Color.Transparent),
                startY = 0f,
                endY = edgePx,
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, edgePx),
        )
    }
    if (hasHiddenBottom) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, edgeColor),
                startY = size.height - edgePx,
                endY = size.height,
            ),
            topLeft = Offset(0f, size.height - edgePx),
            size = Size(size.width, edgePx),
        )
    }
}
