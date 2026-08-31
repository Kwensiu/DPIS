package com.dpis.module.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.dpis.module.settings.ThemeModeStore

internal val LocalDpisClickHapticsEnabled = staticCompositionLocalOf { true }

/** Resolves the stored preference at every Compose root without duplicating mode policy. */
@Composable
fun dpisDarkTheme(): Boolean {
    val context = LocalContext.current
    return ThemeModeStore.isDarkTheme(
        context,
        isSystemInDarkTheme(),
    )
}

/**
 * The Compose counterpart to the existing Material3 XML theme.
 *
 * Explicit dynamicColor values keep previews deterministic. Runtime roots use
 * the stored appearance preference by default.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun DpisTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean? = null,
    themeColor: String? = null,
    paletteStyle: String? = null,
    colorSpecification: String? = null,
    clickHapticsEnabled: Boolean = true,
    transparentWindowBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resolvedDynamicColor = dynamicColor ?: ThemeModeStore.isDynamicColorEnabled(context)
    val resolvedThemeColor = themeColor ?: ThemeModeStore.getThemeColor(context)
    val resolvedPaletteStyle = paletteStyle ?: ThemeModeStore.getPaletteStyle(context)
    val resolvedColorSpecification = colorSpecification ?: ThemeModeStore.getColorSpecification(context)
    val view = LocalView.current
    val seedColor = if (resolvedDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        DpisColorSchemeFactory.seedColor(resolvedThemeColor)
    }
    val targetColors = remember(seedColor, darkTheme, resolvedPaletteStyle, resolvedColorSpecification) {
        DpisColorSchemeFactory.create(
            seedColor = seedColor,
            darkTheme = darkTheme,
            paletteStyle = resolvedPaletteStyle,
            requestedSpecification = resolvedColorSpecification,
        )
    }
    val colors = targetColors.animateDpisAsState()

    SideEffect {
        val activity = context.findActivity()
        val isSeparateDialogWindow = activity?.window?.decorView !== view.rootView
        val drawsTransparentActivityBackground = transparentWindowBackground && !isSeparateDialogWindow
        // Dialog and sheet ComposeViews otherwise remain transparent and expose the
        // AppCompat window background, which still follows the system night mode. A translucent
        // Activity is different: its sheet owns the only opaque Compose surface, so painting the
        // root would hide the Activity behind it.
        val rootColor = if (drawsTransparentActivityBackground) {
            Color.Transparent.toArgb()
        } else if (isSeparateDialogWindow) {
            colors.surfaceContainerHigh.toArgb()
        } else {
            // Workspace roots sit one neutral level above the raw surface so that
            // standard list-item surfaces remain visibly separated in dark mode.
            colors.surfaceContainer.toArgb()
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
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
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
