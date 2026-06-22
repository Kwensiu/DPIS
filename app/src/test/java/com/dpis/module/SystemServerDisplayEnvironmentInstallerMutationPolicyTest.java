package com.dpis.module;

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
    public void safeModeInstallsCoreTargets() {
        assertFalse(SystemServerMutationPolicy.shouldInstallTarget("display-policy-layout", true));
        assertFalse(SystemServerMutationPolicy.shouldInstallTarget("relayout-dispatch", true));
        assertFalse(SystemServerMutationPolicy.shouldInstallTarget("display-content-config", true));
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
        String source = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");

        int launchHookIndex = source.indexOf("if (installLaunchActivityItemHook(xposed, source))");
        assertTrue(launchHookIndex > 0);
        String launchContext = source.substring(Math.max(0, launchHookIndex - 260), launchHookIndex);
        assertTrue(launchContext.contains("SystemServerMutationPolicy.shouldInstallTarget("));
        assertTrue(launchContext.contains("launch-activity-item"));

        int rustHookIndex = source.indexOf("if (HyperOsRustProcessHookInstaller.install(xposed, source))");
        assertTrue(rustHookIndex > 0);
        String rustContext = source.substring(Math.max(0, rustHookIndex - 260), rustHookIndex);
        assertTrue(rustContext.contains("SystemServerMutationPolicy.shouldInstallTarget("));
        assertTrue(rustContext.contains("hyperos-rust-process"));
    }

    @Test
    public void launchActivityItemRestoresViewportConfigMutation()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");
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
        String source = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");
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
        String installer = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");
        String policy = read("src/main/java/com/dpis/module/SystemServerMutationPolicy.java");

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
        String source = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");
        String matcher = read("src/main/java/com/dpis/module/DebugPackageOverride.java");

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
        String source = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");
        String matcher = read("src/main/java/com/dpis/module/DebugPackageOverride.java");

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

        private FakeWindow(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
