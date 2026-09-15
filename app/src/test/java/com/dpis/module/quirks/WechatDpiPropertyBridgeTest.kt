package com.dpis.module.quirks

import com.dpis.module.appconfig.WechatDpiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WechatDpiPropertyBridgeTest {
    @Test
    fun propertyNamesUseStableHexSuffixForPackage() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        val suffix = String.format("%08x", packageName.hashCode())
        assertEquals("debug.dpis.wechat.dpi.$suffix", WechatDpiPropertyBridge.propertyNameForPackage(packageName))
        assertEquals(
            "persist.debug.dpis.wechat.dpi.$suffix",
            WechatDpiPropertyBridge.persistentPropertyNameForPackage(packageName),
        )
    }

    @Test
    fun readDpiTreatsMissingPackageAsUnset() {
        assertEquals(0, WechatDpiPropertyBridge.readDpi(null))
        assertEquals(0, WechatDpiPropertyBridge.readDpi(""))
        assertEquals(0, WechatDpiPropertyBridge.readDpi("   "))
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(null, "360", "480"))
    }

    @Test
    fun readDpiPrefersVolatilePropertyThenPersistent() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        assertEquals(360, WechatDpiPropertyBridge.readDpiForTest(packageName, "360", "480"))
        assertEquals(480, WechatDpiPropertyBridge.readDpiForTest(packageName, "  ", "480"))
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, null, null))
    }

    @Test
    fun readDpiRejectsInvalidOrOutOfRangePayloads() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, "not-a-number", null))
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, "199", null))
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, "1001", null))
        assertEquals(200, WechatDpiPropertyBridge.readDpiForTest(packageName, " 200 ", null))
        assertEquals(1000, WechatDpiPropertyBridge.readDpiForTest(packageName, "1000", "ignored"))
    }

    @Test
    fun deviceReadFallsBackToZeroWhenSystemPropertiesAreUnavailable() {
        assertEquals(0, WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME))
        assertTrue(WechatDpiPropertyBridge.propertyNameForPackage(WechatDpiConfig.PACKAGE_NAME)
            .startsWith("debug.dpis.wechat.dpi."))
    }
}
