package com.dpis.module.quirks

import com.dpis.module.appconfig.WechatDpiConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class WechatDpiPropertyBridgeTest {
    @Test
    fun propertyNamesIncludeAndroidUserAndPackageIdentity() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        val suffix = String.format("%08x", packageName.hashCode())
        assertEquals(
            "debug.dpis.wechat.dpi.0.$suffix",
            WechatDpiPropertyBridge.propertyNameForPackage(packageName, 0),
        )
        assertEquals(
            "debug.dpis.wechat.dpi.999.$suffix",
            WechatDpiPropertyBridge.propertyNameForPackage(packageName, 999),
        )
        assertEquals(
            "persist.debug.dpis.wechat.dpi.999.$suffix",
            WechatDpiPropertyBridge.persistentPropertyNameForPackage(packageName, 999),
        )
    }

    @Test
    fun appProcessWithNoUserSpecificValueDoesNotReadAnotherUsersDpi() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        val values = mapOf(
            WechatDpiPropertyBridge.propertyNameForPackage(packageName, 0) to "500",
            WechatDpiPropertyBridge.persistentPropertyNameForPackage(packageName, 0) to "500",
        )

        assertEquals(
            0,
            WechatDpiPropertyBridge.readDpiForTest(packageName, 999) { values[it] },
        )
    }

    @Test
    fun readDpiTreatsMissingPackageAsUnset() {
        assertEquals(0, WechatDpiPropertyBridge.readDpi(null))
        assertEquals(0, WechatDpiPropertyBridge.readDpi(""))
        assertEquals(0, WechatDpiPropertyBridge.readDpi("   "))
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(null, 0) { "480" })
    }

    @Test
    fun readDpiPrefersVolatilePropertyThenPersistent() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        assertEquals(
            360,
            WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { name ->
                if (name == WechatDpiPropertyBridge.propertyNameForPackage(packageName, 0)) "360"
                else null
            },
        )
        assertEquals(
            480,
            WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { name ->
                if (name == WechatDpiPropertyBridge.persistentPropertyNameForPackage(
                        packageName,
                        0
                    )
                ) "480"
                else " "
            },
        )
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { null })
    }

    @Test
    fun readDpiRejectsInvalidOrOutOfRangePayloads() {
        val packageName = WechatDpiConfig.PACKAGE_NAME
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { "not-a-number" })
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { "199" })
        assertEquals(0, WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { "1001" })
        assertEquals(200, WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { " 200 " })
        assertEquals(1000, WechatDpiPropertyBridge.readDpiForTest(packageName, 0) { "1000" })
    }

}
