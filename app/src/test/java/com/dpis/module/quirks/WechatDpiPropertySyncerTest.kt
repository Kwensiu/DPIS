package com.dpis.module.quirks

import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import org.junit.Assert.assertEquals
import org.junit.Test

class WechatDpiPropertySyncerTest {
    @Test
    fun unusablePackageOrMissingStoreProducesNoWork() {
        WechatDpiPropertySyncer.publishDpiAsync("com.example.app", 360)
        WechatDpiPropertySyncer.syncConfiguredTargetsAsync(null)

        assertEquals("", WechatDpiPropertySyncer.buildSyncCommandForTest(null, 0))
    }

    @Test
    fun clearingDpiTargetsOnlyTheRequestedAndroidUser() {
        assertEquals(
            "setprop 'debug.dpis.wechat.dpi.0.c5fe9776' '0'; " +
                    "setprop 'persist.debug.dpis.wechat.dpi.0.c5fe9776' '0'",
            WechatDpiPropertySyncer.buildDpiCommandForTest("com.tencent.mm", null, 0),
        )
        assertEquals(
            "setprop 'debug.dpis.wechat.dpi.999.c5fe9776' '0'; " +
                    "setprop 'persist.debug.dpis.wechat.dpi.999.c5fe9776' '0'",
            WechatDpiPropertySyncer.buildDpiCommandForTest("com.tencent.mm", null, 999),
        )
    }

    @Test
    fun configuredDpiIsPublishedForTheOwningUser() {
        val store = DpisConfigStore(FakePrefs())
        store.setWechatDpi("com.tencent.mm", 600)

        assertEquals(
            "setprop 'debug.dpis.wechat.dpi.999.c5fe9776' '600'; " +
                    "setprop 'persist.debug.dpis.wechat.dpi.999.c5fe9776' '600'",
            WechatDpiPropertySyncer.buildSyncCommandForTest(store, 999),
        )
    }

    @Test
    fun disabledOrUnconfiguredPackageClearsRuntimeDpi() {
        val disabledStore = DpisConfigStore(FakePrefs()).apply {
            setWechatDpi("com.tencent.mm", 600)
            setTargetDpisEnabled("com.tencent.mm", false)
        }
        val emptyStore = DpisConfigStore(FakePrefs())
        val clearCommand =
            "setprop 'debug.dpis.wechat.dpi.0.c5fe9776' '0'; " +
                    "setprop 'persist.debug.dpis.wechat.dpi.0.c5fe9776' '0'"

        assertEquals(
            clearCommand,
            WechatDpiPropertySyncer.buildSyncCommandForTest(disabledStore, 0)
        )
        assertEquals(clearCommand, WechatDpiPropertySyncer.buildSyncCommandForTest(emptyStore, 0))
    }
}
