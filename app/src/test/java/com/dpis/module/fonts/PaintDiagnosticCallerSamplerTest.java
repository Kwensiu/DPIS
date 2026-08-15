package com.dpis.module.fonts;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaintDiagnosticCallerSamplerTest {
    @After
    public void tearDown() {
        PaintDiagnosticCallerSampler.resetForTest();
    }

    @Test
    public void keepsInitialAndPeriodicSamplesForEachPaintInputBucket() {
        assertTrue(PaintDiagnosticCallerSampler.shouldCapture("X.0ljv", 39f));
        assertTrue(PaintDiagnosticCallerSampler.shouldCapture("X.0ljv", 39f));
        for (int i = 0; i < 30; i++) {
            assertFalse(PaintDiagnosticCallerSampler.shouldCapture("X.0ljv", 39f));
        }
        assertTrue(PaintDiagnosticCallerSampler.shouldCapture("X.0ljv", 39f));
    }

    @Test
    public void samplesDifferentInputsIndependently() {
        assertTrue(PaintDiagnosticCallerSampler.shouldCapture("X.0ljv", 39f));
        assertTrue(PaintDiagnosticCallerSampler.shouldCapture("X.0ljv", 45f));
    }
}
