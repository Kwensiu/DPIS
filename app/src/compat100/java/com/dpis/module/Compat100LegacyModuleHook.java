package com.dpis.module;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("unused")
public final class Compat100LegacyModuleHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final AtomicBoolean RESOURCES_IMPL_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean RESOURCES_MANAGER_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean RESOURCES_READ_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean DISPLAY_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean WINDOW_METRICS_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean FONT_FIELD_REWRITE_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_TARGET_FIELD_HOOKED = new AtomicBoolean(false);
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final String WECHAT_TARGET_FIELD_PROPERTY =
            "debug.dpis.wechat.targetfield.c5fe9776";
    private static final int WECHAT_TARGET_FIELD_MIN = 300;
    private static final int WECHAT_TARGET_FIELD_MAX = 1200;
    private static final ThreadLocal<Boolean> FONT_TEXTVIEW_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> RESOURCES_READ_INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final AtomicReference<Method> CURRENT_PACKAGE_METHOD = new AtomicReference<>();
    private static final Map<String, DpiConfigStore> COMPAT100_HOST_STORE_CACHE =
            new ConcurrentHashMap<>();

    @Override
    public void initZygote(StartupParam startupParam) {
        DpisLog.setLoggingEnabled(ConfigStoreFactory.createForCompat100Host().isGlobalLogEnabled());
        compatDebugLog("compat100 legacy initZygote");
        installSystemServerHooksForCompat100();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String packageName = lpparam.packageName;
        if (SystemServerProcess.isSystemServer(lpparam.processName, packageName)) {
            DpisLog.setLoggingEnabled(ConfigStoreFactory.createForCompat100Host().isGlobalLogEnabled());
            compatDebugLog("compat100 legacy handleLoadPackage: package=" + packageName
                    + ", process=" + lpparam.processName);
            installSystemServerHooksForCompat100();
            return;
        }
        DpiConfigStore store = createCompat100Store(packageName, lpparam.processName);
        DpisLog.setLoggingEnabled(store.isGlobalLogEnabled());
        compatDebugLog("compat100 legacy handleLoadPackage: package=" + packageName
                + ", process=" + lpparam.processName);
        if (WECHAT_PACKAGE.equals(packageName)) {
            if (WECHAT_PACKAGE.equals(lpparam.processName)) {
                installWechatTargetFieldHooks(lpparam.classLoader);
            }
            compatDebugLog("compat100 WeChat target-field route suppresses generic hooks: process="
                    + lpparam.processName);
            return;
        }
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        ModulePackagePlan plan = ModulePackagePlan.resolve(snapshot, packageName);
        if (!plan.shouldInstallCompat100LegacyHooks()) {
            compatDebugLog("compat100 legacy package skipped: package=" + packageName
                    + ", configuredPackages=" + snapshot.getConfiguredPackages());
            return;
        }
        if (shouldSuppressSecondaryProcessViewport(lpparam.processName, plan)) {
            compatDebugLog("compat100 legacy secondary process viewport route suppressed: process="
                    + lpparam.processName + ", package=" + packageName
                    + ", viewportMode=" + plan.targetViewportMode);
            plan = plan.withoutViewportRoute();
            if (!plan.shouldInstallCompat100LegacyHooks()) {
                compatDebugLog("compat100 legacy package skipped after secondary process"
                        + " viewport suppression: package=" + packageName
                        + ", process=" + lpparam.processName);
                return;
            }
        }
        compatDebugLog("compat100 legacy package matched: package=" + packageName
                + ", targetViewportSpec=" + plan.targetViewportSpec
                + ", targetFontScalePercent=" + plan.targetFontScalePercent
                + ", targetTypefaceId=" + plan.targetTypefaceId);
        boolean resourceHooksNeeded = plan.viewportEnabled
                || (plan.fontScaleActive && FontApplyMode.isEnabled(plan.targetFontMode));
        if (resourceHooksNeeded) {
            installResourcesImplHook(packageName, store);
            installResourcesManagerHook(packageName, store);
            installResourcesReadHooks(packageName, store);
        }
        if (plan.typefaceEnabled) {
            installTypefaceOverrideHook(packageName, plan.targetTypefaceId, store);
        }
        if (plan.viewportEnabled) {
            installDisplayHooks(packageName, store);
            installWindowMetricsHook();
        }
        if (FontApplyMode.FIELD_REWRITE.equals(FontApplyMode.normalize(plan.targetFontMode))) {
            installFontFieldRewriteHooks(packageName, store);
        }
    }

    private static DpiConfigStore createCompat100Store(String packageName, String processName) {
        if (packageName != null && packageName.equals(processName)) {
            return ConfigStoreFactory.createForCompat100MainProcessHost(packageName);
        }
        return ConfigStoreFactory.createForCompat100Host(packageName);
    }

    private static boolean shouldSuppressSecondaryProcessViewport(String processName,
                                                                  ModulePackagePlan plan) {
        if (processName == null || processName.isBlank() || plan == null
                || plan.packageName == null || plan.packageName.isBlank()) {
            return false;
        }
        return !processName.equals(plan.packageName)
                && !processName.startsWith(plan.packageName + ":")
                && plan.viewportEnabled;
    }

    private static void installSystemServerHooksForCompat100() {
        try {
            Compat100SystemServerHookInstaller.install();
            compatDebugLog("compat100 legacy system_server hooks ready");
        } catch (Throwable throwable) {
            compatErrorLog("compat100 legacy system_server hooks failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void installTypefaceOverrideHook(String packageName,
                                                    String targetTypefaceId,
                                                    DpiConfigStore store) {
        try {
            Compat100TypefaceOverrideHookInstaller.install(
                    packageName,
                    targetTypefaceId,
                    store,
                    ConfigStoreFactory.createFontLibraryForCompat100Host());
        } catch (Throwable throwable) {
            compatErrorLog("compat100 legacy typeface override hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void installResourcesImplHook(String packageName, DpiConfigStore store) {
        if (!RESOURCES_IMPL_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesImplClass = Class.forName(
                    "android.content.res.ResourcesImpl", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method method = resourcesImplClass.getDeclaredMethod(
                    "updateConfiguration",
                    Configuration.class,
                    DisplayMetrics.class,
                    compatibilityInfoClass);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    ResourcesImplHookInstaller.applyDensityOverride(
                            packageName,
                            (Configuration) param.args[0],
                            (DisplayMetrics) param.args[1],
                            store);
                }
            });
            compatDebugLog("compat100 legacy ResourcesImpl hook ready");
        } catch (Throwable throwable) {
            RESOURCES_IMPL_HOOKED.set(false);
            compatErrorLog("compat100 legacy ResourcesImpl hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void installResourcesManagerHook(String packageName, DpiConfigStore store) {
        if (!RESOURCES_MANAGER_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesManagerClass = Class.forName(
                    "android.app.ResourcesManager", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method method = resourcesManagerClass.getDeclaredMethod(
                    "applyConfigurationToResources",
                    Configuration.class,
                    compatibilityInfoClass);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyResourceOverrides(
                            (Configuration) param.args[0],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManager");
                }
            });
            // compat100 keeps viewport mutations on stable resource/read/display
            // boundaries. ResourcesManager activity/create/key hooks can reapply
            // per-activity configuration during navigation and drift from the
            // system_server launch configuration, so they stay disabled here.
            int activityHookCount = 0;
            int createHookCount = 0;
            int keyHookCount = 0;
            compatDebugLog("compat100 legacy ResourcesManager hook ready"
                    + " (activityHooks=" + activityHookCount
                    + ", createHooks=" + createHookCount
                    + ", keyHooks=" + keyHookCount + ")");
        } catch (Throwable throwable) {
            RESOURCES_MANAGER_HOOKED.set(false);
            compatErrorLog("compat100 legacy ResourcesManager hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static int installUpdateResourcesForActivityHook(Class<?> resourcesManagerClass,
                                                             String packageName,
                                                             DpiConfigStore store) {
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> iBinderClass = Class.forName("android.os.IBinder", false, bootClassLoader);
            Method method = resourcesManagerClass.getDeclaredMethod(
                    "updateResourcesForActivity",
                    iBinderClass,
                    Configuration.class,
                    int.class);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyResourceOverrides(
                            (Configuration) param.args[1],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManagerActivity");
                }
            });
            return 1;
        } catch (Throwable throwable) {
            compatErrorLog("compat100 legacy ResourcesManager activity hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
            return 0;
        }
    }

    private static int installResourceCreationHooks(Class<?> resourcesManagerClass,
                                                    String packageName,
                                                    DpiConfigStore store) {
        int hookedCount = 0;
        Set<Method> hookedMethods = new HashSet<>();
        for (Method method : resourcesManagerClass.getDeclaredMethods()) {
            int configArgIndex = findConfigurationArgIndex(method);
            if (configArgIndex < 0 || !isResourceCreationMethod(method.getName())) {
                continue;
            }
            if (!hookedMethods.add(method)) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyResourceOverrides(
                            (Configuration) param.args[configArgIndex],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManagerCreate(" + method.getName() + ")");
                }
            });
            hookedCount++;
        }
        return hookedCount;
    }

    private static int installResourcesKeyHooks(Class<?> resourcesManagerClass,
                                                String packageName,
                                                DpiConfigStore store) {
        int hookedCount = 0;
        Set<Method> hookedMethods = new HashSet<>();
        for (Method method : resourcesManagerClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (!"createResourcesImpl".equals(methodName)
                    || !hasResourcesKeyFirstArg(method)
                    || !hookedMethods.add(method)) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                            param.thisObject,
                            param.args[0],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManagerKey(" + methodName + ")");
                }
            });
            hookedCount++;
        }
        return hookedCount;
    }

    private static int findConfigurationArgIndex(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (Configuration.class.equals(parameterTypes[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isResourceCreationMethod(String methodName) {
        return methodName != null
                && (methodName.contains("createResources")
                || methodName.contains("getOrCreateResources")
                || methodName.contains("createBaseTokenResources"));
    }

    private static boolean hasResourcesKeyFirstArg(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length > 0
                && "android.content.res.ResourcesKey".equals(parameterTypes[0].getName());
    }

    private static void installResourcesReadHooks(String packageName, DpiConfigStore store) {
        if (!RESOURCES_READ_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesClass = Class.forName(
                    "android.content.res.Resources", false, bootClassLoader);

            Method getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration");
            XposedBridge.hookMethod(getConfigurationMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    if (result instanceof Configuration configuration) {
                        String activePackage = resolveActivePackageName(packageName);
                        DpiConfigStore activeStore = resolveStoreForPackage(activePackage, store);
                        ResourcesReadHookInstaller.applyConfigurationOverride(
                                configuration, activePackage, activeStore,
                                "Compat100ResourcesRead(getConfiguration)");
                    }
                }
            });

            Method getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics");
            XposedBridge.hookMethod(getDisplayMetricsMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    Object thisObject = param.thisObject;
                    if (!(result instanceof DisplayMetrics metrics)
                            || !(thisObject instanceof Resources resources)) {
                        return;
                    }
                    if (Boolean.TRUE.equals(RESOURCES_READ_INTERNAL_UPDATE.get())) {
                        return;
                    }
                    RESOURCES_READ_INTERNAL_UPDATE.set(Boolean.TRUE);
                    try {
                        Configuration config = resources.getConfiguration();
                        String activePackage = resolveActivePackageName(packageName);
                        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, activePackage);
                    } finally {
                        RESOURCES_READ_INTERNAL_UPDATE.set(Boolean.FALSE);
                    }
                }
            });

            Method getSystemMethod = resourcesClass.getDeclaredMethod("getSystem");
            XposedBridge.hookMethod(getSystemMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    if (!(result instanceof Resources resources)) {
                        return;
                    }
                    RESOURCES_READ_INTERNAL_UPDATE.set(Boolean.TRUE);
                    try {
                        Configuration config = resources.getConfiguration();
                        String activePackage = resolveActivePackageName(packageName);
                        DpiConfigStore activeStore = resolveStoreForPackage(activePackage, store);
                        ResourcesReadHookInstaller.applyConfigurationOverride(
                                config, activePackage, activeStore,
                                "Compat100ResourcesRead(getSystem)");
                        ResourcesReadHookInstaller.applyMetricsOverride(
                                resources.getDisplayMetrics(), config, activePackage);
                    } finally {
                        RESOURCES_READ_INTERNAL_UPDATE.set(Boolean.FALSE);
                    }
                }
            });

            compatDebugLog("compat100 legacy Resources read hook ready");
        } catch (Throwable throwable) {
            RESOURCES_READ_HOOKED.set(false);
            compatErrorLog("compat100 legacy Resources read hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void installDisplayHooks(String packageName, DpiConfigStore store) {
        DisplayHookInstaller.setTargetPackageNameForCompat100(packageName);
        DisplayHookInstaller.setTargetStoreForCompat100(store);
        if (!DISPLAY_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> displayClass = Class.forName("android.view.Display", false, bootClassLoader);
            hookDisplayMetricsMethod(displayClass, "getMetrics");
            hookDisplayMetricsMethod(displayClass, "getRealMetrics");
            hookDisplayPointMethod(displayClass, "getSize");
            hookDisplayPointMethod(displayClass, "getRealSize");
            hookDisplayInfoMethod(displayClass, bootClassLoader);
            compatDebugLog("compat100 legacy Display hooks ready for " + packageName);
        } catch (Throwable throwable) {
            DISPLAY_HOOKED.set(false);
            compatErrorLog("compat100 legacy Display hooks failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void hookDisplayMetricsMethod(Class<?> displayClass, String methodName)
            throws ReflectiveOperationException {
        Method method = displayClass.getDeclaredMethod(methodName, DisplayMetrics.class);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                DisplayHookInstaller.applyDisplayMetrics((DisplayMetrics) param.args[0], methodName);
            }
        });
    }

    private static void hookDisplayPointMethod(Class<?> displayClass, String methodName)
            throws ReflectiveOperationException {
        Method method = displayClass.getDeclaredMethod(methodName, Point.class);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                DisplayHookInstaller.applyPoint((Point) param.args[0], methodName);
            }
        });
    }

    private static void hookDisplayInfoMethod(Class<?> displayClass, ClassLoader bootClassLoader) {
        try {
            Class<?> displayInfoClass = Class.forName("android.view.DisplayInfo", false, bootClassLoader);
            Method method = displayClass.getDeclaredMethod("getDisplayInfo", displayInfoClass);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    DisplayHookInstaller.applyDisplayInfo(param.args[0], "getDisplayInfo");
                }
            });
        } catch (Throwable ignored) {
            compatDebugLog("compat100 legacy Display getDisplayInfo hook skipped");
        }
    }

    private static void installWindowMetricsHook() {
        if (!WINDOW_METRICS_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> windowMetricsClass = Class.forName(
                    "android.view.WindowMetrics", false, bootClassLoader);
            Method method = windowMetricsClass.getDeclaredMethod("getBounds");
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    if (!(result instanceof Rect rect) || !WindowFrameOverride.isEnabled()) {
                        return;
                    }
                    VirtualDisplayOverride.Result override = VirtualDisplayState.get();
                    if (override == null) {
                        return;
                    }
                    Rect newRect = new Rect(rect.left, rect.top,
                            rect.left + override.widthPx, rect.top + override.heightPx);
                    param.setResult(newRect);
                }
            });
            compatDebugLog("compat100 legacy WindowMetrics hook ready");
        } catch (Throwable throwable) {
            WINDOW_METRICS_HOOKED.set(false);
            compatErrorLog("compat100 legacy WindowMetrics hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void installFontFieldRewriteHooks(String packageName, DpiConfigStore store) {
        if (!FONT_FIELD_REWRITE_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            float factor = PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor(store, packageName);
            if (factor <= 0f || factor == 1.0f) {
                FONT_FIELD_REWRITE_HOOKED.set(false);
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> textViewClass = Class.forName("android.widget.TextView", false, bootClassLoader);
            Method setTextSizeWithUnit = textViewClass.getDeclaredMethod(
                    "setTextSize", int.class, float.class);
            XposedBridge.hookMethod(setTextSizeWithUnit, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(FONT_TEXTVIEW_UPDATE.get())) {
                        return;
                    }
                    int unit = (Integer) param.args[0];
                    if (shouldScaleTextUnit(unit)) {
                        param.args[1] = ((Float) param.args[1]) * factor;
                        FONT_TEXTVIEW_UPDATE.set(Boolean.TRUE);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    FONT_TEXTVIEW_UPDATE.set(Boolean.FALSE);
                }
            });
            Method setTextSizeSp = textViewClass.getDeclaredMethod("setTextSize", float.class);
            XposedBridge.hookMethod(setTextSizeSp, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(FONT_TEXTVIEW_UPDATE.get())) {
                        return;
                    }
                    // Android's one-argument TextView#setTextSize delegates to the
                    // two-argument overload on AOSP. The shared guard keeps compat100
                    // field rewrite to one scale pass across that nested call chain.
                    param.args[0] = ((Float) param.args[0]) * factor;
                    FONT_TEXTVIEW_UPDATE.set(Boolean.TRUE);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    FONT_TEXTVIEW_UPDATE.set(Boolean.FALSE);
                }
            });
            Method paintSetTextSize = Paint.class.getDeclaredMethod("setTextSize", float.class);
            XposedBridge.hookMethod(paintSetTextSize, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(FONT_TEXTVIEW_UPDATE.get())) {
                        return;
                    }
                    param.args[0] = ((Float) param.args[0]) * factor;
                }
            });
            compatDebugLog("compat100 legacy font field rewrite hooks ready: factor=" + factor);
        } catch (Throwable throwable) {
            FONT_FIELD_REWRITE_HOOKED.set(false);
            compatErrorLog("compat100 legacy font field rewrite hooks failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static boolean shouldScaleTextUnit(int unit) {
        return unit == TypedValue.COMPLEX_UNIT_SP
                || unit == TypedValue.COMPLEX_UNIT_PX
                || unit == TypedValue.COMPLEX_UNIT_DIP
                || unit == TypedValue.COMPLEX_UNIT_PT
                || unit == TypedValue.COMPLEX_UNIT_IN
                || unit == TypedValue.COMPLEX_UNIT_MM;
    }

    private static void applyResourceOverrides(Configuration config,
                                               DpiConfigStore store,
                                               String packageName,
                                               String sourceTag) {
        try {
            ResourcesManagerHookInstaller.applyResourceOverrides(config, store, packageName, sourceTag);
        } catch (Throwable throwable) {
            compatErrorLog("compat100 legacy resource override failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static String resolveActivePackageName(String fallbackPackageName) {
        try {
            Method cached = CURRENT_PACKAGE_METHOD.get();
            if (cached == null) {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method method = activityThreadClass.getDeclaredMethod("currentPackageName");
                method.setAccessible(true);
                if (!CURRENT_PACKAGE_METHOD.compareAndSet(null, method)) {
                    method = CURRENT_PACKAGE_METHOD.get();
                }
                cached = method;
            }
            if (cached != null) {
                Object value = cached.invoke(null);
                if (value instanceof String packageName && !packageName.isBlank()) {
                    return packageName;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallbackPackageName;
    }

    private static DpiConfigStore resolveStoreForPackage(String packageName, DpiConfigStore fallbackStore) {
        if (packageName == null || packageName.isBlank()) {
            return fallbackStore;
        }
        try {
            return COMPAT100_HOST_STORE_CACHE.computeIfAbsent(
                    packageName, ConfigStoreFactory::createForCompat100Host);
        } catch (Throwable ignored) {
            return fallbackStore;
        }
    }

    private static void installWechatTargetFieldHooks(ClassLoader classLoader) {
        if (classLoader == null || !WECHAT_TARGET_FIELD_HOOKED.compareAndSet(false, true)) {
            return;
        }
        if (installWechatTargetFieldGetterHook(classLoader)) {
            return;
        }
        if (installLegacyWechatTargetFieldConstructorHook(classLoader)) {
            return;
        }
        WECHAT_TARGET_FIELD_HOOKED.set(false);
    }

    private static boolean installWechatTargetFieldGetterHook(ClassLoader classLoader) {
        try {
            Class<?> densityManagerClass = Class.forName("w45.f", false, classLoader);
            Method targetGetter = densityManagerClass.getDeclaredMethod("g");
            targetGetter.setAccessible(true);
            XposedBridge.hookMethod(targetGetter, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int target = resolveWechatTargetField();
                    if (target > 0) {
                        param.setResult(target);
                    }
                }
            });
            compatDebugLog("compat100 WeChat target-field getter hook ready: w45.f#g");
            return true;
        } catch (Throwable throwable) {
            compatErrorLog("compat100 WeChat target-field getter hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
            return false;
        }
    }

    private static boolean installLegacyWechatTargetFieldConstructorHook(ClassLoader classLoader) {
        try {
            Class<?> densityManagerClass = Class.forName("q35.f", false, classLoader);
            Field targetField = densityManagerClass.getDeclaredField("screenResolution_target_field");
            targetField.setAccessible(true);
            XposedBridge.hookAllConstructors(densityManagerClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int target = resolveWechatTargetField();
                    if (target <= 0 || param.thisObject == null) {
                        return;
                    }
                    targetField.setInt(param.thisObject, target);
                }
            });
            compatDebugLog("compat100 WeChat target-field constructor hook ready: q35.f");
            return true;
        } catch (Throwable throwable) {
            compatErrorLog("compat100 WeChat target-field constructor hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
            return false;
        }
    }

    private static int resolveWechatTargetField() {
        int value = readSystemPropertyInt(WECHAT_TARGET_FIELD_PROPERTY);
        return value >= WECHAT_TARGET_FIELD_MIN && value <= WECHAT_TARGET_FIELD_MAX ? value : 0;
    }

    private static int readSystemPropertyInt(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return 0;
        }
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getDeclaredMethod("get", String.class, String.class);
            Object raw = get.invoke(null, propertyName, "0");
            if (raw instanceof String value) {
                return Integer.parseInt(value.trim());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static void compatLog(String message) {
        DpisLog.i(message);
    }

    private static void compatDebugLog(String message) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        compatLog(message);
    }

    private static void compatErrorLog(String message) {
        try {
            XposedBridge.log("DPIS " + message);
        } catch (Throwable ignored) {
        }
        try {
            Log.e(DpisLog.TAG, message);
        } catch (Throwable ignored) {
        }
    }
}
