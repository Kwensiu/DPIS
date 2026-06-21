package com.dpis.module;

final class RuntimeDiagnosticLogFingerprint {
    static final String VALUE = "diag-log-2026-06-21-counted-hotpath-v1";

    private RuntimeDiagnosticLogFingerprint() {
    }

    static String field() {
        return "diagnosticLogFingerprint=" + VALUE;
    }
}
