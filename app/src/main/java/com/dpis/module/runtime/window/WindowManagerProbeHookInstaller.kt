package com.dpis.module.runtime.window

import android.graphics.Rect
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.probe.CallerTrace
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object WindowManagerProbeHookInstaller {
    @Volatile
    private var targetPackageName: String? = null
    @Volatile
    private var installedPid = -1

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface, packageName: String?) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) return
        synchronized(this) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) return
            targetPackageName = packageName
            for (windowManagerClass in resolveWindowManagerClasses(ClassLoader.getSystemClassLoader())) {
                hookProbeMethod(xposed, windowManagerClass, "getCurrentWindowMetrics")
                hookProbeMethod(xposed, windowManagerClass, "getMaximumWindowMetrics")
            }
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("WindowManager probe hook ready")
        }
    }

    @Throws(ReflectiveOperationException::class)
    private fun resolveWindowManagerClasses(classLoader: ClassLoader): Set<Class<*>> = linkedSetOf(
        Class.forName("android.view.WindowManagerImpl", false, classLoader),
        Class.forName("android.view.WindowManager", false, classLoader)
    )

    @JvmStatic
    fun buildProbeLog(methodName: String, result: Any?): String =
        if (result == null) buildProbeLog(methodName, "null", null)
        else buildProbeLog(methodName, result.javaClass.name, extractBoundsSummary(result))

    @JvmStatic
    fun buildProbeLog(methodName: String, resultType: String, boundsSummary: String?): String {
        if (boundsSummary.isNullOrEmpty()) return "WindowManager probe($methodName): result=$resultType"
        return "WindowManager probe($methodName): result=$resultType, bounds=$boundsSummary"
    }

    private fun extractBoundsSummary(result: Any?): String? {
        if (result == null) return null
        return try {
            val bounds = result.javaClass.getMethod("getBounds").invoke(result)
            if (bounds is Rect) "${bounds.width()}x${bounds.height()}" else null
        } catch (_: NoSuchMethodException) {
            null
        } catch (_: IllegalAccessException) {
            null
        } catch (_: InvocationTargetException) {
            null
        }
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookProbeMethod(
        xposed: XposedInterface,
        windowManagerClass: Class<*>,
        methodName: String
    ) {
        val method: Method = windowManagerClass.getMethod(methodName)
        xposed.hook(method)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                val result = chain.proceed()
                DpisLog.i(appendCaller(buildProbeLog(methodName, result)))
                result
            }
    }

    private fun appendCaller(message: String): String {
        val caller = CallerTrace.capture(targetPackageName) ?: return message
        return "$message, caller=$caller"
    }
}
