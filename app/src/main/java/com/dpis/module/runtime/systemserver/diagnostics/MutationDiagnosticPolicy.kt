package com.dpis.module.runtime.systemserver.diagnostics

enum class MutationDiagnosticOutcome {
    NO_TARGET,
    POLICY_SKIP,
    CHANGED,
    UNCHANGED,
}

object MutationDiagnosticPolicy {
    @JvmStatic
    fun outcome(
        hasTarget: Boolean,
        policyAllows: Boolean,
        changed: Boolean
    ): MutationDiagnosticOutcome =
        when {
            !hasTarget -> MutationDiagnosticOutcome.NO_TARGET
            !policyAllows -> MutationDiagnosticOutcome.POLICY_SKIP
            changed -> MutationDiagnosticOutcome.CHANGED
            else -> MutationDiagnosticOutcome.UNCHANGED
        }
}
