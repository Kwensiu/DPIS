package com.dpis.module

import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.quirks.WechatDpiEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WechatDpiEditorTest {
    @Test
    fun saveLeavesNonWechatPackagesUntouched() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(WechatDpiEditor.save("360", "com.example.app", true, store))
        assertNull(store.getWechatDpi("com.example.app"))
    }

    @Test
    fun saveRejectsInvalidWechatDpi() {
        val store = DpisConfigStore(FakePrefs())
        assertFalse(WechatDpiEditor.save("12", WechatDpiConfig.PACKAGE_NAME, true, store))
        assertNull(store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME))
    }

    @Test
    fun saveWritesValidWechatDpi() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(WechatDpiEditor.save("360", WechatDpiConfig.PACKAGE_NAME, true, store))
        assertEquals(360, store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME))
    }

    @Test
    fun emptyInputIsValidAndClearsStoredDpi() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(WechatDpiEditor.save("360", WechatDpiConfig.PACKAGE_NAME, true, store))
        assertTrue(WechatDpiEditor.isInputValid(""))
        assertTrue(WechatDpiEditor.save("", WechatDpiConfig.PACKAGE_NAME, true, store))
        assertNull(store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME))
    }
}
