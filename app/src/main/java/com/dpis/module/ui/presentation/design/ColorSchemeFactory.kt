package com.dpis.module.ui.presentation.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dpis.module.settings.ThemeModeStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import java.util.concurrent.ConcurrentHashMap

/**
 * Single authority for DPIS generated colors.
 *
 * A seed, palette style, and specification always produce a complete Material
 * color scheme. Theme rendering and swatch previews must both use this factory.
 * Created schemes are reused so a new Activity's first frame does not wait on HCT.
 */
internal object ColorSchemeFactory {
    private val schemes = ConcurrentHashMap<String, ColorScheme>()

    fun peek(
        seedColor: Color,
        darkTheme: Boolean,
        paletteStyle: String,
        requestedSpecification: String,
    ): ColorScheme? = schemes[key(seedColor, darkTheme, paletteStyle, requestedSpecification)]

    fun create(
        seedColor: Color,
        darkTheme: Boolean,
        paletteStyle: String,
        requestedSpecification: String,
    ): ColorScheme {
        return schemes.getOrPut(key(seedColor, darkTheme, paletteStyle, requestedSpecification)) {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                style = paletteStyle.toMaterialKolorStyle(),
                specVersion = resolveSpecification(paletteStyle, requestedSpecification),
            )
        }
    }

    fun clear() {
        schemes.clear()
    }

    private fun key(
        seedColor: Color,
        darkTheme: Boolean,
        paletteStyle: String,
        requestedSpecification: String,
    ): String {
        val specification = resolveSpecification(paletteStyle, requestedSpecification)
        return "${seedColor.toArgb()}|$darkTheme|$paletteStyle|$specification"
    }

    fun supports2025Specification(paletteStyle: String): Boolean = paletteStyle in setOf(
        ThemeModeStore.STYLE_TONAL_SPOT,
        ThemeModeStore.STYLE_NEUTRAL,
        ThemeModeStore.STYLE_VIBRANT,
        ThemeModeStore.STYLE_EXPRESSIVE,
    )

    fun resolveSpecification(
        paletteStyle: String,
        requestedSpecification: String,
    ): ColorSpec.SpecVersion = if (
        requestedSpecification == ThemeModeStore.SPEC_2025 &&
        supports2025Specification(paletteStyle)
    ) {
        ColorSpec.SpecVersion.SPEC_2025
    } else {
        ColorSpec.SpecVersion.SPEC_2021
    }

    fun seedColor(colorId: String): Color = when (colorId) {
        ThemeModeStore.COLOR_PINK -> Color(0xFFB94073)
        ThemeModeStore.COLOR_RED -> Color(0xFFBA1A1A)
        ThemeModeStore.COLOR_ORANGE -> Color(0xFF944A00)
        ThemeModeStore.COLOR_AMBER -> Color(0xFF8C5300)
        ThemeModeStore.COLOR_YELLOW -> Color(0xFF795900)
        ThemeModeStore.COLOR_LIME -> Color(0xFF5E6400)
        ThemeModeStore.COLOR_GREEN -> Color(0xFF006D39)
        ThemeModeStore.COLOR_CYAN -> Color(0xFF006A64)
        ThemeModeStore.COLOR_TEAL -> Color(0xFF006874)
        ThemeModeStore.COLOR_LIGHT_BLUE -> Color(0xFF00639B)
        ThemeModeStore.COLOR_BLUE -> Color(0xFF335BBC)
        ThemeModeStore.COLOR_INDIGO -> Color(0xFF5355A9)
        ThemeModeStore.COLOR_PURPLE -> Color(0xFF6750A4)
        ThemeModeStore.COLOR_DEEP_PURPLE -> Color(0xFF7E42A4)
        ThemeModeStore.COLOR_BLUE_GREY -> Color(0xFF575D7E)
        ThemeModeStore.COLOR_BROWN -> Color(0xFF7D524A)
        ThemeModeStore.COLOR_GREY -> Color(0xFF5F6162)
        else -> Color(0xFF4A672D)
    }
}

private fun String.toMaterialKolorStyle(): PaletteStyle = when (this) {
    ThemeModeStore.STYLE_NEUTRAL -> PaletteStyle.Neutral
    ThemeModeStore.STYLE_VIBRANT -> PaletteStyle.Vibrant
    ThemeModeStore.STYLE_EXPRESSIVE -> PaletteStyle.Expressive
    ThemeModeStore.STYLE_RAINBOW -> PaletteStyle.Rainbow
    ThemeModeStore.STYLE_FRUIT_SALAD -> PaletteStyle.FruitSalad
    ThemeModeStore.STYLE_MONOCHROME -> PaletteStyle.Monochrome
    ThemeModeStore.STYLE_FIDELITY -> PaletteStyle.Fidelity
    ThemeModeStore.STYLE_CONTENT -> PaletteStyle.Content
    else -> PaletteStyle.TonalSpot
}
