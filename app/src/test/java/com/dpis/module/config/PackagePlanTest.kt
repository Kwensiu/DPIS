package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.hooks.HookExecutionPlan
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagePlanTest {
    @Test
    fun skipsPackagesWithoutConfiguration() {
        val store = DpisConfigStore(FakePrefs())

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertFalse(plan.shouldInstallHooks())
    }

    @Test
    fun installsViewportHooksForConfiguredViewportPackage() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportWidthDp("com.example.app", 411)
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.shouldInstallHooks())
        assertTrue(plan.viewportConfigured)
        assertTrue(plan.viewportEnabled)
        assertFalse(plan.fontScaleActive)
    }

    @Test
    fun autoViewportKeepsLegacyAppProcessViewportHooksAvailable() {
        val store = DpisConfigStore(FakePrefs())
        store.setSystemServerHooksEnabled(true)
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.AUTO)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.viewportConfigured)
        assertTrue(plan.viewportEnabled)
        assertTrue(plan.shouldInstallLegacyHooks())
    }

    @Test
    fun compatViewportUsesAppProcessViewportHooks() {
        val store = DpisConfigStore(FakePrefs())
        store.setSystemServerHooksEnabled(true)
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.viewportConfigured)
        assertTrue(plan.viewportEnabled)
        assertTrue(plan.shouldInstallLegacyHooks())
    }

    @Test
    fun viewportOnlyPackageHasNoSecondaryProcessSafeRouteAfterViewportSuppression() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT)

        val plan = ModulePackagePlan.resolve(store, "com.example.app").withoutViewportRoute()

        assertFalse(plan.viewportConfigured)
        assertFalse(plan.viewportEnabled)
        assertFalse(plan.hasSecondaryProcessSafeRoute())
        assertFalse(plan.shouldInstallHooks())
    }

    @Test
    fun fontRouteSurvivesSecondaryProcessViewportSuppression() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT)
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)

        val plan = ModulePackagePlan.resolve(store, "com.example.app").withoutViewportRoute()

        assertFalse(plan.viewportConfigured)
        assertFalse(plan.viewportEnabled)
        assertTrue(plan.fontEnabled)
        assertTrue(plan.hasSecondaryProcessSafeRoute())
        assertTrue(plan.shouldInstallHooks())
    }

    @Test
    fun installsFontHooksForConfiguredFontPackage() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.shouldInstallHooks())
        assertFalse(plan.viewportConfigured)
        assertTrue(plan.fontScaleActive)
        assertTrue(plan.fontEnabled)
    }

    @Test
    fun ignoresLegacyGlobalHyperOsNativeFlutterFlagForAppProcessDispatch() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)
        store.setFlutterFontHookEnabled(true)
        store.setHyperOsFlutterFontHookEnabled(true)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.fontEnabled)
        assertFalse(plan.hyperOsNativeFlutterFontEnabled)
    }

    @Test
    fun ignoresLegacyGlobalFlutterSettingsFlagForAppProcessDispatch() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION)
        store.setFlutterFontHookEnabled(true)
        store.setFlutterSettingsFontHookEnabled(true)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.fontEnabled)
        assertFalse(plan.flutterSettingsFontEnabled)
        assertFalse(plan.hyperOsNativeFlutterFontEnabled)
    }

    @Test
    fun flutterMasterSwitchGatesFlutterSettingsFlag() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION)
        store.setFlutterFontHookEnabled(false)
        store.setFlutterSettingsFontHookEnabled(true)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.fontEnabled)
        assertFalse(plan.flutterSettingsFontEnabled)
    }

    @Test
    fun flutterMasterSwitchGatesHyperOsNativeFlutterFlag() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)
        store.setFlutterFontHookEnabled(false)
        store.setHyperOsFlutterFontHookEnabled(true)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.fontEnabled)
        assertFalse(plan.hyperOsNativeFlutterFontEnabled)
    }

    @Test
    fun legacyLegacyInstallsFontFieldRewriteOnlyPackage() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.shouldInstallHooks())
        assertTrue(plan.shouldInstallLegacyHooks())
    }

    @Test
    fun legacyLegacyInstallsFontSystemEmulationPackage() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.shouldInstallLegacyHooks())
    }

    @Test
    fun installsTypefaceHooksForTypefaceOnlyPackage() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetTypefaceId("com.example.app", "font_abcd1234")

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.shouldInstallHooks())
        assertFalse(plan.viewportConfigured)
        assertFalse(plan.fontScaleActive)
        assertFalse(plan.fontEnabled)
        assertTrue(plan.typefaceActive)
        assertTrue(plan.typefaceEnabled)
    }

    @Test
    fun legacyLegacyInstallsForTypefaceOnlyPackage() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetTypefaceId("com.example.app", "font_abcd1234")

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertTrue(plan.shouldInstallHooks())
        assertTrue(plan.shouldInstallLegacyHooks())
    }

    @Test
    fun skipsTypefacePackageDisabledByTargetToggle() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetTypefaceId("com.example.app", "font_abcd1234")
        store.setTargetDpisEnabled("com.example.app", false)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertFalse(plan.shouldInstallHooks())
        assertTrue(plan.typefaceActive)
        assertFalse(plan.typefaceEnabled)
    }

    @Test
    fun skipsPackagesDisabledByTargetToggle() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportWidthDp("com.example.app", 411)
        store.setTargetDpisEnabled("com.example.app", false)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertFalse(plan.shouldInstallHooks())
    }

    @Test
    fun disabledPackageStillCarriesInactiveFlutterSupplementsOnlyForDiagnostics() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION)
        store.setFlutterFontHookEnabled(true)
        store.setFlutterSettingsFontHookEnabled(true)
        store.setTargetDpisEnabled("com.example.app", false)

        val plan = ModulePackagePlan.resolve(store, "com.example.app")

        assertFalse(plan.targetDpisEnabled)
        assertTrue(plan.fontScaleActive)
        assertFalse(plan.flutterSettingsFontEnabled)
        assertFalse(plan.shouldInstallHooks())
    }

    @Test
    fun buildExecutionPlanForwardsCustomHookDomainOverride() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)
        assertTrue(
            HookDomainOverrideStore(store).save(
                "com.example.app",
                setOf(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE),
                setOf("removed_domain"),
            ),
        )

        val packagePlan = ModulePackagePlan.resolve(store, "com.example.app")
        val executionPlan = packagePlan.buildExecutionPlan(
            HookRuntimePolicy.fromStore(store),
            DebugFontOverride.none(),
        )

        assertEquals("custom", executionPlan.hookDomainSource)
        assertEquals("textview_absolute_rewrite", executionPlan.hookDomains)
        assertEquals("removed_domain", executionPlan.unknownCustomDomains)
        assertTrue(executionPlan.textViewHooksEnabled)
        assertFalse(executionPlan.resourcesHooksEnabled)
    }

    @Test
    fun buildExecutionPlanForwardsDebugOverride() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent("com.example.app", 120)
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE)

        val packagePlan = ModulePackagePlan.resolve(store, "com.example.app")
        val executionPlan = packagePlan.buildExecutionPlan(
            HookRuntimePolicy.fromStore(store),
            DebugFontOverride.of(false, false, true),
        )

        assertTrue(executionPlan.debugDisableTextViewAbsoluteRewrite)
        assertTrue(executionPlan.textViewHooksEnabled)
        assertTrue(executionPlan.fontDomainPlan.textViewSpRewriteEnabled)
        assertFalse(executionPlan.fontDomainPlan.textViewAbsoluteRewriteEnabled)
        assertEquals(
            "textview_sp_rewrite,textview_current_px_fallback," +
                "paint_text_size_fallback,webview_text_zoom",
            executionPlan.hookDomains,
        )
    }
}
