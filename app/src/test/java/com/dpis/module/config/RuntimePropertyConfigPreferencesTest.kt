package com.dpis.module.config

import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RuntimePropertyConfigPreferencesTest {
    @Test
    fun legacyCompatFontPropertyDefaultsToSystemEmulationWithoutMode() {
        assertEquals(
            FontApplyMode.SYSTEM_EMULATION,
            RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                200, FontApplyMode.OFF, null,
            ),
        )
    }

    @Test
    fun forceFontPropertyDefaultsToFieldRewriteWithoutMode() {
        assertEquals(
            FontApplyMode.FIELD_REWRITE,
            RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                200, FontApplyMode.OFF, 200,
            ),
        )
    }

    @Test
    fun explicitCompatFontModeOverridesPropertyOrigin() {
        assertEquals(
            FontApplyMode.SYSTEM_EMULATION,
            RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                200, FontApplyMode.SYSTEM_EMULATION, 200,
            ),
        )
        assertEquals(
            FontApplyMode.FIELD_REWRITE,
            RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                200, FontApplyMode.FIELD_REWRITE, null,
            ),
        )
    }

    @Test
    fun legacyMainProcessTreatsAutoAsCompatForAnyEnabledViewportTarget() {
        val route = RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET

        assertEquals(
            ViewportApplyMode.COMPAT,
            RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                ViewportApplyMode.AUTO, ViewportTargetSpec.relativeScale(150000), route,
            ),
        )
        assertEquals(
            ViewportApplyMode.COMPAT,
            RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                ViewportApplyMode.AUTO, ViewportTargetSpec.absoluteDp(500), route,
            ),
        )
        assertEquals(
            ViewportApplyMode.AUTO,
            RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                ViewportApplyMode.AUTO, ViewportTargetSpec.off(), route,
            ),
        )
    }

    @Test
    fun modernRuntimeMirrorCanResolveAutoRelativeScaleAsAppProcessRoute() {
        assertEquals(
            ViewportApplyMode.COMPAT,
            RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                ViewportApplyMode.AUTO,
                ViewportTargetSpec.relativeScale(150000),
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET,
            ),
        )
    }

    @Test
    fun emptyRuntimeMirrorDoesNotMarkPackageConfiguredFromGlobalFlagsOnly() {
        val store = DpisConfigStore(
            RuntimePropertyConfigPreferences(
                "com.example.alwaysrunning",
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET,
            ),
        )

        assertFalse(
            ConfigSnapshotLoader.fromStore(store).isConfigured("com.example.alwaysrunning"),
        )
    }
}
