package com.dpis.module.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.dpis.module.settings.ThemeModeStore

internal val LocalDpisClickHapticsEnabled = staticCompositionLocalOf { true }

/** Resolves the stored preference at every Compose root without duplicating mode policy. */
@Composable
fun dpisDarkTheme(): Boolean = ThemeModeStore.isDarkTheme(
    LocalContext.current,
    isSystemInDarkTheme(),
)

/**
 * The Compose counterpart to the existing Material3 XML theme.
 *
 * Explicit dynamicColor values keep previews deterministic. Runtime roots use
 * the stored appearance preference by default.
 */
@Composable
fun DpisTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = ThemeModeStore.isDynamicColorEnabled(LocalContext.current),
    themeColor: String = ThemeModeStore.getThemeColor(LocalContext.current),
    paletteStyle: String = ThemeModeStore.getPaletteStyle(LocalContext.current),
    colorSpecification: String = ThemeModeStore.getColorSpecification(LocalContext.current),
    clickHapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val seedColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        DpisColorSchemeFactory.seedColor(themeColor)
    }
    val targetColors = remember(seedColor, darkTheme, paletteStyle, colorSpecification) {
        DpisColorSchemeFactory.create(
            seedColor = seedColor,
            darkTheme = darkTheme,
            paletteStyle = paletteStyle,
            requestedSpecification = colorSpecification,
        )
    }
    val colors = targetColors.animateDpisAsState()

    SideEffect {
        val activity = context.findActivity()
        val isSeparateDialogWindow = activity?.window?.decorView !== view.rootView
        // Dialog and sheet ComposeViews otherwise remain transparent and expose the
        // AppCompat window background, which still follows the system night mode.
        // Keep dialogs one elevation brighter than pages, including their anti-aliased edge.
        val rootColor = if (isSeparateDialogWindow) {
            colors.surfaceContainerHigh.toArgb()
        } else {
            colors.surface.toArgb()
        }
        view.setBackgroundColor(rootColor)
        if (isSeparateDialogWindow) {
            view.rootView.background?.mutate()?.setTint(rootColor)
        }
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                // Edge-to-edge bars are transparent, so icon contrast must follow the actual
                // DPIS color scheme rather than the system uiMode configuration.
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
