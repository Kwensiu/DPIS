package com.dpis.module

import android.app.AndroidAppHelper
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.DisplayMetrics
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.quirks.WechatDpiMethodLocator
import com.dpis.module.quirks.WechatDpiPropertyBridge
import com.dpis.module.quirks.WechatDpiRuntime
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

internal object WechatDpiLegacyHookInstaller {
    private val hooked = AtomicBoolean(false)
    private val callbackLogged = AtomicBoolean(false)
    private val mutationLogged = AtomicBoolean(false)

    @JvmStatic
    fun install(loadPackage: XC_LoadPackage.LoadPackageParam?) {
        val classLoader = loadPackage?.classLoader ?: return
        if (!hooked.compareAndSet(false, true)) return
        if (!installRoute(classLoader, loadPackage)) hooked.set(false)
    }

    private fun installRoute(
        classLoader: ClassLoader,
        loadPackage: XC_LoadPackage.LoadPackageParam
    ): Boolean {
        val versionCode = resolveWechatVersionCode(loadPackage)
        val result = WechatDpiMethodLocator.locate(classLoader, loadPackage.appInfo, versionCode)
        if (result.methods.isEmpty()) {
            DpisLog.i("legacy WeChat DPI hook skipped: locator=${result.source.logName}, versionCode=$versionCode, reason=${result.failure}")
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME, "wechat_dpi", "displaymetrics", "skipped",
                "locator=${result.source.logName}, versionCode=$versionCode, reason=${result.failure}",
            )
            return false
        }
        return installWechatDpiHook(result, versionCode)
    }

    private fun installWechatDpiHook(
        locatorResult: WechatDpiMethodLocator.Result,
        versionCode: Long
    ): Boolean {
        val methods = locatorResult.methods
        if (methods.isEmpty()) return false
        var installed = 0
        try {
            methods.forEach { metricsMethod ->
                metricsMethod.isAccessible = true
                XposedBridge.hookMethod(metricsMethod, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (callbackLogged.compareAndSet(false, true)) {
                            RuntimeHotPathEvents.event(
                                WechatDpiConfig.PACKAGE_NAME,
                                "wechat_dpi",
                                "displaymetrics",
                                "route_callback_entered",
                                "method=${methodName(metricsMethod)}, configuredDpi=${
                                    WechatDpiPropertyBridge.readDpi(
                                        WechatDpiConfig.PACKAGE_NAME
                                    )
                                }",
                            )
                        }
                        val metrics = param.result as? DisplayMetrics ?: return
                        applyWechatDpi(metrics, methodName(metricsMethod))?.let(param::setResult)
                    }
                })
                installed++
            }
            DpisLog.i("legacy WeChat DPI hook ready: ${methodNames(methods)}, installed=$installed, locator=${locatorResult.source.logName}, versionCode=$versionCode")
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME, "wechat_dpi", "displaymetrics", "hook_ready",
                "installed=$installed, locator=${locatorResult.source.logName}, versionCode=$versionCode",
            )
            return installed > 0
        } catch (throwable: Throwable) {
            DpisLog.e(
                "legacy WeChat DPI hook failed: ${methodNames(methods)}, versionCode=$versionCode, ${throwable.javaClass.name}: ${throwable.message}",
                throwable
            )
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME, "wechat_dpi", "displaymetrics", "skipped",
                "hookFailed=true, versionCode=$versionCode, error=${throwable.javaClass.simpleName}",
            )
            return false
        }
    }

    private fun methodNames(methods: List<Method>): String =
        methods.joinToString("|") { methodName(it) }

    private fun methodName(method: Method): String = "${method.declaringClass.name}#${method.name}"

    private fun applyWechatDpi(metrics: DisplayMetrics, methodName: String): DisplayMetrics? {
        val dpi = WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME)
        val detached = WechatDpiRuntime.detached(metrics, dpi) ?: return null
        if (mutationLogged.compareAndSet(false, true)) {
            DpisLog.i("legacy WeChat DPI applied: method=$methodName, targetDpi=$dpi, densityDpi ${metrics.densityDpi} -> ${detached.densityDpi}, density ${metrics.density} -> ${detached.density}, scaledDensity ${metrics.scaledDensity} -> ${detached.scaledDensity}")
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME, "wechat_dpi", "displaymetrics", "mutation_applied",
                "method=$methodName, targetDpi=$dpi, densityDpi=${metrics.densityDpi}->${detached.densityDpi}, density=${metrics.density}->${detached.density}, scaledDensity=${metrics.scaledDensity}->${detached.scaledDensity}",
            )
        }
        return detached
    }

    private fun resolveWechatVersionCode(loadPackage: XC_LoadPackage.LoadPackageParam): Long =
        resolveWechatVersionCode(loadPackage.appInfo)
            .takeIf { it > 0L }
            ?: resolveWechatVersionCode(AndroidAppHelper.currentApplication())
                .takeIf { it > 0L }
            ?: resolveWechatVersionCode(resolveSystemContext())

    private fun resolveWechatVersionCode(appInfo: Any?): Long = try {
        (appInfo?.javaClass?.getField("longVersionCode")?.get(appInfo) as? Long) ?: 0L
    } catch (_: Throwable) {
        0L
    }

    private fun resolveWechatVersionCode(context: Context?): Long = try {
        val packageInfo = context?.packageManager?.getPackageInfo(WechatDpiConfig.PACKAGE_NAME, 0)
        resolvePackageVersionCode(packageInfo)
    } catch (_: Throwable) {
        0L
    }

    private fun resolvePackageVersionCode(packageInfo: PackageInfo?): Long {
        packageInfo ?: return 0L
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun resolveSystemContext(): Context? = try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val current =
            activityThread.getDeclaredMethod("currentActivityThread").invoke(null) ?: return null
        activityThread.getDeclaredMethod("getSystemContext").invoke(current) as? Context
    } catch (_: Throwable) {
        null
    }
}
