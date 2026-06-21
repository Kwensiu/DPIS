package com.dpis.module;

import android.graphics.Point;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VirtualDisplayOverrideTest {
    @Before
    public void setUp() {
        setTargetPackageName("com.max.xiaoheihe");
        setCurrentPackageResolver();
    }

    @After
    public void tearDown() {
        FeedbackDiagnosticRuntimeEvents.cancel();
        FeedbackDiagnosticRuntimeHotPathEvents.resetForTest();
        DisplayHookInstaller.resetHotPathSamplerForTest();
        VirtualDisplayState.set(null);
        setTargetPackageName(null);
        DisplayHookInstaller.setTargetStoreForLegacy(null);
        clearCurrentPackageResolver();
    }

    @Test
    public void keepsWindowPixelSizeAtPhysicalBounds() {
        VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
                360, 736, 360, 480, 1080, 2208, 300);

        assertEquals(300, result.widthDp);
        assertEquals(613, result.heightDp);
        assertEquals(300, result.smallestWidthDp);
        assertEquals(576, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }

    @Test
    public void appliesDisplayMetricsFromPackageScopedRecord() {
        publishTargetRecord();
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;

        DisplayHookInstaller.applyDisplayMetrics(metrics, "test");

        assertEquals(1080, metrics.widthPixels);
        assertEquals(2208, metrics.heightPixels);
        assertEquals(576, metrics.densityDpi);
    }

    @Test
    public void displayMetricsOverrideRecordsViewportHotpathEvidence() {
        publishTargetRecord();
        FeedbackDiagnosticRuntimeEvents.start("com.max.xiaoheihe", request());
        DisplayHookInstaller.resetHotPathSamplerForTest();
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;

        DisplayHookInstaller.applyDisplayMetrics(metrics, "diagnostic-test");

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event ->
                event.contains("route=viewport")
                        && event.contains("stage=applied")
                        && event.contains("display_metrics_override")));
    }

    @Test
    public void displayMetricsProbeEvidenceIncludesCountedSamples() {
        FeedbackDiagnosticRuntimeEvents.start("com.max.xiaoheihe", request());
        DisplayHookInstaller.resetHotPathSamplerForTest();
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;

        for (int i = 0; i < 50; i++) {
            DisplayHookInstaller.applyDisplayMetrics(metrics, "diagnostic-count-test");
        }

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event ->
                event.contains("route=viewport")
                        && event.contains("stage=probe")
                        && event.contains("display_metrics_override")
                        && event.contains("hitCount=50")
                        && event.contains("suppressedCount=48")));
    }

    @Test
    public void appliesPointFromPackageScopedRecord() {
        publishTargetRecord();
        Point point = new Point();
        point.x = 1;
        point.y = 2;

        DisplayHookInstaller.applyPoint(point, "test");

        assertEquals(1080, point.x);
        assertEquals(2208, point.y);
    }

    @Test
    public void keepsDensityStableAcrossOrientationForSameShortSideTarget() {
        VirtualDisplayOverride.Result portrait = VirtualDisplayOverride.derive(
                412, 915, 412, 420, 1080, 2400, 360);
        VirtualDisplayOverride.Result landscape = VirtualDisplayOverride.derive(
                915, 412, 412, 420, 2400, 1080, 360);

        assertEquals(481, portrait.densityDpi);
        assertEquals(481, landscape.densityDpi);
        assertEquals(360, portrait.widthDp);
        assertEquals(800, landscape.widthDp);
        assertEquals(360, landscape.heightDp);
    }

    @Test
    public void targetMatchingSmallestWidthKeepsDisplayEnvironmentIdentity() {
        VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
                393, 800, 360, 480, 1080, 2208, 360);

        assertEquals(393, result.widthDp);
        assertEquals(800, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(480, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }

    @Test
    public void absolutePhysicalPixelPlanIgnoresDriftedSourceDensity() {
        VirtualDisplayOverride.Result result =
                VirtualDisplayPlan.deriveAbsoluteResultFromPhysicalPixels(
                        360,
                        736,
                        360,
                        1080,
                        2208,
                        500);

        assertEquals(500, result.widthDp);
        assertEquals(1022, result.heightDp);
        assertEquals(500, result.smallestWidthDp);
        assertEquals(346, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }

    private static void setTargetPackageName(String packageName) {
        try {
            Field field = DisplayHookInstaller.class.getDeclaredField("targetPackageName");
            field.setAccessible(true);
            field.set(null, packageName);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String testCurrentPackageName() {
        return "com.max.xiaoheihe";
    }

    private static void setCurrentPackageResolver() {
        try {
            Method method = VirtualDisplayOverrideTest.class.getDeclaredMethod("testCurrentPackageName");
            method.setAccessible(true);
            Field resolverField = DisplayHookInstaller.class.getDeclaredField("currentPackageNameMethod");
            resolverField.setAccessible(true);
            resolverField.set(null, method);
            Field unavailableField =
                    DisplayHookInstaller.class.getDeclaredField("currentPackageNameUnavailable");
            unavailableField.setAccessible(true);
            unavailableField.setBoolean(null, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void clearCurrentPackageResolver() {
        try {
            Field resolverField = DisplayHookInstaller.class.getDeclaredField("currentPackageNameMethod");
            resolverField.setAccessible(true);
            resolverField.set(null, null);
            Field unavailableField =
                    DisplayHookInstaller.class.getDeclaredField("currentPackageNameUnavailable");
            unavailableField.setAccessible(true);
            unavailableField.setBoolean(null, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void publishTargetRecord() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.absoluteDp(300);
        store.setTargetViewportSpec("com.max.xiaoheihe", targetSpec);
        DisplayHookInstaller.setTargetStoreForLegacy(store);
        VirtualDisplayState.publish(
                "com.max.xiaoheihe",
                targetSpec,
                ViewportSourceSnapshot.systemDisplayInfo(360, 736, 360, 480, 1080, 2208),
                new ViewportOverride.Result(300, 613, 300, 576),
                new VirtualDisplayOverride.Result(300, 613, 300, 576, 1080, 2208),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
    }

    private static FeedbackDiagnosticCoordinator.Request request() {
        return new FeedbackDiagnosticCoordinator.Request(
                "com.max.xiaoheihe",
                "Xiaoheihe",
                "1.2.3",
                true,
                true,
                true,
                false,
                ViewportTargetSpec.absoluteDp(300),
                ViewportApplyMode.COMPAT,
                100,
                FontApplyMode.OFF,
                null,
                null,
                null
        );
    }
}
