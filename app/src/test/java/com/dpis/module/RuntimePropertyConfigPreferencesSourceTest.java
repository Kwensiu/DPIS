package com.dpis.module;

import com.dpis.module.hooks.HookDomainOverride;

import static org.junit.Assert.assertTrue;


import org.junit.Test;

public class RuntimePropertyConfigPreferencesSourceTest {
    @Test
    public void usesTtlBasedSnapshotRefresh() throws Exception {
        String source = read("src/main/java/com/dpis/module/RuntimePropertyConfigPreferences.java");

        assertTrue(source.contains("SNAPSHOT_TTL_MILLIS = 2_000L"));
        assertTrue(source.contains("cachedAtMillis"));
        assertTrue(source.contains("(now - cachedAtMillis) < SNAPSHOT_TTL_MILLIS"));
    }

    @Test
    public void readsHookDomainOverrideFromRuntimePropertyMirror() throws Exception {
        String source = read("src/main/java/com/dpis/module/RuntimePropertyConfigPreferences.java");

        assertTrue(source.contains("FontHookDomainPropertyBridge.readOverride(packageName)"));
        assertTrue(source.contains("values.put(hookDomainsKey(), String.join(\",\""));
        assertTrue(source.contains("font.\" + packageName + \".hook_domains"));
    }

    @Test
    public void readsDebugSwitchesFromRuntimePropertyMirror() throws Exception {
        String source = read("src/main/java/com/dpis/module/RuntimePropertyConfigPreferences.java");

        assertTrue(source.contains("DpisConfigStore.KEY_GLOBAL_LOG_ENABLED"));
        assertTrue(source.contains("RuntimeDebugPropertyBridge.readGlobalLogEnabled()"));
        assertTrue(source.contains("DpisConfigStore.KEY_FONT_DEBUG_OVERLAY_ENABLED"));
        assertTrue(source.contains("RuntimeDebugPropertyBridge.readFontDebugOverlayEnabled()"));
    }

    private static String read(String relativePath) throws Exception {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
