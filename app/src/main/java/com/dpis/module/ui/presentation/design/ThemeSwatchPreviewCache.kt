package com.dpis.module.ui.compose

import androidx.compose.material3.ColorScheme
import com.dpis.module.settings.ThemeModeStore

/**
 * Preview schemes for the static theme-color row.
 *
 * Generating a Material scheme is too expensive for the Theme page's first
 * frame. Callers paint the seed immediately, then fill container tones from
 * this cache after a background [prefetch] or [get].
 */
internal object ThemeSwatchPreviewCache {
    fun peek(
        colorId: String,
        paletteStyle: String,
        colorSpecification: String,
    ): ColorScheme? = ColorSchemeFactory.peek(
        seedColor = ColorSchemeFactory.seedColor(colorId),
        darkTheme = false,
        paletteStyle = paletteStyle,
        requestedSpecification = colorSpecification,
    )

    fun get(
        colorId: String,
        paletteStyle: String,
        colorSpecification: String,
    ): ColorScheme = ColorSchemeFactory.create(
        seedColor = ColorSchemeFactory.seedColor(colorId),
        darkTheme = false,
        paletteStyle = paletteStyle,
        requestedSpecification = colorSpecification,
    )

    fun prefetch(paletteStyle: String, colorSpecification: String) {
        for (colorId in ThemeModeStore.supportedThemeColors()) {
            get(colorId, paletteStyle, colorSpecification)
        }
    }

    fun clear() {
        ColorSchemeFactory.clear()
    }
}
