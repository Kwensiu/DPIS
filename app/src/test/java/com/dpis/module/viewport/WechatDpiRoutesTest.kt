package com.dpis.module

import com.dpis.module.quirks.WechatDpiRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WechatDpiRoutesTest {
    @Test
    fun resolvesExactVersionCodeRoutes() {
        assertRoute(3180L, "8.0.78", "le5.f", 2)
        assertRoute(3120L, "8.0.74", "j65.f", 2)
        assertRoute(3100L, "8.0.72", "w45.f")
        assertRoute(3080L, "8.0.71", "q35.f")
        assertRoute(3060L, "8.0.70", "d25.f")
        assertRoute(3040L, "8.0.69", "az4.f")
        assertRoute(2460L, "8.0.42", "hy3.d")
    }

    @Test
    fun recordsExactMethodTargetsForWechat8074() {
        val route = WechatDpiRoutes.forVersionCode(3120L)
        assertNotNull(route)
        assertEquals("d", route!!.densityMethodTargets[0].methodName)
        assertEquals(
            WechatDpiRoutes.MethodTarget.Kind.DISPLAY_METRICS_GETTER,
            route.densityMethodTargets[0].kind
        )
        assertEquals("e", route.densityMethodTargets[1].methodName)
        assertEquals(2, route.densityMethodTargets.size)
        assertTrue(route.bottomTabIconScaleEnabled)
    }

    @Test
    fun recordsBothDensityGettersForWechat8078WithoutBottomTabScale() {
        val route = WechatDpiRoutes.forVersionCode(3180L)
        assertNotNull(route)
        assertEquals("d", route!!.densityMethodTargets[0].methodName)
        assertEquals("e", route.densityMethodTargets[1].methodName)
        assertEquals(2, route.densityMethodTargets.size)
        assertFalse(route.bottomTabIconScaleEnabled)
    }

    @Test
    fun rejectsUnknownVersionCodes() {
        assertNull(WechatDpiRoutes.forVersionCode(0L))
        assertNull(WechatDpiRoutes.forVersionCode(-1L))
        assertNull(WechatDpiRoutes.forVersionCode(9999L))
        assertFalse(WechatDpiRoutes.supportsVersionCode(0L))
        assertFalse(WechatDpiRoutes.supportsVersionCode(9999L))
        assertTrue(WechatDpiRoutes.supportsVersionCode(3100L))
    }

    @Test
    fun routeListHasUniqueVersionCodes() {
        val versionCodes = WechatDpiRoutes.all().map { it.versionCode }
        assertEquals(versionCodes.size, versionCodes.toSet().size)
        assertEquals(7, versionCodes.size)
    }

    private fun assertRoute(
        versionCode: Long,
        versionName: String,
        className: String,
        methodTargetCount: Int = 0
    ) {
        val route = WechatDpiRoutes.forVersionCode(versionCode)
        assertNotNull(route)
        assertEquals(versionCode, route!!.versionCode)
        assertEquals(versionName, route.versionName)
        assertEquals(className, route.className)
        assertEquals(className, route.routeKey())
        assertEquals(methodTargetCount, route.densityMethodTargets.size)
        if (versionCode != 3120L) assertFalse(route.bottomTabIconScaleEnabled)
    }
}
