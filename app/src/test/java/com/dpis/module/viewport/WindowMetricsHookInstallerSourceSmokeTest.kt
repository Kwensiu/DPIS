package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class WindowMetricsHookInstallerSourceSmokeTest {
    @Test
    fun runtimeHotpathEvidenceKeepsPackageRouteAndStages() {
        val source = String(
            Files.readAllBytes(
                Path.of("src/main/java/com/dpis/module/runtime/appprocess/WindowMetricsHookInstaller.kt"),
            ),
            StandardCharsets.UTF_8,
        )

        assertTrue(source.contains("fun install(xposed: XposedInterface, packageName: String?)"))
        assertTrue(source.contains("\"window_metrics_bounds_override\""))
        assertTrue(source.contains("RuntimeHotPathEvidenceSampler"))
        assertTrue(source.contains("RuntimeDiagnosticLogFingerprint.field()"))
        assertTrue(source.contains("resetHotPathSamplerForTest"))
        assertTrue(source.contains("RuntimeHotPathEvents.probe"))
        assertTrue(source.contains("ViewportConsistencyDiagnostics.observeWindowBounds"))
        assertTrue(source.contains("WindowBoundsState.recordAndShouldInvalidateDisplay"))
        assertTrue(source.contains("chain.thisObject"))
        assertTrue(source.contains("RuntimeHotPathEvents.skipped"))
        assertTrue(source.contains("RuntimeHotPathEvents.applied"))
        assertTrue(source.contains("reason=window_frame_override_disabled"))
    }
}
