package com.dpis.module.viewport

import android.content.res.Configuration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VirtualDisplayReuseTest {
    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
    }

    @Test
    fun cachedDisplayDoesNotReplaceConfigurationMatchedShortWindow() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(720, 1584, 720, 240, 1080, 2376),
        )
        val config = Configuration().apply {
            screenWidthDp = 720
            screenHeightDp = 1584
            smallestScreenWidthDp = 720
            densityDpi = 240
        }

        assertNull(
            VirtualDisplayReuse.forMetrics(
                "com.max.xiaoheihe",
                config,
                false,
                1080,
                2208,
            ),
        )
    }

    @Test
    fun cachedDisplayDoesNotReplaceConfigurationMatchedWindowHeight() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(720, 1584, 720, 240, 1080, 2376),
        )
        val config = Configuration().apply {
            screenWidthDp = 720
            screenHeightDp = 1584
            smallestScreenWidthDp = 720
            densityDpi = 240
        }

        assertNull(
            VirtualDisplayReuse.forMetrics(
                "com.max.xiaoheihe",
                config,
                false,
                1080,
                1440,
            ),
        )
    }

    @Test
    fun matchingDisplayConfigurationCanReuseCachedDisplay() {
        val cached = VirtualDisplayOverride.Result(720, 1584, 720, 240, 1080, 2376)
        VirtualDisplayState.set(cached)
        val config = Configuration().apply {
            screenWidthDp = 720
            screenHeightDp = 1584
            smallestScreenWidthDp = 720
            densityDpi = 240
        }

        assertEquals(
            cached,
            VirtualDisplayReuse.forMetrics(
                "com.max.xiaoheihe",
                config,
                false,
                1080,
                2376,
            ),
        )
    }
}
