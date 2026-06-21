package com.dpis.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;

final class WechatDpiModernHookInstaller {
    private static final String WECHAT_BOTTOM_TAB_ICON_VIEW_CLASS =
            "com.tencent.mm.ui.TabIconView";
    private static final Object HOOK_LOCK = new Object();
    private static final Set<Class<?>> HOOKED_DENSITY_MANAGER_CLASSES =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Class<?>> HOOKED_BOTTOM_TAB_ICON_CLASSES =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final AtomicBoolean WECHAT_DPI_CALLBACK_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_DPI_MUTATION_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_BOTTOM_TAB_ICON_CALLBACK_LOGGED =
            new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_BOTTOM_TAB_ICON_MUTATION_LOGGED =
            new AtomicBoolean(false);

    private WechatDpiModernHookInstaller() {
    }

    static void install(XposedInterface xposed, ClassLoader classLoader,
            ApplicationInfo applicationInfo, String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)
                || xposed == null
                || classLoader == null) {
            return;
        }
        installRoute(xposed, classLoader, applicationInfo, packageName);
        installBottomTabIconScaleHook(xposed, classLoader);
    }

    static void installFromLoadedClass(XposedInterface xposed, Class<?> densityManagerClass,
            String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)
                || xposed == null
                || densityManagerClass == null) {
            return;
        }
        installWechatDpiHook(
                xposed,
                WechatDpiMethodLocator.Result.resolved(
                        WechatDpiMethodLocator.Source.LOADED_CLASS,
                        WechatDpiMethodLocator.densityManagerMethods(densityManagerClass)),
                0L);
    }

    private static void installBottomTabIconScaleHook(
            XposedInterface xposed, ClassLoader classLoader) {
        if (xposed == null || classLoader == null) {
            return;
        }
        Class<?> bottomTabIconViewClass;
        try {
            bottomTabIconViewClass = Class.forName(
                    WECHAT_BOTTOM_TAB_ICON_VIEW_CLASS, false, classLoader);
        } catch (ClassNotFoundException throwable) {
            DpisLog.i("modern WeChat DPI bottom tab icon hook skipped: class not found"
                    + ", classLoader=" + describeClassLoaderForLog(classLoader));
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "bottom_tab_icon",
                    "skipped",
                    "reason=class_not_found, classLoader=" + describeClassLoaderForLog(classLoader));
            return;
        } catch (Throwable throwable) {
            DpisLog.e("modern WeChat DPI bottom tab icon hook failed while resolving class: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(),
                    throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "bottom_tab_icon",
                    "skipped",
                    "resolveFailed=true, error=" + throwable.getClass().getSimpleName());
            return;
        }
        Method initMethod = findBottomTabIconInitMethod(bottomTabIconViewClass);
        if (initMethod == null) {
            DpisLog.i("modern WeChat DPI bottom tab icon hook skipped: init method not found"
                    + ", classLoader=" + describeClassLoaderForLog(classLoader));
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "bottom_tab_icon",
                    "skipped",
                    "reason=init_method_not_found, classLoader="
                            + describeClassLoaderForLog(classLoader));
            return;
        }
        Field scaleField = findBottomTabIconScaleField(bottomTabIconViewClass);
        if (scaleField == null) {
            DpisLog.i("modern WeChat DPI bottom tab icon hook skipped: scale field not found"
                    + ", classLoader=" + describeClassLoaderForLog(classLoader));
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "bottom_tab_icon",
                    "skipped",
                    "reason=scale_field_not_found, classLoader="
                            + describeClassLoaderForLog(classLoader));
            return;
        }
        synchronized (HOOK_LOCK) {
            if (!HOOKED_BOTTOM_TAB_ICON_CLASSES.add(bottomTabIconViewClass)) {
                return;
            }
        }
        try {
            initMethod.setAccessible(true);
            scaleField.setAccessible(true);
            xposed.hook(initMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object view = chain.getThisObject();
                        if (WECHAT_BOTTOM_TAB_ICON_CALLBACK_LOGGED.compareAndSet(false, true)) {
                            DpisLog.i("modern WeChat DPI bottom tab icon callback hit: method="
                                    + methodName(initMethod)
                                    + ", configuredDpi="
                                    + WechatDpiPropertyBridge.readDpi(
                                            WechatDpiConfig.PACKAGE_NAME));
                            FeedbackDiagnosticRuntimeHotPathEvents.event(
                                    WechatDpiConfig.PACKAGE_NAME,
                                    "wechat_dpi",
                                    "bottom_tab_icon",
                                    "route_callback_entered",
                                    "method=" + methodName(initMethod)
                                            + ", configuredDpi="
                                            + WechatDpiPropertyBridge.readDpi(
                                                    WechatDpiConfig.PACKAGE_NAME));
                        }
                        Integer configuredDpi = configuredWechatDpiOrNull();
                        if (view != null && configuredDpi != null) {
                            float oldScale = scaleField.getFloat(view);
                            float targetScale = WechatDpiRuntime.bottomTabIconScale(configuredDpi);
                            scaleField.setFloat(view, targetScale);
                            if (WECHAT_BOTTOM_TAB_ICON_MUTATION_LOGGED.compareAndSet(false, true)) {
                                DpisLog.i(
                                        "modern WeChat DPI bottom tab icon scale prepared: field="
                                                + scaleField.getName()
                                                + ", targetDpi=" + configuredDpi
                                                + ", scale " + oldScale + " -> " + targetScale);
                                FeedbackDiagnosticRuntimeHotPathEvents.event(
                                        WechatDpiConfig.PACKAGE_NAME,
                                        "wechat_dpi",
                                        "bottom_tab_icon",
                                        "mutation_applied",
                                        "field=" + scaleField.getName()
                                                + ", targetDpi=" + configuredDpi
                                                + ", scale=" + oldScale + "->" + targetScale);
                            }
                        }
                        return chain.proceed();
                    });
            DpisLog.i("modern WeChat DPI bottom tab icon hook ready: method="
                    + methodName(initMethod)
                    + ", field=" + scaleField.getName()
                    + ", classLoader=" + describeClassLoaderForLog(classLoader));
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "bottom_tab_icon",
                    "hook_ready",
                    "method=" + methodName(initMethod)
                            + ", field=" + scaleField.getName());
        } catch (Throwable throwable) {
            unmarkBottomTabIconClass(bottomTabIconViewClass);
            DpisLog.e("modern WeChat DPI bottom tab icon hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(),
                    throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "bottom_tab_icon",
                    "skipped",
                    "hookFailed=true, error=" + throwable.getClass().getSimpleName());
        }
    }

    private static void unmarkBottomTabIconClass(Class<?> bottomTabIconViewClass) {
        synchronized (HOOK_LOCK) {
            HOOKED_BOTTOM_TAB_ICON_CLASSES.remove(bottomTabIconViewClass);
        }
    }

    private static Method findBottomTabIconInitMethod(Class<?> bottomTabIconViewClass) {
        for (Method method : bottomTabIconViewClass.getDeclaredMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 4
                    && parameterTypes[0] == Integer.TYPE
                    && parameterTypes[1] == Integer.TYPE
                    && parameterTypes[2] == Integer.TYPE
                    && parameterTypes[3] == Boolean.TYPE) {
                return method;
            }
        }
        return null;
    }

    private static Field findBottomTabIconScaleField(Class<?> bottomTabIconViewClass) {
        for (Field field : bottomTabIconViewClass.getDeclaredFields()) {
            if (field.getType() == Float.TYPE && !Modifier.isStatic(field.getModifiers())) {
                return field;
            }
        }
        return null;
    }

    private static boolean installRoute(XposedInterface xposed, ClassLoader classLoader,
            ApplicationInfo applicationInfo, String packageName) {
        long versionCode = resolveWechatVersionCode(applicationInfo, packageName);
        WechatDpiMethodLocator.Result result = WechatDpiMethodLocator.locate(
                classLoader, applicationInfo, versionCode);
        if (result.methods.isEmpty()) {
            DpisLog.i("modern WeChat DPI hook skipped: locator="
                    + result.source.logName + ", versionCode=" + versionCode
                    + ", classLoader=" + describeClassLoaderForLog(classLoader)
                    + ", reason=" + result.failure);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    packageName,
                    "wechat_dpi",
                    "displaymetrics",
                    "skipped",
                    "locator=" + result.source.logName + ", versionCode=" + versionCode
                            + ", reason=" + result.failure);
            return false;
        }
        return installWechatDpiHook(xposed, result, versionCode);
    }

    private static boolean installWechatDpiHook(XposedInterface xposed,
            WechatDpiMethodLocator.Result locatorResult, long versionCode) {
        List<Method> metricsMethods = locatorResult.methods;
        if (metricsMethods.isEmpty()) {
            return false;
        }
        List<Class<?>> densityManagerClasses = markUnhookedClasses(
                declaringClasses(metricsMethods));
        if (densityManagerClasses.isEmpty()) {
            DpisLog.i("modern WeChat DPI hook skipped: already installed for "
                    + methodNames(metricsMethods)
                    + ", locator=" + locatorResult.source.logName
                    + ", versionCode=" + versionCode);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "skipped",
                    "reason=already_installed, locator=" + locatorResult.source.logName
                            + ", versionCode=" + versionCode);
            return true;
        }
        int installed = 0;
        try {
            for (Method metricsMethod : metricsMethods) {
                if (!densityManagerClasses.contains(metricsMethod.getDeclaringClass())) {
                    continue;
                }
                metricsMethod.setAccessible(true);
                xposed.hook(metricsMethod)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Integer configuredDpi = configuredWechatDpiOrNull();
                            if (WECHAT_DPI_CALLBACK_LOGGED.compareAndSet(false, true)) {
                                DpisLog.i("modern WeChat DPI callback hit: method="
                                        + methodName(metricsMethod)
                                        + ", configuredDpi="
                                        + WechatDpiPropertyBridge.readDpi(
                                                WechatDpiConfig.PACKAGE_NAME));
                                FeedbackDiagnosticRuntimeHotPathEvents.event(
                                        WechatDpiConfig.PACKAGE_NAME,
                                        "wechat_dpi",
                                        "displaymetrics",
                                        "route_callback_entered",
                                        "method=" + methodName(metricsMethod)
                                                + ", configuredDpi="
                                                + WechatDpiPropertyBridge.readDpi(
                                                        WechatDpiConfig.PACKAGE_NAME));
                            }
                            if (isTargetFieldGetter(metricsMethod)) {
                                if (configuredDpi != null) {
                                    return configuredDpi;
                                }
                                return chain.proceed();
                            }
                            if (isTargetFieldSetter(metricsMethod)) {
                                if (configuredDpi != null) {
                                    return chain.proceed(new Object[] {configuredDpi});
                                }
                                return chain.proceed();
                            }
                            Object result = chain.proceed();
                            if (result instanceof DisplayMetrics metrics) {
                                applyWechatDpi(metrics,
                                        methodName(metricsMethod));
                            } else if (isDisplayMetricsMutator(metricsMethod)) {
                                applyWechatDpi(displayMetricsArgument(chain.getArgs()),
                                        methodName(metricsMethod));
                            }
                            return result;
                        });
                installed++;
            }
            DpisLog.i("modern WeChat DPI hook ready: "
                    + methodNames(metricsMethods)
                    + ", installed=" + installed + ", locator="
                    + locatorResult.source.logName + ", versionCode=" + versionCode
                    + ", declaringClassLoaders=" + declaringClassLoaders(metricsMethods)
                    + ", configuredDpi="
                    + WechatDpiPropertyBridge.readDpi(
                            WechatDpiConfig.PACKAGE_NAME));
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "hook_ready",
                    "installed=" + installed + ", locator=" + locatorResult.source.logName
                            + ", versionCode=" + versionCode);
            return installed > 0;
        } catch (Throwable throwable) {
            unmarkHookedClasses(densityManagerClasses);
            DpisLog.e("modern WeChat DPI hook failed: "
                    + methodNames(metricsMethods)
                    + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "skipped",
                    "hookFailed=true, versionCode=" + versionCode
                            + ", error=" + throwable.getClass().getSimpleName());
        }
        return false;
    }

    private static List<Class<?>> markUnhookedClasses(List<Class<?>> classes) {
        ArrayList<Class<?>> unhooked = new ArrayList<>();
        synchronized (HOOK_LOCK) {
            for (Class<?> clazz : classes) {
                if (clazz != null && HOOKED_DENSITY_MANAGER_CLASSES.add(clazz)) {
                    unhooked.add(clazz);
                }
            }
        }
        return unhooked;
    }

    private static void unmarkHookedClasses(List<Class<?>> classes) {
        synchronized (HOOK_LOCK) {
            for (Class<?> clazz : classes) {
                HOOKED_DENSITY_MANAGER_CLASSES.remove(clazz);
            }
        }
    }

    private static List<Class<?>> declaringClasses(List<Method> methods) {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        for (Method method : methods) {
            if (method != null && method.getDeclaringClass() != null) {
                classes.add(method.getDeclaringClass());
            }
        }
        return new ArrayList<>(classes);
    }

    private static String declaringClassLoaders(List<Method> methods) {
        LinkedHashSet<String> classLoaders = new LinkedHashSet<>();
        for (Method method : methods) {
            if (method == null || method.getDeclaringClass() == null) {
                continue;
            }
            classLoaders.add(describeClassLoaderForLog(method.getDeclaringClass().getClassLoader()));
        }
        return classLoaders.toString();
    }

    static String describeClassLoaderForLog(ClassLoader classLoader) {
        if (classLoader == null) {
            return "bootstrap";
        }
        String text;
        try {
            text = String.valueOf(classLoader);
        } catch (Throwable throwable) {
            text = classLoader.getClass().getName();
        }
        return classLoader.getClass().getName()
                + "@"
                + Integer.toHexString(System.identityHashCode(classLoader))
                + "("
                + abbreviateForLog(text, 240)
                + ")";
    }

    private static String abbreviateForLog(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static String methodNames(List<Method> methods) {
        StringBuilder builder = new StringBuilder();
        for (Method method : methods) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(methodName(method));
        }
        return builder.toString();
    }

    private static String methodName(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private static boolean isDisplayMetricsMutator(Method method) {
        if (method == null || method.getReturnType() != Void.TYPE) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 2
                && parameterTypes[0] == Configuration.class
                && parameterTypes[1] == DisplayMetrics.class;
    }

    private static boolean isTargetFieldGetter(Method method) {
        return method != null
                && Modifier.isStatic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && method.getReturnType() == Integer.TYPE;
    }

    private static boolean isTargetFieldSetter(Method method) {
        if (method == null
                || !Modifier.isStatic(method.getModifiers())
                || method.getReturnType() != Void.TYPE) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 1
                && parameterTypes[0] == Integer.TYPE;
    }

    private static DisplayMetrics displayMetricsArgument(List<Object> args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        Object value = args.get(1);
        return value instanceof DisplayMetrics ? (DisplayMetrics) value : null;
    }

    private static Integer configuredWechatDpiOrNull() {
        int dpi = WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME);
        return dpi > 0 ? dpi : null;
    }

    private static void applyWechatDpi(DisplayMetrics metrics, String methodName) {
        if (metrics == null) {
            return;
        }
        float oldDensity = metrics.density;
        int oldDensityDpi = metrics.densityDpi;
        float oldScaledDensity = metrics.scaledDensity;
        if (applyWechatDpiToMetrics(metrics)
                && WECHAT_DPI_MUTATION_LOGGED.compareAndSet(false, true)) {
            int targetDpi = WechatDpiPropertyBridge.readDpi(
                    WechatDpiConfig.PACKAGE_NAME);
            DpisLog.i("modern WeChat DPI applied: method=" + methodName
                    + ", targetDpi="
                    + targetDpi
                    + ", densityDpi " + oldDensityDpi + " -> " + metrics.densityDpi
                    + ", density " + oldDensity + " -> " + metrics.density
                    + ", scaledDensity " + oldScaledDensity + " -> "
                    + metrics.scaledDensity);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "mutation_applied",
                    "method=" + methodName + ", targetDpi=" + targetDpi
                            + ", densityDpi=" + oldDensityDpi + "->" + metrics.densityDpi
                            + ", density=" + oldDensity + "->" + metrics.density
                            + ", scaledDensity=" + oldScaledDensity + "->"
                            + metrics.scaledDensity);
        }
    }

    private static boolean applyWechatDpiToMetrics(DisplayMetrics metrics) {
        int dpi = WechatDpiPropertyBridge.readDpi(
                WechatDpiConfig.PACKAGE_NAME);
        return dpi > 0 && WechatDpiRuntime.apply(metrics, dpi);
    }

    private static long resolveWechatVersionCode(ApplicationInfo applicationInfo,
            String packageName) {
        long versionCode = resolveWechatVersionCode((Object) applicationInfo);
        if (versionCode > 0L) {
            return versionCode;
        }
        versionCode = resolveWechatVersionCode(resolveApplicationContext(), packageName);
        if (versionCode > 0L) {
            return versionCode;
        }
        return resolveWechatVersionCode(resolveSystemContext(), packageName);
    }

    private static long resolveWechatVersionCode(Object applicationInfo) {
        try {
            if (applicationInfo == null) {
                return 0L;
            }
            Object value = applicationInfo.getClass().getField("longVersionCode")
                    .get(applicationInfo);
            if (value instanceof Long versionCode) {
                return versionCode;
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    private static long resolveWechatVersionCode(Context context, String packageName) {
        try {
            if (context == null || packageName == null || packageName.isBlank()) {
                return 0L;
            }
            PackageManager packageManager = context.getPackageManager();
            if (packageManager == null) {
                return 0L;
            }
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return resolvePackageVersionCode(packageInfo);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static long resolvePackageVersionCode(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return 0L;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        }
        //noinspection deprecation
        return packageInfo.versionCode;
    }

    private static Context resolveApplicationContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            Object context = currentApplication.invoke(null);
            return context instanceof Context ? (Context) context : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Context resolveSystemContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThread.getDeclaredMethod("currentActivityThread");
            Object thread = currentActivityThread.invoke(null);
            if (thread == null) {
                return null;
            }
            Method getSystemContext = activityThread.getDeclaredMethod("getSystemContext");
            Object context = getSystemContext.invoke(thread);
            return context instanceof Context ? (Context) context : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
