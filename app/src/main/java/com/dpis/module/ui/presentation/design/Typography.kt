package com.dpis.module.ui.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val AppTypography = Typography()

/** Expanded page title used by large/flexible app bars. */
val Typography.expandedPageTitle: TextStyle
    get() = headlineLarge.copy(fontSize = 36.sp, lineHeight = 44.sp)
