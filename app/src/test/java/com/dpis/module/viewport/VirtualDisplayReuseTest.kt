package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.viewport.window.WindowBoundsState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VirtualDisplayReuseTest {
    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
    }

    @Test
    fun noCachedDisplayCannotBeReused() {
        val config = displayConfiguration()

        assertNull(
            VirtualDisplayReuse.forMetrics(
                PACKAGE_NAME,
                config,
                false,
                1080,
                2376,
            ),
        )
    }

    @Test
    fun windowScopedCallbackCannotReuseDisplayBaseline() {
        val cached = cachedDisplay()
        VirtualDisplayState.set(cached)

        assertNull(
            VirtualDisplayReuse.forMetrics(
                PACKAGE_NAME,
                displayConfiguration(),
                true,
                1080,
                2376,
            ),
        )
    }

    @Test
    fun activeWindowCannotReuseDisplayBaseline() {
        val cached = cachedDisplay()
        VirtualDisplayState.set(cached)
        WindowBoundsState.record(PACKAGE_NAME, 0, 0, 1080, 2208)

        assertNull(
            VirtualDisplayReuse.forMetrics(
                PACKAGE_NAME,
                displayConfiguration(),
                false,
                1080,
                2376,
            ),
        )
    }

    @Test
    fun differentTargetWidthCannotReuseCachedDisplay() {
        VirtualDisplayState.set(cachedDisplay())
        val config = displayConfiguration().apply {
            smallestScreenWidthDp = 600
        }

        assertNull(
            VirtualDisplayReuse.forMetrics(
                PACKAGE_NAME,
                config,
                false,
                1080,
                2376,
            ),
        )
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

    private fun cachedDisplay() =
        VirtualDisplayOverride.Result(720, 1584, 720, 240, 1080, 2376)

    private fun displayConfiguration() = Configuration().apply {
        screenWidthDp = 720
        screenHeightDp = 1584
        smallestScreenWidthDp = 720
        densityDpi = 240
    }

    private companion object {
        const val PACKAGE_NAME = "com.max.xiaoheihe"
    }
}
