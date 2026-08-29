package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotTest {
    @Test
    fun snapshotKeepsConfiguredDisabledPackageButSourceSkipsIt() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportWidthDp("com.example.app", 360)
        store.setTargetDpisEnabled("com.example.app", false)

        val snapshot = ConfigSnapshotLoader.fromStore(store)
        val packageConfig = snapshot.getPackage("com.example.app")

        assertTrue(snapshot.isConfigured("com.example.app"))
        assertNotNull(packageConfig)
        assertFalse(packageConfig!!.dpisEnabled)
        assertNull(PerAppDisplayConfigSource(snapshot).get("com.example.app"))
        assertFalse(ModulePackagePlan.resolve(snapshot, "com.example.app").shouldInstallHooks())
    }

    @Test
    fun snapshotPreservesLegacyDefaultModes() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.example.app"))
            .putInt("viewport.com.example.app.width_dp", 360)
            .putInt("font.com.example.app.scale_percent", 125)
            .commit()

        val snapshot = ConfigSnapshotLoader.fromStore(DpisConfigStore(prefs))
        val packageConfig = snapshot.getPackage("com.example.app")

        assertNotNull(packageConfig)
        assertEquals(ViewportApplyMode.SYSTEM, packageConfig!!.targetViewportMode)
        assertEquals(FontApplyMode.SYSTEM_EMULATION, packageConfig.targetFontMode)
    }

    @Test
    fun snapshotIncludesTypefaceId() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetTypefaceId("com.example.app", "font_abcd1234")

        val packageConfig = ConfigSnapshotLoader
            .fromStore(store)
            .getPackage("com.example.app")

        assertEquals("font_abcd1234", packageConfig!!.targetTypefaceId)
    }

    @Test
    fun configuredPackagesAreImmutable() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportWidthDp("com.example.app", 360)

        val snapshot = ConfigSnapshotLoader.fromStore(store)

        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            val configuredPackages = snapshot.getConfiguredPackages() as MutableSet<String?>
            configuredPackages.add("com.example.other")
        }
    }

    @Test
    fun globalFlagsAreCapturedOnce() {
        val store = DpisConfigStore(FakePrefs())
        store.setSystemServerHooksEnabled(false)
        val snapshot = ConfigSnapshotLoader.fromStore(store)

        store.setSystemServerHooksEnabled(true)

        assertFalse(snapshot.isSystemServerHooksEnabled())
        assertTrue(snapshot.hasSystemServerHooksEnabled())
    }

    @Test
    fun packageSnapshotIgnoresLegacyGlobalExperimentalFlags() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 125)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)
        store.setFlutterFontHookEnabled(true)
        store.setFlutterSettingsFontHookEnabled(true)
        store.setHyperOsFlutterFontHookEnabled(true)

        val packageConfig = ConfigSnapshotLoader
            .fromStore(store)
            .getPackage("com.example.app")

        assertNotNull(packageConfig)
        assertFalse(packageConfig!!.flutterFontHookEnabled)
        assertFalse(packageConfig.flutterSettingsFontHookEnabled)
        assertFalse(packageConfig.hyperOsFlutterFontHookEnabled)
    }
}
