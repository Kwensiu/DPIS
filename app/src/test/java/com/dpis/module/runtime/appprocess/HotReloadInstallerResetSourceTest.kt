package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class HotReloadInstallerResetSourceTest {
    @Test
    fun processScopedInstallersDeclareHotReloadReset() {
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/font/ActivityThreadFontHookInstaller.java",
            "public static void resetForHotReload()",
            "installedPid = -1;",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/ChromiumViewportProbeHookInstaller.java",
            "public static void resetForHotReload()",
            "installedPid = -1;",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/DisplayHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.kt",
            "fun resetForHotReload()",
            "ForceTextSizeHookRuntime.resetForHotReload()",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt",
            "fun resetForHotReload()",
            "FontMutationScheduler.resetForHotReload()",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/font/PaintTextSizeFallbackHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/ResourcesProbeHookInstaller.java",
            "public static void resetForHotReload()",
            "installedPid = -1;",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java",
            "public static void resetForHotReload()",
            "installedPid = -1;",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/ViewRootProbeHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/WindowManagerProbeHookInstaller.java",
            "public static void resetForHotReload()",
            "installedPid = -1;",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/WindowMetricsHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/WindowSessionProbeHookInstaller.kt",
            "fun resetForHotReload()",
            "installedPid = -1",
        )
    }

    @Test
    fun appProcessResetterCentralizesHotReloadResetContract() {
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/AppProcessHotReloadResetter.kt",
            "fun resetAll()",
            "ActivityThreadFontHookInstaller.resetForHotReload()",
            "ChromiumViewportProbeHookInstaller.resetForHotReload()",
            "DisplayHookInstaller.resetForHotReload()",
            "ForceTextSizeHookInstaller.resetForHotReload()",
            "PaintTextSizeFallbackHookInstaller.resetForHotReload()",
            "ResourcesProbeHookInstaller.resetForHotReload()",
            "ResourcesManagerHookInstaller.resetForHotReload()",
            "ResourcesImplHookInstaller.resetForHotReload()",
            "ResourcesReadHookInstaller.resetForHotReload()",
            "TypefaceOverrideHookInstaller.resetForHotReload()",
            "ViewRootProbeHookInstaller.resetForHotReload()",
            "WebViewFontHookInstaller.resetForHotReload()",
            "WindowBoundsState.resetForHotReload()",
            "WindowManagerProbeHookInstaller.resetForHotReload()",
            "WindowMetricsHookInstaller.resetForHotReload()",
            "WindowSessionProbeHookInstaller.resetForHotReload()",
        )
    }

    @Test
    fun sharedHookInstallersClearInstalledFlagsForHotReload() {
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/ResourcesImplHookInstaller.kt",
            "fun resetForHotReload()",
            "hookInstalled = false",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/ResourcesManagerHookInstaller.kt",
            "fun resetForHotReload()",
            "hookInstalled = false",
        )
        assertSourceContains(
            "src/main/java/com/dpis/module/runtime/appprocess/ResourcesReadHookInstaller.kt",
            "fun resetForHotReload()",
            "hookInstalled = false",
        )
    }

    private fun assertSourceContains(relativePath: String, vararg expected: String) {
        val source = SourceSmokeTestPaths.read(relativePath)
        for (snippet in expected) {
            assertTrue("$relativePath should contain: $snippet", source.contains(snippet))
        }
    }
}
