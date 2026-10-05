package com.dpis.module

import org.junit.Assert
import org.junit.Test
import java.io.IOException

class FontDebugStatsProviderSourceSmokeTest {
    @Test
    @Throws(IOException::class)
    fun manifestDeclaresInternalFontDebugStatsFallbacks() {
        val manifest: String = read("src/main/AndroidManifest.xml")

        Assert.assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsProvider\""))
        Assert.assertTrue(manifest.contains("android:authorities=\"\${applicationId}.fontdebugstats\""))
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsProvider")
        Assert.assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsIngestService\""))
        Assert.assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsIngestActivity\""))
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsIngestService")
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsIngestActivity")
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsReceiver")
    }

    @Test
    @Throws(IOException::class)
    fun fontStatsReportersUseProviderTransportInsteadOfDirectBroadcasts() {
        val fontReporter: String =
            read("src/main/java/com/dpis/module/fonts/FontDebugStatsReporter.java")
        val viewportReporter: String =
            read("src/main/java/com/dpis/module/viewport/runtime/ViewportDebugReporter.kt")

        Assert.assertTrue(fontReporter.contains("FontDebugStatsTransport.sendUpdate(context, extras)"))
        Assert.assertTrue(viewportReporter.contains("FontDebugStatsTransport.sendUpdate(context, extras)"))
        Assert.assertFalse(fontReporter.contains("context.sendBroadcast(intent)"))
        Assert.assertFalse(viewportReporter.contains("context.sendBroadcast(intent)"))
    }

    @Test
    @Throws(IOException::class)
    fun receiverAndProviderShareSamePreferenceWriter() {
        val receiver: String =
            read("src/main/java/com/dpis/module/fonts/FontDebugStatsReceiver.java")
        val provider: String =
            read("src/main/java/com/dpis/module/fonts/FontDebugStatsProvider.java")

        Assert.assertTrue(receiver.contains("FontDebugStatsUpdateWriter.applyExtras("))
        Assert.assertTrue(provider.contains("FontDebugStatsUpdateWriter.applyExtras("))
    }

    @Test
    @Throws(IOException::class)
    fun transportPrefersXposedRemotePreferencesBeforeProviderFallback() {
        val transport: String =
            read("src/main/java/com/dpis/module/fonts/FontDebugStatsTransport.java")
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(transport.contains("xposed.getRemotePreferences(DpisConfigStore.GROUP)"))
        Assert.assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".fonts.FontDebugStatsReceiver\""))
        Assert.assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".fonts.FontDebugStatsIngestService\""))
        Assert.assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".fonts.FontDebugStatsIngestActivity\""))
        Assert.assertTrue(transport.contains("FontDebugStatsUpdateWriter.applyExtras(preferences, extras)"))
        Assert.assertTrue(transport.contains("context.getContentResolver().call(buildUri()"))
        Assert.assertTrue(transport.contains("context.sendBroadcast(intent)"))
        Assert.assertTrue(transport.contains("context.startService(intent)"))
        Assert.assertTrue(transport.contains("context.startActivity(intent)"))
        Assert.assertTrue(transport.contains("FontDebugStatsFileBridge.write(context, extras)"))
        Assert.assertFalse(transport.contains("return;\n        } catch (Throwable throwable)"))
        Assert.assertTrue(moduleMain.contains("FontDebugStatsTransport.initialize(this)"))
    }

    @Test
    @Throws(IOException::class)
    fun overlayImportsFileBridgeBeforeRendering() {
        val overlay: String = read("src/main/java/com/dpis/module/fonts/FontDebugOverlayService.kt")

        Assert.assertTrue(overlay.contains("HandlerThread"))
        Assert.assertTrue(overlay.contains("scheduleBridgeImportIfNeeded()"))
        Assert.assertTrue(overlay.contains("FontDebugStatsFileBridge.importIfNewer(this)"))
        Assert.assertTrue(overlay.contains("FontDebugLogcatBridge.importRecent(this)"))
    }

    @Test
    @Throws(IOException::class)
    fun settingsExposeSafeCacheCleanup() {
        val content: String =
            read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceContent.kt")
        val source: String =
            read("src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt")

        Assert.assertTrue(content.contains("R.string.settings_clear_cache_label"))
        Assert.assertTrue(
            content.indexOf("R.string.settings_hide_launcher_icon_label")
                    < content.indexOf("R.string.settings_language_label")
        )
        Assert.assertTrue(
            content.indexOf("R.string.settings_language_label")
                    < content.indexOf("R.string.settings_clear_cache_label")
        )
        Assert.assertTrue(source.contains("SafeCacheCleaner.formatCacheUsage("))
        Assert.assertTrue(source.contains("SafeCacheCleaner.clearAll("))
    }

    @Test
    @Throws(IOException::class)
    fun manifestDoesNotRequestReadLogsForLogcatFallback() {
        val manifest: String = read("src/main/AndroidManifest.xml")

        Assert.assertFalse(manifest.contains("android.permission.READ_LOGS"))
    }

    companion object {
        private fun assertComponentExportedFalse(manifest: String, componentName: String?) {
            val nameIndex = manifest.indexOf("android:name=\"" + componentName + "\"")
            Assert.assertTrue("missing component " + componentName, nameIndex >= 0)
            val exportedIndex = manifest.indexOf("android:exported=\"false\"", nameIndex)
            Assert.assertTrue(
                "component should be non-exported " + componentName,
                exportedIndex >= 0 && exportedIndex - nameIndex < 200
            )
        }

        @Throws(IOException::class)
        private fun read(relativePath: String?): String {
            return SourceSmokeTestPaths.read(relativePath)
        }
    }
}
