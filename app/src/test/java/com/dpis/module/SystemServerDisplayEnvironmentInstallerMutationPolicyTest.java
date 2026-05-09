package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class SystemServerDisplayEnvironmentInstallerMutationPolicyTest {
    @Test
    public void preProceedEnabledForConfigDispatchAndActivityStartOnly() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPreProceedMutationsForTest("config-dispatch"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPreProceedMutationsForTest("activity-start"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPreProceedMutationsForTest("display-policy-layout"));
    }

    @Test
    public void postProceedDisabledForConfigDispatchOnly() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPostProceedMutationsForTest("config-dispatch"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPostProceedMutationsForTest("activity-start"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPostProceedMutationsForTest("display-content-config"));
    }

    @Test
    public void interceptEnterLoggingDisabledForHotEntries() {
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldLogInterceptEnterForTest("display-policy-layout"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldLogInterceptEnterForTest("relayout-dispatch"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldLogInterceptEnterForTest("activity-start"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldLogInterceptEnterForTest("config-dispatch"));
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
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("display-policy-layout", true));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("relayout-dispatch", true));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("display-content-config", true));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("activity-start", true));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("config-dispatch", true));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("launch-activity-item", true));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("hyperos-rust-process", true));
    }

    @Test
    public void fullModeInstallsAllTargets() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("display-policy-layout", false));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("relayout-dispatch", false));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("display-content-config", false));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("activity-start", false));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("config-dispatch", false));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("launch-activity-item", false));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldInstallTargetForTest("hyperos-rust-process", false));
    }

    @Test
    public void hyperOsBootstrapHooksAreGuardedBySafeModeInSource() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");

        int launchHookIndex = source.indexOf("if (installLaunchActivityItemHook(xposed, source))");
        assertTrue(launchHookIndex > 0);
        String launchContext = source.substring(Math.max(0, launchHookIndex - 260), launchHookIndex);
        assertTrue(launchContext.contains("shouldInstallTarget("));
        assertTrue(launchContext.contains("launch-activity-item"));

        int rustHookIndex = source.indexOf("if (HyperOsRustProcessHookInstaller.install(xposed, source))");
        assertTrue(rustHookIndex > 0);
        String rustContext = source.substring(Math.max(0, rustHookIndex - 260), rustHookIndex);
        assertTrue(rustContext.contains("shouldInstallTarget("));
        assertTrue(rustContext.contains("hyperos-rust-process"));
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

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
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
