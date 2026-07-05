package com.dpis.module;

import com.dpis.module.runtime.font.WebViewFontHookInstaller;

import com.dpis.module.runtime.font.TypefaceOverrideHookInstaller;

import com.dpis.module.runtime.font.PaintTextSizeFallbackHookInstaller;

import com.dpis.module.runtime.font.ForceTextSizeHookInstaller;

import com.dpis.module.runtime.font.ActivityThreadFontHookInstaller;

import com.dpis.module.runtime.appprocess.ChromiumViewportProbeHookInstaller;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class HotReloadInstallerResetSourceTest {
    @Test
    public void processScopedInstallersDeclareHotReloadReset() throws IOException {
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/ActivityThreadFontHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/appprocess/ChromiumViewportProbeHookInstaller.java",
                "public static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/DisplayHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/PaintTextSizeFallbackHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/ResourcesProbeHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/WindowManagerProbeHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/WindowMetricsHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
        assertSourceContains("src/main/java/com/dpis/module/WindowSessionProbeHookInstaller.java",
                "static void resetForHotReload()",
                "installedPid = -1;");
    }

    @Test
    public void appProcessResetterCentralizesHotReloadResetContract() throws IOException {
        assertSourceContains("src/main/java/com/dpis/module/AppProcessHotReloadResetter.java",
                "static void resetAll()",
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
        assertSourceContains("src/main/java/com/dpis/module/ResourcesImplHookInstaller.java",
                "static void resetForHotReload()",
                "hookInstalled = false;");
        assertSourceContains("src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java",
                "static void resetForHotReload()",
                "hookInstalled = false;");
        assertSourceContains("src/main/java/com/dpis/module/ResourcesReadHookInstaller.java",
                "static void resetForHotReload()",
                "hookInstalled = false;");
    }

    private static void assertSourceContains(String relativePath, String... expected) throws IOException {
        String source = SourceSmokeTestPaths.read(relativePath);
        for (String snippet : expected) {
            assertTrue(relativePath + " should contain: " + snippet, source.contains(snippet));
        }
    }
}
