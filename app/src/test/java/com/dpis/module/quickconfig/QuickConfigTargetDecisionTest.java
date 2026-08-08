package com.dpis.module.quickconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class QuickConfigTargetDecisionTest {
    @Test
    public void missingUsageAccessRequestsAuthorizationBeforeResolvingForegroundApp() {
        QuickConfigTargetDecision.Result result = QuickConfigTargetDecision.decide(
                null, false, null);

        assertEquals(QuickConfigTargetDecision.Kind.REQUEST_USAGE_ACCESS, result.kind());
        assertNull(result.packageName());
    }

    @Test
    public void grantedUsageAccessUsesResolvedForegroundPackage() {
        QuickConfigTargetDecision.Result result = QuickConfigTargetDecision.decide(
                null, true, "com.example.target");

        assertEquals(QuickConfigTargetDecision.Kind.TARGET, result.kind());
        assertEquals("com.example.target", result.packageName());
    }

    @Test
    public void explicitPackageDoesNotRequireUsageAccess() {
        QuickConfigTargetDecision.Result result = QuickConfigTargetDecision.decide(
                "com.example.explicit", false, null);

        assertEquals(QuickConfigTargetDecision.Kind.TARGET, result.kind());
        assertEquals("com.example.explicit", result.packageName());
    }

    @Test
    public void grantedUsageAccessWithoutTargetRemainsUnavailable() {
        QuickConfigTargetDecision.Result result = QuickConfigTargetDecision.decide(
                null, true, null);

        assertEquals(QuickConfigTargetDecision.Kind.UNAVAILABLE, result.kind());
        assertNull(result.packageName());
    }
}
