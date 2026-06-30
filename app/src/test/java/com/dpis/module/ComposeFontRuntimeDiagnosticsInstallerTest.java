package com.dpis.module;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ComposeFontRuntimeDiagnosticsInstallerTest {
    @After
    public void tearDown() {
        ResourcesFontScheduler.clearForTest();
    }

    @Test
    public void missingCurrentApplicationDefersOnlyBeforeCallbacksRegister() {
        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldDeferRegistration(null, false));
        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldDeferRegistration(null, true));
    }

    @Test
    public void presentApplicationDoesNotDefer() {
        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldDeferRegistration(
                new android.app.Application(), false));
    }

    @Test
    public void layoutEvaluationUsesThrottleWindow() {
        long nowMs = 10_000L;

        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs, 0L));
        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs, nowMs - 100L));
        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs,
                nowMs - ComposeFontRuntimeDiagnosticsInstaller.LAYOUT_EVALUATE_THROTTLE_MS));
    }

    @Test
    public void targetFactorResolvesFromCurrentNullablePercent() {
        assertNull(ComposeFontRuntimeDiagnosticsInstaller.resolveTargetFactor(null));
        assertNull(ComposeFontRuntimeDiagnosticsInstaller.resolveTargetFactor(0));
        assertNull(ComposeFontRuntimeDiagnosticsInstaller.resolveTargetFactor(-1));
        assertEquals(1.5f, ComposeFontRuntimeDiagnosticsInstaller.resolveTargetFactor(150), 0f);
    }

    @Test
    public void targetFactorResolutionReflectsCurrentStoreValue() {
        String packageName = "com.example.compose";
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        assertNull(ComposeFontRuntimeDiagnosticsInstaller.resolveCurrentTargetFactorForTest(
                store, packageName));

        store.setTargetFontScalePercent(packageName, 150);
        assertEquals(1.5f, ComposeFontRuntimeDiagnosticsInstaller.resolveCurrentTargetFactorForTest(
                store, packageName), 0f);

        store.setTargetFontScalePercent(packageName, 125);
        assertEquals(1.25f, ComposeFontRuntimeDiagnosticsInstaller.resolveCurrentTargetFactorForTest(
                store, packageName), 0f);

        store.clearTargetFontScalePercent(packageName);
        assertNull(ComposeFontRuntimeDiagnosticsInstaller.resolveCurrentTargetFactorForTest(
                store, packageName));
    }

    @Test
    public void diagnosticsSkipOnlyAfterResourcesReadConflictTargetSuppression() {
        String packageName = "com.example.compose";
        Object resources = new Object();

        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldSkipForTargetSuppression(
                packageName, 1.4f));

        ResourcesFontScheduler.observeResourcesFontScale(resources, packageName, 1.0f, 1.4f);
        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldSkipForTargetSuppression(
                packageName, 1.4f));

        ResourcesFontScheduler.observeResourcesFontScale(resources, packageName, 1.4f, 1.4f);
        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldSkipForTargetSuppression(
                packageName, 1.4f));
    }

    @Test
    public void diagnosticsContinueForComposeBaseSuppression() {
        String packageName = "com.example.compose";
        Object resources = new Object();
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan,
                1.4f,
                3.0f,
                4.2f,
                1.4f,
                true);

        ResourcesFontScheduler.observe(packageName, "root-a", resources, evidence,
                1.4f, 1.4f, 1_000L);

        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldSkipForTargetSuppression(
                packageName, 1.4f));
    }
}
