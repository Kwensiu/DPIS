package com.dpis.module.ui.presentation.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared edge-fade dimensions used by scrollable Compose surfaces. */
internal object EdgeFadeTokens {
    val Width = 20.dp
    val MinimumContainerHeight = 40.dp
}

/**
 * Color for content that dissolves into the surface behind it.
 *
 * [visibility] scales the owning surface's own alpha; it must not replace that
 * alpha with an independent shadow color.
 */
internal fun owningSurfaceFadeColor(owningSurface: Color, visibility: Float): Color {
    val amount = visibility.coerceIn(0f, 1f)
    return owningSurface.copy(alpha = owningSurface.alpha * amount)
}
