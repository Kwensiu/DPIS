package com.dpis.module;

import com.dpis.module.diagnostics.RuntimeHotPathEvents;

import com.dpis.module.runtime.appprocess.WindowMetricsHookInstaller;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public final class WindowMetricsHookInstallerSourceSmokeTest {
    @Test
    public void runtimeHotpathEvidenceKeepsPackageRouteAndStages() throws Exception {
        String source = new String(
                Files.readAllBytes(Path.of("src/main/java/com/dpis/module/runtime/appprocess/WindowMetricsHookInstaller.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("static void install(XposedInterface xposed, String packageName)"));
        assertTrue(source.contains("\"window_metrics_bounds_override\""));
        assertTrue(source.contains("RuntimeHotPathEvidenceSampler"));
        assertTrue(source.contains("RuntimeDiagnosticLogFingerprint.field()"));
        assertTrue(source.contains("resetHotPathSamplerForTest"));
        assertTrue(source.contains("RuntimeHotPathEvents.probe"));
        assertTrue(source.contains("RuntimeHotPathEvents.skipped"));
        assertTrue(source.contains("RuntimeHotPathEvents.applied"));
        assertTrue(source.contains("reason=window_frame_override_disabled"));
    }
}
