package com.dpis.module.quirks

import android.os.Process
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
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

    fun shouldInstallDedicatedRoute(packageName: String?): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return false
        val userId = currentUserId() ?: return false
        return shouldInstallDedicatedRoute(
            packageName,
            WechatDpiConfig.normalize(WechatDpiPropertyBridge.readDpi(packageName, userId)) != null,
            viewportDensityOwner(packageName, userId),
        )
    }

    fun viewportDensityOwner(packageName: String?): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return false
        val userId = currentUserId() ?: return false
        return viewportDensityOwner(packageName, userId)
    }

    private fun viewportDensityOwner(packageName: String?, userId: Int): Boolean {
        val targetSpec = ViewportPropertyBridge.readTargetSpec(packageName, userId)
        return targetSpec.isEnabled &&
                ViewportApplyMode.isEnabled(
                    ViewportPropertyBridge.readCompatMode(
                        packageName,
                        userId
                    )
                )
    }

    private fun currentUserId(): Int = runCatching {
        PackageUidResolver.userIdFromUid(Process.myUid())
    }.getOrNull() ?: 0

    fun targetDpi(packageName: String?, userId: Int): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) return null
        return WechatDpiConfig.normalize(WechatDpiPropertyBridge.readDpi(packageName, userId))
    }

}
