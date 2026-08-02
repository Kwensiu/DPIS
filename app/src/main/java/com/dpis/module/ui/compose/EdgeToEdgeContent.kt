package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Keeps the final scrollable item clear of the gesture handle without shrinking
 * the scroll viewport above the navigation area.
 */
@Composable
internal fun edgeToEdgeContentBottomPadding(extraPadding: Dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extraPadding
