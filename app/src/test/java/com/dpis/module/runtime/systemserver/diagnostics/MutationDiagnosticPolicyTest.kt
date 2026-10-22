package com.dpis.module.runtime.systemserver.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MutationDiagnosticPolicyTest {
    @Test
    fun distinguishesNoTargetPolicySkipChangedAndUnchanged() {
        assertEquals(
            MutationDiagnosticOutcome.NO_TARGET,
            MutationDiagnosticPolicy.outcome(false, false, false),
        )
        assertEquals(
            MutationDiagnosticOutcome.POLICY_SKIP,
            MutationDiagnosticPolicy.outcome(true, false, false),
        )
        assertEquals(
            MutationDiagnosticOutcome.CHANGED,
            MutationDiagnosticPolicy.outcome(true, true, true),
        )
        assertEquals(
            MutationDiagnosticOutcome.UNCHANGED,
            MutationDiagnosticPolicy.outcome(true, true, false),
        )
    }

    @Test
    fun callbackStageKeysDoNotSuppressDifferentUsers() {
        val userZero = HookLog.callbackStageKey(
            "activity-start", "source_lookup", "com.example.target", 0,
        )
        val user999 = HookLog.callbackStageKey(
            "activity-start", "source_lookup", "com.example.target", 999,
        )

        assertNotEquals(userZero, user999)
    }
}
