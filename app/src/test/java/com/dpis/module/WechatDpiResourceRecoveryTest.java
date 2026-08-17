package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WechatDpiResourceRecoveryTest {
    @Test
    public void reapplyDecisionPreservesConfiguredIndependentDpiAfterMetricsDrift() {
        assertEquals("reapplied", WechatDpiRecoveryPolicy.decision(368, 480));
        assertEquals("confirmed", WechatDpiRecoveryPolicy.decision(368, 368));
    }

    @Test
    public void reapplyDecisionRejectsMissingConfigurationAndInvalidMetrics() {
        assertEquals("config_missing", WechatDpiRecoveryPolicy.decision(0, 480));
        assertEquals("metrics_invalid", WechatDpiRecoveryPolicy.decision(368, 0));
    }
}
