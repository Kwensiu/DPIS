package com.dpis.module

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import com.dpis.module.config.ConfigSnapshot
import com.dpis.module.config.ConfigSnapshotLoader
import com.dpis.module.config.ModulePackagePlan
import com.dpis.module.diagnostics.RuntimeBridgeEvents
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.appprocess.DisplayHookInstaller
import com.dpis.module.runtime.appprocess.ResourcesImplHookInstaller
import com.dpis.module.runtime.appprocess.ResourcesManagerHookInstaller
import com.dpis.module.runtime.appprocess.ResourcesReadHookInstaller
import com.dpis.module.runtime.appprocess.WindowFrameOverride
import com.dpis.module.runtime.font.PaintTextSizeFallbackHookInstaller
import com.dpis.module.runtime.systemserver.SystemServerProcess
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Suppress("unused", "java:S1872")
class LegacyModuleHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        DpisLog.setLoggingEnabled(LegacyConfigStoreFactory.create().isGlobalLogEnabled())
        compatDebugLog("legacy initZygote")
        installSystemServerHooksForLegacy()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName: String? = lpparam.packageName
        // The Legacy APK uses the traditional Xposed entrypoint, not a distinct API 100 path.
        LegacyXposedSelfActivation.markIfSelfPackage(
            packageName,
            lpparam.classLoader,
            "legacy-handle-load-package"
        )
        if (SystemServerProcess.isSystemServer(lpparam.processName, packageName)) {
            DpisLog.setLoggingEnabled(LegacyConfigStoreFactory.create().isGlobalLogEnabled())
            compatDebugLog(
                ("legacy handleLoadPackage: package=" + packageName
                        + ", process=" + lpparam.processName)
            )
            installSystemServerHooksForLegacy()
            return
        }
        val store: DpisConfigStore = createLegacyStore(
            packageName,
            lpparam.processName
        )
        DpisLog.setLoggingEnabled(store.isGlobalLogEnabled())
        compatDebugLog(
            ("legacy handleLoadPackage: package=" + packageName
                    + ", process=" + lpparam.processName)
        )
        emitDiagnosticSessionDiscovery(
            packageName,
            lpparam.processName
        )
        if (LegacyAppSpecificRouteInstaller.handleLoadPackage(lpparam)) {
            return
        }
        val snapshot: ConfigSnapshot = ConfigSnapshotLoader.fromStore(store)
        var plan: ModulePackagePlan = ModulePackagePlan.resolve(snapshot, packageName)
        if (!plan.shouldInstallLegacyHooks()) {
            compatDebugLog(
                ("legacy package skipped: package=" + packageName
                        + ", configuredPackages=" + snapshot.getConfiguredPackages())
            )
            return
        }
        if (shouldSuppressSecondaryProcessViewport(
                lpparam.processName,
                plan
            )
        ) {
            compatDebugLog(
                ("legacy secondary process viewport route suppressed: process="
                        + lpparam.processName + ", package=" + packageName
                        + ", viewportMode=" + plan.targetViewportMode)
            )
            plan = plan.withoutViewportRoute()
            if (!plan.shouldInstallLegacyHooks()) {
                compatDebugLog(
                    ("legacy package skipped after secondary process"
                            + " viewport suppression: package=" + packageName
                            + ", process=" + lpparam.processName)
                )
                return
            }
        }
        compatDebugLog(
            ("legacy package matched: package=" + packageName
                    + ", targetViewportSpec=" + plan.targetViewportSpec
                    + ", targetFontScalePercent=" + plan.targetFontScalePercent
                    + ", targetTypefaceId=" + plan.targetTypefaceId)
        )
        val resourceHooksNeeded = plan.viewportEnabled
                || (plan.fontScaleActive && FontApplyMode.isEnabled(plan.targetFontMode))
        if (resourceHooksNeeded) {
            installResourcesImplHook(packageName, store)
            installResourcesManagerHook(
                packageName,
                store
            )
            installResourcesReadHooks(packageName, store)
        }
        if (plan.typefaceEnabled) {
            installTypefaceOverrideHook(
                packageName,
                plan.targetTypefaceId,
                store
            )
        }
        if (plan.viewportEnabled) {
            installDisplayHooks(packageName, store)
            installWindowMetricsHook()
        }
        if (FontApplyMode.FIELD_REWRITE.equals(FontApplyMode.normalize(plan.targetFontMode))) {
            installFontFieldRewriteHooks(
                packageName,
                store
            )
        }
    }

    companion object {
        private val RESOURCES_IMPL_HOOKED: AtomicBoolean = AtomicBoolean(false)
        private val RESOURCES_MANAGER_HOOKED: AtomicBoolean = AtomicBoolean(false)
        private val RESOURCES_READ_HOOKED: AtomicBoolean = AtomicBoolean(false)
        private val DISPLAY_HOOKED: AtomicBoolean = AtomicBoolean(false)
        private val WINDOW_METRICS_HOOKED: AtomicBoolean = AtomicBoolean(false)
        private val FONT_FIELD_REWRITE_HOOKED: AtomicBoolean = AtomicBoolean(false)
        private val FONT_TEXTVIEW_UPDATE: ThreadLocal<Boolean?> =
            ThreadLocal.withInitial({ false })
        private val RESOURCES_READ_INTERNAL_UPDATE: ThreadLocal<Boolean?> =
            ThreadLocal.withInitial({ false })
        private val CURRENT_PACKAGE_METHOD: AtomicReference<Method?> = AtomicReference()
        private val LEGACY_HOST_STORE_CACHE: MutableMap<String?, DpisConfigStore?> =
            ConcurrentHashMap()

        private fun createLegacyStore(packageName: String?, processName: String?): DpisConfigStore {
            if (packageName != null && packageName.equals(processName)) {
                return LegacyConfigStoreFactory.createMainProcess(packageName)
            }
            return LegacyConfigStoreFactory.create(packageName)
        }

        private fun shouldSuppressSecondaryProcessViewport(
            processName: String?,
            plan: ModulePackagePlan?
        ): Boolean {
            if (processName == null || processName.isBlank() || plan == null || plan.packageName == null || plan.packageName.isBlank()) {
                return false
            }
            return !processName.equals(plan.packageName) && !processName.startsWith(plan.packageName + ":") && plan.viewportEnabled
        }

        private fun installSystemServerHooksForLegacy() {
            try {
                LegacySystemServerHookInstaller.install()
                compatDebugLog("legacy system_server hooks ready")
            } catch (throwable: Throwable) {
                compatErrorLog(
                    ("legacy system_server hooks failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun installTypefaceOverrideHook(
            packageName: String?,
            targetTypefaceId: String?,
            store: DpisConfigStore?
        ) {
            try {
                LegacyTypefaceOverrideHookInstaller.install(
                    packageName,
                    targetTypefaceId,
                    store,
                    LegacyConfigStoreFactory.createFontLibrary()
                )
            } catch (throwable: Throwable) {
                compatErrorLog(
                    ("legacy typeface override hook failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun installResourcesImplHook(packageName: String?, store: DpisConfigStore?) {
            if (!RESOURCES_IMPL_HOOKED.compareAndSet(
                    false,
                    true
                )
            ) {
                return
            }
            try {
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val resourcesImplClass: Class<*> = Class.forName(
                    "android.content.res.ResourcesImpl", false, bootClassLoader
                )
                val compatibilityInfoClass: Class<*>? = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader
                )
                val method: Method? = resourcesImplClass.getDeclaredMethod(
                    "updateConfiguration",
                    Configuration::class.java,
                    DisplayMetrics::class.java,
                    compatibilityInfoClass
                )
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        ResourcesImplHookInstaller.applyDensityOverride(
                            packageName,
                            param.args[0] as Configuration?,
                            param.args[1] as DisplayMetrics?,
                            store
                        )
                    }
                })
                compatDebugLog("legacy ResourcesImpl hook ready")
            } catch (throwable: Throwable) {
                RESOURCES_IMPL_HOOKED.set(false)
                compatErrorLog(
                    ("legacy ResourcesImpl hook failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun installResourcesManagerHook(packageName: String?, store: DpisConfigStore?) {
            if (!RESOURCES_MANAGER_HOOKED.compareAndSet(
                    false,
                    true
                )
            ) {
                return
            }
            try {
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val resourcesManagerClass: Class<*> = Class.forName(
                    "android.app.ResourcesManager", false, bootClassLoader
                )
                val compatibilityInfoClass: Class<*>? = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader
                )
                val method: Method? = resourcesManagerClass.getDeclaredMethod(
                    "applyConfigurationToResources",
                    Configuration::class.java,
                    compatibilityInfoClass
                )
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        applyResourceOverrides(
                            param.args[0] as Configuration?,
                            store,
                            packageName,
                            "LegacyResourcesManager"
                        )
                    }
                })
                // legacy keeps viewport mutations on stable resource/read/display
                // boundaries. ResourcesManager activity/create/key hooks can reapply
                // per-activity configuration during navigation and drift from the
                // system_server launch configuration, so they stay disabled here.
                val activityHookCount = 0
                val createHookCount = 0
                val keyHookCount = 0
                compatDebugLog(
                    ("legacy ResourcesManager hook ready"
                            + " (activityHooks=" + activityHookCount
                            + ", createHooks=" + createHookCount
                            + ", keyHooks=" + keyHookCount + ")")
                )
            } catch (throwable: Throwable) {
                RESOURCES_MANAGER_HOOKED.set(false)
                compatErrorLog(
                    ("legacy ResourcesManager hook failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun installUpdateResourcesForActivityHook(
            resourcesManagerClass: Class<*>,
            packageName: String?,
            store: DpisConfigStore?
        ): Int {
            try {
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val iBinderClass: Class<*>? =
                    Class.forName("android.os.IBinder", false, bootClassLoader)
                val method: Method? = resourcesManagerClass.getDeclaredMethod(
                    "updateResourcesForActivity",
                    iBinderClass,
                    Configuration::class.java,
                    Int::class.javaPrimitiveType
                )
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        applyResourceOverrides(
                            param.args[1] as Configuration?,
                            store,
                            packageName,
                            "LegacyResourcesManagerActivity"
                        )
                    }
                })
                return 1
            } catch (throwable: Throwable) {
                compatErrorLog(
                    ("legacy ResourcesManager activity hook failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
                return 0
            }
        }

        private fun installResourceCreationHooks(
            resourcesManagerClass: Class<*>,
            packageName: String?,
            store: DpisConfigStore?
        ): Int {
            var hookedCount = 0
            val hookedMethods: MutableSet<Method?> = HashSet<Method?>()
            for (method in resourcesManagerClass.declaredMethods) {
                val configArgIndex: Int =
                    findConfigurationArgIndex(method)
                if (configArgIndex < 0 || !isResourceCreationMethod(
                        method.name
                    )
                ) {
                    continue
                }
                if (!hookedMethods.add(method)) {
                    continue
                }
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        applyResourceOverrides(
                            param.args[configArgIndex] as Configuration?,
                            store,
                            packageName,
                            "LegacyResourcesManagerCreate(" + method.name + ")"
                        )
                    }
                })
                hookedCount++
            }
            return hookedCount
        }

        private fun installResourcesKeyHooks(
            resourcesManagerClass: Class<*>,
            packageName: String?,
            store: DpisConfigStore?
        ): Int {
            var hookedCount = 0
            val hookedMethods: MutableSet<Method?> = HashSet<Method?>()
            for (method in resourcesManagerClass.declaredMethods) {
                val methodName: String? = method.name
                if (!"createResourcesImpl".equals(methodName) || !hasResourcesKeyFirstArg(
                        method
                    ) || !hookedMethods.add(method)
                ) {
                    continue
                }
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                            param.thisObject,
                            param.args[0],
                            store,
                            packageName,
                            "LegacyResourcesManagerKey(" + methodName + ")"
                        )
                    }
                })
                hookedCount++
            }
            return hookedCount
        }

        private fun findConfigurationArgIndex(method: Method): Int {
            val parameterTypes: Array<Class<*>?> = method.parameterTypes
            for (i in parameterTypes.indices) {
                if (Configuration::class.java.equals(parameterTypes[i])) {
                    return i
                }
            }
            return -1
        }

        private fun isResourceCreationMethod(methodName: String?): Boolean {
            return methodName != null
                    && (methodName.contains("createResources")
                    || methodName.contains("getOrCreateResources")
                    || methodName.contains("createBaseTokenResources"))
        }

        private fun hasResourcesKeyFirstArg(method: Method): Boolean {
            val parameterTypes: Array<Class<*>?> = method.parameterTypes
            return parameterTypes.size > 0
                    && "android.content.res.ResourcesKey" == parameterTypes[0]?.name
        }

        private fun installResourcesReadHooks(packageName: String?, store: DpisConfigStore?) {
            if (!RESOURCES_READ_HOOKED.compareAndSet(
                    false,
                    true
                )
            ) {
                return
            }
            try {
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val resourcesClass: Class<*> = Class.forName(
                    "android.content.res.Resources", false, bootClassLoader
                )

                val getConfigurationMethod: Method? =
                    resourcesClass.getDeclaredMethod("getConfiguration")
                XposedBridge.hookMethod(getConfigurationMethod, object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val result: Any? = param.result
                        if (result is Configuration) {
                            val activePackage: String? =
                                resolveActivePackageName(
                                    packageName
                                )
                            val activeStore: DpisConfigStore? =
                                resolveStoreForPackage(
                                    activePackage,
                                    store
                                )
                            ResourcesReadHookInstaller.applyConfigurationOverride(
                                result, activePackage, activeStore ?: return,
                                "LegacyResourcesRead(getConfiguration)"
                            )
                        }
                    }
                })

                val getDisplayMetricsMethod: Method? =
                    resourcesClass.getDeclaredMethod("getDisplayMetrics")
                XposedBridge.hookMethod(getDisplayMetricsMethod, object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val result: Any? = param.result
                        val thisObject: Any? = param.thisObject
                        if (result !is DisplayMetrics || thisObject !is Resources) {
                            return
                        }
                        if (RESOURCES_READ_INTERNAL_UPDATE.get() == true) {
                            return
                        }
                        RESOURCES_READ_INTERNAL_UPDATE.set(
                            true
                        )
                        try {
                            val config: Configuration? = thisObject.configuration
                            val activePackage: String? =
                                resolveActivePackageName(
                                    packageName
                                )
                            ResourcesReadHookInstaller.applyMetricsOverride(
                                result,
                                config,
                                activePackage
                            )
                        } finally {
                            RESOURCES_READ_INTERNAL_UPDATE.remove()
                        }
                    }
                })

                val getSystemMethod: Method? = resourcesClass.getDeclaredMethod("getSystem")
                XposedBridge.hookMethod(getSystemMethod, object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val result: Any? = param.result
                        if (result !is Resources) {
                            return
                        }
                        RESOURCES_READ_INTERNAL_UPDATE.set(
                            true
                        )
                        try {
                            val config: Configuration? = result.configuration
                            val activePackage: String? =
                                resolveActivePackageName(
                                    packageName
                                )
                            val activeStore: DpisConfigStore? =
                                resolveStoreForPackage(
                                    activePackage,
                                    store
                                )
                            ResourcesReadHookInstaller.applyConfigurationOverride(
                                config, activePackage, activeStore ?: return,
                                "LegacyResourcesRead(getSystem)"
                            )
                            ResourcesReadHookInstaller.applyMetricsOverride(
                                result.displayMetrics, config, activePackage
                            )
                        } finally {
                            RESOURCES_READ_INTERNAL_UPDATE.remove()
                        }
                    }
                })

                compatDebugLog("legacy Resources read hook ready")
            } catch (throwable: Throwable) {
                RESOURCES_READ_HOOKED.set(false)
                compatErrorLog(
                    ("legacy Resources read hook failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun installDisplayHooks(packageName: String?, store: DpisConfigStore?) {
            DisplayHookInstaller.setTargetPackageNameForLegacy(packageName)
            DisplayHookInstaller.setTargetStoreForLegacy(store)
            if (!DISPLAY_HOOKED.compareAndSet(
                    false,
                    true
                )
            ) {
                return
            }
            try {
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val displayClass: Class<*> =
                    Class.forName("android.view.Display", false, bootClassLoader)
                hookDisplayMetricsMethod(
                    displayClass,
                    "getMetrics"
                )
                hookDisplayMetricsMethod(
                    displayClass,
                    "getRealMetrics"
                )
                hookDisplayPointMethod(
                    displayClass,
                    "getSize"
                )
                hookDisplayPointMethod(
                    displayClass,
                    "getRealSize"
                )
                hookDisplayInfoMethod(
                    displayClass,
                    bootClassLoader
                )
                compatDebugLog("legacy Display hooks ready for " + packageName)
            } catch (throwable: Throwable) {
                DISPLAY_HOOKED.set(false)
                compatErrorLog(
                    ("legacy Display hooks failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        @Throws(ReflectiveOperationException::class)
        private fun hookDisplayMetricsMethod(displayClass: Class<*>, methodName: String?) {
            val method: Method? =
                displayClass.getDeclaredMethod(methodName, DisplayMetrics::class.java)
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    DisplayHookInstaller.applyDisplayMetrics(
                        param.args[0] as DisplayMetrics?,
                        methodName ?: return
                    )
                }
            })
        }

        @Throws(ReflectiveOperationException::class)
        private fun hookDisplayPointMethod(displayClass: Class<*>, methodName: String?) {
            val method: Method? = displayClass.getDeclaredMethod(methodName, Point::class.java)
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    DisplayHookInstaller.applyPoint(param.args[0] as Point?, methodName ?: return)
                }
            })
        }

        private fun hookDisplayInfoMethod(displayClass: Class<*>, bootClassLoader: ClassLoader?) {
            try {
                val displayInfoClass: Class<*>? =
                    Class.forName("android.view.DisplayInfo", false, bootClassLoader)
                val method: Method? =
                    displayClass.getDeclaredMethod("getDisplayInfo", displayInfoClass)
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        DisplayHookInstaller.applyDisplayInfo(param.args[0], "getDisplayInfo")
                    }
                })
            } catch (ignored: Throwable) {
                compatDebugLog("legacy Display getDisplayInfo hook skipped")
            }
        }

        private fun installWindowMetricsHook() {
            if (!WINDOW_METRICS_HOOKED.compareAndSet(
                    false,
                    true
                )
            ) {
                return
            }
            try {
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val windowMetricsClass: Class<*> = Class.forName(
                    "android.view.WindowMetrics", false, bootClassLoader
                )
                val method: Method? = windowMetricsClass.getDeclaredMethod("getBounds")
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val result: Any? = param.result
                        if (result !is Rect || !WindowFrameOverride.isEnabled()) {
                            return
                        }
                        val override: VirtualDisplayOverride.Result? = VirtualDisplayState.get()
                        if (override == null) {
                            return
                        }
                        val newRect: Rect = Rect(
                            result.left, result.top,
                            result.left + override.widthPx, result.top + override.heightPx
                        )
                        param.setResult(newRect)
                    }
                })
                compatDebugLog("legacy WindowMetrics hook ready")
            } catch (throwable: Throwable) {
                WINDOW_METRICS_HOOKED.set(false)
                compatErrorLog(
                    ("legacy WindowMetrics hook failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun installFontFieldRewriteHooks(packageName: String?, store: DpisConfigStore?) {
            if (!FONT_FIELD_REWRITE_HOOKED.compareAndSet(
                    false,
                    true
                )
            ) {
                return
            }
            try {
                val factor: Float =
                    PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor(
                        store ?: return,
                        packageName ?: return
                    )
                if (factor <= 0f || factor == 1.0f) {
                    FONT_FIELD_REWRITE_HOOKED.set(false)
                    return
                }
                val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
                val textViewClass: Class<*> =
                    Class.forName("android.widget.TextView", false, bootClassLoader)
                val setTextSizeWithUnit: Method? = textViewClass.getDeclaredMethod(
                    "setTextSize", Int::class.javaPrimitiveType, Float::class.javaPrimitiveType
                )
                XposedBridge.hookMethod(setTextSizeWithUnit, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        if (FONT_TEXTVIEW_UPDATE.get() == true) {
                            return
                        }
                        val unit: Int = (param.args[0] as Integer).toInt()
                        if (shouldScaleTextUnit(unit)) {
                            param.args[1] = (param.args[1] as Float?)!! * factor
                            FONT_TEXTVIEW_UPDATE.set(
                                true
                            )
                        }
                    }

                    protected override fun afterHookedMethod(param: MethodHookParam?) {
                        FONT_TEXTVIEW_UPDATE.remove()
                    }
                })
                val setTextSizeSp: Method? =
                    textViewClass.getDeclaredMethod("setTextSize", Float::class.javaPrimitiveType)
                XposedBridge.hookMethod(setTextSizeSp, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        if (FONT_TEXTVIEW_UPDATE.get() == true) {
                            return
                        }
                        // Android's one-argument TextView#setTextSize delegates to the
                        // two-argument overload on AOSP. The shared guard keeps legacy
                        // field rewrite to one scale pass across that nested call chain.
                        param.args[0] = (param.args[0] as Float?)!! * factor
                        FONT_TEXTVIEW_UPDATE.set(true)
                    }

                    protected override fun afterHookedMethod(param: MethodHookParam?) {
                        FONT_TEXTVIEW_UPDATE.remove()
                    }
                })
                val paintSetTextSize: Method? = Paint::class.java.getDeclaredMethod(
                    "setTextSize",
                    Float::class.javaPrimitiveType
                )
                XposedBridge.hookMethod(paintSetTextSize, object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        if (FONT_TEXTVIEW_UPDATE.get() == true) {
                            return
                        }
                        param.args[0] = (param.args[0] as Float?)!! * factor
                    }
                })
                compatDebugLog("legacy font field rewrite hooks ready: factor=" + factor)
            } catch (throwable: Throwable) {
                FONT_FIELD_REWRITE_HOOKED.set(false)
                compatErrorLog(
                    ("legacy font field rewrite hooks failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun shouldScaleTextUnit(unit: Int): Boolean {
            return unit == TypedValue.COMPLEX_UNIT_SP || unit == TypedValue.COMPLEX_UNIT_PX || unit == TypedValue.COMPLEX_UNIT_DIP || unit == TypedValue.COMPLEX_UNIT_PT || unit == TypedValue.COMPLEX_UNIT_IN || unit == TypedValue.COMPLEX_UNIT_MM
        }

        private fun applyResourceOverrides(
            config: Configuration?,
            store: DpisConfigStore?,
            packageName: String?,
            sourceTag: String?
        ) {
            try {
                ResourcesManagerHookInstaller.applyResourceOverrides(
                    config,
                    store,
                    packageName,
                    sourceTag ?: ""
                )
            } catch (throwable: Throwable) {
                compatErrorLog(
                    ("legacy resource override failed: "
                            + throwable.javaClass.name + ": " + throwable.message)
                )
            }
        }

        private fun resolveActivePackageName(fallbackPackageName: String?): String? {
            try {
                var cached: Method? =
                    CURRENT_PACKAGE_METHOD.get()
                if (cached == null) {
                    val activityThreadClass: Class<*> = Class.forName("android.app.ActivityThread")
                    var method: Method = activityThreadClass.getDeclaredMethod("currentPackageName")
                    method.isAccessible = true
                    if (!CURRENT_PACKAGE_METHOD.compareAndSet(
                            null,
                            method
                        )
                    ) {
                        method =
                            CURRENT_PACKAGE_METHOD.get()!!
                    }
                    cached = method
                }
                if (cached != null) {
                    val value: Any? = cached.invoke(null)
                    if (value is String && !value.isBlank()) {
                        return value
                    }
                }
            } catch (ignored: Throwable) {
            }
            return fallbackPackageName
        }

        private fun resolveStoreForPackage(
            packageName: String?,
            fallbackStore: DpisConfigStore?
        ): DpisConfigStore? {
            if (packageName == null || packageName.isBlank()) {
                return fallbackStore
            }
            try {
                return LEGACY_HOST_STORE_CACHE.computeIfAbsent(
                    packageName, LegacyConfigStoreFactory::create
                )
            } catch (ignored: Throwable) {
                return fallbackStore
            }
        }

        private fun compatLog(message: String?) {
            DpisLog.i(message)
        }

        private fun compatDebugLog(message: String?) {
            if (!DpisLog.isLoggingEnabled()) {
                return
            }
            compatLog(message)
        }

        private fun compatErrorLog(message: String?) {
            try {
                XposedBridge.log("DPIS " + message)
            } catch (ignored: Throwable) {
            }
            try {
                Log.e(DpisLog.TAG, message ?: "")
            } catch (ignored: Throwable) {
            }
        }

        private fun emitDiagnosticSessionDiscovery(packageName: String?, processName: String?) {
            RuntimeBridgeEvents.setBridgeSink(XposedBridge::log)
            RuntimeBridgeEvents.emitSessionDiscovery(packageName, processName)
        }
    }
}
