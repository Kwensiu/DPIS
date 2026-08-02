package com.dpis.module.ui.compose

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DpisLightColors = lightColorScheme(
    primary = Color(0xFF1D5D86),
    secondary = Color(0xFF4F6275),
    tertiary = Color(0xFF6A5677),
    error = Color(0xFFBA1A1A)
)

private val DpisDarkColors = darkColorScheme(
    primary = Color(0xFF9DCCFF),
    secondary = Color(0xFFB7C9DD),
    tertiary = Color(0xFFD6BDE3),
    error = Color(0xFFFFB4AB)
)

internal val LocalDpisClickHapticsEnabled = staticCompositionLocalOf { true }

/**
 * The Compose counterpart to the existing Material3 XML theme.
 *
 * Dynamic color is deliberately an input, not a stored preference. Existing
 * settings remain the only authority for DPIS appearance and interface scale.
 */
@Composable
fun DpisTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = true,
    clickHapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DpisDarkColors
        else -> DpisLightColors
    }

    CompositionLocalProvider(
        LocalDpisTokens provides DpisTokens(),
        LocalDpisClickHapticsEnabled provides clickHapticsEnabled
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = DpisTypography,
            shapes = DpisShapes,
            content = content
        )
    }
}
