package com.dpis.module.runtime.systemserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dpis.module.SourceSmokeTestPaths;
import com.dpis.module.config.PerAppDisplayConfig;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.runtime.appprocess.WebApkCarrierResolver;
import com.dpis.module.viewport.PerAppDisplayEnvironment;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge;
import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class MutationPolicyTest {
    private static void assertCurrentCoverageAllows(MutationField field) {
        assertTrue(MutationPolicy.shouldApplyMutationField("launch-activity-item", field));
        assertTrue(MutationPolicy.shouldApplyMutationField("config-dispatch", field));
        assertTrue(MutationPolicy.shouldApplyMutationField("activity-start", field));
        assertTrue(MutationPolicy.shouldApplyMutationField("display-content-config", field));
        assertTrue(MutationPolicy.shouldApplyMutationField("display-policy-layout", field));
        assertTrue(MutationPolicy.shouldApplyMutationField("relayout-dispatch", field));
    }

    @Test
    public void preProceedEnabledForConfigDispatchAndActivityStartOnly() {
        assertTrue(MutationPolicy.shouldApplyPreProceedMutations("config-dispatch"));
        assertTrue(MutationPolicy.shouldApplyPreProceedMutations("activity-start"));
        assertFalse(MutationPolicy.shouldApplyPreProceedMutations("display-policy-layout"));
    }

    @Test
    public void postProceedDisabledForConfigDispatchOnly() {
        assertFalse(MutationPolicy.shouldApplyPostProceedMutations("config-dispatch"));
        assertTrue(MutationPolicy.shouldApplyPostProceedMutations("activity-start"));
        assertTrue(MutationPolicy.shouldApplyPostProceedMutations("display-content-config"));
    }

    @Test
    public void interceptEnterLoggingDisabledForHotEntries() {
        assertFalse(HookLogGate.shouldLogInterceptEnter("display-policy-layout"));
        assertFalse(HookLogGate.shouldLogInterceptEnter("relayout-dispatch"));
        assertTrue(HookLogGate.shouldLogInterceptEnter("activity-start"));
        assertTrue(HookLogGate.shouldLogInterceptEnter("config-dispatch"));
    }

    @Test
    public void hotEntryQuickGateRequiresConfiguredPackageHint() {
        Set<String> configured = new LinkedHashSet<>();
        configured.add("com.max.xiaoheihe");

        assertFalse(RelayoutWindow
                .shouldInspectHotEntryForTest(
                        "display-policy-layout",
                        new FakeWindow("Window{u0 com.android.launcher/com.android.launcher.Launcher}"),
                        configured));
        assertTrue(RelayoutWindow
                .shouldInspectHotEntryForTest(
                        "display-policy-layout",
                        new FakeWindow("Window{u0 com.max.xiaoheihe/com.max.xiaoheihe.MainActivity}"),
                        configured));
        assertTrue(RelayoutWindow
                .shouldInspectHotEntryForTest(
                        "activity-start",
                        new FakeWindow("Window{u0 com.android.launcher/com.android.launcher.Launcher}"),
                        configured));
    }

    @Test
    public void hotEntryQuickGateSeesWebApkOwnerInsideChromeCarrierText() {
        Set<String> configured = new LinkedHashSet<>();
        configured.add("org.chromium.webapk.a5e359e2ce8b830bb_v2");

        assertTrue(RelayoutWindow
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

        assertTrue(HotPathInspector.shouldInspectHotEntry(
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

        assertFalse(RelayoutWindow
                .shouldInspectResolvedRelayoutTargetForTest(
                        new FakeWindow("WindowStateWithoutPackageText"),
                        configured));
        assertTrue(RelayoutWindow
                .shouldInspectResolvedRelayoutTargetForTest(
                        new FakeWindow("WindowStateWithoutPackageText", "com.android.chrome"),
                        configured));
    }

    @Test
    public void relayoutMutationAllowsOnlyDirectTargetApplicationWindow() {
        assertTrue(RelayoutWindow
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
        assertFalse(RelayoutWindow
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
        assertFalse(RelayoutWindow
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
        assertTrue(RelayoutWindow
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
        assertTrue(RelayoutWindow
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
        assertFalse(RelayoutWindow
                .canApplyRelayoutMutationForTest(
                        new FakeWindow(
                                "Window{u0 com.android.chrome/com.google.android.apps.chrome.Main}"),
                        "com.android.chrome"));
    }

    @Test
    public void safeModeInstallsCoreTargets() {
        assertFalse(MutationPolicy.shouldInstallTarget("display-policy-layout", true));
        assertFalse(MutationPolicy.shouldInstallTarget("display-content-config", true));
        assertTrue(MutationPolicy.shouldInstallTarget("relayout-dispatch", true));
        assertTrue(MutationPolicy.shouldInstallTarget("activity-start", true));
        assertTrue(MutationPolicy.shouldInstallTarget("config-dispatch", true));
        assertTrue(MutationPolicy.shouldInstallTarget("launch-activity-item", true));
        assertTrue(MutationPolicy.shouldInstallTarget("display-manager-info", true));
        assertTrue(MutationPolicy.shouldInstallTarget("hyperos-rust-process", true));
    }

    @Test
    public void fullModeInstallsAllTargets() {
        assertTrue(MutationPolicy.shouldInstallTarget("display-policy-layout", false));
        assertTrue(MutationPolicy.shouldInstallTarget("relayout-dispatch", false));
        assertTrue(MutationPolicy.shouldInstallTarget("display-content-config", false));
        assertTrue(MutationPolicy.shouldInstallTarget("activity-start", false));
        assertTrue(MutationPolicy.shouldInstallTarget("config-dispatch", false));
        assertTrue(MutationPolicy.shouldInstallTarget("launch-activity-item", false));
        assertTrue(MutationPolicy.shouldInstallTarget("display-manager-info", false));
        assertTrue(MutationPolicy.shouldInstallTarget("hyperos-rust-process", false));
    }

    @Test
    public void hyperOsBootstrapHooksAreGuardedBySafeModeInSource() throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/HookInstaller.java");

        assertTrue(source.contains("MutationPolicy.shouldInstallTarget("));
        assertTrue(source.contains("launch-activity-item"));
        assertTrue(source.contains("HyperOsRustProcessHookInstaller.install(xposed, source)"));
        assertTrue(source.contains("hyperos-rust-process"));
        assertTrue(source.contains("PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback("));
    }

    @Test
    public void systemServerHookCatalogOwnsModernEntryDefinitions() throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/HookInstaller.java");
        String catalog = read("src/main/java/com/dpis/module/runtime/systemserver/HookCatalog.kt");
        String spec = read("src/main/java/com/dpis/module/runtime/systemserver/HookSpec.kt");

        assertTrue(installer.contains("HookCatalog.methodHookSpecs()"));
        assertTrue(installer.contains("HookCatalog.LAUNCH_ACTIVITY_ITEM"));
        assertFalse(installer.contains("private static final HookTarget[] HOOK_TARGETS"));
        assertTrue(catalog.contains("val LAUNCH_ACTIVITY_ITEM"));
        assertTrue(catalog.contains("val CONFIG_DISPATCH"));
        assertTrue(catalog.contains("val DISPLAY_MANAGER_INFO"));
        assertTrue(catalog.contains("\"system_server_launch_activity_item\""));
        assertTrue(catalog.contains("\"system_server_display_manager_info\""));
        assertTrue(spec.contains("fun hookIdFor(method: Method)"));
        assertTrue(spec.contains("fun hookIdFor(constructor: Constructor<*>)"));
    }

    @Test
    public void partialSystemServerInstallDoesNotCloseProcessGate() throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/HookInstaller.java");
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
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/LaunchActivityItemApply.kt");
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/HookInstaller.java");
        int methodIndex = source.indexOf("fun applyArgs");
        int nextMethodIndex = source.indexOf("fun applyObject", methodIndex);
        assertTrue(methodIndex > 0);
        assertTrue(nextMethodIndex > methodIndex);

        String method = source.substring(methodIndex, nextMethodIndex);
        assertTrue(method.contains("resolveLaunchViewportEnvironment("));
        assertTrue(method.contains("applyConfiguration(configuration, environment)"));
        assertTrue(method.contains("applyLaunchActivityItemConfigurationFields("));
        assertTrue(installer.contains("LaunchActivityItemApply.handleConstructor(source, chain)"));
        assertTrue(source.contains("applyObject(source, chain.thisObject)"));
        assertTrue(source.contains("system_server launch-activity-item post-init failed"));
    }

    @Test
    public void relativeScaleUsesMarkerGatedExplicitSystemServerViewportMutation()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/ConfigSelection.kt");
        String marker = read("src/main/java/com/dpis/module/runtime/systemserver/RelativeViewport.kt");
        String write = read("src/main/java/com/dpis/module/runtime/systemserver/EnvironmentWrite.kt");
        String callback = read("src/main/java/com/dpis/module/runtime/systemserver/HookCallback.kt");
        assertTrue(source.contains("fun hasSystemServerViewportOverride"));
        assertTrue(source.contains("if (ViewportApplyMode.SYSTEM == mode)"));
        assertTrue(source.contains("ViewportApplyMode.AUTO == mode"));
        assertTrue(source.contains("return ViewportApplyMode.AUTO == mode"));
        assertTrue(write.contains("val applyViewport = environment != null"));
        assertTrue(write.contains("hasSystemServerViewportOverride(config)"));
        assertTrue(callback.contains("hasSystemServerViewportOverride(config)"));
        assertTrue(marker.contains("fun resolveMarkerGatedEnvironment("));
        assertTrue(callback.contains("config.targetViewportSpec.isRelativeScale()"));
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

        assertTrue(RelativeViewport.hasCompleteMarkerResult(
                ViewportRuntimeMarkerBridge.ParseResult.hit(completeRecord, 0L)));
        assertFalse(RelativeViewport.hasCompleteMarkerResult(
                ViewportRuntimeMarkerBridge.ParseResult.hit(incompleteRecord, 0L)));
        assertTrue(RelativeViewport
                .canDeriveFromSystemServerSource(
                        configuration(), ViewportRuntimeMarkerBridge.ParseResult.miss("empty")));
        assertFalse(RelativeViewport
                .canDeriveFromSystemServerSource(
                        configuration(), ViewportRuntimeMarkerBridge.ParseResult.hit(incompleteRecord, 0L)));
        assertFalse(RelativeViewport
                .canDeriveFromSystemServerSource(
                        configuration(), ViewportRuntimeMarkerBridge.ParseResult.miss("stale")));
        assertTrue(RelativeViewport.isStaleMarker(
                ViewportRuntimeMarkerBridge.ParseResult.miss("stale")));
    }

    @Test
    public void windowScopedMaintenanceMayReuseCompleteMarkerResultOnly() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        ViewportRuntimeMarkerBridge.ParseResult marker =
                ViewportRuntimeMarkerBridge.ParseResult.hit(markerRecord(targetSpec), 0L);

        assertTrue(RelativeViewport
                .environmentMatchesMarkerResult(
                        new PerAppDisplayEnvironment(540, 1188, 540, 320, 1080, 2376),
                        marker));
        assertFalse(RelativeViewport
                .environmentMatchesMarkerResult(
                        new PerAppDisplayEnvironment(360, 640, 360, 480, 1080, 2376),
                        marker));
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

        assertTrue(RelativeViewport
                .canReuseCompleteMarkerResult(portrait, portraitMarker));
        assertFalse(RelativeViewport
                .canReuseCompleteMarkerResult(landscape, portraitMarker));
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

        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, viewportOnly));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, fontOnly));
        assertNull(ConfigSelection.selectConfigForSystemServerEntry(null, neither));
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

        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry("launch-activity-item", fontOnly));
        assertNull(ConfigSelection.selectConfigForSystemServerEntry("config-dispatch", fontOnly));
        assertNull(ConfigSelection.selectConfigForSystemServerEntry("display-manager-info", fontOnly));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry("config-dispatch", viewportOnly));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry("display-manager-info", viewportOnly));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry("config-dispatch", viewportAndFont));
    }

    @Test
    public void fieldPolicyKeepsViewportMultiEntryButNarrowsFontScaleToLaunch() {
        assertCurrentCoverageAllows(MutationField.VIEWPORT);
        assertTrue(MutationPolicy.shouldApplyMutationField(
                "launch-activity-item", MutationField.FONT_SCALE));
        assertFalse(MutationPolicy.shouldApplyMutationField(
                "config-dispatch", MutationField.FONT_SCALE));
        assertFalse(MutationPolicy.shouldApplyMutationField(
                "activity-start", MutationField.FONT_SCALE));
        assertFalse(MutationPolicy.shouldApplyMutationField(
                "display-content-config", MutationField.FONT_SCALE));
        assertFalse(MutationPolicy.shouldApplyMutationField(
                "display-policy-layout", MutationField.FONT_SCALE));
        assertFalse(MutationPolicy.shouldApplyMutationField(
                "relayout-dispatch", MutationField.FONT_SCALE));
    }

    @Test
    public void applyEnvironmentDefersFieldBaselineToMutationPolicy()
            throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/EnvironmentWrite.kt");
        String policy = read("src/main/java/com/dpis/module/runtime/systemserver/MutationPolicy.kt");

        assertTrue(installer.contains("MutationPolicy.shouldApplyMutationField"));
        assertTrue(installer.contains("MutationField.VIEWPORT"));
        assertTrue(installer.contains("MutationField.FONT_SCALE"));
        assertTrue(installer.contains("VIEWPORT uses a marker-gated"));
        assertTrue(installer.contains("FONT_SCALE is launch-only"));
        assertTrue(installer.contains("CONFIG_FONT_SCALE"));
        assertTrue(installer.contains("relaunch"));
        assertTrue(policy.contains("shouldApplyMutationField"));
    }

    @Test
    public void emitsWhenMessageChangesAndNoThrottle() {
        assertTrue(HookLog
                .shouldEmitLog("a", "b", 1000L, 900L, 0L));
    }

    @Test
    public void suppressesWhenMessageUnchanged() {
        assertFalse(HookLog
                .shouldEmitLog("same", "same", 1000L, 0L, 1200L));
    }

    @Test
    public void suppressesWhenWithinThrottleWindow() {
        assertFalse(HookLog
                .shouldEmitLog("old", "new", 1500L, 1000L, 1200L));
    }

    @Test
    public void emitsWhenThrottleWindowElapsed() {
        assertTrue(HookLog
                .shouldEmitLog("old", "new", 2300L, 1000L, 1200L));
    }

    @Test
    public void resolvesSamplingIntervalByEntryRisk() {
        assertEquals(1200L, HookLogGate.resolveLogMinIntervalMs("display-policy-layout"));
        assertEquals(1200L, HookLogGate.resolveLogMinIntervalMs("relayout-dispatch"));
        assertEquals(800L, HookLogGate.resolveLogMinIntervalMs("activity-start"));
        assertEquals(800L, HookLogGate.resolveLogMinIntervalMs("config-dispatch"));
        assertEquals(400L, HookLogGate.resolveLogMinIntervalMs("unknown-entry"));
    }

    @Test
    public void debugSystemServerFontDisableIsPackageScoped() {
        assertTrue(FontScale
                .isDisabledForTest(
                        "com.ss.android.ugc.aweme",
                        "com.ss.android.ugc.aweme"));
        assertTrue(FontScale
                .isDisabledForTest(
                        "com.ss.android.ugc.aweme",
                        "*"));
        assertFalse(FontScale
                .isDisabledForTest(
                        "com.ss.android.ugc.aweme",
                        "tv.danmaku.bili"));
        assertFalse(FontScale
                .isDisabledForTest(
                        "com.ss.android.ugc.aweme",
                        ""));
    }

    @Test
    public void debugSystemServerFontDisableIsDebugOnlyAndLoggedInSource()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/FontScale.kt");
        String matcher = read("src/main/java/com/dpis/module/runtime/probe/DebugPackageOverride.java");

        assertTrue(source.contains("debug.dpis.font.disable_system_server_package"));
        assertTrue(source.contains("isDisabled(config.packageName)"));
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

        assertTrue(FontScale
                .shouldYieldForTest(
                        systemModeConfig,
                        "com.ss.android.ugc.aweme"));
        assertFalse(FontScale
                .shouldYieldForTest(
                        compatModeConfig,
                        "com.ss.android.ugc.aweme"));
        assertFalse(FontScale
                .shouldYieldForTest(
                        systemModeConfig,
                        "tv.danmaku.bili"));
    }

    @Test
    public void debugSystemServerFontFallbackIsDebugOnlyAndLoggedInSource()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/runtime/systemserver/FontScale.kt");
        String matcher = read("src/main/java/com/dpis/module/runtime/probe/DebugPackageOverride.java");

        assertTrue(source.contains("debug.dpis.font.system_server_fallback_package"));
        assertTrue(source.contains("shouldYield(config)"));
        assertTrue(source.contains("reason=debug-system-server-font-fallback-yield"));
        assertTrue(matcher.contains("if (!BuildConfig.DEBUG || packageName == null"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
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
        assertTrue(ConfigSelection
                .hasSystemServerFontOverride(automaticConfig));
        assertTrue(ConfigSelection
                .hasSystemServerFontOverride(customWithoutSystemServerFont));
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

    private record FakeLayoutParams(@SuppressWarnings("unused") String packageName,
                                    @SuppressWarnings("unused") int type) {
            private FakeLayoutParams(String packageName) {
                this(packageName, 0);
            }

            private FakeLayoutParams(String packageName, int type) {
                this.packageName = packageName;
                this.type = type;
            }
        }

    private record FakePackageOwner(@SuppressWarnings("unused") String packageName) {
            private FakePackageOwner(String packageName) {
                this.packageName = packageName;
            }
        }
}
