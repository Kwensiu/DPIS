package com.dpis.module.ui.presentation.design

import com.dpis.module.settings.ThemeModeStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import com.dpis.module.ui.presentation.design.ColorSchemeFactory
import com.dpis.module.ui.presentation.design.ThemeSwatchPreviewCache

class ThemeSwatchPreviewCacheTest {
    @Before
    @After
    fun resetCache() {
        ThemeSwatchPreviewCache.clear()
    }

    @Test
    fun peekMissesUntilTheSchemeIsCreated() {
        assertNull(
            ThemeSwatchPreviewCache.peek(
                ThemeModeStore.COLOR_GREEN,
                ThemeModeStore.STYLE_TONAL_SPOT,
                ThemeModeStore.SPEC_2025,
            ),
        )

        val created = ThemeSwatchPreviewCache.get(
            ThemeModeStore.COLOR_GREEN,
            ThemeModeStore.STYLE_TONAL_SPOT,
            ThemeModeStore.SPEC_2025,
        )

        assertEquals(
            created,
            ThemeSwatchPreviewCache.peek(
                ThemeModeStore.COLOR_GREEN,
                ThemeModeStore.STYLE_TONAL_SPOT,
                ThemeModeStore.SPEC_2025,
            ),
        )
    }

    @Test
    fun prefetchFillsEverySupportedSeedForTheRequestedStyleOnly() {
        ThemeSwatchPreviewCache.prefetch(
            ThemeModeStore.STYLE_TONAL_SPOT,
            ThemeModeStore.SPEC_2025,
        )

        for (colorId in ThemeModeStore.supportedThemeColors()) {
            assertNotNull(
                ThemeSwatchPreviewCache.peek(
                    colorId,
                    ThemeModeStore.STYLE_TONAL_SPOT,
                    ThemeModeStore.SPEC_2025,
                ),
            )
        }
        assertNull(
            ThemeSwatchPreviewCache.peek(
                ThemeModeStore.COLOR_GREEN,
                ThemeModeStore.STYLE_VIBRANT,
                ThemeModeStore.SPEC_2025,
            ),
        )
    }

    @Test
    fun cachedPreviewUsesTheSameContainerTonesAsTheFactory() {
        val cached = ThemeSwatchPreviewCache.get(
            ThemeModeStore.COLOR_GREEN,
            ThemeModeStore.STYLE_TONAL_SPOT,
            ThemeModeStore.SPEC_2025,
        )
        val created = ColorSchemeFactory.create(
            seedColor = ColorSchemeFactory.seedColor(ThemeModeStore.COLOR_GREEN),
            darkTheme = false,
            paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
            requestedSpecification = ThemeModeStore.SPEC_2025,
        )

        assertEquals(created.secondaryContainer, cached.secondaryContainer)
        assertEquals(created.tertiaryContainer, cached.tertiaryContainer)
    }
}
