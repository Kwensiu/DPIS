package com.dpis.module.ui.compose

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Semantic dimensions shared by new Compose surfaces; feature screens should not invent values. */
@Immutable
data class DpisTokens(
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 16.dp,
    val spaceLg: Dp = 24.dp,
    val spaceXl: Dp = 32.dp,
    val contentMaxWidth: Dp = 1_280.dp
)

val LocalDpisTokens = staticCompositionLocalOf { DpisTokens() }

val DpisTypography = Typography()

/** Expanded page title used by large/flexible app bars. */
val Typography.dpisExpandedPageTitle: TextStyle
    get() = headlineLarge.copy(fontSize = 36.sp, lineHeight = 44.sp)

val DpisShapes = Shapes()
