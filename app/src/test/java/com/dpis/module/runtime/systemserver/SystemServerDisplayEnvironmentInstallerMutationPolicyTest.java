package com.dpis.module.runtime.systemserver;

import com.dpis.module.*;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.viewport.PerAppDisplayEnvironment;

import com.dpis.module.viewport.ViewportRuntimeMarkerBridge;
import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.hooks.HookDomainOverride;

import com.dpis.module.runtime.DebugPackageOverride;

import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class SystemServerDisplayEnvironmentInstallerMutationPolicyTest {
    @Test
    public void preProceedEnabledForConfigDispatchAndActivityStartOnly() {
        assertTrue(SystemServerMutationPolicy.shouldApplyPreProceedMutations("config-dispatch"));
        assertTrue(SystemServerMutationPolicy.shouldApplyPreProceedMutations("activity-start"));
        assertFalse(SystemServerMutationPolicy.shouldApplyPreProceedMutations("display-policy-layout"));
    }

    @Test
    public void postProceedDisabledForConfigDispatchOnly() {
        assertFalse(SystemServerMutationPolicy.shouldApplyPostProceedMutations("config-dispatch"));
        assertTrue(SystemServerMutationPolicy.shouldApplyPostProceedMutations("activity-start"));
        assertTrue(SystemServerMutationPolicy.shouldApplyPostProceedMutations("display-content-config"));
    }

    @Test
    public void interceptEnterLoggingDisabledForHotEntries() {
        assertFalse(SystemServerHookLogGate.shouldLogInterceptEnter("display-policy-layout"));
        assertFalse(SystemServerHookLogGate.shouldLogInterceptEnter("relayout-dispatch"));
        assertTrue(SystemServerHookLogGate.shouldLogInterceptEnter("activity-start"));
        assertTrue(SystemServerHookLogGate.shouldLogInterceptEnter("config-dispatch"));
    }

    @Test
    public void hotEntryQuickGateRequiresConfiguredPackageHint() {
        Set<String> configured = new LinkedHashSet<>();
        configured.add("com.max.xiaoheihe");

        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInspectHotEntryForTest(
                        "display-policy-layout",
                        new FakeWindow("Window{u0 com.android.launcher/com.android.launcher.Launcher}"),
                        configured));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInspectHotEntryForTest(
                        "display-policy-layout",
                        new FakeWindow("Window{u0 com.max.xiaoheihe/com.max.xiaoheihe.MainActivity}"),
                        configured));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInspectHotEntryForTest(
                        "activity-start",
                        new FakeWindow("Window{u0 com.android.launcher/com.android.launcher.Launcher}"),
                        configured));
    }

    @Test
    public void hotEntryQuickGateSeesWebApkOwnerInsideChromeCarrierText() {
        Set<String> configured = new LinkedHashSet<>();
        configured.add("org.chromium.webapk.a5e359e2ce8b830bb_v2");

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInspectHotEntryForTest(
                        "display-policy-layout",
                        new FakeWindow("Window{u0 com.android.chrome/"
                                + "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity "
                                + WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA
                        + "=org.chromium.webapk.a5e359e2ce8b830bb_v2}"),
                        configured));
    }

    @Test
    public void relayoutQuickGateScansLaterWindowArguments() {
        Set<String> configured = new LinkedHashSet<>();
        configured.add("com.android.chrome");

        assertTrue(SystemServerHotPathInspector.shouldInspectHotEntry(
                "relayout-dispatch",
                "WindowManagerService",
                java.util.List.of(
                        "session",
                        "client",
                        "attrs",
                        "requestedWidth",
                        "requestedHeight",
                        "Window{7a03626 u0 com.android.chrome/com.google.android.apps.chrome.Main}"),
                configured));
    }

    @Test
    public void relayoutQuickGateAllowsResolvedWindowStateForPackageResolver() {
        Set<String> configured = new LinkedHashSet<>();
        configured.add("com.android.chrome");

        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInspectResolvedRelayoutTargetForTest(
                        new FakeWindow("WindowStateWithoutPackageText"),
                        configured));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInspectResolvedRelayoutTargetForTest(
                        new FakeWindow("WindowStateWithoutPackageText", "com.android.chrome"),
                        configured));
    }

    @Test
    public void relayoutMutationAllowsOnlyDirectTargetApplicationWindow() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "WindowStateWithoutPackageText",
                                "com.android.chrome",
                                1,
                                "com.android.chrome",
                                null),
                        "com.android.chrome"));
    }

    @Test
    public void relayoutMutationRejectsDifferentOwnerApplicationWindow() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "WindowStateWithoutPackageText",
                                "com.other.app",
                                1,
                                "com.other.app",
                                null),
                        "com.android.chrome"));
    }

    @Test
    public void relayoutMutationRejectsSystemWindowWithTargetActivityRecord() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "Window{u0 com.android.systemui/NotificationShade "
                                        + "for com.android.chrome}",
                                "com.android.systemui",
                                2040,
                                "com.android.chrome",
                                null),
                        "com.android.chrome"));
    }

    @Test
    public void relayoutMutationAllowsExplicitSystemUiTargetWindow() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "Window{u0 com.android.systemui/NotificationShade}",
                                "com.android.systemui",
                                2040,
                                "com.android.systemui",
                                null),
                        "com.android.systemui"));
    }

    @Test
    public void relayoutMutationAcceptsTokenOwnerPackage() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "WindowStateWithoutPackageText",
                                null,
                                1,
                                null,
                                "com.android.chrome"),
                        "com.android.chrome"));
    }

    @Test
    public void relayoutMutationRejectsTextOnlyPackageFallback() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "Window{u0 com.android.chrome/com.google.android.apps.chrome.Main}"),
                        "com.android.chrome"));
    }

    @Test
    public void safeModeInstallsCoreTargets() {
        assertFalse(SystemServerMutationPolicy.shouldInstallTarget("display-policy-layout", true));
        assertFalse(SystemServerMutationPolicy.shouldInstallTarget("display-content-config", true));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("relayout-dispatch", true));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("activity-start", true));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("config-dispatch", true));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("launch-activity-item", true));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("display-manager-info", true));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("hyperos-rust-process", true));
    }

    @Test
    public void fullModeInstallsAllTargets() {
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("display-policy-layout", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("relayout-dispatch", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("display-content-config", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("activity-start", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("config-dispatch", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("launch-activity-item", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("display-manager-info", false));
        assertTrue(SystemServerMutationPolicy.shouldInstallTarget("hyperos-rust-process", false));
    }

    @Test
    public void hyperOsBootstrapHooksAreGuardedBySafeModeInSource() throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");

        assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallTarget("));
        assertTrue(source.contains("launch-activity-item"));
        assertTrue(source.contains("HyperOsRustProcessHookInstaller.install(xposed, source)"));
        assertTrue(source.contains("hyperos-rust-process"));
        assertTrue(source.contains("PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback("));
    }

    @Test
    public void systemServerHookCatalogOwnsModernEntryDefinitions() throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        String catalog = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerHookCatalog.java");
        String spec = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerHookSpec.java");

        assertTrue(installer.contains("SystemServerHookCatalog.methodHookSpecs()"));
        assertTrue(installer.contains("SystemServerHookCatalog.LAUNCH_ACTIVITY_ITEM"));
        assertFalse(installer.contains("private static final HookTarget[] HOOK_TARGETS"));
        assertTrue(catalog.contains("static final SystemServerHookSpec LAUNCH_ACTIVITY_ITEM"));
        assertTrue(catalog.contains("static final SystemServerHookSpec CONFIG_DISPATCH"));
        assertTrue(catalog.contains("static final SystemServerHookSpec DISPLAY_MANAGER_INFO"));
        assertTrue(catalog.contains("\"system_server_launch_activity_item\""));
        assertTrue(catalog.contains("\"system_server_display_manager_info\""));
        assertTrue(spec.contains("String hookIdFor(Method method)"));
        assertTrue(spec.contains("String hookIdFor(Constructor<?> constructor)"));
    }

    @Test
    public void partialSystemServerInstallDoesNotCloseProcessGate() throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(installer.contains("markInstalledWhenComplete(missingCount);"));
        assertTrue(installer.contains("if (missingCount == 0)"));
        assertTrue(installer.contains("private static final Set<String> installedEntries"));
        assertTrue(installer.contains("isEntryInstalled(hookSpec.entryName)"));
        assertTrue(installer.contains("markEntryInstalled(hookSpec.entryName);"));
        assertTrue(installer.contains("boolean isComplete()"));
        assertTrue(moduleMain.contains("systemServerInstallAttempted = result.isComplete();"));
    }

    @Test
    public void launchActivityItemRestoresViewportConfigMutation()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        int methodIndex = source.indexOf("private static void applyLaunchActivityItemArgs");
        int nextMethodIndex = source.indexOf("private static void logViewportMarkerProbe", methodIndex);
        assertTrue(methodIndex > 0);
        assertTrue(nextMethodIndex > methodIndex);

        String method = source.substring(methodIndex, nextMethodIndex);
        assertTrue(method.contains("resolveMarkerGatedEnvironment("));
        assertTrue(method.contains("applyConfiguration(configuration, environment)"));
        assertTrue(method.contains("applyLaunchActivityItemConfigurationFields("));
        assertTrue(source.contains("applyLaunchActivityItemObject(source, chain.getThisObject())"));
        assertTrue(source.contains("system_server launch-activity-item post-init failed"));
    }

    @Test
    public void relativeScaleUsesMarkerGatedExplicitSystemServerViewportMutation()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        assertTrue(source.contains("private static boolean hasSystemServerViewportOverride"));
        assertTrue(source.contains("if (ViewportApplyMode.SYSTEM.equals(mode))"));
        assertTrue(source.contains("ViewportApplyMode.AUTO.equals(mode)"));
        assertTrue(source.contains("&& !config.targetViewportSpec.isRelativeScale()"));
        assertTrue(source.contains("boolean applyViewport = environment != null"));
        assertTrue(source.contains("hasSystemServerViewportOverride(config);"));
        assertTrue(source.contains("if (!hasSystemServerViewportOverride(config))"));
        assertTrue(source.contains("resolveMarkerGatedEnvironment("));
        assertTrue(source.contains("config.targetViewportSpec.isRelativeScale()"));
    }

    @Test
    public void relativeScaleMaintenanceRequiresMarkerOrDisplayBaseline() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        ViewportRuntimeMarkerBridge.MarkerRecord completeRecord = markerRecord(targetSpec);
        ViewportRuntimeMarkerBridge.MarkerRecord incompleteRecord =
                new ViewportRuntimeMarkerBridge.MarkerRecord(
                        "pkg",
                        targetSpec.fingerprint(),
                        "source",
                        540,
                        "result",
                        ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                        1L);

        assertTrue(SystemServerDisplayEnvironmentInstaller.hasCompleteMarkerResultForTest(
                ViewportRuntimeMarkerBridge.ParseResult.hit(completeRecord, 0L)));
        assertFalse(SystemServerDisplayEnvironmentInstaller.hasCompleteMarkerResultForTest(
                ViewportRuntimeMarkerBridge.ParseResult.hit(incompleteRecord, 0L)));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .canDeriveRelativeScaleFromSystemServerSourceForTest(
                        configuration(), ViewportRuntimeMarkerBridge.ParseResult.miss("empty")));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .canDeriveRelativeScaleFromSystemServerSourceForTest(
                        configuration(), ViewportRuntimeMarkerBridge.ParseResult.hit(incompleteRecord, 0L)));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .canDeriveRelativeScaleFromSystemServerSourceForTest(
                        configuration(), ViewportRuntimeMarkerBridge.ParseResult.miss("stale")));
        assertTrue(SystemServerDisplayEnvironmentInstaller.isStaleMarkerForTest(
                ViewportRuntimeMarkerBridge.ParseResult.miss("stale")));
    }

    @Test
    public void windowScopedMaintenanceMayReuseCompleteMarkerResultOnly() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        ViewportRuntimeMarkerBridge.ParseResult marker =
                ViewportRuntimeMarkerBridge.ParseResult.hit(markerRecord(targetSpec), 0L);

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .environmentMatchesMarkerResultForTest(
                        new PerAppDisplayEnvironment(540, 1188, 540, 320, 1080, 2376),
                        marker));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .environmentMatchesMarkerResultForTest(
                        new PerAppDisplayEnvironment(360, 640, 360, 480, 1080, 2376),
                        marker));
    }

    @Test
    public void completeMarkerResultRequiresMatchingOrientationForReuse() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        ViewportRuntimeMarkerBridge.ParseResult portraitMarker =
                ViewportRuntimeMarkerBridge.ParseResult.hit(markerRecord(targetSpec), 0L);
        android.content.res.Configuration portrait = configuration();
        android.content.res.Configuration landscape = new android.content.res.Configuration();
        landscape.screenWidthDp = 792;
        landscape.screenHeightDp = 360;
        landscape.smallestScreenWidthDp = 360;
        landscape.densityDpi = 480;

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .canReuseCompleteMarkerResultForTest(portrait, portraitMarker));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .canReuseCompleteMarkerResultForTest(landscape, portraitMarker));
    }

    private static ViewportRuntimeMarkerBridge.MarkerRecord markerRecord(ViewportTargetSpec targetSpec) {
        return new ViewportRuntimeMarkerBridge.MarkerRecord(
                "pkg",
                targetSpec.fingerprint(),
                ViewportRuntimeMarkerBridge.configurationSignature(
                        360, 792, 360, 480, ViewportSourceSnapshot.SCOPE_DISPLAY),
                540,
                ViewportRuntimeMarkerBridge.configurationSignature(
                        540, 1188, 540, 320, ViewportSourceSnapshot.SCOPE_DISPLAY),
                540,
                1188,
                540,
                320,
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                1L);
    }

    private static android.content.res.Configuration configuration() {
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 792;
        config.smallestScreenWidthDp = 360;
        config.densityDpi = 480;
        return config;
    }

    @Test
    public void systemServerMutationFieldsAreSelectedIndependently() {
        PerAppDisplayConfig viewportOnly = new PerAppDisplayConfig(
                "tv.danmaku.bili",
                ViewportTargetSpec.absoluteDp(600),
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic());
        PerAppDisplayConfig fontOnly = new PerAppDisplayConfig(
                "tv.danmaku.bili",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                150,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                HookDomainOverride.automatic());
        PerAppDisplayConfig neither = new PerAppDisplayConfig(
                "tv.danmaku.bili",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic());

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(viewportOnly));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(fontOnly));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(neither));
    }

    @Test
    public void systemServerConfigSelectionIsFieldAwarePerEntry() {
        PerAppDisplayConfig viewportOnly = new PerAppDisplayConfig(
                "com.example.viewport",
                ViewportTargetSpec.absoluteDp(600),
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic());
        PerAppDisplayConfig fontOnly = new PerAppDisplayConfig(
                "com.example.font",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                150,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                HookDomainOverride.automatic());
        PerAppDisplayConfig viewportAndFont = new PerAppDisplayConfig(
                "com.example.both",
                ViewportTargetSpec.absoluteDp(600),
                ViewportApplyMode.SYSTEM,
                150,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                HookDomainOverride.automatic());

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerEntryForTest(
                        "launch-activity-item", fontOnly));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerEntryForTest(
                        "config-dispatch", fontOnly));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerEntryForTest(
                        "display-manager-info", fontOnly));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerEntryForTest(
                        "config-dispatch", viewportOnly));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerEntryForTest(
                        "display-manager-info", viewportOnly));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerEntryForTest(
                        "config-dispatch", viewportAndFont));
    }

    @Test
    public void fieldPolicyKeepsViewportMultiEntryButNarrowsFontScaleToLaunch() {
        assertCurrentCoverageAllows(SystemServerMutationField.VIEWPORT);
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField(
                        "launch-activity-item", SystemServerMutationField.FONT_SCALE));
        assertFalse(SystemServerMutationPolicy.shouldApplyMutationField(
                        "config-dispatch", SystemServerMutationField.FONT_SCALE));
        assertFalse(SystemServerMutationPolicy.shouldApplyMutationField(
                        "activity-start", SystemServerMutationField.FONT_SCALE));
        assertFalse(SystemServerMutationPolicy.shouldApplyMutationField(
                        "display-content-config", SystemServerMutationField.FONT_SCALE));
        assertFalse(SystemServerMutationPolicy.shouldApplyMutationField(
                        "display-policy-layout", SystemServerMutationField.FONT_SCALE));
        assertFalse(SystemServerMutationPolicy.shouldApplyMutationField(
                        "relayout-dispatch", SystemServerMutationField.FONT_SCALE));
    }

    @Test
    public void systemServerMutationSchedulerTodoDocumentsFieldSemantics()
            throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        String policy = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerMutationPolicy.java");

        assertTrue(installer.contains("TODO(system-mutation-scheduler)"));
        assertTrue(installer.contains("SystemServerMutationField.VIEWPORT"));
        assertTrue(installer.contains("SystemServerMutationField.FONT_SCALE"));
        assertTrue(installer.contains("VIEWPORT uses a marker-gated"));
        assertTrue(installer.contains("FONT_SCALE is launch-only"));
        assertTrue(installer.contains("CONFIG_FONT_SCALE"));
        assertTrue(installer.contains("relaunch"));
        assertTrue(policy.contains("shouldApplyMutationField"));
    }

    @Test
    public void emitsWhenMessageChangesAndNoThrottle() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldEmitLogForTest("a", "b", 1000L, 900L, 0L));
    }

    @Test
    public void suppressesWhenMessageUnchanged() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldEmitLogForTest("same", "same", 1000L, 0L, 1200L));
    }

    @Test
    public void suppressesWhenWithinThrottleWindow() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldEmitLogForTest("old", "new", 1500L, 1000L, 1200L));
    }

    @Test
    public void emitsWhenThrottleWindowElapsed() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldEmitLogForTest("old", "new", 2300L, 1000L, 1200L));
    }

    @Test
    public void resolvesSamplingIntervalByEntryRisk() {
        assertEquals(1200L, SystemServerHookLogGate.resolveLogMinIntervalMs("display-policy-layout"));
        assertEquals(1200L, SystemServerHookLogGate.resolveLogMinIntervalMs("relayout-dispatch"));
        assertEquals(800L, SystemServerHookLogGate.resolveLogMinIntervalMs("activity-start"));
        assertEquals(800L, SystemServerHookLogGate.resolveLogMinIntervalMs("config-dispatch"));
        assertEquals(400L, SystemServerHookLogGate.resolveLogMinIntervalMs("unknown-entry"));
    }

    @Test
    public void debugSystemServerFontDisableIsPackageScoped() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .isSystemServerFontDisabledByDebugOverrideForTest(
                        "com.ss.android.ugc.aweme",
                        "com.ss.android.ugc.aweme"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .isSystemServerFontDisabledByDebugOverrideForTest(
                        "com.ss.android.ugc.aweme",
                        "*"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .isSystemServerFontDisabledByDebugOverrideForTest(
                        "com.ss.android.ugc.aweme",
                        "tv.danmaku.bili"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .isSystemServerFontDisabledByDebugOverrideForTest(
                        "com.ss.android.ugc.aweme",
                        ""));
    }

    @Test
    public void debugSystemServerFontDisableIsDebugOnlyAndLoggedInSource()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        String matcher = read("src/main/java/com/dpis/module/runtime/DebugPackageOverride.java");

        assertTrue(source.contains("debug.dpis.font.disable_system_server_package"));
        assertTrue(source.contains("isSystemServerFontDisabledByDebugOverride(config.packageName)"));
        assertTrue(source.contains("reason=debug-disable-system-server-font"));
        assertTrue(matcher.contains("if (!BuildConfig.DEBUG || packageName == null"));
    }

    @Test
    public void debugSystemServerFontFallbackYieldsOnlyForSystemEmulationRoute() {
        PerAppDisplayConfig systemModeConfig = new PerAppDisplayConfig(
                "com.ss.android.ugc.aweme",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                80,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                HookDomainOverride.automatic());
        PerAppDisplayConfig compatModeConfig = new PerAppDisplayConfig(
                "com.ss.android.ugc.aweme",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                80,
                FontApplyMode.FIELD_REWRITE,
                false,
                HookDomainOverride.automatic());

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldYieldSystemServerFontToAppProcessFallbackForTest(
                        systemModeConfig,
                        "com.ss.android.ugc.aweme"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldYieldSystemServerFontToAppProcessFallbackForTest(
                        compatModeConfig,
                        "com.ss.android.ugc.aweme"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldYieldSystemServerFontToAppProcessFallbackForTest(
                        systemModeConfig,
                        "tv.danmaku.bili"));
    }

    @Test
    public void debugSystemServerFontFallbackIsDebugOnlyAndLoggedInSource()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");
        String matcher = read("src/main/java/com/dpis/module/runtime/DebugPackageOverride.java");

        assertTrue(source.contains("debug.dpis.font.system_server_fallback_package"));
        assertTrue(source.contains("shouldYieldSystemServerFontToAppProcessFallback(config)"));
        assertTrue(source.contains("reason=debug-system-server-font-fallback-yield"));
        assertTrue(matcher.contains("if (!BuildConfig.DEBUG || packageName == null"));
    }

    @Test
    public void systemServerFontMutationIgnoresCompatCustomHookDomains() {
        PerAppDisplayConfig automaticConfig = new PerAppDisplayConfig(
                "tv.danmaku.bili",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                150,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                HookDomainOverride.automatic());
        PerAppDisplayConfig customWithoutSystemServerFont = new PerAppDisplayConfig(
                "tv.danmaku.bili",
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                150,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                new HookDomainOverride(
                        true,
                        Set.of(FontHookDomainRegistry.ID_RESOURCES_FONT,
                                FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT,
                                FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM),
                        Set.of()));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .hasSystemServerFontOverrideForTest(automaticConfig));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .hasSystemServerFontOverrideForTest(customWithoutSystemServerFont));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static void assertCurrentCoverageAllows(SystemServerMutationField field) {
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField("launch-activity-item", field));
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField("config-dispatch", field));
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField("activity-start", field));
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField("display-content-config", field));
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField("display-policy-layout", field));
        assertTrue(SystemServerMutationPolicy.shouldApplyMutationField("relayout-dispatch", field));
    }

    private static final class FakeWindow {
        private final String text;
        @SuppressWarnings("unused")
        private final FakeLayoutParams mAttrs;
        @SuppressWarnings("unused")
        private final FakePackageOwner mActivityRecord;
        @SuppressWarnings("unused")
        private final FakePackageOwner mToken;

        private FakeWindow(String text) {
            this(text, null, 0, null, null);
        }

        private FakeWindow(String text, String packageName) {
            this(text, packageName, 0, null, null);
        }

        private FakeWindow(String text, String packageName, int windowType,
                           String activityPackageName, String tokenPackageName) {
            this.text = text;
            this.mAttrs = packageName == null ? null : new FakeLayoutParams(packageName, windowType);
            this.mActivityRecord = activityPackageName == null
                    ? null
                    : new FakePackageOwner(activityPackageName);
            this.mToken = tokenPackageName == null ? null : new FakePackageOwner(tokenPackageName);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static final class FakeLayoutParams {
        @SuppressWarnings("unused")
        private final String packageName;
        @SuppressWarnings("unused")
        private final int type;

        private FakeLayoutParams(String packageName) {
            this(packageName, 0);
        }

        private FakeLayoutParams(String packageName, int type) {
            this.packageName = packageName;
            this.type = type;
        }
    }

    private static final class FakePackageOwner {
        @SuppressWarnings("unused")
        private final String packageName;

        private FakePackageOwner(String packageName) {
            this.packageName = packageName;
        }
    }
}
