package com.dpis.module;

import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.viewport.ViewportPropertySyncer;
import com.dpis.module.viewport.ViewportApplyMode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class DebugConfigBroadcastReceiverSourceTest {
    @Test
    public void debugManifestExposesDebugConfigReceiverOnlyForDebugBuilds() throws IOException {
        String debugManifest = read("src/debug/AndroidManifest.xml");
        String mainManifest = read("src/main/AndroidManifest.xml");

        assertTrue(debugManifest.contains(".DebugConfigActivity"));
        assertTrue(debugManifest.contains(".DebugConfigBroadcastReceiver"));
        assertTrue(debugManifest.contains("android:exported=\"true\""));
        assertTrue(debugManifest.contains("io.github.kwensiu.dpis.DEBUG_SET_PACKAGE_CONFIG"));
        assertFalse(mainManifest.contains("DebugConfigActivity"));
        assertFalse(mainManifest.contains("DebugConfigBroadcastReceiver"));
        assertFalse(mainManifest.contains("DEBUG_SET_PACKAGE_CONFIG"));
    }

    @Test
    public void debugConfigApplierWritesConfigAndPublishesRuntimeProperties() throws IOException {
        String source = read("src/debug/java/com/dpis/module/DebugConfigApplier.java");

        assertTrue(source.contains("if (!BuildConfig.DEBUG)"));
        assertTrue(source.contains("DpisApplication.getConfigStore()"));
        assertTrue(source.contains("store.setTargetViewportWidthDp(packageName, widthDp)"));
        assertTrue(source.contains("store.setTargetViewportApplyMode(packageName, normalizedMode)"));
        assertTrue(source.contains("ViewportPropertySyncer.publishTargetAsync(packageName, widthDp, normalizedMode)"));
        assertTrue(source.contains("store.setTargetFontScalePercent(packageName, fontScalePercent)"));
        assertTrue(source.contains("store.setTargetFontApplyMode(packageName, normalizedMode)"));
        assertTrue(source.contains("FontRuntimePropertySyncer.publishTargetAsync(packageName, fontScalePercent"));
        assertTrue(source.contains("store.setSystemServerSafeModeEnabled("));
        assertTrue(source.contains("store.setGlobalLogEnabled(loggingEnabled)"));
        assertTrue(source.contains("store.setFontDebugOverlayEnabled("));
        assertTrue(source.contains("restartTargetAsync(packageName)"));
        assertTrue(source.contains("DPIS_DEBUG_CONFIG"));
    }

    @Test
    public void debugConfigActivityAndReceiverUseSharedApplier() throws IOException {
        String activity = read("src/debug/java/com/dpis/module/DebugConfigActivity.java");
        String receiver = read("src/debug/java/com/dpis/module/DebugConfigBroadcastReceiver.java");

        assertTrue(activity.contains("DebugConfigApplier.apply(this, getIntent(), false)"));
        assertTrue(receiver.contains("DebugConfigApplier.apply(context, intent, true)"));
    }

    @Test
    public void debugConfigClassesStayOutOfMainSourceSet() {
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "java", "com", "dpis", "module", "DebugConfigActivity.java"));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "java", "com", "dpis", "module", "DebugConfigApplier.java"));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "java", "com", "dpis", "module", "DebugConfigBroadcastReceiver.java"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
