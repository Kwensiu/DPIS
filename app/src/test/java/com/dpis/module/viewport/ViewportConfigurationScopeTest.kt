package com.dpis.module.viewport

import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportConfigurationScopeTest {
    @Test
    fun matchingBoundsAreDisplayScoped() {
        assertFalse(ViewportConfigurationScope.isWindowScopedBounds(3200, 2136, 3200, 2136))
    }

    @Test
    fun smallerFreeformBoundsAreWindowScoped() {
        assertTrue(ViewportConfigurationScope.isWindowScopedBounds(1155, 2053, 3200, 2136))
    }

    @Test
    fun invalidBoundsAreNotWindowScoped() {
        assertFalse(ViewportConfigurationScope.isWindowScopedBounds(0, 100, 3200, 2136))
        assertFalse(ViewportConfigurationScope.isWindowScopedBounds(4000, 100, 3200, 2136))
    }

    @Test
    fun displayConfigurationRequiresPositiveDimensionsAndScale() {
        val valid = Configuration().apply {
            screenWidthDp = 411
            screenHeightDp = 891
            smallestScreenWidthDp = 411
            densityDpi = 420
            fontScale = 1f
        }
        assertTrue(ViewportConfigurationScope.isValidDisplayConfiguration(valid))

        valid.fontScale = 0f
        assertFalse(ViewportConfigurationScope.isValidDisplayConfiguration(valid))
        assertFalse(ViewportConfigurationScope.isValidDisplayConfiguration(null))
    }
}
