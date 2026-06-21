package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class RuntimeDiagnosticLogFingerprintTest {
    @Test
    public void fieldCarriesStableDiagnosticLogFingerprint() {
        String field = RuntimeDiagnosticLogFingerprint.field();

        assertTrue(field.startsWith("diagnosticLogFingerprint="));
        assertTrue(field.contains("counted-hotpath"));
    }
}
