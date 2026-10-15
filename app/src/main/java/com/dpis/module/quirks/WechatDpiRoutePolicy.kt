package com.dpis.module.quirks

import com.dpis.module.appconfig.WechatDpiConfig

/**
 * The WeChat route carries only a saved WeChat DPI.
 * Viewport width and font scale stay on the generic routes.
 */
object WechatDpiRoutePolicy {
    fun shouldInstallDedicatedRoute(
        packageName: String?,
        dedicatedDpiConfigured: Boolean,
    ): Boolean = WechatDpiConfig.appliesTo(packageName) && dedicatedDpiConfigured

    fun shouldInstallDedicatedRoute(packageName: String?): Boolean =
        shouldInstallDedicatedRoute(
            packageName,
            WechatDpiConfig.normalize(WechatDpiPropertyBridge.readDpi(packageName)) != null,
        )

    fun targetDpi(dedicatedDpi: Int): Int? = WechatDpiConfig.normalize(dedicatedDpi)

    fun targetDpi(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) return null
        return targetDpi(WechatDpiPropertyBridge.readDpi(packageName))
    }
}
