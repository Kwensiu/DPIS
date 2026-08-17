package com.dpis.module

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.quirks.WechatDpiRuntime
import com.dpis.module.runtime.WechatDpiPropertyBridge
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reasserts the independent WeChat density when framework resource state is
 * recreated after the app has been backgrounded. It deliberately leaves
 * Configuration unchanged: independent DPI owns only DisplayMetrics.
 */
object WechatDpiResourceRecovery {
    private const val PACKAGE_NAME = WechatDpiConfig.PACKAGE_NAME
    private const val HISTORY_PREFIX = "DPIS_WECHAT_DPI_HISTORY"

    private val resourcesHookInstalled = AtomicBoolean(false)
    private val foregroundMonitorInstalled = AtomicBoolean(false)

    @JvmStatic
    fun installResourcesHook(
        xposed: XposedInterface,
        apiCapabilities: ModernApiCapabilities,
    ): Boolean {
        if (!resourcesHookInstalled.compareAndSet(false, true)) {
            return true
        }
        return try {
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val resourcesImplClass = Class.forName(
                "android.content.res.ResourcesImpl",
                false,
                bootClassLoader,
            )
            val compatibilityInfoClass = Class.forName(
                "android.content.res.CompatibilityInfo",
                false,
                bootClassLoader,
            )
            val method = resourcesImplClass.getDeclaredMethod(
                "updateConfiguration",
                android.content.res.Configuration::class.java,
                DisplayMetrics::class.java,
                compatibilityInfoClass,
            )
            apiCapabilities.applyStableHookId(
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "wechat_dpi_resources_recovery",
            ).intercept { chain ->
                recover(chain.getArg(1) as? DisplayMetrics, "ResourcesImpl.updateConfiguration")
                chain.proceed()
            }
            history("hook_ready", "source=ResourcesImpl.updateConfiguration")
            true
        } catch (throwable: Throwable) {
            resourcesHookInstalled.set(false)
            history(
                "hook_failed",
                "source=ResourcesImpl.updateConfiguration,error=${throwable.javaClass.simpleName}",
            )
            false
        }
    }

    @JvmStatic
    fun installForegroundMonitor(context: Context): Boolean {
        if (!foregroundMonitorInstalled.compareAndSet(false, true)) {
            return true
        }
        val application = context.applicationContext as? Application
        if (application == null) {
            foregroundMonitorInstalled.set(false)
            history("monitor_unavailable", "source=Application.attach,reason=no_application")
            return false
        }
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                recover(activity.resources?.displayMetrics, "Activity.onResume")
            }

            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        history("monitor_ready", "source=Application.attach")
        return true
    }

    @JvmStatic
    fun resetForHotReload() {
        resourcesHookInstalled.set(false)
        foregroundMonitorInstalled.set(false)
        synchronized(this) {
            lastState = null
        }
    }

    @JvmStatic
    private fun recover(metrics: DisplayMetrics?, source: String) {
        val targetDpi = WechatDpiPropertyBridge.readDpi(PACKAGE_NAME)
        val observedDpi = metrics?.densityDpi ?: 0
        when (WechatDpiRecoveryPolicy.decision(targetDpi, observedDpi)) {
            "config_missing" -> history("config_missing", "source=$source")
            "metrics_invalid" -> history("metrics_invalid", "source=$source,targetDpi=$targetDpi")
            "confirmed" -> history(
                "confirmed",
                "source=$source,targetDpi=$targetDpi,observedDpi=$observedDpi",
            )
            else -> {
                val recovered = WechatDpiRuntime.apply(metrics, targetDpi)
                history(
                    if (recovered) "reapplied" else "reapply_failed",
                    "source=$source,targetDpi=$targetDpi,observedDpi=$observedDpi"
                        + ",resultDpi=${metrics?.densityDpi ?: 0}",
                )
            }
        }
    }

    private var lastState: String? = null

    private fun history(stage: String, detail: String) {
        val state = "$stage|" + detail.split(',')
            .filterNot { it.startsWith("source=") }
            .joinToString(",")
        synchronized(this) {
            if (state == lastState) {
                return
            }
            lastState = state
        }
        DpisLog.routeHistory("$HISTORY_PREFIX package=$PACKAGE_NAME stage=$stage $detail")
    }
}
