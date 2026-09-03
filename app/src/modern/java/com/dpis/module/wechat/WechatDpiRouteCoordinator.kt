package com.dpis.module.wechat

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.dpis.module.DpisLog
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.util.concurrent.atomic.AtomicBoolean

/** Owns every Modern lifecycle entry for the WeChat-only DPI route. */
object WechatDpiRouteCoordinator {
    private val applicationAttachHookInstalled = AtomicBoolean(false)

    @JvmStatic
    fun handleModuleLoaded(xposed: XposedModule?, processName: String?) {
        if (xposed == null || !WechatDpiConfig.appliesTo(processName)) return
        installApplicationAttachHook(xposed)
    }

    @JvmStatic
    fun handlePackageReady(
        xposed: XposedModule?,
        param: XposedModuleInterface.PackageReadyParam?,
        processName: String?,
    ): Boolean {
        if (param == null || !WechatDpiConfig.appliesTo(param.packageName)) return false
        if (WechatDpiConfig.appliesTo(processName)) {
            installPackageReadyRoute(
                xposed,
                param.packageName,
                param.classLoader,
                param.applicationInfo,
                processName,
                WechatDpiInstallPhase.PACKAGE_READY,
            )
        }
        DpisLog.i("modern app-specific route installed alongside generic hooks: package=" +
            WechatDpiConfig.PACKAGE_NAME + ", process=" + processName)
        return false
    }

    @JvmStatic
    fun handlePackageReadyReplay(
        xposed: XposedModule?,
        packageName: String?,
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
        processName: String?,
    ) {
        if (xposed == null || !WechatDpiConfig.appliesTo(packageName) ||
            !WechatDpiConfig.appliesTo(processName)) return
        installPackageReadyRoute(
            xposed, packageName, classLoader, applicationInfo, processName,
            WechatDpiInstallPhase.HOT_RELOAD_PACKAGE_READY,
        )
    }

    private fun installPackageReadyRoute(
        xposed: XposedModule?,
        packageName: String?,
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
        processName: String?,
        phase: WechatDpiInstallPhase,
    ) {
        if (xposed == null || packageName == null || classLoader == null) return
        val source = phase.routeName
        DpisLog.i("modern WeChat DPI route enter: package=$packageName, process=$processName, source=$source")
        xposed.log(Log.INFO, DpisLog.TAG, "DPIS modern WeChat DPI route enter: package=$packageName, process=$processName, source=$source")
        RuntimeHotPathEvents.event(packageName, "wechat_dpi", source, "route_callback_entered", "source=$source, process=$processName")
        try {
            val outcome = WechatDpiModernHookInstaller.install(xposed, classLoader, applicationInfo, packageName, phase)
            DpisLog.i("modern WeChat DPI route install attempted: package=$packageName, process=$processName, source=$source, outcome=$outcome, classLoader=${WechatDpiModernHookInstaller.describeClassLoaderForLog(classLoader)}")
            RuntimeHotPathEvents.event(packageName, "wechat_dpi", source, installOutcomeStage(outcome), "outcome=$outcome, process=$processName")
        } catch (throwable: Throwable) {
            DpisLog.e("modern WeChat DPI route install failed: package=$packageName, process=$processName, source=$source, ${throwable.javaClass.name}: ${throwable.message}", throwable)
            RuntimeHotPathEvents.event(packageName, "wechat_dpi", source, "skipped", "installFailed=true, process=$processName, error=${throwable.javaClass.simpleName}")
        }
    }

    private fun installApplicationAttachHook(xposed: XposedInterface) {
        if (!applicationAttachHookInstalled.compareAndSet(false, true)) return
        try {
            val method = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                val result = chain.proceed()
                val context = chain.args.firstOrNull() as? Context
                if (context != null && WechatDpiConfig.appliesTo(context.packageName)) {
                    WechatDpiResourceRecovery.installForegroundMonitor(context)
                    val classLoader = context.classLoader
                    val packageName = context.packageName
                    DpisLog.i(
                        "modern WeChat DPI application-attach route enter: package=$packageName, " +
                            "classLoader=${WechatDpiModernHookInstaller.describeClassLoaderForLog(classLoader)}",
                    )
                    RuntimeHotPathEvents.event(packageName, "wechat_dpi", "application_attach", "route_callback_entered", "classLoader=${WechatDpiModernHookInstaller.describeClassLoaderForLog(classLoader)}")
                    val outcome = WechatDpiModernHookInstaller.install(xposed, classLoader, context.applicationInfo, packageName, WechatDpiInstallPhase.APPLICATION_ATTACH)
                    DpisLog.i("modern WeChat DPI application-attach retry result: package=$packageName, outcome=$outcome, classLoader=${WechatDpiModernHookInstaller.describeClassLoaderForLog(classLoader)}")
                    RuntimeHotPathEvents.event(packageName, "wechat_dpi", "application_attach", installOutcomeStage(outcome), "outcome=$outcome, classLoader=${WechatDpiModernHookInstaller.describeClassLoaderForLog(classLoader)}")
                }
                result
            }
            DpisLog.i("modern WeChat DPI application-attach hook ready: process=${WechatDpiConfig.PACKAGE_NAME}")
            RuntimeHotPathEvents.event(WechatDpiConfig.PACKAGE_NAME, "wechat_dpi", "application_attach", "hook_ready", "process=${WechatDpiConfig.PACKAGE_NAME}")
        } catch (throwable: Throwable) {
            applicationAttachHookInstalled.set(false)
            DpisLog.e("modern WeChat DPI application-attach hook failed: ${throwable.javaClass.name}: ${throwable.message}", throwable)
            RuntimeHotPathEvents.event(WechatDpiConfig.PACKAGE_NAME, "wechat_dpi", "application_attach", "skipped", "hookFailed=true, error=${throwable.javaClass.simpleName}")
        }
    }

    private fun installOutcomeStage(outcome: WechatDpiInstallOutcome) = when (outcome) {
        WechatDpiInstallOutcome.INSTALLED -> "mutation_candidate"
        WechatDpiInstallOutcome.DEFERRED -> "deferred"
        WechatDpiInstallOutcome.SKIPPED -> "skipped"
    }

    @JvmStatic
    fun shouldSuppressModuleLoadedGenericHooks(packageName: String?, processName: String?): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return false
        DpisLog.i("modern app-specific route allowing generic hooks alongside: package=${WechatDpiConfig.PACKAGE_NAME}, process=$processName")
        return false
    }
}
