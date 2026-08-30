package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.viewport.DpiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSnapshotTest {
    @Test
    fun reportsFailureWhenViewportWidthCommitFails() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()))
        prefs.setCommitResult(false)

        assertFalse(store.setTargetViewportWidthDp("bin.mt.plus.canary", 320))
        assertEquals(
            DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP,
            store.getTargetViewportWidthDp("bin.mt.plus.canary"),
        )
    }

    @Test
    fun reportsFailureWhenViewportWidthClearCommitFails() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()))
        prefs.setCommitResult(false)

        assertFalse(store.clearTargetViewportWidthDp("bin.mt.plus.canary"))
        assertEquals(
            DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP,
            store.getTargetViewportWidthDp("bin.mt.plus.canary"),
        )
    }

    @Test
    fun disablesSystemServerHooksByDefault() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.isSystemServerHooksEnabled())
    }

    @Test
    fun enablesSystemServerSafeModeByDefault() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.isSystemServerSafeModeEnabled())
    }

    @Test
    fun updatesSystemServerGlobalToggles() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setSystemServerHooksEnabled(false))
        assertTrue(store.setSystemServerSafeModeEnabled(false))
        assertFalse(store.isSystemServerHooksEnabled())
        assertFalse(store.isSystemServerSafeModeEnabled())
    }

    @Test
    fun disablesGlobalLogsByDefault() {
        val store = DpisConfigStore(FakePrefs())

        assertFalse(store.isGlobalLogEnabled())
    }

    @Test
    fun updatesGlobalLogToggle() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setGlobalLogEnabled(false))
        assertFalse(store.isGlobalLogEnabled())
    }

    @Test
    fun startupDisclaimerRequiresExplicitAcceptance() {
        val store = DpisConfigStore(FakePrefs())

        assertFalse(store.isStartupDisclaimerAccepted)
        assertTrue(store.setStartupDisclaimerAccepted(true))
        assertTrue(store.isStartupDisclaimerAccepted)
    }

    @Test
    fun storeReadsOnlyItsOwnPreferences() {
        val localPrefs = FakePrefs()
        val remotePrefs = FakePrefs()
        remotePrefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.max.xiaoheihe"))
            .putInt("font.com.max.xiaoheihe.scale_percent", 165)
            .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
            .commit()
        val store = DpisConfigStore(localPrefs)

        assertFalse(store.getConfiguredPackages().contains("com.max.xiaoheihe"))
        assertNull(store.getTargetFontScalePercent("com.max.xiaoheihe"))
        assertFalse(store.isGlobalLogEnabled())
    }

    @Test
    fun storeWritesOnlyItsOwnPreferences() {
        val localPrefs = FakePrefs()
        val remotePrefs = FakePrefs()
        val store = DpisConfigStore(localPrefs)

        assertTrue(store.setTargetFontScalePercent("com.max.xiaoheihe", 150))
        assertTrue(store.setTargetViewportWidthDp("com.max.xiaoheihe", 360))
        assertTrue(store.setStartupDisclaimerAccepted(true))
        assertTrue(store.setInterfaceScalePercent(73))

        assertEquals(150, store.getTargetFontScalePercent("com.max.xiaoheihe"))
        assertEquals(360, store.getTargetViewportWidthDp("com.max.xiaoheihe"))
        assertTrue(store.isStartupDisclaimerAccepted)
        assertEquals(73, store.interfaceScalePercent)
        assertTrue(remotePrefs.getAll().isEmpty())
    }

    @Test
    fun localOnlyUiStateUsesExplicitLocalPreferences() {
        val remotePrefs = FakePrefs()
        val localPrefs = FakePrefs()
        val store = DpisConfigStore(remotePrefs, null, null, localPrefs)

        assertTrue(store.setTargetFontScalePercent("com.max.xiaoheihe", 150))
        assertTrue(store.setInterfaceScalePercent(73))
        assertTrue(store.setStartupDisclaimerAccepted(true))

        assertEquals(150, store.getTargetFontScalePercent("com.max.xiaoheihe"))
        assertEquals(73, store.interfaceScalePercent)
        assertTrue(store.isStartupDisclaimerAccepted)
        assertTrue(remotePrefs.getAll().containsKey("font.com.max.xiaoheihe.scale_percent"))
        assertFalse(remotePrefs.getAll().containsKey(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT))
        assertFalse(remotePrefs.getAll().containsKey(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED))
        assertEquals(73, localPrefs.getInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 0))
        assertTrue(localPrefs.getBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, false))
    }

    @Test
    fun ensureSeedConfigUsesOnlyCurrentStoreExistence() {
        val prefs = FakePrefs()
        prefs.edit().putInt("viewport.com.max.xiaoheihe.width_dp", 300).commit()
        val store = DpisConfigStore(prefs)
        val seed = linkedMapOf<String?, Int?>(
            "com.max.xiaoheihe" to DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP,
        )

        assertTrue(store.ensureSeedConfig(seed))

        assertEquals(300, store.getTargetViewportWidthDp("com.max.xiaoheihe"))
    }

    @Test
    fun snapshotAllUsesOnlyCurrentStoreValues() {
        val prefs = FakePrefs()
        prefs.edit()
            .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
            .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
            .putInt("viewport.com.max.xiaoheihe.width_dp", 420)
            .putString("font.library.entries", "[{\"id\":\"font_abcd1234\"}]")
            .commit()
        val store = DpisConfigStore(prefs)

        val snapshot = store.snapshotAll()

        assertEquals(true, snapshot[DpisConfigStore.KEY_GLOBAL_LOG_ENABLED])
        assertEquals(420, snapshot["viewport.com.max.xiaoheihe.width_dp"])
        assertEquals("[{\"id\":\"font_abcd1234\"}]", snapshot["font.library.entries"])
        assertEquals(true, snapshot[DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED])
    }

    @Test
    fun snapshotRuntimeDeliveryExcludesLocalOnlyUiStateAndTemplates() {
        val remotePrefs = FakePrefs()
        remotePrefs.edit()
            .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
            .putInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 73)
            .putString("default_config.font.typeface_id", "remote_default_font")
            .putStringSet(QuickTemplateStore.KEY_TEMPLATE_IDS, setOf("template_a"))
            .putString("template.template_a.name", "Remote")
            .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
            .commit()
        val store = DpisConfigStore(remotePrefs)

        val snapshot = store.snapshotRuntimeDelivery()

        assertFalse(snapshot.containsKey(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED))
        assertFalse(snapshot.containsKey(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT))
        assertFalse(snapshot.containsKey("default_config.font.typeface_id"))
        assertFalse(snapshot.containsKey(QuickTemplateStore.KEY_TEMPLATE_IDS))
        assertFalse(snapshot.containsKey("template.template_a.name"))
        assertEquals(true, snapshot[DpisConfigStore.KEY_GLOBAL_LOG_ENABLED])
    }
}
