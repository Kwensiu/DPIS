package com.dpis.module.ui.compose

import com.dpis.module.settings.ThemeModeStore
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ColorSchemeFactoryTest {
    @Test
    fun expressiveSupports2025Specification() {
        assertEquals(
            ColorSpec.SpecVersion.SPEC_2025,
            ColorSchemeFactory.resolveSpecification(
                ThemeModeStore.STYLE_EXPRESSIVE,
                ThemeModeStore.SPEC_2025,
            ),
        )
    }

    @Test
    fun unsupportedStyleFallsBackTo2021Specification() {
        assertEquals(
            ColorSpec.SpecVersion.SPEC_2021,
            ColorSchemeFactory.resolveSpecification(
                ThemeModeStore.STYLE_FIDELITY,
                ThemeModeStore.SPEC_2025,
            ),
        )
    }

    @Test
    fun differentSeedsGenerateDifferentCompleteSchemes() {
        val purple = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_PURPLE),
            darkTheme = false,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        )
        val green = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_GREEN),
            darkTheme = false,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        )

        assertNotEquals(purple.primary, green.primary)
        assertNotEquals(purple.secondaryContainer, green.secondaryContainer)
        assertNotEquals(purple.tertiaryContainer, green.tertiaryContainer)
        assertNotEquals(purple.surface, green.surface)
        assertNotEquals(purple.surfaceContainer, green.surfaceContainer)
    }

    @Test
    fun defaultStaticThemeColorUsesPurpleSeed() {
        assertEquals(
            ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_PURPLE),
            ColorSchemeFactory.seedColor(ThemeModeStore.DEFAULT_STATIC_THEME_COLOR),
        )
    }
}
