package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class FontDebugStatsProviderSourceSmokeTest {
    @Test
    public void manifestDeclaresInternalFontDebugStatsFallbacks() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".FontDebugStatsProvider\""));
        assertTrue(manifest.contains("android:authorities=\"${applicationId}.fontdebugstats\""));
        assertComponentExportedFalse(manifest, ".FontDebugStatsProvider");
        assertTrue(manifest.contains("android:name=\".FontDebugStatsIngestService\""));
        assertTrue(manifest.contains("android:name=\".FontDebugStatsIngestActivity\""));
        assertComponentExportedFalse(manifest, ".FontDebugStatsIngestService");
        assertComponentExportedFalse(manifest, ".FontDebugStatsIngestActivity");
        assertComponentExportedFalse(manifest, ".FontDebugStatsReceiver");
    }

    @Test
    public void fontStatsReportersUseProviderTransportInsteadOfDirectBroadcasts() throws IOException {
        String fontReporter = read("src/main/java/com/dpis/module/FontDebugStatsReporter.java");
        String viewportReporter = read("src/main/java/com/dpis/module/ViewportDebugReporter.java");

        assertTrue(fontReporter.contains("FontDebugStatsTransport.sendUpdate(context, extras)"));
        assertTrue(viewportReporter.contains("FontDebugStatsTransport.sendUpdate(context, extras)"));
        assertTrue(!fontReporter.contains("context.sendBroadcast(intent)"));
        assertTrue(!viewportReporter.contains("context.sendBroadcast(intent)"));
    }

    @Test
    public void receiverAndProviderShareSamePreferenceWriter() throws IOException {
        String receiver = read("src/main/java/com/dpis/module/FontDebugStatsReceiver.java");
        String provider = read("src/main/java/com/dpis/module/FontDebugStatsProvider.java");

        assertTrue(receiver.contains("FontDebugStatsUpdateWriter.applyExtras("));
        assertTrue(provider.contains("FontDebugStatsUpdateWriter.applyExtras("));
    }

    @Test
    public void transportPrefersXposedRemotePreferencesBeforeProviderFallback() throws IOException {
        String transport = read("src/main/java/com/dpis/module/FontDebugStatsTransport.java");
        String moduleMain = read("src/main/java/com/dpis/module/ModuleMain.java");

        assertTrue(transport.contains("xposed.getRemotePreferences(DpiConfigStore.GROUP)"));
        assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".FontDebugStatsReceiver\""));
        assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".FontDebugStatsIngestService\""));
        assertTrue(transport.contains("MODULE_CLASS_PACKAGE + \".FontDebugStatsIngestActivity\""));
        assertTrue(transport.contains("FontDebugStatsUpdateWriter.applyExtras(preferences, extras)"));
        assertTrue(transport.contains("context.getContentResolver().call(buildUri()"));
        assertTrue(transport.contains("context.sendBroadcast(intent)"));
        assertTrue(transport.contains("context.startService(intent)"));
        assertTrue(transport.contains("context.startActivity(intent)"));
        assertTrue(transport.contains("FontDebugStatsFileBridge.write(extras)"));
        assertTrue(!transport.contains("return;\n        } catch (Throwable throwable)"));
        assertTrue(moduleMain.contains("FontDebugStatsTransport.initialize(this)"));
    }

    @Test
    public void overlayImportsFileBridgeBeforeRendering() throws IOException {
        String overlay = read("src/main/java/com/dpis/module/FontDebugOverlayService.java");

        assertTrue(overlay.contains("HandlerThread"));
        assertTrue(overlay.contains("scheduleBridgeImportIfNeeded()"));
        assertTrue(overlay.contains("FontDebugStatsFileBridge.importIfNewer(this)"));
        assertTrue(overlay.contains("FontDebugLogcatBridge.importRecent(this)"));
    }

    @Test
    public void manifestDoesNotRequestReadLogsForLogcatFallback() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(!manifest.contains("android.permission.READ_LOGS"));
    }

    private static void assertComponentExportedFalse(String manifest, String componentName) {
        int nameIndex = manifest.indexOf("android:name=\"" + componentName + "\"");
        assertTrue("missing component " + componentName, nameIndex >= 0);
        int exportedIndex = manifest.indexOf("android:exported=\"false\"", nameIndex);
        assertTrue("component should be non-exported " + componentName,
                exportedIndex >= 0 && exportedIndex - nameIndex < 200);
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
