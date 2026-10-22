package com.dpis.module.quirks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WechatDpiRoutePolicyTest {
    @Test
    fun viewportAndFontDoNotOpenTheWeChatRoute() {
        assertFalse(
            WechatDpiRoutePolicy.shouldInstallDedicatedRoute(
                "com.tencent.mm",
                dedicatedDpiConfigured = false,
            ),
        )
    }

    @Test
    fun savedWeChatDpiStaysOnTheWeChatRoute() {
        assertTrue(
            WechatDpiRoutePolicy.shouldInstallDedicatedRoute(
                "com.tencent.mm",
                dedicatedDpiConfigured = true,
            ),
        )
    }

    @Test
    fun viewportDensityOwnerSuppressesDedicatedWeChatDpiRoute() {
        assertFalse(
            WechatDpiRoutePolicy.shouldInstallDedicatedRoute(
                "com.tencent.mm",
                dedicatedDpiConfigured = true,
                viewportDensityOwner = true,
            ),
        )
    }

    @Test
    fun disabledViewportLeavesDedicatedWeChatDpiRouteAvailable() {
        assertTrue(
            WechatDpiRoutePolicy.shouldInstallDedicatedRoute(
                "com.tencent.mm",
                dedicatedDpiConfigured = true,
                viewportDensityOwner = false,
            ),
        )
    }

    @Test
    fun anotherPackageNeverInstalls() {
        assertFalse(
            WechatDpiRoutePolicy.shouldInstallDedicatedRoute(
                "com.example.other",
                dedicatedDpiConfigured = true,
            ),
        )
    }

    @Test
    fun emptyPackageNeverOwnsViewportDensity() {
        assertFalse(WechatDpiRoutePolicy.viewportDensityOwner(null))
        assertFalse(WechatDpiRoutePolicy.viewportDensityOwner("com.example.other"))
    }

    @Test
    fun savedDpiIsResolvedFromTheRequestedAndroidUser() {
        val packageName = "com.tencent.mm"
        val values = mapOf(
            WechatDpiPropertyBridge.propertyNameForPackage(packageName, 0) to "500",
            WechatDpiPropertyBridge.propertyNameForPackage(packageName, 999) to "700",
        )

        assertEquals(
            700,
            WechatDpiPropertyBridge.readDpiForTest(packageName, 999) { values[it] },
        )
    }

    @Test
    fun nonWeChatPackageCannotResolveDedicatedDpi() {
        assertNull(WechatDpiRoutePolicy.targetDpi("com.example.other", 0))
    }

    @Test
    fun emptyWeChatDpiLeavesTheGetterUnchanged() {
        assertNull(WechatDpiRoutePolicy.targetDpi("com.tencent.mm", 0))
    }
}
