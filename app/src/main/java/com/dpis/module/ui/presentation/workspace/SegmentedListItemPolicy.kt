package com.dpis.module.ui.compose

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared shape policy for every segmented list group.
 *
 * A one-item group has large corners on every edge. Multi-item groups use Material3's default
 * segmented shape policy so its outer and inner radii, including interaction transitions, remain
 * owned by the design system.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun dpisSegmentedShapes(index: Int, count: Int): ListItemShapes {
    val safeCount = count.coerceAtLeast(1)
    val safeIndex = index.coerceIn(0, safeCount - 1)
    val shapes = ListItemDefaults.segmentedShapes(safeIndex, safeCount)
    return if (safeCount == 1) {
        shapes.copy(shape = RoundedCornerShape(16.dp))
    } else {
        shapes
    }
}

/**
 * Applies the press shape from Material's segmented-list policy to custom segmented surfaces.
 *
 * Some rows need bespoke content interactions, such as a slider, and therefore cannot delegate
 * their whole container to [androidx.compose.material3.SegmentedListItem]. Keeping this state
 * resolver beside the shared shape policy prevents those rows from silently losing edge motion.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun rememberSegmentedPressedShape(
    shapes: ListItemShapes,
    interactionSource: MutableInteractionSource,
): Shape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "Segmented pressed shape",
    )
    val baseShape = shapes.shape
    val pressedShape = shapes.pressedShape
    return if (baseShape is Interpolatable && pressedShape is Interpolatable) {
        // Material3 keeps its own equivalent helper internal. Its public shape contract supports
        // this interpolation, which lets custom rows follow the same visual state without using
        // non-public APIs.
        Interpolatable.lerp(baseShape, pressedShape, progress) as? Shape ?: pressedShape
    } else {
        if (isPressed) pressedShape else baseShape
    }
}
