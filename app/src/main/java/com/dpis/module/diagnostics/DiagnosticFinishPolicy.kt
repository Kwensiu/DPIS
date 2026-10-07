package com.dpis.module.diagnostics

/** Defines when returning to DPIS is allowed to finish a diagnostic session. */
object DiagnosticFinishPolicy {
    fun canFinishAfterDpisResume(
        running: Boolean,
        targetLaunchStarted: Boolean,
        finishing: Boolean,
    ): Boolean = running && targetLaunchStarted && !finishing
}
