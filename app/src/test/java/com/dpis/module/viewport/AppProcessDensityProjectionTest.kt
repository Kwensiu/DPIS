package com.dpis.module.viewport

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import org.junit.Assert.assertEquals
import org.junit.Test

class AppProcessDensityProjectionTest {
    @Test
    fun projectionIsIdempotentWhenTheSameMetricsObjectIsReadAgain() {
        val packageName = "com.example.target"
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(128000))
        val metrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2376
            densityDpi = 480
            density = 3f
            scaledDensity = 3.6f
        }

        AppProcessDensityProjection.applyMetrics(metrics, packageName, store)
        AppProcessDensityProjection.applyMetrics(metrics, packageName, store)

        assertEquals(375, metrics.densityDpi)
        assertEquals(2.34375f, metrics.density, 0.001f)
    }

    @Test
    fun projectsAbsoluteWidthUsingTheSameDensityBoundary() {
        val packageName = "com.example.target"
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(540))
        val metrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2376
            densityDpi = 480
            density = 3f
            scaledDensity = 3f
        }

        AppProcessDensityProjection.applyMetrics(metrics, packageName, store)

        assertEquals(320, metrics.densityDpi)
        assertEquals(2f, metrics.density, 0.001f)
    }

    @Test
    fun projectsRelativeScaleWithoutChangingPhysicalPixels() {
        val packageName = "com.example.target"
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(128000))

        val config = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            densityDpi = 480
        }
        val metrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2376
            densityDpi = 480
            density = 3f
            scaledDensity = 3.6f
        }

        assertEquals(true, AppProcessDensityProjection.applyConfiguration(config, packageName, store))
        assertEquals(true, AppProcessDensityProjection.applyMetrics(metrics, packageName, store))
        assertEquals(461, config.smallestScreenWidthDp)
        assertEquals(1014, config.screenHeightDp)
        assertEquals(375, config.densityDpi)
        assertEquals(375, metrics.densityDpi)
        assertEquals(1080, metrics.widthPixels)
        assertEquals(2376, metrics.heightPixels)
        assertEquals(2.34375f, metrics.density, 0.001f)
        assertEquals(2.8125f, metrics.scaledDensity, 0.001f)
    }
}
