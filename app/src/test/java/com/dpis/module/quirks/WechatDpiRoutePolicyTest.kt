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
    fun anotherPackageNeverInstalls() {
        assertFalse(
            WechatDpiRoutePolicy.shouldInstallDedicatedRoute(
                "com.example.other",
                dedicatedDpiConfigured = true,
            ),
        )
    }

    @Test
    fun savedWeChatDpiIsTheGetterDensity() {
        assertEquals(480, WechatDpiRoutePolicy.targetDpi(480))
    }

    @Test
    fun emptyWeChatDpiLeavesTheGetterUnchanged() {
        assertNull(WechatDpiRoutePolicy.targetDpi(0))
    }
}
