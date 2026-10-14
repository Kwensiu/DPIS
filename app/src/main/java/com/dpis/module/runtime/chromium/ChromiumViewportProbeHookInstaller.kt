package com.dpis.module.runtime.chromium

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.ProcessScopedInstallGate
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.atomic.AtomicInteger

object ChromiumViewportProbeHookInstaller {
    private const val MAX_LOGS = 12
    private val logCount = AtomicInteger()
    @Volatile
    private var installedPid = -1

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
        logCount.set(0)
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface?, classLoader: ClassLoader?) {
        if (xposed == null || classLoader == null ||
            ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)
        ) return
        synchronized(this) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) return
            hookResourceManagerCreate(xposed, classLoader)
            hookWindowAndroidConstructors(xposed, classLoader)
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("DPIS_WEBAPK Chromium viewport probe ready")
        }
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookResourceManagerCreate(xposed: XposedInterface, classLoader: ClassLoader) {
        val resourceManagerClass =
            Class.forName("org.chromium.ui.resources.ResourceManager", false, classLoader)
        val windowAndroidClass =
            Class.forName("org.chromium.ui.base.WindowAndroid", false, classLoader)
        val create = resourceManagerClass.getDeclaredMethod(
            "create",
            windowAndroidClass,
            Long::class.javaPrimitiveType
        )
        xposed.hook(create)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                logContext("ResourceManager.create", contextFromWindowAndroid(chain.getArg(0)))
                chain.proceed()
            }
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookWindowAndroidConstructors(xposed: XposedInterface, classLoader: ClassLoader) {
        val windowAndroidClass =
            Class.forName("org.chromium.ui.base.WindowAndroid", false, classLoader)
        for (constructor in windowAndroidClass.declaredConstructors) {
            val parameterTypes = constructor.parameterTypes
            if (parameterTypes.isEmpty() || parameterTypes[0] != Context::class.java) continue
            xposed.hook(constructor)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    logContext("WindowAndroid.<init>", chain.getArg(0) as? Context)
                    chain.proceed()
                }
        }
    }

    private fun contextFromWindowAndroid(windowAndroid: Any?): Context? {
        val holder = readField(windowAndroid, "W") ?: return null
        return try {
            holder.javaClass.getMethod("get").invoke(holder) as? Context
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun readField(target: Any?, fieldName: String?): Any? {
        if (target == null || fieldName == null) return null
        return try {
            target.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.get(target)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun logContext(source: String, context: Context?) {
        if (context == null || logCount.incrementAndGet() > MAX_LOGS) return
        val resources: Resources = context.resources
        val config: Configuration = resources.configuration
        val metrics: DisplayMetrics = resources.displayMetrics
        DpisLog.i(
            "DPIS_WEBAPK Chromium viewport probe: source=$source"
                    + ", context=${context.javaClass.name}"
                    + ", widthDp=${config.screenWidthDp}"
                    + ", heightDp=${config.screenHeightDp}"
                    + ", smallestWidthDp=${config.smallestScreenWidthDp}"
                    + ", densityDpi=${config.densityDpi}"
                    + ", metricsDensityDpi=${metrics.densityDpi}"
                    + ", metricsDensity=${metrics.density}"
                    + ", metricsWidthPx=${metrics.widthPixels}"
                    + ", metricsHeightPx=${metrics.heightPixels}"
        )
    }
}
