package com.dpis.module;

import android.graphics.Rect;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

final class WindowMetricsHookInstaller {
    private static final String ROUTE_NAME = "window_metrics_bounds_override";
    private static volatile boolean hookInstalled;
    private static final RuntimeHotPathEvidenceSampler HOTPATH_SAMPLER =
            new RuntimeHotPathEvidenceSampler();

    private WindowMetricsHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (WindowMetricsHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> windowMetricsClass = Class.forName(
                    "android.view.WindowMetrics", false, bootClassLoader);
            Method getBoundsMethod = windowMetricsClass.getDeclaredMethod("getBounds");
            xposed.hook(getBoundsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
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
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "viewport",
                                    ROUTE_NAME,
                                    sampledDetail);
                        }
                        return newRect;
            });
            hookInstalled = true;
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
            FeedbackDiagnosticRuntimeHotPathEvents.probe(
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
            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                    packageName,
                    "viewport",
                    ROUTE_NAME,
                    sampledDetail);
        }
    }

    static void resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest();
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
