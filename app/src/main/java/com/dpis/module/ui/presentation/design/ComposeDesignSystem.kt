package com.dpis.module.ui.presentation.design

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import com.dpis.module.settings.ThemeModeStore

internal val LocalClickHapticsEnabled = staticCompositionLocalOf { true }

/** Resolves the stored preference at every Compose root without duplicating mode policy. */
@Composable
fun resolveDarkTheme(): Boolean {
    val context = LocalContext.current
    return ThemeModeStore.isDarkTheme(
        context,
        isSystemInDarkTheme(),
    )
}

/**
 * Applies the runtime appearance: [ColorSchemeFactory] → Material 3 Expressive,
 * window paint from that scheme, and [LocalSemanticColors].
 *
 * XML `Theme.Dpis` is window chrome only. Explicit dynamicColor values keep
 * previews deterministic; runtime roots use the stored preference by default.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
fun ComposeDesignSystem(
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
        ColorSchemeFactory.seedColor(resolvedThemeColor)
    }
    val colors = remember(
        seedColor,
        darkTheme,
        resolvedDynamicColor,
        resolvedThemeColor,
        resolvedPaletteStyle,
        resolvedColorSpecification,
    ) {
        ColorSchemeFactory.create(
            seedColor = seedColor,
            darkTheme = darkTheme,
            paletteStyle = resolvedPaletteStyle,
            requestedSpecification = resolvedColorSpecification,
        )
    }

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
        } else if (!drawsTransparentActivityBackground) {
            AppWindowBackground.update(rootColor)
            activity?.let { host ->
                host.window.setBackgroundDrawable(ColorDrawable(rootColor))
            }
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
        LocalSpacing provides Spacing(),
        LocalSemanticColors provides colors.toSemanticColors(),
        LocalClickHapticsEnabled provides clickHapticsEnabled,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
        ) {
            Box(Modifier.semantics { testTagsAsResourceId = true }) {
                content()
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal object AppWindowBackground {
    @Volatile
    var surfaceContainerArgb: Int? = null
        private set

    fun update(surfaceContainerArgb: Int) {
        this.surfaceContainerArgb = surfaceContainerArgb
    }
}

/**
 * Paints the window before Compose's first frame so the system can start the
 * Activity transition against an opaque destination instead of waiting.
 */
fun Activity.applyComposeWindowBackground() {
    val translucentAttrs = theme.obtainStyledAttributes(
        intArrayOf(android.R.attr.windowIsTranslucent),
    )
    val translucent = translucentAttrs.getBoolean(0, false)
    translucentAttrs.recycle()
    if (translucent) {
        return
    }
    val cached = AppWindowBackground.surfaceContainerArgb
    val color = if (cached != null) {
        cached
    } else {
        val backgroundAttrs = theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.colorBackground),
        )
        val fallback = backgroundAttrs.getColor(0, android.graphics.Color.BLACK)
        backgroundAttrs.recycle()
        fallback
    }
    window.setBackgroundDrawable(ColorDrawable(color))
}

/** Shared Activity shell for standalone Compose pages. Feature screens own their content. */
fun ComponentActivity.setFeatureContent(
    transparentWindowBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        ComposeDesignSystem(
            darkTheme = resolveDarkTheme(),
            transparentWindowBackground = transparentWindowBackground,
            content = content,
        )
    }
}
