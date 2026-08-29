package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.runtime.systemserver.PerAppDisplayConfig
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayConfigSourceTest {
    @Test
    fun returnsNullWhenViewportAndFontAreBothMissing() {
        val store = DpisConfigStore(FakePrefs())

        val config = PerAppDisplayConfigSource(store).get("com.example.target")

        assertNull(config)
    }

    @Test
    fun returnsFontOnlyConfigWhenViewportMissing() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 125))

        val config = PerAppDisplayConfigSource(store).get("com.example.target")

        assertNotNull(config)
        assertFalse(config!!.hasViewportOverride())
        assertEquals(0, config.targetViewportWidthDp())
        assertEquals(125, config.targetFontScalePercent)
    }

    @Test
    fun returnsNullWhenTargetDpisDisabled() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 500))
        assertTrue(store.setTargetFontScalePercent("com.example.target", 300))
        assertTrue(store.setTargetFontApplyMode("com.example.target", FontApplyMode.SYSTEM_EMULATION))
        assertTrue(store.setTargetDpisEnabled("com.example.target", false))

        val config = PerAppDisplayConfigSource(store).get("com.example.target")

        assertNull(config)
    }

    @Test
    fun providerReflectsUpdatedStoreOnEachRead() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 300))
        val source = PerAppDisplayConfigSource { ConfigSnapshotLoader.fromStore(store) }
        assertNotNull(source.get("com.example.target"))

        assertTrue(store.clearTargetPackageConfig("com.example.target"))

        assertNull(source.get("com.example.target"))
    }

    @Test
    fun usesPackageFallbackWhenSnapshotHasNoPackage() {
        val source = PerAppDisplayConfigSource(
            ConfigSnapshot::empty,
            { packageName ->
                PackageConfigSnapshot(
                    packageName,
                    true,
                    500,
                    ViewportApplyMode.SYSTEM,
                    200,
                    FontApplyMode.FIELD_REWRITE,
                    null,
                    false,
                    false,
                    false,
                )
            },
        )

        val config = source.get("com.example.target")

        assertNotNull(config)
        assertEquals(500, config!!.targetViewportWidthDp())
        assertEquals(200, config.targetFontScalePercent)
        assertEquals(FontApplyMode.FIELD_REWRITE, config.targetFontMode)
    }

    @Test
    fun doesNotUsePackageFallbackWhenSnapshotDisablesPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 200))
        assertTrue(store.setTargetDpisEnabled("com.example.target", false))
        val source = PerAppDisplayConfigSource(
            { ConfigSnapshotLoader.fromStore(store) },
            { packageName ->
                PackageConfigSnapshot(
                    packageName,
                    true,
                    null,
                    ViewportApplyMode.OFF,
                    200,
                    FontApplyMode.FIELD_REWRITE,
                    null,
                    false,
                    false,
                    false,
                )
            },
        )

        assertNull(source.get("com.example.target"))
    }

    @Test
    fun usesPackageFallbackWhenSnapshotHasStaleEnabledPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 100))
        val source = PerAppDisplayConfigSource(
            { ConfigSnapshotLoader.fromStore(store) },
            { packageName ->
                PackageConfigSnapshot(
                    packageName,
                    true,
                    600,
                    ViewportApplyMode.SYSTEM,
                    200,
                    FontApplyMode.FIELD_REWRITE,
                    null,
                    false,
                    false,
                    false,
                )
            },
        )

        val config = source.get("com.example.target")

        assertNotNull(config)
        assertEquals(600, config!!.targetViewportWidthDp())
        assertEquals(200, config.targetFontScalePercent)
    }

    @Test
    fun keepsViewportConfigWhenViewportExists() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 360))

        val config = PerAppDisplayConfigSource(store).get("com.example.target")

        assertNotNull(config)
        assertTrue(config!!.hasViewportOverride())
        assertEquals(360, config.targetViewportWidthDp())
    }

    @Test
    fun nativeFlutterFlagReflectsFinalDomainPlan() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.miui.gallery", 200))
        assertTrue(store.setTargetFontApplyMode("com.miui.gallery", FontApplyMode.FIELD_REWRITE))

        val automatic = PerAppDisplayConfigSource(store).get("com.miui.gallery")

        assertNotNull(automatic)
        assertTrue(automatic!!.hyperOsFlutterFontHookEnabled)

        assertTrue(
            HookDomainOverrideStore(store).save(
                "com.miui.gallery",
                setOf(FontHookDomainRegistry.ID_RESOURCES_FONT),
                emptySet(),
            ),
        )

        val customWithoutNative = PerAppDisplayConfigSource(store).get("com.miui.gallery")

        assertNotNull(customWithoutNative)
        assertFalse(customWithoutNative!!.hyperOsFlutterFontHookEnabled)
    }

    @Test
    fun reportsSystemServerHooksEnabledByDefault() {
        val store = DpisConfigStore(FakePrefs())

        val enabled = PerAppDisplayConfigSource(store).isSystemServerHooksEnabled()

        assertTrue(enabled)
    }

    @Test
    fun reportsSystemServerHooksDisabledWhenStoreFlagOff() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setSystemServerHooksEnabled(false))

        val enabled = PerAppDisplayConfigSource(store).isSystemServerHooksEnabled()

        assertFalse(enabled)
    }
}
