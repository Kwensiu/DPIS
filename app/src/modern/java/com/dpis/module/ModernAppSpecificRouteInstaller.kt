package com.dpis.module

import android.content.pm.ApplicationInfo
import com.dpis.module.wechat.WechatDpiRouteCoordinator
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * Dispatches modern lifecycle callbacks to package-specific route owners.
 * Package-specific hooks belong to their own package so this class stays a
 * stable extension point instead of accumulating application implementations.
 */
object ModernAppSpecificRouteInstaller {
    @JvmStatic
    fun handleModuleLoaded(xposed: XposedModule?, processName: String?) {
        WechatDpiRouteCoordinator.handleModuleLoaded(xposed, processName)
    }

    @JvmStatic
    fun handlePackageReady(
        xposed: XposedModule?,
        param: XposedModuleInterface.PackageReadyParam?,
        processName: String?,
    ): Boolean = WechatDpiRouteCoordinator.handlePackageReady(xposed, param, processName)

    @JvmStatic
    fun handlePackageReadyReplay(
        xposed: XposedModule?,
        packageName: String?,
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
        processName: String?,
    ) {
        WechatDpiRouteCoordinator.handlePackageReadyReplay(
            xposed,
            packageName,
            classLoader,
            applicationInfo,
            processName,
        )
    }

    @JvmStatic
    fun shouldSuppressModuleLoadedGenericHooks(packageName: String?, processName: String?): Boolean =
        WechatDpiRouteCoordinator.shouldSuppressModuleLoadedGenericHooks(packageName, processName)
}
