package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FontDebugStatsProviderSourceSmokeTest {
    @Test
    @Throws(IOException::class)
    fun crossProcessStatsUseOneExportedProviderAndValidateTheCaller() {
        val manifest = read("src/main/AndroidManifest.xml")
        val provider = read("src/main/java/com/dpis/module/fonts/diagnostics/FontDebugStatsProvider.kt")
        val transport = read("src/main/java/com/dpis/module/fonts/diagnostics/FontDebugStatsTransport.kt")

        assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsProvider\""))
        assertComponentExportedTrue(manifest, ".fonts.FontDebugStatsProvider")
        assertTrue(provider.contains("Binder.getCallingUid()"))
        assertTrue(provider.contains("FontDebugStatsCallerPolicy.isAuthorized("))
        assertTrue(provider.contains("getConfiguredPackages()"))
        assertTrue(provider.contains("FontDebugStatsUpdateWriter.applyExtras(appContext, extras)"))
        assertTrue(transport.contains("contentResolver.call("))
        assertFalse(transport.contains("sendBroadcast("))
        assertFalse(transport.contains("startService("))
        assertFalse(transport.contains("startActivity("))
    }

    @Test
    @Throws(IOException::class)
    fun fontAndViewportReportersShareTheStatsTransport() {
        val fontReporter =
            read("src/main/java/com/dpis/module/fonts/diagnostics/FontDebugStatsReporter.kt")
        val viewportReporter = read("src/main/java/com/dpis/module/viewport/runtime/ViewportDebugReporter.kt")

        assertTrue(fontReporter.contains("FontDebugStatsTransport.sendUpdate"))
        assertTrue(viewportReporter.contains("FontDebugStatsTransport.sendUpdate"))
    }

    @Test
    @Throws(IOException::class)
    fun overlayCanStillImportOlderDiagnosticFiles() {
        val overlay = read("src/main/java/com/dpis/module/fonts/diagnostics/FontDebugOverlayService.kt")

        assertTrue(overlay.contains("FontDebugStatsFileBridge.importIfNewer(this)"))
        assertTrue(overlay.contains("FontDebugLogcatBridge.importRecent(this)"))
    }

    @Test
    @Throws(IOException::class)
    fun manifestDoesNotRequestReadLogsForTheLegacyFallback() {
        val manifest = read("src/main/AndroidManifest.xml")

        assertFalse(manifest.contains("android.permission.READ_LOGS"))
    }

    private fun assertComponentExportedTrue(manifest: String, componentName: String) {
        val nameIndex = manifest.indexOf("android:name=\"$componentName\"")
        assertTrue("missing component $componentName", nameIndex >= 0)
        val exportedIndex = manifest.indexOf("android:exported=\"true\"", nameIndex)
        assertTrue(
            "component should be exported $componentName",
            exportedIndex >= 0 && exportedIndex - nameIndex < 200,
        )
    }

    @Throws(IOException::class)
    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
