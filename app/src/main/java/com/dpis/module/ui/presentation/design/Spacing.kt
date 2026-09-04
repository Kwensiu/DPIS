package com.dpis.module.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Semantic dimensions shared by new Compose surfaces; feature screens should not invent values. */
@Immutable
data class Spacing(
    // The shared scale keeps spacing discoverable: no spacing, then 4/8/12/16/24/32dp.
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val contentMaxWidth: Dp = 1_280.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
