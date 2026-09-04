package com.dpis.module.ui.compose

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
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
