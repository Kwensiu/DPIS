package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class FontDebugStatsProviderSourceSmokeTest {
    @Test
    public void manifestDeclaresInternalFontDebugStatsFallbacks() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsProvider\""));
        assertTrue(manifest.contains("android:authorities=\"${applicationId}.fontdebugstats\""));
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsProvider");
        assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsIngestService\""));
        assertTrue(manifest.contains("android:name=\".fonts.FontDebugStatsIngestActivity\""));
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsIngestService");
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsIngestActivity");
        assertComponentExportedFalse(manifest, ".fonts.FontDebugStatsReceiver");
    }

    @Test
    public void fontStatsReportersUseProviderTransportInsteadOfDirectBroadcasts() throws IOException {
        String fontReporter = read("src/main/java/com/dpis/module/fonts/FontDebugStatsReporter.java");
        String viewportReporter = read("src/main/java/com/dpis/module/viewport/ViewportDebugReporter.java");

        assertTrue(fontReporter.contains("FontDebugStatsTransport.sendUpdate(context, extras)"));
        assertTrue(viewportReporter.contains("FontDebugStatsTransport.sendUpdate(context, extras)"));
        assertFalse(fontReporter.contains("context.sendBroadcast(intent)"));
        assertFalse(viewportReporter.contains("context.sendBroadcast(intent)"));
    }

    @Test
    public void receiverAndProviderShareSamePreferenceWriter() throws IOException {
        String receiver = read("src/main/java/com/dpis/module/fonts/FontDebugStatsReceiver.java");
        String provider = read("src/main/java/com/dpis/module/fonts/FontDebugStatsProvider.java");

        assertTrue(receiver.contains("FontDebugStatsUpdateWriter.applyExtras("));
        assertTrue(provider.contains("FontDebugStatsUpdateWriter.applyExtras("));
    }

    @Test
    public void transportPrefersXposedRemotePreferencesBeforeProviderFallback() throws IOException {
        String transport = read("src/main/java/com/dpis/module/fonts/FontDebugStatsTransport.java");
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(transport.contains("xposed.getRemotePreferences(DpisConfigStore.GROUP)"));
        assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".fonts.FontDebugStatsReceiver\""));
        assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".fonts.FontDebugStatsIngestService\""));
        assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".fonts.FontDebugStatsIngestActivity\""));
        assertTrue(transport.contains("FontDebugStatsUpdateWriter.applyExtras(preferences, extras)"));
        assertTrue(transport.contains("context.getContentResolver().call(buildUri()"));
        assertTrue(transport.contains("context.sendBroadcast(intent)"));
        assertTrue(transport.contains("context.startService(intent)"));
        assertTrue(transport.contains("context.startActivity(intent)"));
        assertTrue(transport.contains("FontDebugStatsFileBridge.write(context, extras)"));
        assertFalse(transport.contains("return;\n        } catch (Throwable throwable)"));
        assertTrue(moduleMain.contains("FontDebugStatsTransport.initialize(this)"));
    }

    @Test
    public void overlayImportsFileBridgeBeforeRendering() throws IOException {
        String overlay = read("src/main/java/com/dpis/module/fonts/FontDebugOverlayService.kt");

        assertTrue(overlay.contains("HandlerThread"));
        assertTrue(overlay.contains("scheduleBridgeImportIfNeeded()"));
        assertTrue(overlay.contains("FontDebugStatsFileBridge.importIfNewer(this)"));
        assertTrue(overlay.contains("FontDebugLogcatBridge.importRecent(this)"));
    }

    @Test
    public void settingsExposeSafeCacheCleanup() throws IOException {
        String layout = read("src/main/res/layout/view_system_server_settings_content.xml");
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.kt");

        assertTrue(layout.contains("android:id=\"@+id/row_clear_cache\""));
        assertTrue(layout.indexOf("android:id=\"@+id/row_language\"")
                < layout.indexOf("android:id=\"@+id/row_clear_cache\""));
        assertTrue(layout.indexOf("android:id=\"@+id/row_clear_cache\"")
                < layout.indexOf("android:id=\"@+id/row_hide_launcher_icon\""));
        assertTrue(source.contains("SafeCacheCleaner.formatCacheUsage("));
        assertTrue(source.contains("SafeCacheCleaner.clearAll("));
    }

    @Test
    public void manifestDoesNotRequestReadLogsForLogcatFallback() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");

        assertFalse(manifest.contains("android.permission.READ_LOGS"));
    }

    private static void assertComponentExportedFalse(String manifest, String componentName) {
        int nameIndex = manifest.indexOf("android:name=\"" + componentName + "\"");
        assertTrue("missing component " + componentName, nameIndex >= 0);
        int exportedIndex = manifest.indexOf("android:exported=\"false\"", nameIndex);
        assertTrue("component should be non-exported " + componentName,
                exportedIndex >= 0 && exportedIndex - nameIndex < 200);
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
