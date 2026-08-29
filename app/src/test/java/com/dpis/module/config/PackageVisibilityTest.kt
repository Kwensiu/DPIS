package com.dpis.module.config

import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs
import com.dpis.module.fonts.FontApplyMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageVisibilityTest {
    @Test
    fun configuredPackagesAndVisibilityIncludeMixedLegacyAndAggregatedState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("font.com.example.legacy.mode", FontApplyMode.FIELD_REWRITE)
            .putBoolean("package_config.com.example.disabled.target.dpis_enabled", false)
            .putString("package_config.com.example.domains.font.hook_domains", "resources_font")
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"))
        assertTrue(store.getConfiguredPackages().contains("com.example.disabled"))
        assertTrue(store.getConfiguredPackages().contains("com.example.domains"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.disabled"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.domains"))
    }

    @Test
    fun hasAnyUserVisiblePackageConfigIgnoresNonPackageResidualKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putBoolean("global_log_enabled", true)
            .putString("runtime.last_route", "modern")
            .commit()

        assertFalse(DpisConfigStore(prefs).hasAnyUserVisiblePackageConfig())
    }

    @Test
    fun hasAnyUserVisiblePackageConfigDetectsRealPackageConfig() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, linkedSetOf("com.example.app"))
            .putString("package_config.com.example.app.viewport.target_type", "relative_scale")
            .putInt("package_config.com.example.app.viewport.scale_permille", 1500)
            .commit()

        assertTrue(DpisConfigStore(prefs).hasAnyUserVisiblePackageConfig())
    }
}
