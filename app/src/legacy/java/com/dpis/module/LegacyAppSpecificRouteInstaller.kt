package com.dpis.module

import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import de.robv.android.xposed.callbacks.XC_LoadPackage

object LegacyAppSpecificRouteInstaller {
    // Keep the Xposed callback bridge static for the existing LegacyModuleHook call site.
    @JvmStatic
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?): Boolean {
        if (lpparam == null || !WechatDpiConfig.appliesTo(lpparam.packageName)) {
            return false
        }
        if (WechatDpiConfig.appliesTo(lpparam.processName)) {
            DpisLog.i(
                "legacy WeChat DPI route enter: package=${lpparam.packageName}, " +
                        "process=${lpparam.processName}",
            )
            RuntimeHotPathEvents.event(
                lpparam.packageName,
                "wechat_dpi",
                "legacy_load_package",
                "route_callback_entered",
                "process=${lpparam.processName}",
            )
            try {
                WechatDpiLegacyHookInstaller.install(lpparam)
                DpisLog.i(
                    "legacy WeChat DPI route install attempted: package=${lpparam.packageName}, " +
                            "process=${lpparam.processName}",
                )
                RuntimeHotPathEvents.event(
                    lpparam.packageName,
                    "wechat_dpi",
                    "legacy_load_package",
                    "mutation_candidate",
                    "installAttempted=true, process=${lpparam.processName}",
                )
            } catch (throwable: Throwable) {
                DpisLog.e(
                    "legacy WeChat DPI route install failed: package=${lpparam.packageName}, " +
                            "process=${lpparam.processName}, ${throwable.javaClass.name}: " +
                            throwable.message,
                    throwable,
                )
                RuntimeHotPathEvents.event(
                    lpparam.packageName,
                    "wechat_dpi",
                    "legacy_load_package",
                    "skipped",
                    "installFailed=true, process=${lpparam.processName}, " +
                            "error=${throwable.javaClass.simpleName}",
                )
            }
        }
        DpisLog.i(
            "legacy app-specific route installed alongside generic hooks: " +
                    "package=${WechatDpiConfig.PACKAGE_NAME}, process=${lpparam.processName}",
        )
        return false
    }
}
