package com.dpis.module.quirks

import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertyBridge

/**
 * The WeChat route carries only a saved WeChat DPI.
 * Viewport width and font scale stay on the generic routes.
 */
object WechatDpiRoutePolicy {
    fun shouldInstallDedicatedRoute(
        packageName: String?,
        dedicatedDpiConfigured: Boolean,
        viewportDensityOwner: Boolean,
    ): Boolean = WechatDpiConfig.appliesTo(packageName) &&
            dedicatedDpiConfigured && !viewportDensityOwner

    fun shouldInstallDedicatedRoute(
        packageName: String?,
        dedicatedDpiConfigured: Boolean,
    ): Boolean = shouldInstallDedicatedRoute(
        packageName,
        dedicatedDpiConfigured,
        viewportDensityOwner(packageName),
    )

    fun shouldInstallDedicatedRoute(packageName: String?): Boolean =
        shouldInstallDedicatedRoute(
            packageName,
            WechatDpiConfig.normalize(WechatDpiPropertyBridge.readDpi(packageName)) != null,
        )

    fun viewportDensityOwner(packageName: String?): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return false
        val targetSpec = ViewportPropertyBridge.readTargetSpec(packageName)
        return targetSpec.isEnabled &&
                ViewportApplyMode.isEnabled(ViewportPropertyBridge.readCompatMode(packageName))
    }

    fun targetDpi(dedicatedDpi: Int): Int? = WechatDpiConfig.normalize(dedicatedDpi)

    fun targetDpi(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) return null
        return targetDpi(WechatDpiPropertyBridge.readDpi(packageName))
    }
}
