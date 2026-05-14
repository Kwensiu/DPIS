package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public void systemHookOffDisablesViewportEmulationHooks() {
        HookRuntimePolicy policy = createPolicy(false, false);

        boolean enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy,
                true,
                ViewportApplyMode.SYSTEM_EMULATION);

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
    public void fieldRewriteFontScaleEnablesResourcesFontDomain() {
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
        assertTrue(domainPlan.resourcesFontEnabled);
        assertTrue(AppProcessHookInstaller.resolveResourcesHooksEnabled(false, fontHookPlan, domainPlan));
    }

    @Test
    public void fieldRewriteDomainKeepsFlutterSettingsExperimental() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true));

        assertTrue(domainPlan.resourcesFontEnabled);
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

        assertTrue(domainPlan.resourcesFontEnabled);
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

        assertTrue(fieldRewritePlan.resourcesFontEnabled);
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

        assertTrue(source.contains("TypefaceOverrideHookInstaller.install("));
        assertTrue(source.indexOf("installFromPlan(xposed, packageName, store, plan);")
                < source.indexOf("TypefaceOverrideHookInstaller.install("));
        assertFalse(source.contains("HookExecutionPlanner.buildPlan("
                + "policy, packageName, viewportConfigured, viewportMode, fontScaleActive, fontMode,"
                + " typefaceActive"));
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

        assertTrue(source.contains("debug.dpis.font.force_flutter_settings_package"));
        assertTrue(source.contains("debug.dpis.font.flutter_settings_only_package"));
        assertTrue(source.contains("debug.dpis.font.disable_textview_absolute_rewrite_package"));
        assertTrue(source.contains("if (!BuildConfig.DEBUG || packageName == null"));
        assertTrue(source.contains("DebugFontOverride.of("));
        assertTrue(source.contains("HookExecutionPlanner.buildPlan("));
        assertTrue(planner.contains("if (resolvedDebug.forceFlutterSettings)"));
        assertTrue(planner.contains("shapedDomains.add(FontHookDomainRegistry.ID_FLUTTER_SETTINGS);"));
        assertTrue(planner.contains("if (resolvedDebug.disableTextViewAbsoluteRewrite)"));
        assertTrue(planner.contains("shapedDomains.remove(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE);"));
        assertTrue(planner.contains("!resolvedDebug.flutterSettingsOnly"));
    }

    @Test
    public void composeDiagnosticsAreWiredOnlyThroughResourcesFontDomain() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/AppProcessHookInstaller.java");
        String installer = readSource(
                "src/main/java/com/dpis/module/ComposeFontRuntimeDiagnosticsInstaller.java");

        assertTrue(source.contains("ComposeFontRuntimeDiagnosticsInstaller.shouldInstall(plan)"));
        assertTrue(source.contains("ComposeFontRuntimeDiagnosticsInstaller.install("));
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
        java.nio.file.Path path = java.nio.file.Paths.get(relativePath);
        if (!java.nio.file.Files.exists(path)) {
            path = java.nio.file.Paths.get("app", relativePath);
        }
        return new String(java.nio.file.Files.readAllBytes(path),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
