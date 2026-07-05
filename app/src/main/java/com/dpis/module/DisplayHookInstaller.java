package com.dpis.module;

import com.dpis.module.viewport.DensityOverride;

import com.dpis.module.viewport.VirtualDisplayOverride;
import com.dpis.module.viewport.VirtualDisplayState;

import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.runtime.hookapi.ModernApiCapabilities;
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver;

import android.graphics.Point;
import android.util.DisplayMetrics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.dpis.module.runtime.ProcessScopedInstallGate;
import com.dpis.module.runtime.RuntimeDiagnosticLogFingerprint;
import com.dpis.module.runtime.RuntimeHotPathEvidenceSampler;

import io.github.libxposed.api.XposedInterface;

final class DisplayHookInstaller {
    private static volatile int installedPid = -1;
    private static volatile String targetPackageName;
    private static volatile DpisConfigStore targetStore;
    private static volatile Method currentPackageNameMethod;
    private static volatile boolean currentPackageNameUnavailable;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final RuntimeHotPathEvidenceSampler HOTPATH_SAMPLER =
            new RuntimeHotPathEvidenceSampler();
    private static final String HOOK_ID_DISPLAY_GET_METRICS = "display_get_metrics";
    private static final String HOOK_ID_DISPLAY_GET_REAL_METRICS = "display_get_real_metrics";
    private static final String HOOK_ID_DISPLAY_GET_DISPLAY_INFO = "display_get_display_info";
    private static final String HOOK_ID_DISPLAY_GET_SIZE = "display_get_size";
    private static final String HOOK_ID_DISPLAY_GET_REAL_SIZE = "display_get_real_size";

    private DisplayHookInstaller() {
    }

    static void resetForHotReload() {
        installedPid = -1;
    }

    static void install(XposedInterface xposed, String packageName, DpisConfigStore store)
            throws ReflectiveOperationException {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return;
        }
        synchronized (DisplayHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return;
            }
            targetPackageName = packageName;
            targetStore = store;
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> displayClass = Class.forName("android.view.Display", false, bootClassLoader);
            hookDisplayMetricsMethod(xposed, displayClass, "getMetrics");
            hookDisplayMetricsMethod(xposed, displayClass, "getRealMetrics");
            hookDisplayInfoMethod(xposed, displayClass, bootClassLoader);
            hookPointMethod(xposed, displayClass, "getSize");
            hookPointMethod(xposed, displayClass, "getRealSize");
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i("Display hook ready, " + RuntimeDiagnosticLogFingerprint.field());
        }
    }

    static void setTargetPackageNameForLegacy(String packageName) {
        targetPackageName = packageName;
    }

    static void setTargetStoreForLegacy(DpisConfigStore store) {
        targetStore = store;
    }

    private static void hookDisplayMetricsMethod(XposedInterface xposed, Class<?> displayClass,
                                                 String methodName)
            throws ReflectiveOperationException {
        Method method = displayClass.getDeclaredMethod(methodName, DisplayMetrics.class);
        XposedInterface.HookBuilder hookBuilder =
                xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE);
        if ("getMetrics".equals(methodName)) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                    .applyStableHookId(hookBuilder, HOOK_ID_DISPLAY_GET_METRICS);
        } else if ("getRealMetrics".equals(methodName)) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                    .applyStableHookId(hookBuilder, HOOK_ID_DISPLAY_GET_REAL_METRICS);
        }
        hookBuilder
                .intercept(chain -> {
                    Object result = chain.proceed();
                    DisplayMetrics metrics = (DisplayMetrics) chain.getArg(0);
                    applyDisplayMetrics(metrics, methodName);
                    return result;
                });
    }

    private static void hookPointMethod(XposedInterface xposed, Class<?> displayClass,
                                        String methodName) throws ReflectiveOperationException {
        Method method = displayClass.getDeclaredMethod(methodName, Point.class);
        XposedInterface.HookBuilder hookBuilder =
                xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE);
        if ("getSize".equals(methodName)) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                    .applyStableHookId(hookBuilder, HOOK_ID_DISPLAY_GET_SIZE);
        } else if ("getRealSize".equals(methodName)) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                    .applyStableHookId(hookBuilder, HOOK_ID_DISPLAY_GET_REAL_SIZE);
        }
        hookBuilder
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Point point = (Point) chain.getArg(0);
                    applyPoint(point, methodName);
                    return result;
                });
    }

    private static void hookDisplayInfoMethod(XposedInterface xposed,
                                              Class<?> displayClass,
                                              ClassLoader bootClassLoader) {
        try {
            Class<?> displayInfoClass = Class.forName("android.view.DisplayInfo", false, bootClassLoader);
            Method method = displayClass.getDeclaredMethod("getDisplayInfo", displayInfoClass);
            // Stable id lets API 102 replace the same display info hook during hot reload.
            ModernApiCapabilitiesResolver.fromXposed(xposed).applyStableHookId(
                            xposed.hook(method)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_DISPLAY_GET_DISPLAY_INFO)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object displayInfo = chain.getArg(0);
                        applyDisplayInfo(displayInfo, "getDisplayInfo");
                        return result;
                    });
        } catch (ReflectiveOperationException ignored) {
            DpisLog.i("Display getDisplayInfo hook skipped");
        }
    }

    static void applyDisplayMetrics(DisplayMetrics metrics, String sourceTag) {
        if (metrics == null) {
            return;
        }
        String routeName = "display_metrics_override";
        String currentPackageName = resolveCurrentPackageName();
        recordViewportProbeAtMostEvery(
                routeName,
                "source=" + sourceTag
                        + ", " + RuntimeDiagnosticLogFingerprint.field()
                        + ", callback=Display." + sourceTag);
        if (!shouldApplyOverrideForPackage(targetPackageName, currentPackageName)) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag
                            + ", reason=package_mismatch_or_unresolved"
                            + ", targetPackage=" + safeValue(targetPackageName)
                            + ", currentPackage=" + safeValue(currentPackageName));
            return;
        }
        String effectivePackageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(
                targetStore, targetPackageName);
        DpisConfigStore effectiveStore = WebApkRuntimeOwnerBridge.resolveEffectiveStore(
                targetStore, effectivePackageName);
        VirtualDisplayOverride.Result override = resolvePackageScopedOverride(
                effectivePackageName, effectiveStore);
        if (override == null) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag + ", reason=no_package_scoped_override");
            return;
        }
        int originalDensityDpi = metrics.densityDpi;
        float originalDensity = metrics.density;
        float originalScaledDensity = metrics.scaledDensity;
        int originalWidthPixels = metrics.widthPixels;
        int originalHeightPixels = metrics.heightPixels;
        float fontScale = metrics.density > 0f ? (metrics.scaledDensity / metrics.density) : 1.0f;
        if (fontScale <= 0f) {
            fontScale = 1.0f;
        }
        metrics.densityDpi = override.densityDpi;
        metrics.density = DensityOverride.densityFromDpi(override.densityDpi);
        metrics.scaledDensity = metrics.density * fontScale;
        metrics.widthPixels = override.widthPx;
        metrics.heightPixels = override.heightPx;
        boolean changed = originalDensityDpi != metrics.densityDpi
                || Float.compare(originalDensity, metrics.density) != 0
                || Float.compare(originalScaledDensity, metrics.scaledDensity) != 0
                || originalWidthPixels != metrics.widthPixels
                || originalHeightPixels != metrics.heightPixels;
        if (!changed) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag + ", reason=stable_target"
                            + ", widthPx=" + metrics.widthPixels
                            + ", heightPx=" + metrics.heightPixels
                            + ", densityDpi=" + metrics.densityDpi);
            return;
        }
        String message = "Display override(" + sourceTag + "): package="
                + safeValue(effectivePackageName)
                + ", widthPx=" + metrics.widthPixels
                + ", heightPx=" + metrics.heightPixels
                + ", densityDpi=" + metrics.densityDpi;
        if (logIfChanged(effectivePackageName + ":metrics:" + sourceTag, message)) {
            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                    effectivePackageName,
                    "viewport",
                    routeName,
                    "source=" + sourceTag
                            + ", widthPx=" + originalWidthPixels + "->" + metrics.widthPixels
                            + ", heightPx=" + originalHeightPixels + "->" + metrics.heightPixels
                            + ", densityDpi=" + originalDensityDpi + "->" + metrics.densityDpi
                            + ", density=" + originalDensity + "->" + metrics.density
                            + ", scaledDensity=" + originalScaledDensity + "->"
                            + metrics.scaledDensity);
        }
    }

    static void applyPoint(Point point, String sourceTag) {
        if (point == null) {
            return;
        }
        String routeName = "display_size_override";
        String currentPackageName = resolveCurrentPackageName();
        recordViewportProbeAtMostEvery(
                routeName,
                "source=" + sourceTag
                        + ", " + RuntimeDiagnosticLogFingerprint.field()
                        + ", callback=Display." + sourceTag);
        if (!shouldApplyOverrideForPackage(targetPackageName, currentPackageName)) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag
                            + ", reason=package_mismatch_or_unresolved"
                            + ", targetPackage=" + safeValue(targetPackageName)
                            + ", currentPackage=" + safeValue(currentPackageName));
            return;
        }
        String effectivePackageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(
                targetStore, targetPackageName);
        DpisConfigStore effectiveStore = WebApkRuntimeOwnerBridge.resolveEffectiveStore(
                targetStore, effectivePackageName);
        VirtualDisplayOverride.Result override = resolvePackageScopedOverride(
                effectivePackageName, effectiveStore);
        if (override == null) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag + ", reason=no_package_scoped_override");
            return;
        }
        int originalX = point.x;
        int originalY = point.y;
        point.x = override.widthPx;
        point.y = override.heightPx;
        if (originalX == point.x && originalY == point.y) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag + ", reason=stable_target"
                            + ", size=" + point.x + "x" + point.y);
            return;
        }
        String message = "Display override(" + sourceTag + "): package="
                + safeValue(effectivePackageName) + ", size=" + point.x + "x" + point.y;
        if (logIfChanged(effectivePackageName + ":point:" + sourceTag, message)) {
            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                    effectivePackageName,
                    "viewport",
                    routeName,
                    "source=" + sourceTag
                            + ", widthPx=" + originalX + "->" + point.x
                            + ", heightPx=" + originalY + "->" + point.y);
        }
    }

    static void applyDisplayInfo(Object displayInfo, String sourceTag) {
        if (displayInfo == null) {
            return;
        }
        String routeName = "display_info_override";
        String currentPackageName = resolveCurrentPackageName();
        recordViewportProbeAtMostEvery(
                routeName,
                "source=" + sourceTag
                        + ", " + RuntimeDiagnosticLogFingerprint.field()
                        + ", callback=Display." + sourceTag);
        if (!shouldApplyOverrideForPackage(targetPackageName, currentPackageName)) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag
                            + ", reason=package_mismatch_or_unresolved"
                            + ", targetPackage=" + safeValue(targetPackageName)
                            + ", currentPackage=" + safeValue(currentPackageName));
            return;
        }
        String effectivePackageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(
                targetStore, targetPackageName);
        DpisConfigStore effectiveStore = WebApkRuntimeOwnerBridge.resolveEffectiveStore(
                targetStore, effectivePackageName);
        VirtualDisplayOverride.Result override = resolvePackageScopedOverride(
                effectivePackageName, effectiveStore);
        if (override == null) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag + ", reason=no_package_scoped_override");
            return;
        }
        boolean changed = false;
        changed |= writeIntField(displayInfo, "logicalDensityDpi", override.densityDpi);
        changed |= writeIntField(displayInfo, "logicalWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "logicalHeight", override.heightPx);
        changed |= writeIntField(displayInfo, "appWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "appHeight", override.heightPx);
        changed |= writeIntField(displayInfo, "smallestNominalAppWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "smallestNominalAppHeight", override.heightPx);
        changed |= writeIntField(displayInfo, "largestNominalAppWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "largestNominalAppHeight", override.heightPx);
        if (!changed) {
            recordViewportSkipAtMostEvery(
                    routeName,
                    "source=" + sourceTag + ", reason=stable_target"
                            + ", logical=" + override.widthPx + "x" + override.heightPx
                            + ", densityDpi=" + override.densityDpi);
            return;
        }
        String message = "Display override(" + sourceTag + "): package="
                + safeValue(effectivePackageName) + ", logical=" + override.widthPx + "x"
                + override.heightPx + ", densityDpi=" + override.densityDpi;
        if (logIfChanged(effectivePackageName + ":displayInfo:" + sourceTag, message)) {
            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                    effectivePackageName,
                    "viewport",
                    routeName,
                    "source=" + sourceTag
                            + ", logicalWidth=" + override.widthPx
                            + ", logicalHeight=" + override.heightPx
                            + ", densityDpi=" + override.densityDpi);
        }
    }

    static boolean shouldApplyOverrideForPackage(String packageName) {
        return shouldApplyOverrideForPackage(packageName, resolveCurrentPackageName());
    }

    static boolean shouldApplyOverrideForPackage(String packageName, String currentPackageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        if (currentPackageName == null || currentPackageName.isBlank()) {
            return false;
        }
        return packageName.equals(currentPackageName);
    }

    static VirtualDisplayOverride.Result resolvePackageScopedOverrideForTest(String packageName,
                                                                             DpisConfigStore store) {
        return resolvePackageScopedOverride(packageName, store);
    }

    private static VirtualDisplayOverride.Result resolvePackageScopedOverride() {
        return resolvePackageScopedOverride(targetPackageName, targetStore);
    }

    private static VirtualDisplayOverride.Result resolvePackageScopedOverride(String packageName,
                                                                             DpisConfigStore store) {
        if (packageName == null || packageName.isBlank() || store == null) {
            return null;
        }
        ViewportTargetSpec targetSpec = store.getTargetViewportSpec(packageName);
        if (!targetSpec.isEnabled()) {
            return null;
        }
        ViewportRuntimeRecord record =
                VirtualDisplayState.findDisplayRecordForTarget(packageName, targetSpec);
        return record != null ? record.virtualDisplayResult : null;
    }

    private static boolean writeIntField(Object target, String fieldName, int value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            int current = field.getInt(target);
            if (current == value) {
                return false;
            }
            field.setInt(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
            return true;
        }
        return false;
    }

    static void resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest();
    }

    private static void recordViewportProbeAtMostEvery(String routeName, String detail) {
        RuntimeHotPathEvidenceSampler.Sample sample =
                HOTPATH_SAMPLER.sample("probe:" + routeName + "|" + detail, detail);
        if (sample.emit) {
            String sampledDetail = sample.detail;
            DpisLog.i(
                "DPIS_VIEWPORT Display callback: package=" + safeValue(targetPackageName)
                        + ", route=" + routeName
                        + ", " + sampledDetail);
            FeedbackDiagnosticRuntimeHotPathEvents.probe(
                    targetPackageName,
                    "viewport",
                    routeName,
                    sampledDetail);
        }
    }

    private static void recordViewportSkipAtMostEvery(String routeName, String detail) {
        RuntimeHotPathEvidenceSampler.Sample sample =
                HOTPATH_SAMPLER.sample("skip:" + routeName + "|" + detail, detail);
        if (sample.emit) {
            String sampledDetail = sample.detail;
            DpisLog.i(
                "DPIS_VIEWPORT Display skip: package=" + safeValue(targetPackageName)
                        + ", route=" + routeName
                        + ", " + sampledDetail);
            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                    targetPackageName,
                    "viewport",
                    routeName,
                    sampledDetail);
        }
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String resolveCurrentPackageName() {
        try {
            Method method = currentPackageNameMethod;
            if (method == null) {
                if (currentPackageNameUnavailable) {
                    return null;
                }
                synchronized (DisplayHookInstaller.class) {
                    method = currentPackageNameMethod;
                    if (method == null && !currentPackageNameUnavailable) {
                        method = Class.forName("android.app.ActivityThread")
                                .getDeclaredMethod("currentPackageName");
                        currentPackageNameMethod = method;
                    }
                }
            }
            if (method == null) {
                return null;
            }
            Object value = method.invoke(null);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            currentPackageNameUnavailable = true;
            return null;
        }
    }
}
