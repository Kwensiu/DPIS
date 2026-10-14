package com.dpis.module.runtime.probe

object RuntimeDiagnosticLogFingerprint {
    @JvmField
    val VALUE = "diag-log-2026-06-21-counted-hotpath-v1"

    @JvmStatic
    fun field(): String = "diagnosticLogFingerprint=$VALUE"
}
