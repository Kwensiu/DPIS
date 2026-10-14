package com.dpis.module

import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.lifecycle.XposedSelfActivation
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.atomic.AtomicBoolean

internal object LegacyXposedSelfActivation {
    private const val APPLICATION_CLASS_NAME = "com.dpis.module.DpisApplication"
    private const val MARK_METHOD_NAME = "markXposedSelfLoaded"
    private const val LEGACY_CONSTRUCTOR_MARKER_FIELD_NAME =
        "xposedSelfLoadedByLegacyConstructorHook"
    private val CONSTRUCTOR_HOOK_INSTALLED = AtomicBoolean(false)

    @JvmStatic
    fun markIfSelfPackage(packageName: String?, classLoader: ClassLoader?, source: String?) {
        if (BuildConfig.APPLICATION_ID != packageName || classLoader == null) return
        if (XposedSelfActivation.markIfSelfPackage(packageName, classLoader, source)) {
            XposedBridge.log("DPIS legacy xposed self activation marked: source=$source")
        }
        installConstructorMarker(classLoader, source)
    }

    @JvmStatic
    fun markIfSelfPackageForTest(
        packageName: String?,
        classLoader: ClassLoader?,
        source: String?,
    ) {
        markIfSelfPackage(packageName, classLoader, source)
    }

    private fun installConstructorMarker(classLoader: ClassLoader, source: String?) {
        if (!CONSTRUCTOR_HOOK_INSTALLED.compareAndSet(false, true)) return
        try {
            val applicationClass = classLoader.loadClass(APPLICATION_CLASS_NAME)
            XposedBridge.hookAllConstructors(applicationClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val marker = applicationClass.getDeclaredField(
                        LEGACY_CONSTRUCTOR_MARKER_FIELD_NAME,
                    ).apply { isAccessible = true }
                    marker.setBoolean(param.thisObject, true)
                    val mark = applicationClass.getDeclaredMethod(MARK_METHOD_NAME).apply {
                        isAccessible = true
                    }
                    mark.invoke(null)
                    XposedBridge.log(
                        "DPIS legacy xposed self activation constructor marked: " +
                                "source=$source, appClass=${applicationClass.name}",
                    )
                    DpisLog.i(
                        "legacy xposed self activation constructor marked: source=$source",
                    )
                }
            })
            XposedBridge.log(
                "DPIS legacy xposed self activation constructor hook ready: " +
                        "source=$source, appClass=${applicationClass.name}",
            )
            DpisLog.i("legacy xposed self activation constructor hook ready: source=$source")
        } catch (throwable: Throwable) {
            CONSTRUCTOR_HOOK_INSTALLED.set(false)
            XposedBridge.log(
                "DPIS legacy xposed self activation constructor hook failed: " +
                        "source=$source, error=$throwable",
            )
            DpisLog.e(
                "legacy xposed self activation constructor hook failed: source=$source",
                throwable,
            )
        }
    }
}
