package com.dpis.module

import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.quirks.WechatDpiEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

    @Test
    fun applyAfterPersistKeepsOriginalFailureAndMapsWechatErrors() {
        val store = DpisConfigStore(FakePrefs())
        val failed = AppConfigSaveHandler.Result.failure(R.string.system_settings_save_failed)
        assertSame(
            failed,
            WechatDpiEditor.applyAfterPersist(
                failed,
                "360",
                WechatDpiConfig.PACKAGE_NAME,
                true,
                store,
            ),
        )

        val success = AppConfigSaveHandler.Result.success(0)
        assertSame(
            success,
            WechatDpiEditor.applyAfterPersist(
                success,
                "360",
                "com.example.app",
                true,
                store,
            ),
        )
        assertEquals(
            R.string.status_save_invalid,
            WechatDpiEditor.applyAfterPersist(
                success,
                "12",
                WechatDpiConfig.PACKAGE_NAME,
                true,
                store,
            ).messageResId,
        )
        assertEquals(
            R.string.system_settings_save_failed,
            WechatDpiEditor.applyAfterPersist(
                success,
                "360",
                WechatDpiConfig.PACKAGE_NAME,
                true,
                null,
            ).messageResId,
        )
        assertTrue(
            WechatDpiEditor.applyAfterPersist(
                success,
                "360",
                WechatDpiConfig.PACKAGE_NAME,
                true,
                store,
            ).success,
        )
        assertEquals(360, store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME))
    }
}
