package com.dpis.module.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticFinishPolicyTest {
    @Test
    fun finishesOnlyAfterTargetLaunchWhenDpisResumes() {
        assertTrue(
            DiagnosticFinishPolicy.canFinishAfterDpisResume(
                running = true,
                targetLaunchStarted = true,
                finishing = false,
            )
        )
        assertFalse(
            DiagnosticFinishPolicy.canFinishAfterDpisResume(
                running = false,
                targetLaunchStarted = true,
                finishing = false,
            )
        )
        assertFalse(
            DiagnosticFinishPolicy.canFinishAfterDpisResume(
                running = true,
                targetLaunchStarted = false,
                finishing = false,
            )
        )
        assertFalse(
            DiagnosticFinishPolicy.canFinishAfterDpisResume(
                running = true,
                targetLaunchStarted = true,
                finishing = true,
            )
        )
    }
}
