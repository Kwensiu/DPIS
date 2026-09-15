package com.dpis.module.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp

/**
 * A horizontally scrollable row whose edge fades reflect the current scroll position.
 *
 * Overflow dissolves into [owningSurfaceColor]. Pass the color of the surface behind this
 * row. Do not use a black occlusion shadow; that belongs to [edgeOcclusionFade]. The fade is
 * only present while content remains hidden on that side, so a fully visible row and either
 * terminal scroll position do not retain misleading decoration.
 */
@Composable
internal fun HorizontalScrollWithEdgeFade(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    owningSurfaceColor: Color,
    edgeWidth: Dp = EdgeFadeTokens.Width,
    content: @Composable RowScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val startFadeVisibility by animateFloatAsState(
        targetValue = if (scrollState.value > 0) 1f else 0f,
        animationSpec = tween(HorizontalEdgeFadeTokens.VisibilityAnimationDurationMillis),
        label = "horizontal-edge-fade-start",
    )
    val endFadeVisibility by animateFloatAsState(
        targetValue = if (scrollState.value < scrollState.maxValue) 1f else 0f,
        animationSpec = tween(HorizontalEdgeFadeTokens.VisibilityAnimationDurationMillis),
        label = "horizontal-edge-fade-end",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EdgeFadeTokens.MinimumContainerHeight)
            .horizontalEdgeFade(
                startVisibility = startFadeVisibility,
                endVisibility = endFadeVisibility,
                owningSurfaceColor = owningSurfaceColor,
                edgeWidth = edgeWidth,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
    }
}

/** Draws start/end surface fades inside the receiving node's bounds. */
internal fun Modifier.horizontalEdgeFade(
    startVisibility: Float,
    endVisibility: Float,
    owningSurfaceColor: Color,
    edgeWidth: Dp = EdgeFadeTokens.Width,
): Modifier = drawWithContent {
    drawContent()
    drawHorizontalEdgeFade(startVisibility, endVisibility, owningSurfaceColor, edgeWidth)
}

private fun DrawScope.drawHorizontalEdgeFade(
    startVisibility: Float,
    endVisibility: Float,
    owningSurfaceColor: Color,
    edgeWidth: Dp,
) {
    val edgePx = edgeWidth.toPx().coerceAtMost(size.width / 2f)
    if (edgePx <= 0f) return
    val startColor = owningSurfaceFadeColor(owningSurfaceColor, startVisibility)
    val endColor = owningSurfaceFadeColor(owningSurfaceColor, endVisibility)
    if (startColor.alpha > 0f) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(startColor, Color.Transparent),
                startX = 0f,
                endX = edgePx,
            ),
            topLeft = Offset.Zero,
            size = Size(edgePx, size.height),
        )
    }
    if (endColor.alpha > 0f) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, endColor),
                startX = size.width - edgePx,
                endX = size.width,
            ),
            topLeft = Offset(size.width - edgePx, 0f),
            size = Size(edgePx, size.height),
        )
    }
}

internal object HorizontalEdgeFadeTokens {
    const val VisibilityAnimationDurationMillis = 180
}
