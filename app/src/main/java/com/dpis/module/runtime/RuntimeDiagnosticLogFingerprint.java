package com.dpis.module.runtime;

public final class RuntimeDiagnosticLogFingerprint {
    public static final String VALUE = "diag-log-2026-06-21-counted-hotpath-v1";

    private RuntimeDiagnosticLogFingerprint() {
    }

    public static String field() {
        return "diagnosticLogFingerprint=" + VALUE;
    }
}
