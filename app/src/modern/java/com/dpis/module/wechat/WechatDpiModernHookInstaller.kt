package com.dpis.module.wechat

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.util.DisplayMetrics
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.quirks.WechatDpiMethodLocator
import com.dpis.module.quirks.WechatDpiPropertyBridge
import com.dpis.module.quirks.WechatDpiRoutes
import com.dpis.module.quirks.WechatDpiRuntime
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Coordinates Modern WeChat route selection and density getter hooks. */
internal object WechatDpiModernHookInstaller {
    private const val HOOK_ID_DENSITY_PREFIX = "wechat_dpi_density_"
    private val hookLock = Any()
    private val hookedDensityManagerClasses =
        Collections.newSetFromMap(IdentityHashMap<Class<*>, Boolean>())
    private val callbackLogged = AtomicBoolean(false)
    private val mutationLogged = AtomicBoolean(false)

    @JvmStatic
    fun install(
        xposed: XposedInterface?,
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
        packageName: String?,
        phase: WechatDpiInstallPhase,
    ): WechatDpiInstallOutcome {
        if (!WechatDpiConfig.appliesTo(packageName) || xposed == null || classLoader == null) {
            return WechatDpiInstallOutcome.SKIPPED
        }
        val resolvedPackageName = packageName ?: return WechatDpiInstallOutcome.SKIPPED
        val versionCode = resolveWechatVersionCode(applicationInfo, packageName)
        val staticRoute = WechatDpiRoutes.forVersionCode(versionCode)
        val locator = WechatDpiMethodLocator.locate(
            classLoader,
            applicationInfo,
            versionCode,
            phase.allowsDexKit
        )
        logRoutePlan(resolvedPackageName, staticRoute, locator, versionCode, phase)
        WechatDpiModernBottomTabHookInstaller.install(
            xposed,
            classLoader,
            phase,
            staticRoute?.bottomTabIconScaleEnabled == true,
        )
        if (locator.methods.isEmpty()) {
            val deferred = !phase.allowsDexKit
            val stage = if (deferred) "deferred" else "skipped"
            val detail =
                "locator=${locator.source.logName}, versionCode=$versionCode, attempt=${phase.routeName}, reason=${locator.failure}"
            DpisLog.i(
                "modern WeChat DPI hook $stage: $detail, classLoader=${
                    describeClassLoaderForLog(
                        classLoader
                    )
                }"
            )
            RuntimeHotPathEvents.event(
                resolvedPackageName,
                "wechat_dpi",
                "displaymetrics",
                stage,
                detail
            )
            return if (deferred) WechatDpiInstallOutcome.DEFERRED else WechatDpiInstallOutcome.SKIPPED
        }
        return if (installDensityHooks(xposed, locator, versionCode, phase)) {
            WechatDpiInstallOutcome.INSTALLED
        } else {
            WechatDpiInstallOutcome.SKIPPED
        }
    }

    private fun installDensityHooks(
        xposed: XposedInterface,
        locator: WechatDpiMethodLocator.Result,
        versionCode: Long,
        phase: WechatDpiInstallPhase,
    ): Boolean {
        val methods = locator.methods
        val classes = markUnhookedClasses(methods)
        if (classes.isEmpty()) return true
        return try {
            val capabilities = ModernApiCapabilitiesResolver.fromXposed(xposed)
            var installed = 0
            methods.filter { it.declaringClass in classes }.forEach { method ->
                method.isAccessible = true
                capabilities.applyStableHookId(
                    xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                    "$HOOK_ID_DENSITY_PREFIX${method.declaringClass.name}_${method.name}",
                ).intercept { chain ->
                    val result = chain.proceed()
                    logWechatDpiCallback(method, result, phase)
                    if (result is DisplayMetrics) {
                        applyWechatDpi(result, methodName(method), phase) ?: result
                    } else {
                        result
                    }
                }
                installed++
            }
            DpisLog.i(
                "modern WeChat DPI hook ready: ${methodNames(methods)}, installed=$installed, " +
                        "locator=${locator.source.logName}, versionCode=$versionCode, configuredDpi=" +
                        WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME),
            )
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME,
                "wechat_dpi",
                "displaymetrics",
                "hook_ready",
                "attempt=${phase.routeName}, installed=$installed, locator=${locator.source.logName}, versionCode=$versionCode",
            )
            installed > 0
        } catch (error: Throwable) {
            unmarkHookedClasses(classes)
            DpisLog.e(
                "modern WeChat DPI hook failed: ${methodNames(methods)}, versionCode=$versionCode, ${error.javaClass.name}: ${error.message}",
                error
            )
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME,
                "wechat_dpi",
                "displaymetrics",
                "skipped",
                "attempt=${phase.routeName}, hookFailed=true, versionCode=$versionCode, error=${error.javaClass.simpleName}",
            )
            false
        }
    }

    private fun markUnhookedClasses(methods: List<Method>): List<Class<*>> =
        synchronized(hookLock) {
            methods.map { it.declaringClass }.distinct()
                .filter { hookedDensityManagerClasses.add(it) }
        }

    private fun unmarkHookedClasses(classes: List<Class<*>>) = synchronized(hookLock) {
        hookedDensityManagerClasses.removeAll(classes.toSet())
    }

    private fun logRoutePlan(
        packageName: String,
        staticRoute: WechatDpiRoutes.Route?,
        locator: WechatDpiMethodLocator.Result,
        versionCode: Long,
        phase: WechatDpiInstallPhase,
    ) {
        val detail = "versionCode=$versionCode, locator=${locator.source.logName}, " +
                "class=${staticRoute?.className ?: "unknown"}, metricsTargets=${
                    routeTargetNames(
                        staticRoute
                    )
                }, " +
                "bottomTab=${staticRoute?.bottomTabIconScaleEnabled == true}, attempt=${phase.routeName}, " +
                "dexkitDeferred=${!phase.allowsDexKit}, retiredTargets=${retiredTargets(versionCode)}, retiredActive=false"
        DpisLog.i("modern WeChat DPI route plan: $detail")
        RuntimeHotPathEvents.event(
            packageName,
            "wechat_dpi",
            "displaymetrics",
            "config_resolved",
            detail
        )
    }

    private fun logWechatDpiCallback(method: Method, result: Any?, phase: WechatDpiInstallPhase) {
        if (!callbackLogged.compareAndSet(false, true)) return
        val detail =
            "attempt=${phase.routeName}, method=${methodName(method)}, firstCallbackMethod=${method.name}, " +
                    "result=${result?.javaClass?.name ?: "null"}, configuredDpi=${
                        WechatDpiPropertyBridge.readDpi(
                            WechatDpiConfig.PACKAGE_NAME
                        )
                    }"
        DpisLog.i("modern WeChat DPI callback hit: $detail")
        RuntimeHotPathEvents.event(
            WechatDpiConfig.PACKAGE_NAME,
            "wechat_dpi",
            "displaymetrics",
            "route_callback_entered",
            detail
        )
    }

    private fun applyWechatDpi(
        metrics: DisplayMetrics,
        methodName: String,
        phase: WechatDpiInstallPhase
    ): DisplayMetrics? {
        val targetDpi = WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME)
        val detached = WechatDpiRuntime.detached(metrics, targetDpi) ?: return null
        if (mutationLogged.compareAndSet(false, true)) {
            val detail =
                "attempt=${phase.routeName}, method=$methodName, targetDpi=$targetDpi, densityDpi=${metrics.densityDpi}->${detached.densityDpi}, density=${metrics.density}->${detached.density}"
            DpisLog.i("modern WeChat DPI applied: $detail")
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME,
                "wechat_dpi",
                "displaymetrics",
                "mutation_applied",
                detail
            )
        }
        return detached
    }

    @JvmStatic
    fun describeClassLoaderForLog(classLoader: ClassLoader?): String {
        if (classLoader == null) return "bootstrap"
        val text = runCatching { classLoader.toString() }.getOrElse { classLoader.javaClass.name }
        val shortened = if (text.length <= 240) text else text.substring(0, 237) + "..."
        return "${classLoader.javaClass.name}@${
            Integer.toHexString(
                System.identityHashCode(
                    classLoader
                )
            )
        }($shortened)"
    }

    private fun methodNames(methods: List<Method>): String =
        methods.joinToString("|", transform = ::methodName)

    private fun methodName(method: Method): String = "${method.declaringClass.name}#${method.name}"

    private fun routeTargetNames(route: WechatDpiRoutes.Route?): String =
        route?.densityMethodTargets?.mapNotNull { it.methodName.takeIf(String::isNotBlank) }
            ?.joinToString(",")?.takeIf(String::isNotEmpty) ?: "unknown"

    private fun retiredTargets(versionCode: Long): String =
        if (versionCode == 3120L) "g,k,l" else "none"

    private fun resolveWechatVersionCode(
        applicationInfo: ApplicationInfo?,
        packageName: String?
    ): Long {
        resolveWechatVersionCode(applicationInfo as Any?).takeIf { it > 0L }?.let { return it }
        resolveWechatVersionCode(resolveApplicationContext(), packageName).takeIf { it > 0L }
            ?.let { return it }
        return resolveWechatVersionCode(resolveSystemContext(), packageName)
    }

    private fun resolveWechatVersionCode(applicationInfo: Any?): Long = try {
        applicationInfo?.javaClass?.getField("longVersionCode")?.get(applicationInfo) as? Long ?: 0L
    } catch (_: Throwable) {
        0L
    }

    private fun resolveWechatVersionCode(context: Context?, packageName: String?): Long = try {
        if (context == null || packageName.isNullOrBlank()) return 0L
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (_: Throwable) {
        0L
    }

    private fun resolveApplicationContext(): Context? = try {
        val activityThread = Class.forName("android.app.ActivityThread")
        activityThread.getDeclaredMethod("currentApplication").invoke(null) as? Context
    } catch (_: Throwable) {
        null
    }

    private fun resolveSystemContext(): Context? = try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val thread =
            activityThread.getDeclaredMethod("currentActivityThread").invoke(null) ?: return null
        activityThread.getDeclaredMethod("getSystemContext").invoke(thread) as? Context
    } catch (_: Throwable) {
        null
    }
}
