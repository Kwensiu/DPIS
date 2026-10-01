package com.dpis.module

import android.util.DisplayMetrics
import com.dpis.module.quirks.WechatDpiRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test

class WechatDpiRuntimeTest {
    @Test
    fun returnsAnIndependentMetricsCopyAtTheTargetDpi() {
        val source = DisplayMetrics()
        source.density = 2.0f
        source.densityDpi = 320
        source.scaledDensity = 2.5f

        val detached = WechatDpiRuntime.detached(source, 400)

        assertNotSame(source, detached)
        assertEquals(2.0f, source.density, 0.0001f)
        assertEquals(320, source.densityDpi)
        assertEquals(2.5f, source.scaledDensity, 0.0001f)
        assertEquals(2.5f, detached!!.density, 0.0001f)
        assertEquals(400, detached.densityDpi)
        assertEquals(3.125f, detached.scaledDensity, 0.0001f)
    }

    @Test
    fun ignoresMissingDpiOrUnusableMetrics() {
        val metrics = DisplayMetrics()
        metrics.density = 0f
        metrics.densityDpi = 320
        metrics.scaledDensity = 2.0f

        assertNull(WechatDpiRuntime.detached(metrics, 400))
        assertNull(WechatDpiRuntime.detached(metrics, 0))
        assertNull(WechatDpiRuntime.detached(null, 400))
    }

    @Test
    fun computesWechatBottomTabIconScaleCompatibly() {
        assertEquals(1.1666666f, WechatDpiRuntime.bottomTabIconScale(400), 0.0001f)
        assertEquals(0.5833333f, WechatDpiRuntime.bottomTabIconScale(200), 0.0001f)
    }
}
