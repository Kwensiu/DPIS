package com.dpis.module.ui.presentation.design

import com.dpis.module.settings.ThemeModeStore
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import com.dpis.module.ui.presentation.design.ColorSchemeFactory
import com.dpis.module.ui.presentation.design.toSemanticColors
import com.dpis.module.ui.presentation.design.toWearColorScheme

class ColorSchemeFactoryTest {
    @After
    fun resetCache() {
        ColorSchemeFactory.clear()
    }
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

    @Test
    fun createReusesTheSchemeForTheSameInputs() {
        val first = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_GREEN),
            darkTheme = true,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        )
        val second = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_GREEN),
            darkTheme = true,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        )
        assertSame(first, second)
    }

    @Test
    fun semanticColorsFollowTheGeneratedSchemeLightOrDark() {
        val light = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_PURPLE),
            darkTheme = false,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        ).toSemanticColors()
        val dark = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_PURPLE),
            darkTheme = true,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        ).toSemanticColors()

        assertNotEquals(light.successContainer, dark.successContainer)
        assertNotEquals(light.warningContainer, dark.warningContainer)
        assertNotEquals(light.successContainer, light.warningContainer)
        assertNotEquals(light.successContainer, light.onSuccessContainer)
        assertNotEquals(dark.successContainer, dark.onSuccessContainer)
    }

    @Test
    fun wearMappingCopiesPhoneSchemeRoles() {
        val phone = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_GREEN),
            darkTheme = true,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        )
        val wear = phone.toWearColorScheme(androidx.wear.compose.material3.ColorScheme())

        assertEquals(phone.primary, wear.primary)
        assertEquals(phone.surfaceContainer, wear.surfaceContainer)
        assertEquals(phone.background, wear.background)
        assertEquals(phone.error, wear.error)
        assertEquals(phone.onPrimary, wear.onPrimary)
    }
}
