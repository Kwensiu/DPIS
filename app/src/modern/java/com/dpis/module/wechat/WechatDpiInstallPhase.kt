package com.dpis.module.wechat

/**
 * Names a WeChat DPI installation attempt and whether it may perform the
 * expensive APK-wide DexKit discovery at that lifecycle point.
 */
enum class WechatDpiInstallPhase(
    val routeName: String,
    val allowsDexKit: Boolean,
) {
    PACKAGE_READY("package_ready", false),
    APPLICATION_ATTACH("application_attach", true),
    HOT_RELOAD_PACKAGE_READY("hot_reload_package_ready", true),
}
