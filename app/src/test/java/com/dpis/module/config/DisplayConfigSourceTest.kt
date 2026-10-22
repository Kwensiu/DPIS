package com.dpis.module.config

import com.dpis.module.FakePrefs
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayConfigSourceTest {
    @Test
    fun diagnosticsDistinguishSnapshotOwnerMismatchFromMissingPackage() {
        val store = DpisConfigStore(FakePrefs(), 0)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 125))

        val diagnostics = PerAppDisplayConfigSource(
            ConfigSnapshotLoader.fromStore(store),
        ).inspect(PackageIdentity("com.example.target", 999))

        assertFalse(diagnostics.snapshotOwnerMatches)
        assertTrue(diagnostics.snapshotPackageHit)
        assertFalse(diagnostics.fallbackHit)
        assertEquals("snapshot_owner_mismatch", diagnostics.reason)
        assertNull(diagnostics.config)
    }

    @Test
    fun diagnosticsExposeFallbackHitAndFinalViewportMode() {
        val source = PerAppDisplayConfigSource(
            PerAppDisplayConfigSource.SnapshotProvider { ConfigSnapshot.empty() },
            { packageName, _ ->
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

        val diagnostics = source.inspect(PackageIdentity("com.example.target", 0))

        assertTrue(diagnostics.fallbackAttempted)
        assertTrue(diagnostics.fallbackHit)
        assertEquals("matched", diagnostics.reason)
        assertEquals(ViewportApplyMode.SYSTEM, diagnostics.targetViewportMode)
        assertNotNull(diagnostics.config)
    }

    @Test
    fun returnsNullWhenViewportAndFontAreBothMissing() {
        val store = DpisConfigStore(FakePrefs())

        val config = PerAppDisplayConfigSource(store).get("com.example.target", 0)

        assertNull(config)
    }

    @Test
    fun returnsFontOnlyConfigWhenViewportMissing() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 125))

        val config = PerAppDisplayConfigSource(store).get("com.example.target", 0)

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

        val config = PerAppDisplayConfigSource(store).get("com.example.target", 0)

        assertNull(config)
    }

    @Test
    fun providerReflectsUpdatedStoreOnEachRead() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 300))
        val source = PerAppDisplayConfigSource { ConfigSnapshotLoader.fromStore(store) }
        assertNotNull(source.get("com.example.target", 0))

        assertTrue(store.clearTargetPackageConfig("com.example.target"))

        assertNull(source.get("com.example.target", 0))
    }

    @Test
    fun usesPackageFallbackWhenSnapshotHasNoPackage() {
        val source = PerAppDisplayConfigSource(
            ConfigSnapshot::empty,
            { packageName, _ ->
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

        val config = source.get("com.example.target", 0)

        assertNotNull(config)
        assertEquals(500, config!!.targetViewportWidthDp())
        assertEquals(200, config.targetFontScalePercent)
        assertEquals(FontApplyMode.FIELD_REWRITE, config.targetFontMode)
    }

    @Test
    fun packageFallbackReadsViewportPropertiesForRequestedUser() {
        var resolvedUserId: Int? = null
        val source = PerAppDisplayConfigSource(
            ConfigSnapshot::empty,
            { packageName, userId ->
                resolvedUserId = userId
                PackageConfigSnapshot(
                    packageName,
                    true,
                    400,
                    ViewportApplyMode.SYSTEM,
                    null,
                    FontApplyMode.OFF,
                    null,
                    false,
                    false,
                    false,
                )
            },
        )

        source.get("com.example.target", 999)

        assertEquals(999, resolvedUserId)
    }

    @Test
    fun doesNotUsePackageFallbackWhenSnapshotDisablesPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 200))
        assertTrue(store.setTargetDpisEnabled("com.example.target", false))
        val source = PerAppDisplayConfigSource(
            { ConfigSnapshotLoader.fromStore(store) },
            { packageName, _ ->
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

        assertNull(source.get("com.example.target", 0))
    }

    @Test
    fun usesPackageFallbackWhenSnapshotHasStaleEnabledPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.target", 100))
        val source = PerAppDisplayConfigSource(
            { ConfigSnapshotLoader.fromStore(store) },
            { packageName, _ ->
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

        val config = source.get("com.example.target", 0)

        assertNotNull(config)
        assertEquals(600, config!!.targetViewportWidthDp())
        assertEquals(200, config.targetFontScalePercent)
    }

    @Test
    fun matchingSnapshotOwnerCanProvideViewportForAnyAndroidUser() {
        val prefs = FakePrefs()
        val userZeroStore = DpisConfigStore(prefs, 0)
        val user999Store = DpisConfigStore(prefs, 999)
        assertTrue(userZeroStore.setTargetViewportWidthDp("com.android.chrome", 288))
        assertTrue(
            userZeroStore.setTargetViewportApplyMode(
                "com.android.chrome",
                ViewportApplyMode.SYSTEM
            )
        )
        assertTrue(user999Store.setTargetViewportWidthDp("com.android.chrome", 576))
        assertTrue(
            user999Store.setTargetViewportApplyMode(
                "com.android.chrome",
                ViewportApplyMode.SYSTEM
            )
        )
        val userZeroSource =
            PerAppDisplayConfigSource(ConfigSnapshotLoader.fromStore(userZeroStore))
        val user999Source = PerAppDisplayConfigSource(ConfigSnapshotLoader.fromStore(user999Store))

        assertEquals(288, userZeroSource.get("com.android.chrome", 0)?.targetViewportWidthDp())
        assertNull(userZeroSource.get("com.android.chrome", 999))
        assertEquals(576, user999Source.get("com.android.chrome", 999)?.targetViewportWidthDp())
        assertNull(user999Source.get("com.android.chrome", 0))
    }

    @Test
    fun keepsViewportConfigWhenViewportExists() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 360))

        val config = PerAppDisplayConfigSource(store).get("com.example.target", 0)

        assertNotNull(config)
        assertTrue(config!!.hasViewportOverride())
        assertEquals(360, config.targetViewportWidthDp())
    }

    @Test
    fun compatViewportIsNotDeliveredAsASystemServerOverride() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 320))
        assertTrue(store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.COMPAT))

        assertNull(PerAppDisplayConfigSource(store).get("com.example.target", 0))
    }

    @Test
    fun compatFontConfigDoesNotCarryAViewportOverride() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 320))
        assertTrue(store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.COMPAT))
        assertTrue(store.setTargetFontScalePercent("com.example.target", 125))

        val config = PerAppDisplayConfigSource(store).get("com.example.target", 0)

        assertNotNull(config)
        assertFalse(config!!.hasViewportOverride())
        assertEquals(ViewportApplyMode.COMPAT, config.targetViewportMode)
        assertEquals(125, config.targetFontScalePercent)
    }

    @Test
    fun nativeFlutterFlagReflectsFinalDomainPlan() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.miui.gallery", 200))
        assertTrue(store.setTargetFontApplyMode("com.miui.gallery", FontApplyMode.FIELD_REWRITE))

        val automatic = PerAppDisplayConfigSource(store).get("com.miui.gallery", 0)

        assertNotNull(automatic)
        assertTrue(automatic!!.hyperOsFlutterFontHookEnabled)

        assertTrue(
            HookDomainOverrideStore(store).save(
                "com.miui.gallery",
                setOf(FontHookDomainRegistry.ID_RESOURCES_FONT),
                emptySet(),
            ),
        )

        val customWithoutNative = PerAppDisplayConfigSource(store).get("com.miui.gallery", 0)

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
