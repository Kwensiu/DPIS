package com.dpis.module;

final class AppProcessHotReloadResetter {
    private AppProcessHotReloadResetter() {
    }

    static void resetAll() {
        // Keep hot-reload reset ownership in one place so new app-process
        // installers have a single integration point instead of growing
        // ModuleMain replay with another handwritten call.
        ActivityThreadFontHookInstaller.resetForHotReload();
        ChromiumViewportProbeHookInstaller.resetForHotReload();
        DisplayHookInstaller.resetForHotReload();
        ForceTextSizeHookInstaller.resetForHotReload();
        PaintTextSizeFallbackHookInstaller.resetForHotReload();
        ResourcesProbeHookInstaller.resetForHotReload();
        ResourcesManagerHookInstaller.resetForHotReload();
        ResourcesImplHookInstaller.resetForHotReload();
        ResourcesReadHookInstaller.resetForHotReload();
        TypefaceOverrideHookInstaller.resetForHotReload();
        ViewRootProbeHookInstaller.resetForHotReload();
        WebViewFontHookInstaller.resetForHotReload();
        WindowManagerProbeHookInstaller.resetForHotReload();
        WindowMetricsHookInstaller.resetForHotReload();
        WindowSessionProbeHookInstaller.resetForHotReload();
    }
}
