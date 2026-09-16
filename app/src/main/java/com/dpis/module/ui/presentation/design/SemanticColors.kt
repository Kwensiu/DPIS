package com.dpis.module.ui.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Product roles Material 3 does not have. Tones follow the generated scheme's
 * light or dark surface, not XML night qualifiers.
 */
@Immutable
data class SemanticColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val LocalSemanticColors = staticCompositionLocalOf<SemanticColors> {
    error("ComposeDesignSystem must provide SemanticColors")
}

internal fun ColorScheme.toSemanticColors(): SemanticColors {
    val dark = background.luminance() < 0.5f
    return if (dark) {
        SemanticColors(
            successContainer = Color(0xFF3B6B40),
            onSuccessContainer = Color(0xFFD5EFCF),
            warningContainer = Color(0xFF6B5420),
            onWarningContainer = Color(0xFFF8E7C0),
        )
    } else {
        SemanticColors(
            successContainer = Color(0xFFD5EFCF),
            onSuccessContainer = Color(0xFF16351A),
            warningContainer = Color(0xFFF8E7C0),
            onWarningContainer = Color(0xFF4A3400),
        )
    }
}
