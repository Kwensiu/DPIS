package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ComposeFontRuntimeDiagnosticsInstallerTest {
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
    public void layoutListenerAttachesOnlyWhenRuntimeLoggingEnabled() {
        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldAttachLayoutListener(true));
        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldAttachLayoutListener(false));
    }

    @Test
    public void layoutEvaluationRequiresLoggingAndThrottleWindow() {
        long nowMs = 10_000L;

        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs, 0L, false));
        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs, 0L, true));
        assertFalse(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs, nowMs - 100L, true));
        assertTrue(ComposeFontRuntimeDiagnosticsInstaller.shouldEvaluateFromLayout(
                nowMs,
                nowMs - ComposeFontRuntimeDiagnosticsInstaller.LAYOUT_EVALUATE_THROTTLE_MS,
                true));
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

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
}
