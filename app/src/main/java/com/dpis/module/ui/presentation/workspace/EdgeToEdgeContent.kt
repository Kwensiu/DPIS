package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/**
 * Keeps the final scrollable item clear of the gesture handle without shrinking
 * the scroll viewport above the navigation area.
 */
@Composable
internal fun edgeToEdgeContentBottomPadding(extraPadding: Dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extraPadding

/** Shared breathing room after the final scroll item without reserving a full gesture inset. */
@Composable
internal fun workspaceContentPadding(base: androidx.compose.foundation.layout.PaddingValues):
    androidx.compose.foundation.layout.PaddingValues {
    val direction = LocalLayoutDirection.current
    return androidx.compose.foundation.layout.PaddingValues(
        start = base.calculateStartPadding(direction),
        top = base.calculateTopPadding(),
        end = base.calculateEndPadding(direction),
        bottom = base.calculateBottomPadding() + LocalSpacing.current.xl
    )
}
