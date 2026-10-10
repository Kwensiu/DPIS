package com.dpis.module.quickconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickConfigTargetDecisionTest {
    @Test
    fun missingUsageAccessRequestsAuthorizationBeforeResolvingForegroundApp() {
        val result = QuickConfigTargetDecision.decide(null, false, null)
        assertEquals(QuickConfigTargetDecision.Kind.REQUEST_USAGE_ACCESS, result.kind)
        assertNull(result.packageName)
    }

    @Test
    fun grantedUsageAccessUsesResolvedForegroundPackage() {
        val result = QuickConfigTargetDecision.decide(null, true, "com.example.target")
        assertEquals(QuickConfigTargetDecision.Kind.TARGET, result.kind)
        assertEquals("com.example.target", result.packageName)
    }

    @Test
    fun explicitPackageDoesNotRequireUsageAccess() {
        val result = QuickConfigTargetDecision.decide("com.example.explicit", false, null)
        assertEquals(QuickConfigTargetDecision.Kind.TARGET, result.kind)
        assertEquals("com.example.explicit", result.packageName)
    }

    @Test
    fun grantedUsageAccessWithoutTargetRemainsUnavailable() {
        val result = QuickConfigTargetDecision.decide(null, true, null)
        assertEquals(QuickConfigTargetDecision.Kind.UNAVAILABLE, result.kind)
        assertNull(result.packageName)
    }
}
