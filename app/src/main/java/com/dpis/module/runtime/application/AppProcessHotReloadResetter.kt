package com.dpis.module.runtime.application

import com.dpis.module.runtime.chromium.ChromiumViewportProbeHookInstaller
import com.dpis.module.runtime.display.DisplayHookInstaller
import com.dpis.module.runtime.font.ActivityThreadFontHookInstaller
import com.dpis.module.runtime.font.ForceTextSizeHookInstaller
import com.dpis.module.runtime.font.PaintTextSizeFallbackHookInstaller
import com.dpis.module.runtime.font.TypefaceOverrideHookInstaller
import com.dpis.module.runtime.font.WebViewFontHookInstaller
import com.dpis.module.runtime.resources.ResourcesImplHookInstaller
import com.dpis.module.runtime.resources.ResourcesManagerHookInstaller
import com.dpis.module.runtime.resources.ResourcesProbeHookInstaller
import com.dpis.module.runtime.resources.ResourcesReadHookInstaller
import com.dpis.module.runtime.webapk.WebApkRuntimeOwnerBridge
import com.dpis.module.runtime.window.ViewRootProbeHookInstaller
import com.dpis.module.runtime.window.WindowManagerProbeHookInstaller
import com.dpis.module.runtime.window.WindowMetricsHookInstaller
import com.dpis.module.runtime.window.WindowSessionProbeHookInstaller
import com.dpis.module.viewport.window.WindowBoundsState

object AppProcessHotReloadResetter {
    /**
     * Keep hot-reload reset ownership in one place so new app-process
     * installers have a single integration point instead of growing
     * ModuleMain replay with another handwritten call.
     */
    @JvmStatic
    fun resetAll() {
        ActivityThreadFontHookInstaller.resetForHotReload()
        ChromiumViewportProbeHookInstaller.resetForHotReload()
        DisplayHookInstaller.resetForHotReload()
        ForceTextSizeHookInstaller.resetForHotReload()
        PaintTextSizeFallbackHookInstaller.resetForHotReload()
        ResourcesProbeHookInstaller.resetForHotReload()
        ResourcesManagerHookInstaller.resetForHotReload()
        ResourcesImplHookInstaller.resetForHotReload()
        ResourcesReadHookInstaller.resetForHotReload()
        TypefaceOverrideHookInstaller.resetForHotReload()
        ViewRootProbeHookInstaller.resetForHotReload()
        WebViewFontHookInstaller.resetForHotReload()
        WebApkRuntimeOwnerBridge.resetForHotReload()
        WindowBoundsState.resetForHotReload()
        WindowManagerProbeHookInstaller.resetForHotReload()
        WindowMetricsHookInstaller.resetForHotReload()
        WindowSessionProbeHookInstaller.resetForHotReload()
    }
}
