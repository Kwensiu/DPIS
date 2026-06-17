package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppProcessHookInstallerTest {
    @Test
    public void safeModeKeepsFieldRewriteWhenSystemHooksEnabled() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertTrue(plan.fieldRewriteEnabled);
    }

    @Test
    public void nonSafeModeKeepsFieldRewrite() {
        HookRuntimePolicy policy = createPolicy(false);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertTrue(plan.fieldRewriteEnabled);
    }

    @Test
    public void emulationModeStaysEmulationInSafeMode() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.SYSTEM_EMULATION);

        assertTrue(plan.emulationEnabled);
        assertFalse(plan.fieldRewriteEnabled);
    }

    @Test
    public void systemHookOffDisablesEmulationMode() {
        HookRuntimePolicy policy = createPolicy(false, false);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.SYSTEM_EMULATION);

        assertFalse(plan.emulationEnabled);
        assertFalse(plan.fieldRewriteEnabled);
    }

    @Test
    public void safeModeWithSystemHookOffKeepsFieldRewrite() {
        HookRuntimePolicy policy = createPolicy(true, false);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertTrue(plan.fieldRewriteEnabled);
    }

    @Test
    public void explicitSystemViewportInstallsResourcesFallbackHooks() {
        HookRuntimePolicy policy = createPolicy(false, true);

        boolean enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy,
                true,
                ViewportApplyMode.SYSTEM);

        assertTrue(enabled);
    }

    @Test
    public void absoluteSystemViewportSkipsDisplaySupplementHooks() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true),
                true,
                ViewportApplyMode.SYSTEM,
                false,
                FontApplyMode.OFF,
                false,
                false,
                DebugFontOverride.none());

        assertTrue(plan.viewportEnabled);
        assertTrue(plan.resourcesHooksEnabled);
        assertFalse(AppProcessHookInstaller.shouldInstallAppProcessViewportSupplementHooksForTest(
                plan,
                ViewportTargetSpec.absoluteDp(300)));
    }

    @Test
    public void compatViewportKeepsDisplaySupplementHooks() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, false),
                true,
                ViewportApplyMode.AUTO,
                false,
                FontApplyMode.OFF,
                false,
                false,
                DebugFontOverride.none());

        assertTrue(plan.viewportEnabled);
        assertTrue(AppProcessHookInstaller.shouldInstallAppProcessViewportSupplementHooksForTest(
                plan,
                ViewportTargetSpec.absoluteDp(300)));
    }

    @Test
    public void viewportOnlyRouteKeepsResourcesImplHook() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, false),
                true,
                ViewportApplyMode.COMPAT,
                false,
                FontApplyMode.OFF,
                false,
                false,
                DebugFontOverride.none());

        assertTrue(plan.viewportEnabled);
        assertTrue(plan.resourcesHooksEnabled);
        assertTrue(plan.resourcesWriteHooksEnabled);
        assertTrue(plan.resourcesImplHookEnabled);
        assertTrue(plan.resourcesReadHooksEnabled);
        assertTrue(plan.resourcesReadPolicy.viewportHandlingEnabled);
        assertFalse(plan.resourcesReadPolicy.configurationFontOverrideEnabled);
        assertFalse(plan.resourcesReadPolicy.metricsTargetFontOverrideEnabled);
        assertFalse(plan.fontDomainPlan.resourcesFontEnabled);
        assertTrue(AppProcessHookInstaller.shouldInstallResourcesImplHookForTest(plan));
    }

    @Test
    public void customResourcesFontRouteUsesImplSeedAndReadSideResourcesHooks() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                new HookDomainOverride(true,
                        java.util.Set.of(FontHookDomainRegistry.ID_RESOURCES_FONT),
                        java.util.Set.of()),
                DebugFontOverride.none());

        assertTrue(plan.resourcesHooksEnabled);
        assertFalse(plan.resourcesWriteHooksEnabled);
        assertTrue(plan.resourcesImplHookEnabled);
        assertTrue(plan.resourcesReadHooksEnabled);
        assertFalse(plan.resourcesReadPolicy.viewportHandlingEnabled);
        assertTrue(plan.resourcesReadPolicy.configurationFontOverrideEnabled);
        assertFalse(plan.resourcesReadPolicy.metricsTargetFontOverrideEnabled);
        assertTrue(plan.fontDomainPlan.resourcesFontEnabled);
        assertTrue(AppProcessHookInstaller.shouldInstallResourcesImplHookForTest(plan));
    }

    @Test
    public void fontEmulationKeepsResourcesReadViewportHandling() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true),
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false,
                DebugFontOverride.none());

        assertTrue(plan.resourcesWriteHooksEnabled);
        assertTrue(plan.resourcesReadHooksEnabled);
        assertTrue(plan.resourcesReadPolicy.viewportHandlingEnabled);
        assertFalse(plan.resourcesReadPolicy.configurationFontOverrideEnabled);
        assertTrue(plan.resourcesReadPolicy.metricsTargetFontOverrideEnabled);
    }

    @Test
    public void relativeSystemViewportSkipsDisplaySupplementHooks() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true),
                true,
                ViewportApplyMode.AUTO,
                false,
                FontApplyMode.OFF,
                false,
                false,
                DebugFontOverride.none());

        assertTrue(plan.viewportEnabled);
        assertFalse(AppProcessHookInstaller.shouldInstallAppProcessViewportSupplementHooksForTest(
                plan,
                ViewportTargetSpec.relativeScale(1200)));
    }

    @Test
    public void systemHookOffDisablesExplicitSystemViewportHooks() {
        HookRuntimePolicy policy = createPolicy(false, false);

        boolean enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy,
                true,
                ViewportApplyMode.SYSTEM);

        assertFalse(enabled);
    }

    @Test
    public void systemHookOffKeepsViewportReplaceHooks() {
        HookRuntimePolicy policy = createPolicy(false, false);

        boolean enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy,
                true,
                ViewportApplyMode.FIELD_REWRITE);

        assertTrue(enabled);
    }

    @Test
    public void inactiveFontScaleDisablesFontHooks() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                false,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertFalse(plan.fieldRewriteEnabled);
    }

    @Test
    public void fieldRewriteFontScaleUsesCompatDomainsWithoutResourcesFontByDefault() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan fontHookPlan =
                AppProcessHookInstaller.resolveFontHookPlan(
                        policy,
                        true,
                        FontApplyMode.FIELD_REWRITE);
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(fontHookPlan);

        assertFalse(fontHookPlan.emulationEnabled);
        assertTrue(fontHookPlan.fieldRewriteEnabled);
        assertFalse(domainPlan.resourcesFontEnabled);
        assertFalse(AppProcessHookInstaller.resolveResourcesHooksEnabled(false, fontHookPlan, domainPlan));
    }

    @Test
    public void fieldRewriteDomainKeepsFlutterSettingsExperimental() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true));

        assertFalse(domainPlan.resourcesFontEnabled);
        assertTrue(domainPlan.webViewTextZoomEnabled);
        assertTrue(domainPlan.textViewHooksEnabled);
        assertTrue(domainPlan.textViewSpRewriteEnabled);
        assertTrue(domainPlan.textViewAbsoluteRewriteEnabled);
        assertTrue(domainPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void emulationDomainKeepsFlutterSettingsExperimental() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(true, false));

        assertTrue(domainPlan.resourcesFontEnabled);
        assertTrue(domainPlan.webViewTextZoomEnabled);
        assertFalse(domainPlan.textViewHooksEnabled);
        assertFalse(domainPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void flutterSettingsDomainIsIndependentlyGatedBySupplementFlag() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(true, false),
                        true,
                        false);

        assertTrue(domainPlan.resourcesFontEnabled);
        assertTrue(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void disabledFontPlanSuppressesFlutterSettingsSupplementDomain() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, false),
                        true,
                        false);

        assertFalse(domainPlan.resourcesFontEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void fieldRewriteKeepsRecommendedTextViewAndPaintFallbacks() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true));

        assertTrue(domainPlan.webViewTextZoomEnabled);
        assertTrue(domainPlan.textViewHooksEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertTrue(domainPlan.textViewSpRewriteEnabled);
        assertTrue(domainPlan.textViewAbsoluteRewriteEnabled);
        assertTrue(domainPlan.textViewCurrentPxFallbackEnabled);
        assertTrue(domainPlan.paintFallbackEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void hyperOsNativeFlutterDomainIsGatedByArbitration() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true),
                        true);

        assertFalse(domainPlan.resourcesFontEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertTrue(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void disabledFontPlanDoesNotEnableNativeFlutterDomain() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, false),
                        true);

        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void fontDomainPlanKeepsUnifiedDispatchForEveryFontApplyMode() {
        FontHookArbitration.FontDomainPlan emulationPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(true, false));
        FontHookArbitration.FontDomainPlan fieldRewritePlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true));

        assertTrue(emulationPlan.webViewTextZoomEnabled);
        assertFalse(emulationPlan.flutterSettingsEnabled);
        assertTrue(emulationPlan.resourcesFontEnabled);
        assertFalse(emulationPlan.textViewHooksEnabled);
        assertFalse(emulationPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(emulationPlan.hyperOsNativeFlutterEnabled);
        assertFalse(emulationPlan.genericNativeFlutterEnabled);

        assertFalse(fieldRewritePlan.resourcesFontEnabled);
        assertTrue(fieldRewritePlan.webViewTextZoomEnabled);
        assertTrue(fieldRewritePlan.textViewHooksEnabled);
        assertFalse(fieldRewritePlan.flutterSettingsEnabled);
        assertTrue(fieldRewritePlan.textViewSpRewriteEnabled);
        assertTrue(fieldRewritePlan.textViewAbsoluteRewriteEnabled);
        assertTrue(fieldRewritePlan.textViewCurrentPxFallbackEnabled);
        assertTrue(fieldRewritePlan.paintFallbackEnabled);
        assertFalse(fieldRewritePlan.hyperOsNativeFlutterEnabled);
        assertFalse(fieldRewritePlan.genericNativeFlutterEnabled);
    }

    @Test
    public void typefacePlanDoesNotEnableFontScaleHooks() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetTypefaceId("com.example.app", "font_abcd1234");

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");
        AppProcessHookInstaller.FontHookPlan fontHookPlan =
                AppProcessHookInstaller.resolveFontHookPlan(
                        null, plan.fontScaleActive, plan.targetFontMode);

        assertFalse(fontHookPlan.emulationEnabled);
        assertFalse(fontHookPlan.fieldRewriteEnabled);
        assertTrue(plan.typefaceEnabled);
    }

    @Test
    public void typefaceInstallerIsIndependentFromResourcesHookGate() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppProcessHookInstaller.java");
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("TypefaceOverrideHookInstaller.install("));
        assertTrue(source.indexOf("installTypefaceHooks(xposed, packageName, store, packagePlan.targetTypefaceId);")
                < source.indexOf("installFromPlan(xposed, packageName, store, plan,"
                + " packagePlan.targetViewportSpec);"));
        assertTrue(moduleMain.contains("packagePlan.targetTypefaceId"));
        assertTrue(moduleMain.contains("retryTypefaceHooksWithPackageReady"));
        assertTrue(moduleMain.contains("AppProcessHookInstaller.installTypefaceHooks("));
        assertTrue(source.contains("failed to install typeface hooks: package="));
        assertFalse(source.contains("HookExecutionPlanner.buildPlan("));
    }

    @Test
    public void skipsProbeHookPathWhenSafetyModeEnabled() throws Exception {
        HookRuntimePolicy policy = createPolicy(true);

        assertFalse(AppProcessHookInstaller.shouldInstallProbeHooks(policy));
    }

    @Test
    public void nullPolicyDisablesProbeHookPath() {
        assertFalse(AppProcessHookInstaller.shouldInstallProbeHooks(null));
    }

    @Test
    public void nullPolicyFallsBackToProbeDisabledModeLabel() {
        assertTrue("probe disabled".equals(AppProcessHookInstaller.resolveProbeInstallMode(null)));
    }

    @Test
    public void allowsProbeHookPathWhenSafetyModeDisabledAndGlobalLoggingEnabled()
            throws Exception {
        HookRuntimePolicy policy = createPolicy(false, true, true);

        assertTrue(AppProcessHookInstaller.shouldInstallProbeHooks(policy));
    }

    @Test
    public void debugFlutterSettingsPropertyMatchesExactPackageOrWildcardOnly() {
        assertTrue(AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "com.example.app"));
        assertTrue(AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "*"));
        assertFalse(AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                "com.example.other"));
        assertFalse(AppProcessHookInstaller.isDebugPropertyPackageMatchForTest(
                "debug.dpis.font.flutter_settings_only_package",
                "com.example.app",
                ""));
    }

    @Test
    public void debugFlutterSettingsPropertiesAreDebugOnlyAndPackageScoped() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/AppProcessHookInstaller.java");
        String planner = readSource("src/main/java/com/dpis/module/HookExecutionPlanner.java");
        String packagePlan = readSource("src/main/java/com/dpis/module/ModulePackagePlan.java");

        assertTrue(source.contains("debug.dpis.font.force_flutter_settings_package"));
        assertTrue(source.contains("debug.dpis.font.flutter_settings_only_package"));
        assertTrue(source.contains("debug.dpis.font.disable_textview_absolute_rewrite_package"));
        assertTrue(source.contains("debug.dpis.font.disable_activity_thread_package"));
        assertTrue(source.contains("debug.dpis.viewport.disable_display_supplement_package"));
        assertTrue(source.contains("debug.dpis.viewport.disable_resources_impl_package"));
        assertTrue(source.contains("debug.dpis.viewport.disable_resources_read_package"));
        assertTrue(source.contains("DebugPackageOverride.matches("));
        assertTrue(readSource("src/main/java/com/dpis/module/DebugPackageOverride.java")
                .contains("if (!BuildConfig.DEBUG || packageName == null"));
        assertTrue(source.contains("DebugFontOverride.of("));
        assertTrue(source.contains("packagePlan.buildExecutionPlan("));
        assertFalse(source.contains("HookExecutionPlanner.buildPlan("));
        assertTrue(packagePlan.contains("HookExecutionPlanner.buildPlan("));
        assertTrue(planner.contains("if (resolvedDebug.forceFlutterSettings)"));
        assertTrue(planner.contains("shapedDomains.add(FontHookDomainRegistry.ID_FLUTTER_SETTINGS);"));
        assertTrue(planner.contains("if (resolvedDebug.disableTextViewAbsoluteRewrite)"));
        assertTrue(planner.contains("shapedDomains.remove(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE);"));
        assertTrue(planner.contains("if (resolvedDebug.disableActivityThreadFont)"));
        assertTrue(planner.contains("shapedDomains.remove(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT);"));
        assertTrue(planner.contains("!resolvedDebug.flutterSettingsOnly"));
    }

    @Test
    public void composeDiagnosticsAreWiredOnlyThroughResourcesFontDomain() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/AppProcessHookInstaller.java");
        String installer = readSource(
                "src/main/java/com/dpis/module/ComposeFontRuntimeDiagnosticsInstaller.java");

        assertTrue(source.contains("ComposeFontRuntimeDiagnosticsInstaller.shouldInstall(plan)"));
        assertTrue(source.contains("ComposeFontRuntimeDiagnosticsInstaller.install("));
        assertTrue(source.contains("ResourcesReadHookInstaller.install("));
        assertTrue(source.contains("plan.resourcesReadPolicy"));
        assertTrue(installer.contains("domainPlan.resourcesFontEnabled"));
        assertTrue(installer.contains("store.getTargetFontScalePercent(packageName)"));
        assertTrue(installer.contains("activity.getWindow()"));
        assertTrue(installer.contains("getDecorView()"));
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onResume\")"));
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onPause\")"));
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onStop\")"));
        assertTrue(installer.contains("Activity.class.getDeclaredMethod(\"onDestroy\")"));
        assertTrue(installer.contains("addOnGlobalLayoutListener"));
        assertTrue(installer.contains("removeOnGlobalLayoutListener"));
        assertTrue(installer.contains("ComposeResourcesFontEvidence.summarize("));
        assertTrue(installer.contains("FontDebugStatsReporter.record("));
        assertFalse(installer.contains("ForceTextSizeHookInstaller"));
    }

    @Test
    public void composeDiagnosticsDoNotSuppressGlobalTextViewOrResourceFontRoutes()
            throws Exception {
        String installer = readSource(
                "src/main/java/com/dpis/module/ComposeFontRuntimeDiagnosticsInstaller.java");

        assertFalse(installer.contains("textViewCurrentPxFallbackEnabled = false"));
        assertFalse(installer.contains("paintFallbackEnabled = false"));
        assertFalse(installer.contains("resourcesFontEnabled = false"));
        assertFalse(installer.contains("setTargetFontScalePercent"));
        assertFalse(installer.contains("clearTargetFontScalePercent"));
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode) {
        return createPolicy(safeMode, true);
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode, boolean systemHooksEnabled) {
        return createPolicy(safeMode, systemHooksEnabled, false);
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode,
                                                  boolean systemHooksEnabled,
                                                  boolean globalLogEnabled) {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerSafeModeEnabled(safeMode);
        store.setSystemServerHooksEnabled(systemHooksEnabled);
        store.setGlobalLogEnabled(globalLogEnabled);
        return HookRuntimePolicy.fromStore(store);
    }

    private static String readSource(String relativePath) throws Exception {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
