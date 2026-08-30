package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportTest {
    @Test
    fun viewportGettersReadAggregatedViewportKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString(
                "package_config.com.example.app.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE,
            )
            .putInt("package_config.com.example.app.viewport.scale_milli_percent", 125000)
            .putString("package_config.com.example.app.viewport.mode", ViewportApplyMode.SYSTEM)
            .commit()
        val store = DpisConfigStore(prefs)

        assertEquals(ViewportTargetType.RELATIVE_SCALE, store.getTargetViewportType("com.example.app"))
        assertEquals(125000, store.getTargetViewportScaleMilliPercent("com.example.app"))
        assertEquals(
            ViewportTargetSpec.relativeScale(125000),
            store.getTargetViewportSpec("com.example.app"),
        )
        assertEquals(ViewportApplyMode.SYSTEM, store.getTargetViewportApplyMode("com.example.app"))
    }

    @Test
    fun viewportSetterWritesLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(125000)))

        assertEquals(
            ViewportTargetType.RELATIVE_SCALE,
            prefs.getString("viewport.com.example.app.target_type", null),
        )
        assertEquals(
            ViewportTargetType.RELATIVE_SCALE,
            prefs.getString("package_config.com.example.app.viewport.target_type", null),
        )
        assertEquals(125000, prefs.getInt("viewport.com.example.app.scale_milli_percent", 0))
        assertEquals(
            125000,
            prefs.getInt("package_config.com.example.app.viewport.scale_milli_percent", 0),
        )
        // The permille keys remain as a compatibility mirror for older runtime readers.
        assertEquals(1250, prefs.getInt("viewport.com.example.app.scale_permille", 0))
        assertEquals(
            1250,
            prefs.getInt("package_config.com.example.app.viewport.scale_permille", 0),
        )
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun viewportSpecSetterPreservesExistingApplyMode() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.SYSTEM))

        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(125000)))

        assertEquals(ViewportApplyMode.SYSTEM, prefs.getString("viewport.com.example.app.mode", null))
        assertEquals(
            ViewportApplyMode.SYSTEM,
            prefs.getString("package_config.com.example.app.viewport.mode", null),
        )
        assertEquals(ViewportApplyMode.SYSTEM, store.getTargetViewportApplyMode("com.example.app"))
    }

    @Test
    fun viewportWidthSetterPreservesExistingApplyMode() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT))

        assertTrue(store.setTargetViewportWidthDp("com.example.app", 411))

        assertEquals(ViewportApplyMode.COMPAT, prefs.getString("viewport.com.example.app.mode", null))
        assertEquals(
            ViewportApplyMode.COMPAT,
            prefs.getString("package_config.com.example.app.viewport.mode", null),
        )
        assertEquals(ViewportApplyMode.COMPAT, store.getTargetViewportApplyMode("com.example.app"))
    }

    @Test
    fun viewportClearRemovesLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(125000)))
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.SYSTEM))

        assertTrue(store.clearTargetViewportWidthDp("com.example.app"))

        assertNull(store.getTargetViewportScaleMilliPercent("com.example.app"))
        assertEquals(ViewportTargetType.OFF, store.getTargetViewportType("com.example.app"))
        assertEquals(ViewportApplyMode.OFF, store.getTargetViewportApplyMode("com.example.app"))
        for (key in listOf(
            "viewport.com.example.app.target_type",
            "viewport.com.example.app.scale_milli_percent",
            "viewport.com.example.app.scale_permille",
            "viewport.com.example.app.mode",
            "package_config.com.example.app.viewport.target_type",
            "package_config.com.example.app.viewport.scale_milli_percent",
            "package_config.com.example.app.viewport.scale_permille",
            "package_config.com.example.app.viewport.mode",
        )) {
            assertFalse(prefs.contains(key))
        }
        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
    }

    @Test
    fun legacyOnlyViewportKeysRemainReadable() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("viewport.com.example.app.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putInt("viewport.com.example.app.width_dp", 411)
            .putString("viewport.com.example.app.mode", ViewportApplyMode.COMPAT)
            .commit()
        val store = DpisConfigStore(prefs)

        assertEquals(ViewportTargetType.ABSOLUTE_DP, store.getTargetViewportType("com.example.app"))
        assertEquals(411, store.getTargetViewportWidthDp("com.example.app"))
        assertEquals(
            ViewportTargetSpec.absoluteDp(411),
            store.getTargetViewportSpec("com.example.app"),
        )
        assertEquals(ViewportApplyMode.COMPAT, store.getTargetViewportApplyMode("com.example.app"))
    }

    @Test
    fun configuredPackagesIncludeMixedLegacyAndAggregatedViewportState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("viewport.com.example.legacy.width_dp", 411)
            .putString(
                "package_config.com.example.aggregated.viewport.mode",
                ViewportApplyMode.SYSTEM,
            )
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"))
        assertTrue(store.getConfiguredPackages().contains("com.example.aggregated"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.aggregated"))
    }

    @Test
    fun aggregatedRelativeTargetTypeOnlyRemainsDraftState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString(
                "package_config.org.chromium.webapk.test_v2.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE,
            )
            .commit()
        val store = DpisConfigStore(prefs)

        assertFalse(store.getConfiguredPackages().contains("org.chromium.webapk.test_v2"))
        assertFalse(store.hasRealPackageConfig("org.chromium.webapk.test_v2"))
        assertFalse(store.hasUserVisiblePackageConfig("org.chromium.webapk.test_v2"))
    }

    @Test
    fun legacyRelativeTargetTypeOnlyRemainsDraftState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("viewport.com.example.draft.target_type", ViewportTargetType.RELATIVE_SCALE)
            .commit()
        val store = DpisConfigStore(prefs)

        assertFalse(store.getConfiguredPackages().contains("com.example.draft"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.draft"))
    }
}
