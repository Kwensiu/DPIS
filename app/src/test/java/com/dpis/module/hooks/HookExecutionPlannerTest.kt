package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.FontMode
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookExecutionPlan
import com.dpis.module.hooks.HookExecutionPlanner
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HookExecutionPlannerTest {
    @Test
    fun fontOffDisablesFontRoutes() {
        val plan =
            build(false, true, false, fontEnabled = false, fontMode = FontApplyMode.FIELD_REWRITE)
        assertEquals(FontMode.OFF, plan.fontMode)
        assertFalse(plan.resourcesHooksEnabled)
        assertFalse(plan.activityThreadFontEnabled)
        assertFalse(plan.textViewHooksEnabled)
        assertFalse(plan.webViewTextZoomEnabled)
        assertFalse(plan.flutterSettingsEnabled)
        assertFalse(plan.hyperOsNativeFlutterEnabled)
    }

    @Test
    fun emulationEnablesSemanticRoutesAndSupplements() {
        val plan = build(
            false,
            true,
            false,
            fontEnabled = true,
            fontMode = FontApplyMode.SYSTEM_EMULATION,
            flutterSettingsEnabled = true,
            hyperOsFlutterEnabled = true
        )
        assertEquals(FontMode.EMULATION, plan.fontMode)
        assertTrue(plan.resourcesHooksEnabled)
        assertTrue(plan.activityThreadFontEnabled)
        assertFalse(plan.textViewHooksEnabled)
        assertTrue(plan.webViewTextZoomEnabled)
        assertTrue(plan.flutterSettingsEnabled)
        assertTrue(plan.hyperOsNativeFlutterEnabled)
        assertTrue(plan.domainPlan.hasActivityThreadFont())
        assertTrue(plan.domainPlan.hasSystemServerFont())
        assertTrue(plan.domainPlan.hasFlutterSettings())
        assertTrue(plan.domainPlan.hasHyperOsNativeFlutter())
    }

    @Test
    fun fieldRewriteEnablesCoreTextViewDomainsAndDisablesActivityThreadRoute() {
        val plan =
            build(false, true, false, fontEnabled = true, fontMode = FontApplyMode.FIELD_REWRITE)
        assertEquals(FontMode.FIELD_REWRITE, plan.fontMode)
        assertFalse(plan.resourcesHooksEnabled)
        assertFalse(plan.activityThreadFontEnabled)
        assertTrue(plan.textViewHooksEnabled)
        assertTrue(plan.webViewTextZoomEnabled)
        assertFalse(plan.fontDomainPlan.resourcesFontEnabled)
        assertTrue(plan.fontDomainPlan.textViewSpRewriteEnabled)
        assertTrue(plan.fontDomainPlan.textViewAbsoluteRewriteEnabled)
        assertEquals(plan.domainPlan.enabledDomainsCsv(), plan.hookDomains)
        assertEquals(
            "textview_sp_rewrite,textview_absolute_rewrite,textview_current_px_fallback,paint_text_size_fallback,webview_text_zoom",
            plan.hookDomains,
        )
        assertEquals("field-rewrite-domain-plan", plan.fontDomainPlan.reason)
    }

    @Test
    fun viewportOnlyStillEnablesResourcesHooks() {
        val plan = build(
            false,
            true,
            false,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.FIELD_REWRITE
        )
        assertEquals(FontMode.OFF, plan.fontMode)
        assertTrue(plan.viewportEnabled)
        assertTrue(plan.resourcesHooksEnabled)
    }

    @Test
    fun viewportAutoUsesSystemFirstRouteWithAppProcessFallbackHooksWhenAvailable() {
        val plan =
            build(false, true, false, viewportEnabled = true, viewportMode = ViewportApplyMode.AUTO)
        assertEquals(ViewportApplyMode.SYSTEM, plan.resolvedViewportMode)
        assertTrue(plan.viewportEnabled)
        assertTrue(plan.resourcesHooksEnabled)
    }

    @Test
    fun viewportAutoFallsBackToCompatWhenSystemUnavailable() {
        val plan = build(
            false,
            false,
            false,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.AUTO
        )
        assertEquals(ViewportApplyMode.COMPAT, plan.resolvedViewportMode)
        assertTrue(plan.viewportEnabled)
        assertTrue(plan.resourcesHooksEnabled)
    }

    @Test
    fun debugForceFlutterSettingsKeepsOtherDomainsUnlessOnlyModeRequested() {
        val plan = build(
            false,
            true,
            false,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.FIELD_REWRITE,
            fontEnabled = true,
            fontMode = FontApplyMode.FIELD_REWRITE,
            flutterSettingsEnabled = true,
            hyperOsFlutterEnabled = true,
            debug = DebugFontOverride.of(true, false),
        )
        assertTrue(plan.debugForceFlutterSettings)
        assertFalse(plan.debugFlutterSettingsOnly)
        assertTrue(plan.flutterSettingsEnabled)
        assertTrue(plan.resourcesHooksEnabled)
        assertTrue(plan.textViewHooksEnabled)
        assertTrue(plan.hyperOsNativeFlutterEnabled)
        assertEquals("force-flutter-settings", plan.reason.debugOverride)
    }

    @Test
    fun debugFlutterSettingsOnlySuppressesNonFlutterSettingsRoutes() {
        val plan = build(
            false,
            true,
            true,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.FIELD_REWRITE,
            fontEnabled = true,
            fontMode = FontApplyMode.FIELD_REWRITE,
            debug = DebugFontOverride.of(true, true),
        )
        assertTrue(plan.debugFlutterSettingsOnly)
        assertTrue(plan.flutterSettingsEnabled)
        assertFalse(plan.resourcesHooksEnabled)
        assertFalse(plan.activityThreadFontEnabled)
        assertFalse(plan.textViewHooksEnabled)
        assertFalse(plan.webViewTextZoomEnabled)
        assertFalse(plan.hyperOsNativeFlutterEnabled)
        assertTrue(plan.viewportEnabled)
        assertFalse(plan.resourcesProbeEnabled)
        assertTrue(plan.viewportProbeEnabled)
        assertEquals("debug-flutter-settings-only", plan.reason.suppressed)
        assertEquals("flutter-settings-only", plan.reason.debugOverride)
    }

    @Test
    fun debugDisableTextViewAbsoluteRewriteKeepsSpRewriteRoute() {
        val plan = build(
            false, true, false, fontEnabled = true, fontMode = FontApplyMode.FIELD_REWRITE,
            debug = DebugFontOverride.of(false, false, true),
        )
        assertTrue(plan.debugDisableTextViewAbsoluteRewrite)
        assertTrue(plan.textViewHooksEnabled)
        assertTrue(plan.fontDomainPlan.textViewSpRewriteEnabled)
        assertFalse(plan.fontDomainPlan.textViewAbsoluteRewriteEnabled)
        assertEquals("disable-textview-absolute", plan.reason.debugOverride)
    }

    @Test
    fun debugDisableActivityThreadKeepsOtherEmulationRoutes() {
        val plan = build(
            false, true, false, fontEnabled = true, fontMode = FontApplyMode.SYSTEM_EMULATION,
            debug = DebugFontOverride.of(false, false, false, true),
        )
        assertEquals(FontMode.EMULATION, plan.fontMode)
        assertTrue(plan.debugDisableActivityThreadFont)
        assertTrue(plan.resourcesHooksEnabled)
        assertFalse(plan.activityThreadFontEnabled)
        assertTrue(plan.domainPlan.hasResourcesFont())
        assertTrue(plan.domainPlan.hasSystemServerFont())
        assertFalse(plan.domainPlan.hasActivityThreadFont())
        assertEquals("disable-activity-thread-font", plan.reason.debugOverride)
    }

    @Test
    fun probesDependOnPolicyAndFinalRoutes() {
        val enabled = build(
            false,
            true,
            true,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.FIELD_REWRITE,
            fontEnabled = true,
            fontMode = FontApplyMode.SYSTEM_EMULATION
        )
        val disabled = build(
            true,
            true,
            true,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.FIELD_REWRITE,
            fontEnabled = true,
            fontMode = FontApplyMode.SYSTEM_EMULATION
        )
        assertTrue(enabled.resourcesProbeEnabled)
        assertTrue(enabled.viewportProbeEnabled)
        assertEquals("full", enabled.probeInstallMode)
        assertFalse(disabled.resourcesProbeEnabled)
        assertFalse(disabled.viewportProbeEnabled)
        assertEquals("safe mode", disabled.probeInstallMode)
    }

    @Test
    fun systemHooksOffDisablesSystemEmulationButKeepsFieldRewrite() {
        val emulation = build(
            false,
            false,
            false,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.SYSTEM_EMULATION,
            fontEnabled = true,
            fontMode = FontApplyMode.SYSTEM_EMULATION
        )
        val rewrite = build(
            false,
            false,
            false,
            viewportEnabled = true,
            viewportMode = ViewportApplyMode.FIELD_REWRITE,
            fontEnabled = true,
            fontMode = FontApplyMode.FIELD_REWRITE
        )
        assertFalse(emulation.viewportEnabled)
        assertEquals(FontMode.OFF, emulation.fontMode)
        assertEquals("viewport-system-hooks-off", emulation.reason.fallback)
        assertTrue(rewrite.viewportEnabled)
        assertEquals(FontMode.FIELD_REWRITE, rewrite.fontMode)
    }

    @Test
    fun customFieldRewritePathReplacesAutomaticDomainsAndRejectsActivityThread() {
        val plan = build(
            false, true, false, packageName = "com.example.app", fontEnabled = true,
            fontMode = FontApplyMode.FIELD_REWRITE,
            override = HookDomainOverride(
                true,
                setOf(
                    FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                    FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT
                ),
                setOf("removed_domain")
            ),
        )
        assertEquals("custom", plan.hookDomainSource)
        assertEquals("textview_absolute_rewrite", plan.hookDomains)
        assertEquals("removed_domain", plan.unknownCustomDomains)
        assertFalse(plan.resourcesHooksEnabled)
        assertFalse(plan.activityThreadFontEnabled)
        assertTrue(plan.textViewHooksEnabled)
        assertFalse(plan.webViewTextZoomEnabled)
    }

    @Test
    fun emptyCustomFieldRewritePathDisablesAllFontDomains() {
        val plan = build(
            false,
            true,
            false,
            packageName = "com.example.app",
            fontEnabled = true,
            fontMode = FontApplyMode.FIELD_REWRITE,
            override = HookDomainOverride(true, emptySet(), emptySet())
        )
        assertEquals("custom", plan.hookDomainSource)
        assertEquals("", plan.hookDomains)
        assertFalse(plan.resourcesHooksEnabled)
        assertFalse(plan.textViewHooksEnabled)
        assertFalse(plan.webViewTextZoomEnabled)
        assertFalse(plan.flutterSettingsEnabled)
    }

    @Test
    fun customDomainsAreIgnoredOutsideFieldRewriteRuntime() {
        val plan = build(
            false,
            true,
            false,
            packageName = "com.example.app",
            fontEnabled = true,
            fontMode = FontApplyMode.SYSTEM_EMULATION,
            override = HookDomainOverride(
                true,
                setOf(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE),
                setOf("ignored_domain")
            )
        )
        assertEquals("auto", plan.hookDomainSource)
        assertEquals("ignored_domain", plan.unknownCustomDomains)
        assertTrue(plan.resourcesHooksEnabled)
        assertTrue(plan.activityThreadFontEnabled)
        assertFalse(plan.textViewHooksEnabled)
    }

    @Test
    fun builtinDomainsAreReplacedByCustomFieldRewritePath() {
        val builtin = setOf(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER)
        val automatic = HookExecutionPlanner.buildPlanWithBuiltinDomainsForTest(
            policy(),
            "com.example.app",
            false,
            ViewportApplyMode.OFF,
            true,
            FontApplyMode.FIELD_REWRITE,
            false,
            false,
            HookDomainOverride.automatic(),
            DebugFontOverride.none(),
            builtin
        )
        val custom = HookExecutionPlanner.buildPlanWithBuiltinDomainsForTest(
            policy(),
            "com.example.app",
            false,
            ViewportApplyMode.OFF,
            true,
            FontApplyMode.FIELD_REWRITE,
            false,
            false,
            HookDomainOverride(true, setOf(FontHookDomainRegistry.ID_RESOURCES_FONT), emptySet()),
            DebugFontOverride.none(),
            builtin
        )
        assertEquals("hyperos_native_flutter", automatic.builtinDomains)
        assertTrue(automatic.hyperOsNativeFlutterEnabled)
        assertEquals("auto", automatic.hookDomainSource)
        assertEquals("", custom.builtinDomains)
        assertFalse(custom.hyperOsNativeFlutterEnabled)
        assertEquals("custom", custom.hookDomainSource)
        assertEquals("resources_font", custom.hookDomains)
    }

    @Test
    fun hyperOsDefaultPackagesReceiveNativeFlutterDomain() {
        for (packageName in listOf("com.miui.gallery", "com.miui.weather2")) {
            val plan = build(
                false,
                true,
                false,
                packageName = packageName,
                fontEnabled = true,
                fontMode = FontApplyMode.FIELD_REWRITE
            )
            assertEquals("hyperos_native_flutter", plan.builtinDomains)
            assertTrue(plan.hyperOsNativeFlutterEnabled)
        }
    }

    @Test
    fun bilibiliAndDouyinUseModeDrivenAutomaticDomainsOnly() {
        val generic = build(
            false,
            true,
            false,
            packageName = "com.example.video",
            fontEnabled = true,
            fontMode = FontApplyMode.SYSTEM_EMULATION
        )
        for (packageName in listOf("tv.danmaku.bili", "com.ss.android.ugc.aweme")) {
            val plan = build(
                false,
                true,
                false,
                packageName = packageName,
                fontEnabled = true,
                fontMode = FontApplyMode.SYSTEM_EMULATION
            )
            assertEquals(generic.hookDomains, plan.hookDomains)
            assertEquals("", plan.builtinDomains)
            assertEquals("auto", plan.hookDomainSource)
            assertTrue(plan.domainPlan.hasSystemServerFont())
        }
    }

    private fun build(
        safeMode: Boolean,
        systemHooksEnabled: Boolean,
        globalLogEnabled: Boolean,
        packageName: String = "com.example.app",
        viewportEnabled: Boolean = false,
        viewportMode: String = ViewportApplyMode.OFF,
        fontEnabled: Boolean = false,
        fontMode: String = FontApplyMode.OFF,
        flutterSettingsEnabled: Boolean = false,
        hyperOsFlutterEnabled: Boolean = false,
        override: HookDomainOverride = HookDomainOverride.automatic(),
        debug: DebugFontOverride = DebugFontOverride.none(),
    ): HookExecutionPlan = HookExecutionPlanner.buildPlan(
        policy(safeMode, systemHooksEnabled, globalLogEnabled),
        packageName,
        viewportEnabled,
        viewportMode,
        fontEnabled,
        fontMode,
        flutterSettingsEnabled,
        hyperOsFlutterEnabled,
        override,
        debug,
    )

    private fun policy(
        safeMode: Boolean = false,
        systemHooksEnabled: Boolean = true,
        globalLogEnabled: Boolean = false
    ): HookRuntimePolicy {
        val store = DpisConfigStore(FakePrefs())
        store.setSystemServerSafeModeEnabled(safeMode)
        store.setSystemServerHooksEnabled(systemHooksEnabled)
        store.setGlobalLogEnabled(globalLogEnabled)
        return HookRuntimePolicy.fromStore(store)
    }
}
