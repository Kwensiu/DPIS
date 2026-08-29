package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.DpiConfig
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageStateTest {
    @Test
    fun parsesConfiguredPackageSetFromStoredStrings() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(
                DpisConfigStore.KEY_TARGET_PACKAGES,
                linkedSetOf("com.max.xiaoheihe", "bin.mt.plus.canary"),
            )
            .putInt("viewport.com.max.xiaoheihe.width_dp", 360)
            .putInt("viewport.bin.mt.plus.canary.width_dp", 420)
            .commit()

        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"))
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
        assertFalse(store.getConfiguredPackages().contains("com.example.other"))
    }

    @Test
    fun userVisibleConfigIncludesModeOnlySavedPreference() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(
                DpisConfigStore.KEY_TARGET_PACKAGES,
                setOf("org.mozilla.firefox"),
            )
            .putString(
                "viewport.org.mozilla.firefox.target_type",
                ViewportTargetType.ABSOLUTE_DP,
            )
            .putString("font.org.mozilla.firefox.mode", FontApplyMode.FIELD_REWRITE)
            .commit()

        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("org.mozilla.firefox"))
        assertTrue(store.hasUserVisiblePackageConfig("org.mozilla.firefox"))
    }

    @Test
    fun clearingLastValueLeavesUserVisibleConfigWhenModePreferenceRemains() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("org.mozilla.firefox"))
            .putInt("viewport.org.mozilla.firefox.scale_milli_percent", 120000)
            .putString("viewport.org.mozilla.firefox.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putString("font.org.mozilla.firefox.mode", FontApplyMode.FIELD_REWRITE)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.clearTargetViewportValue("org.mozilla.firefox"))

        assertTrue(store.getConfiguredPackages().contains("org.mozilla.firefox"))
        assertTrue(store.hasUserVisiblePackageConfig("org.mozilla.firefox"))
    }

    @Test
    fun configuredPackagesDerivesCandidatesFromSavedPackageState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("font.com.example.modeonly.mode", FontApplyMode.FIELD_REWRITE)
            .putBoolean("target.com.example.disabled.dpis_enabled", false)
            .putString("font.com.example.domains.hook_domains", "textview_sp")
            .commit()

        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.example.modeonly"))
        assertTrue(store.getConfiguredPackages().contains("com.example.disabled"))
        assertTrue(store.getConfiguredPackages().contains("com.example.domains"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.modeonly"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.disabled"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.domains"))
    }

    @Test
    fun defaultDpisEnabledValueDoesNotCreateConfiguredPackage() {
        val prefs = FakePrefs()
        prefs.edit().putBoolean("target.com.example.default.dpis_enabled", true).commit()

        val store = DpisConfigStore(prefs)

        assertFalse(store.getConfiguredPackages().contains("com.example.default"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.default"))
    }

    @Test
    fun defaultPackageDraftValuesDoNotCreateConfiguredPackage() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.example.default"))
            .putString("viewport.com.example.default.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putString("viewport.com.example.default.mode", ViewportApplyMode.AUTO)
            .putString("font.com.example.default.mode", FontApplyMode.SYSTEM_EMULATION)
            .commit()

        val store = DpisConfigStore(prefs)

        assertFalse(store.hasRealPackageConfig("com.example.default"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.default"))
    }

    @Test
    fun aggregatedViewportTypeWithoutValueIsNotConfigured() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.example.default"))
            .putString(
                "package_config.com.example.default.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE,
            )
            .commit()

        val store = DpisConfigStore(prefs)

        assertFalse(store.hasRealPackageConfig("com.example.default"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.default"))
    }

    @Test
    fun resolvesEffectiveDensityFromTargetValue() {
        val prefs = FakePrefs()
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 360).commit()

        val store = DpisConfigStore(prefs)

        assertEquals(360, store.getTargetViewportWidthDp("bin.mt.plus.canary"))
    }

    @Test
    fun returnsNullEffectiveDensityWhenTargetMissing() {
        val store = DpisConfigStore(FakePrefs())

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"))
    }

    @Test
    fun returnsNullEffectiveDensityWhenStoredViewportWidthIsNonPositive() {
        val prefs = FakePrefs()
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 0).commit()

        val store = DpisConfigStore(prefs)

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"))
    }

    @Test
    fun defaultsViewportModeToOffWhenLegacyWidthIsInvalid() {
        val prefs = FakePrefs()
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 0).commit()

        val store = DpisConfigStore(prefs)

        assertEquals(ViewportApplyMode.OFF, store.getTargetViewportApplyMode("bin.mt.plus.canary"))
    }

    @Test
    fun seedsMissingPackageListAndTargetValuesWithoutOverwritingExistingValues() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("bin.mt.plus.canary"))
            .putInt("viewport.bin.mt.plus.canary.width_dp", 420)
            .commit()

        val store = DpisConfigStore(prefs)
        store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps())

        assertEquals(420, store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertEquals(
            DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP,
            store.getTargetViewportWidthDp("com.max.xiaoheihe"),
        )
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"))
    }

    @Test
    fun updatesViewportWidthForConfiguredPackage() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()))

        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360))

        assertEquals(360, store.getTargetViewportWidthDp("bin.mt.plus.canary"))
    }

    @Test
    fun clearsViewportWidthWhenDisabled() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()))

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"))

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }
}
