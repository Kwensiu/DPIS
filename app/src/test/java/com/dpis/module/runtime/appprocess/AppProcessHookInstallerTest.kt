package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.ModulePackagePlan
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookExecutionPlanner
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.AppProcessHookInstaller
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProcessHookInstallerTest {
    @Test
    fun safeModeKeepsFieldRewriteWhenSystemHooksEnabled() {
        val plan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = true),
            true,
            FontApplyMode.FIELD_REWRITE,
        )

        assertFalse(plan.emulationEnabled)
        assertTrue(plan.fieldRewriteEnabled)
    }

    @Test
    fun nonSafeModeKeepsFieldRewrite() {
        val plan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = false),
            true,
            FontApplyMode.FIELD_REWRITE,
        )

        assertFalse(plan.emulationEnabled)
        assertTrue(plan.fieldRewriteEnabled)
    }

    @Test
    fun emulationModeStaysEmulationInSafeMode() {
        val plan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = true),
            true,
            FontApplyMode.SYSTEM_EMULATION,
        )

        assertTrue(plan.emulationEnabled)
        assertFalse(plan.fieldRewriteEnabled)
    }

    @Test
    fun systemHookOffDisablesEmulationMode() {
        val plan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = false, systemHooksEnabled = false),
            true,
            FontApplyMode.SYSTEM_EMULATION,
        )

        assertFalse(plan.emulationEnabled)
        assertFalse(plan.fieldRewriteEnabled)
    }

    @Test
    fun safeModeWithSystemHookOffKeepsFieldRewrite() {
        val plan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = true, systemHooksEnabled = false),
            true,
            FontApplyMode.FIELD_REWRITE,
        )

        assertFalse(plan.emulationEnabled)
        assertTrue(plan.fieldRewriteEnabled)
    }

    @Test
    fun explicitSystemViewportInstallsResourcesFallbackHooks() {
        val enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
            createPolicy(safeMode = false, systemHooksEnabled = true),
            true,
            ViewportApplyMode.SYSTEM,
        )

        assertTrue(enabled)
    }

    @Test
    fun absoluteSystemViewportRecordsWindowBounds() {
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false, systemHooksEnabled = true),
            true,
            ViewportApplyMode.SYSTEM,
            false,
            FontApplyMode.OFF,
            false,
            false,
            DebugFontOverride.none(),
        )

        assertTrue(plan.viewportEnabled)
        assertTrue(plan.resourcesHooksEnabled)
        assertTrue(
            AppProcessHookInstaller.shouldInstallAppProcessViewportSupplementHooksForTest(
                plan
            )
        )
        assertTrue(AppProcessHookInstaller.shouldInstallDisplayMetricsHooksForTest(plan))
    }

    @Test
    fun autoViewportDoesNotInstallDisplaySupplementHooksWhenSystemUnavailable() {
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false, systemHooksEnabled = false),
            true,
            ViewportApplyMode.AUTO,
            false,
            FontApplyMode.OFF,
            false,
            false,
            DebugFontOverride.none(),
        )

        assertTrue(plan.viewportEnabled)
        assertEquals(ViewportApplyMode.COMPAT, plan.resolvedViewportMode)
        assertTrue(
            AppProcessHookInstaller.shouldInstallAppProcessViewportSupplementHooksForTest(
                plan
            )
        )
    }

    @Test
    fun viewportOnlyRouteKeepsResourcesImplHook() {
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false, systemHooksEnabled = false),
            true,
            ViewportApplyMode.COMPAT,
            false,
            FontApplyMode.OFF,
            false,
            false,
            DebugFontOverride.none(),
        )

        assertTrue(plan.viewportEnabled)
        assertTrue(plan.resourcesHooksEnabled)
        assertTrue(plan.resourcesWriteHooksEnabled)
        assertTrue(plan.resourcesImplHookEnabled)
        assertTrue(plan.resourcesReadHooksEnabled)
        assertTrue(plan.resourcesReadPolicy.viewportHandlingEnabled)
        assertFalse(plan.resourcesReadPolicy.configurationFontOverrideEnabled)
        assertFalse(plan.resourcesReadPolicy.metricsTargetFontOverrideEnabled)
        assertFalse(plan.fontDomainPlan.resourcesFontEnabled)
        assertTrue(AppProcessHookInstaller.shouldInstallResourcesImplHookForTest(plan))
    }

    @Test
    fun customResourcesFontRouteUsesImplSeedAndReadSideResourcesHooks() {
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false, systemHooksEnabled = true),
            "com.example.app",
            false,
            ViewportApplyMode.OFF,
            true,
            FontApplyMode.FIELD_REWRITE,
            false,
            false,
            HookDomainOverride(
                true,
                setOf(FontHookDomainRegistry.ID_RESOURCES_FONT),
                emptySet(),
            ),
            DebugFontOverride.none(),
        )

        assertTrue(plan.resourcesHooksEnabled)
        assertFalse(plan.resourcesWriteHooksEnabled)
        assertTrue(plan.resourcesImplHookEnabled)
        assertTrue(plan.resourcesReadHooksEnabled)
        assertFalse(plan.resourcesReadPolicy.viewportHandlingEnabled)
        assertTrue(plan.resourcesReadPolicy.configurationFontOverrideEnabled)
        assertFalse(plan.resourcesReadPolicy.metricsTargetFontOverrideEnabled)
        assertTrue(plan.fontDomainPlan.resourcesFontEnabled)
        assertTrue(AppProcessHookInstaller.shouldInstallResourcesImplHookForTest(plan))
    }

    @Test
    fun fontEmulationKeepsResourcesReadViewportHandling() {
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false, systemHooksEnabled = true),
            false,
            ViewportApplyMode.OFF,
            true,
            FontApplyMode.SYSTEM_EMULATION,
            false,
            false,
            DebugFontOverride.none(),
        )

        assertTrue(plan.resourcesWriteHooksEnabled)
        assertTrue(plan.resourcesReadHooksEnabled)
        assertTrue(plan.resourcesReadPolicy.viewportHandlingEnabled)
        assertFalse(plan.resourcesReadPolicy.configurationFontOverrideEnabled)
        assertTrue(plan.resourcesReadPolicy.metricsTargetFontOverrideEnabled)
    }

    @Test
    fun relativeSystemViewportRecordsWindowBounds() {
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false, systemHooksEnabled = true),
            true,
            ViewportApplyMode.AUTO,
            false,
            FontApplyMode.OFF,
            false,
            false,
            DebugFontOverride.none(),
        )

        assertTrue(plan.viewportEnabled)
        assertTrue(
            AppProcessHookInstaller.shouldInstallAppProcessViewportSupplementHooksForTest(
                plan
            )
        )
        assertTrue(AppProcessHookInstaller.shouldInstallDisplayMetricsHooksForTest(plan))
    }

    @Test
    fun systemHookOffDisablesExplicitSystemViewportHooks() {
        val enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
            createPolicy(safeMode = false, systemHooksEnabled = false),
            true,
            ViewportApplyMode.SYSTEM,
        )

        assertFalse(enabled)
    }

    @Test
    fun systemHookOffKeepsViewportReplaceHooks() {
        val enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
            createPolicy(safeMode = false, systemHooksEnabled = false),
            true,
            ViewportApplyMode.FIELD_REWRITE,
        )

        assertTrue(enabled)
    }

    @Test
    fun inactiveFontScaleDisablesFontHooks() {
        val plan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = true),
            false,
            FontApplyMode.FIELD_REWRITE,
        )

        assertFalse(plan.emulationEnabled)
        assertFalse(plan.fieldRewriteEnabled)
    }

    @Test
    fun fieldRewriteFontScaleUsesCompatDomainsWithoutResourcesFontByDefault() {
        val fontHookPlan = AppProcessHookInstaller.resolveFontHookPlan(
            createPolicy(safeMode = true),
            true,
            FontApplyMode.FIELD_REWRITE,
        )
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(fontHookPlan)

        assertFalse(fontHookPlan.emulationEnabled)
        assertTrue(fontHookPlan.fieldRewriteEnabled)
        assertFalse(domainPlan.resourcesFontEnabled)
        assertFalse(
            AppProcessHookInstaller.resolveResourcesHooksEnabled(
                false,
                fontHookPlan,
                domainPlan
            )
        )
    }

    @Test
    fun fieldRewriteDomainKeepsFlutterSettingsExperimental() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(false, true),
        )

        assertFalse(domainPlan.resourcesFontEnabled)
        assertTrue(domainPlan.webViewTextZoomEnabled)
        assertTrue(domainPlan.textViewHooksEnabled)
        assertTrue(domainPlan.textViewSpRewriteEnabled)
        assertTrue(domainPlan.textViewAbsoluteRewriteEnabled)
        assertTrue(domainPlan.textViewCurrentPxFallbackEnabled)
        assertFalse(domainPlan.flutterSettingsEnabled)
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun emulationDomainKeepsFlutterSettingsExperimental() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(true, false),
        )

        assertTrue(domainPlan.resourcesFontEnabled)
        assertTrue(domainPlan.webViewTextZoomEnabled)
        assertFalse(domainPlan.textViewHooksEnabled)
        assertFalse(domainPlan.textViewCurrentPxFallbackEnabled)
        assertFalse(domainPlan.flutterSettingsEnabled)
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun flutterSettingsDomainIsIndependentlyGatedBySupplementFlag() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(true, false),
            true,
            false,
        )

        assertTrue(domainPlan.resourcesFontEnabled)
        assertTrue(domainPlan.flutterSettingsEnabled)
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun disabledFontPlanSuppressesFlutterSettingsSupplementDomain() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(false, false),
            true,
            false,
        )

        assertFalse(domainPlan.resourcesFontEnabled)
        assertFalse(domainPlan.flutterSettingsEnabled)
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun fieldRewriteKeepsRecommendedTextViewAndPaintFallbacks() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(false, true),
        )

        assertTrue(domainPlan.webViewTextZoomEnabled)
        assertTrue(domainPlan.textViewHooksEnabled)
        assertFalse(domainPlan.flutterSettingsEnabled)
        assertTrue(domainPlan.textViewSpRewriteEnabled)
        assertTrue(domainPlan.textViewAbsoluteRewriteEnabled)
        assertTrue(domainPlan.textViewCurrentPxFallbackEnabled)
        assertTrue(domainPlan.paintFallbackEnabled)
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun hyperOsNativeFlutterDomainIsGatedByArbitration() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(false, true),
            true,
        )

        assertFalse(domainPlan.resourcesFontEnabled)
        assertFalse(domainPlan.flutterSettingsEnabled)
        assertTrue(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun disabledFontPlanDoesNotEnableNativeFlutterDomain() {
        val domainPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(false, false),
            true,
        )

        assertFalse(domainPlan.flutterSettingsEnabled)
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled)
        assertFalse(domainPlan.genericNativeFlutterEnabled)
    }

    @Test
    fun fontDomainPlanKeepsUnifiedDispatchForEveryFontApplyMode() {
        val emulationPlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(true, false),
        )
        val fieldRewritePlan = AppProcessHookInstaller.resolveFontDomainPlan(
            AppProcessHookInstaller.FontHookPlan(false, true),
        )

        assertTrue(emulationPlan.webViewTextZoomEnabled)
        assertFalse(emulationPlan.flutterSettingsEnabled)
        assertTrue(emulationPlan.resourcesFontEnabled)
        assertFalse(emulationPlan.textViewHooksEnabled)
        assertFalse(emulationPlan.textViewCurrentPxFallbackEnabled)
        assertFalse(emulationPlan.hyperOsNativeFlutterEnabled)
        assertFalse(emulationPlan.genericNativeFlutterEnabled)

        assertFalse(fieldRewritePlan.resourcesFontEnabled)
        assertTrue(fieldRewritePlan.webViewTextZoomEnabled)
        assertTrue(fieldRewritePlan.textViewHooksEnabled)
        assertFalse(fieldRewritePlan.flutterSettingsEnabled)
        assertTrue(fieldRewritePlan.textViewSpRewriteEnabled)
        assertTrue(fieldRewritePlan.textViewAbsoluteRewriteEnabled)
        assertTrue(fieldRewritePlan.textViewCurrentPxFallbackEnabled)
        assertTrue(fieldRewritePlan.paintFallbackEnabled)
        assertFalse(fieldRewritePlan.hyperOsNativeFlutterEnabled)
        assertFalse(fieldRewritePlan.genericNativeFlutterEnabled)
    }

    @Test
    fun typefacePlanDoesNotEnableFontScaleHooks() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetTypefaceId("com.example.app", "font_abcd1234")

        val plan = ModulePackagePlan.resolve(store, "com.example.app")
        val fontHookPlan = AppProcessHookInstaller.resolveFontHookPlan(
            null,
            plan.fontScaleActive,
            plan.targetFontMode,
        )

        assertFalse(fontHookPlan.emulationEnabled)
        assertFalse(fontHookPlan.fieldRewriteEnabled)
        assertTrue(plan.typefaceEnabled)
    }

    @Test
    fun typefaceInstallerIsIndependentFromResourcesHookGate() {
        val source =
            read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt")
        val moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java")

        assertTrue(source.contains("TypefaceOverrideHookInstaller.install("))
        assertTrue(source.contains("ModernApiCapabilitiesResolver.fromXposed(xposed)"))
        assertTrue(source.contains("installFromPlan("))
        assertTrue(source.contains("apiCapabilities: ModernApiCapabilities"))
        assertTrue(
            source.indexOf(
                "installTypefaceHooks(xposed, packageName, store, packagePlan.targetTypefaceId)",
            ) < source.indexOf("installFromPlan("),
        )
        assertTrue(source.contains("packagePlan.targetViewportSpec,"))
        assertTrue(source.contains("policy,"))
        assertTrue(source.contains("apiCapabilities"))
        assertTrue(moduleMain.contains("packagePlan.targetTypefaceId"))
        assertTrue(moduleMain.contains("retryTypefaceHooksWithPackageReady"))
        assertTrue(moduleMain.contains("AppProcessHookInstaller.installTypefaceHooks("))
        assertTrue(source.contains("failed to install typeface hooks: package="))
        assertFalse(source.contains("HookExecutionPlanner.buildPlan("))
    }

    @Test
    fun skipsProbeHookPathWhenSafetyModeEnabled() {
        assertFalse(AppProcessHookInstaller.shouldInstallProbeHooks(createPolicy(safeMode = true)))
    }

    @Test
    fun nullPolicyDisablesProbeHookPath() {
        assertFalse(AppProcessHookInstaller.shouldInstallProbeHooks(null))
    }

    @Test
    fun nullPolicyFallsBackToProbeDisabledModeLabel() {
        assertTrue("probe disabled" == AppProcessHookInstaller.resolveProbeInstallMode(null))
    }

    @Test
    fun allowsProbeHookPathWhenSafetyModeDisabledAndGlobalLoggingEnabled() {
        assertTrue(
            AppProcessHookInstaller.shouldInstallProbeHooks(
                createPolicy(safeMode = false, systemHooksEnabled = true, globalLogEnabled = true),
            ),
        )
    }

    @Test
    fun debugFlutterSettingsPropertyMatchesExactPackageOrWildcardOnly() {
        assertTrue(
            AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "com.example.app",
            ),
        )
        assertTrue(
            AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "*",
            ),
        )
        assertFalse(
            AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "com.example.other",
            ),
        )
        assertFalse(
            AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "",
            ),
        )
    }

    @Test
    fun debugFlutterSettingsPropertiesAreDebugOnlyAndPackageScoped() {
        val debugOverride = DebugFontOverride.of(true, true)
        val plan = HookExecutionPlanner.buildPlan(
            createPolicy(safeMode = false),
            "com.example.app",
            false,
            ViewportApplyMode.OFF,
            true,
            FontApplyMode.FIELD_REWRITE,
            true,
            true,
            HookDomainOverride.automatic(),
            debugOverride,
        )

        assertTrue(
            AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "com.example.app",
            ),
        )
        assertFalse(
            AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.other",
                "com.example.app",
            ),
        )
        assertTrue(plan.flutterSettingsEnabled)
        assertTrue(plan.debugForceFlutterSettings)
        assertTrue(plan.debugFlutterSettingsOnly)
        assertFalse(plan.resourcesHooksEnabled)
        assertTrue(plan.hookDomains.contains(FontHookDomainRegistry.ID_FLUTTER_SETTINGS))
    }

    @Test
    fun composeDiagnosticsAreWiredOnlyThroughResourcesFontDomain() {
        val source =
            read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt")
        val installer = read(
            "src/main/java/com/dpis/module/runtime/font/ComposeFontRuntimeDiagnosticsInstaller.java",
        )

        assertTrue(source.contains("ComposeFontRuntimeDiagnosticsInstaller.shouldInstall(plan)"))
        assertTrue(source.contains("ComposeFontRuntimeDiagnosticsInstaller.install("))
        assertTrue(source.contains("ResourcesReadHookInstaller.install("))
        assertTrue(source.contains("plan.resourcesReadPolicy"))
        assertTrue(installer.contains("domainPlan.resourcesFontEnabled"))
        assertTrue(installer.contains("store.getTargetFontScalePercent(packageName)"))
        assertTrue(installer.contains("activity.getWindow()"))
        assertTrue(installer.contains("getDecorView()"))
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onResume\")"))
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onPause\")"))
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onStop\")"))
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onDestroy\")"))
        assertTrue(installer.contains("addOnGlobalLayoutListener"))
        assertTrue(installer.contains("removeOnGlobalLayoutListener"))
        assertTrue(installer.contains("ComposeResourcesFontEvidence.summarize("))
        assertTrue(installer.contains("FontDebugStatsReporter.record("))
        assertFalse(installer.contains("ForceTextSizeHookInstaller"))
    }

    @Test
    fun composeDiagnosticsDoNotSuppressGlobalTextViewOrResourceFontRoutes() {
        val installer = read(
            "src/main/java/com/dpis/module/runtime/font/ComposeFontRuntimeDiagnosticsInstaller.java",
        )

        assertFalse(installer.contains("textViewCurrentPxFallbackEnabled = false"))
        assertFalse(installer.contains("paintFallbackEnabled = false"))
        assertFalse(installer.contains("resourcesFontEnabled = false"))
        assertFalse(installer.contains("setTargetFontScalePercent"))
        assertFalse(installer.contains("clearTargetFontScalePercent"))
    }

    private fun createPolicy(
        safeMode: Boolean,
        systemHooksEnabled: Boolean = true,
        globalLogEnabled: Boolean = false,
    ): HookRuntimePolicy {
        val store = DpisConfigStore(FakePrefs())
        store.setSystemServerSafeModeEnabled(safeMode)
        store.setSystemServerHooksEnabled(systemHooksEnabled)
        store.setGlobalLogEnabled(globalLogEnabled)
        return HookRuntimePolicy.fromStore(store)
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
