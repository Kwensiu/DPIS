package com.dpis.module.runtime.appprocess;

import com.dpis.module.diagnostics.RuntimeHotPathEvents;

import com.dpis.module.*;
import com.dpis.module.viewport.VirtualDisplayOverride;
import com.dpis.module.viewport.VirtualDisplayState;

import com.dpis.module.runtime.hookapi.ModernApiCapabilities;
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver;

import android.graphics.Rect;

import java.lang.reflect.Method;

import com.dpis.module.runtime.ProcessScopedInstallGate;
import com.dpis.module.runtime.RuntimeDiagnosticLogFingerprint;
import com.dpis.module.runtime.RuntimeHotPathEvidenceSampler;

import io.github.libxposed.api.XposedInterface;

public final class WindowMetricsHookInstaller {
    private static final String ROUTE_NAME = "window_metrics_bounds_override";
    private static final String HOOK_ID_WINDOW_METRICS_GET_BOUNDS = "window_metrics_get_bounds";
    private static volatile int installedPid = -1;
    private static final RuntimeHotPathEvidenceSampler HOTPATH_SAMPLER =
            new RuntimeHotPathEvidenceSampler();

    private WindowMetricsHookInstaller() {
    }

    public static void resetForHotReload() {
        installedPid = -1;
    }

    public static void install(XposedInterface xposed, String packageName) throws ReflectiveOperationException {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return;
        }
        synchronized (WindowMetricsHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> windowMetricsClass = Class.forName(
                    "android.view.WindowMetrics", false, bootClassLoader);
            Method getBoundsMethod = windowMetricsClass.getDeclaredMethod("getBounds");
            // 102 can replace this hook in place; 101 just ignores the hint.
            ModernApiCapabilitiesResolver.fromXposed(xposed).applyStableHookId(
                            xposed.hook(getBoundsMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_WINDOW_METRICS_GET_BOUNDS)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof Rect rect)) {
                            return result;
                        }
                        recordProbeAtMostEvery(packageName,
                                "source=WindowMetrics.getBounds"
                                        + ", " + RuntimeDiagnosticLogFingerprint.field()
                                        + ", bounds=" + rect.width() + "x" + rect.height());
                        if (!WindowFrameOverride.isEnabled()) {
                            recordSkipAtMostEvery(packageName,
                                    "source=WindowMetrics.getBounds"
                                            + ", reason=window_frame_override_disabled"
                                            + ", bounds=" + rect.width() + "x" + rect.height());
                            return result;
                        }
                        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
                        if (override == null) {
                            recordSkipAtMostEvery(packageName,
                                    "source=WindowMetrics.getBounds"
                                            + ", reason=no_virtual_display_state"
                                            + ", bounds=" + rect.width() + "x" + rect.height());
                            return result;
                        }
                        Rect newRect = new Rect(rect.left, rect.top,
                                rect.left + override.widthPx, rect.top + override.heightPx);
                        String detail = "source=WindowMetrics.getBounds"
                                + ", bounds=" + rect.width() + "x" + rect.height()
                                + "->" + newRect.width() + "x" + newRect.height();
                        RuntimeHotPathEvidenceSampler.Sample sample =
                                HOTPATH_SAMPLER.sample("applied|" + packageName + "|" + detail,
                                        detail);
                        if (sample.emit) {
                            String sampledDetail = sample.detail;
                            DpisLog.i("WindowMetrics override: bounds=" + rect.width() + "x"
                                    + rect.height() + " -> " + newRect.width() + "x"
                                    + newRect.height()
                                    + ", " + sampledDetail);
                            RuntimeHotPathEvents.applied(
                                    packageName,
                                    "viewport",
                                    ROUTE_NAME,
                                    sampledDetail);
                        }
                        return newRect;
            });
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i("WindowMetrics hook ready, " + RuntimeDiagnosticLogFingerprint.field());
        }
    }

    private static void recordProbeAtMostEvery(String packageName, String detail) {
        RuntimeHotPathEvidenceSampler.Sample sample =
                HOTPATH_SAMPLER.sample("probe|" + packageName + "|" + detail, detail);
        if (sample.emit) {
            String sampledDetail = sample.detail;
            DpisLog.i(
                "DPIS_VIEWPORT WindowMetrics callback: package=" + safeValue(packageName)
                        + ", route=" + ROUTE_NAME
                        + ", " + sampledDetail);
            RuntimeHotPathEvents.probe(
                    packageName,
                    "viewport",
                    ROUTE_NAME,
                    sampledDetail);
        }
    }

    private static void recordSkipAtMostEvery(String packageName, String detail) {
        RuntimeHotPathEvidenceSampler.Sample sample =
                HOTPATH_SAMPLER.sample("skip|" + packageName + "|" + detail, detail);
        if (sample.emit) {
            String sampledDetail = sample.detail;
            DpisLog.i(
                "DPIS_VIEWPORT WindowMetrics skip: package=" + safeValue(packageName)
                        + ", route=" + ROUTE_NAME
                        + ", " + sampledDetail);
            RuntimeHotPathEvents.skipped(
                    packageName,
                    "viewport",
                    ROUTE_NAME,
                    sampledDetail);
        }
    }

    public static void resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest();
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
