package com.dpis.module;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class HotReloadInstallerResetSourceTest {
    @Test
    public void processScopedInstallersDeclareHotReloadReset() throws IOException {
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/ActivityThreadFontHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ChromiumViewportProbeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/DisplayHookInstaller.kt",
                "fun resetForHotReload()",
                "installedPid = -1");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/PaintTextSizeFallbackHookInstaller.kt",
                "fun resetForHotReload()",
                "installedPid = -1");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ResourcesProbeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.kt",
                "fun resetForHotReload()",
                "installedPid = -1");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ViewRootProbeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt",
                "fun resetForHotReload()",
                "installedPid = -1");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/WindowManagerProbeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/WindowMetricsHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/WindowSessionProbeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
    }

    @Test
    public void appProcessResetterCentralizesHotReloadResetContract() throws IOException {
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHotReloadResetter.java",
                "public static void resetAll()",
                "ActivityThreadFontHookInstaller.resetForHotReload();",
                "ChromiumViewportProbeHookInstaller.resetForHotReload();",
                "DisplayHookInstaller.resetForHotReload();",
                "ForceTextSizeHookInstaller.resetForHotReload();",
                "PaintTextSizeFallbackHookInstaller.resetForHotReload();",
                "ResourcesProbeHookInstaller.resetForHotReload();",
                "ResourcesManagerHookInstaller.resetForHotReload();",
                "ResourcesImplHookInstaller.resetForHotReload();",
                "ResourcesReadHookInstaller.resetForHotReload();",
                "TypefaceOverrideHookInstaller.resetForHotReload();",
                "ViewRootProbeHookInstaller.resetForHotReload();",
                "WebViewFontHookInstaller.resetForHotReload();",
                "WindowManagerProbeHookInstaller.resetForHotReload();",
                "WindowMetricsHookInstaller.resetForHotReload();",
                "WindowSessionProbeHookInstaller.resetForHotReload();");
    }

    @Test
    public void sharedHookInstallersClearInstalledFlagsForHotReload() throws IOException {
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ResourcesImplHookInstaller.kt",
                "fun resetForHotReload()",
                "hookInstalled = false");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ResourcesManagerHookInstaller.kt",
                "fun resetForHotReload()",
                "hookInstalled = false");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ResourcesReadHookInstaller.kt",
                "fun resetForHotReload()",
                "hookInstalled = false");
    }

    private static void assertSourceContains(String relativePath, String... expected) throws IOException {
        String source = SourceSmokeTestPaths.read(relativePath);
        for (String snippet : expected) {
            assertTrue(relativePath + " should contain: " + snippet, source.contains(snippet));
        }
    }
}
