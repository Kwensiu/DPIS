package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FontHookArbitrationTest {
    @Test
    public void fieldRewriteUsesResourcesFontDomainAndSkipsTextViewFallbacks() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertTrue(plan.resourcesFontEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.textViewSpRewriteEnabled);
        assertFalse(plan.textViewAbsoluteRewriteEnabled);
        assertFalse(plan.textViewCurrentPxFallbackEnabled);
        assertFalse(plan.paintFallbackEnabled);
        assertFalse(plan.flutterSettingsEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
        assertFalse(plan.genericNativeFlutterEnabled);
    }

    @Test
    public void activeEmulationEnablesSemanticFontDomainsOnly() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        assertTrue(plan.resourcesFontEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.textViewSpRewriteEnabled);
        assertFalse(plan.textViewAbsoluteRewriteEnabled);
        assertFalse(plan.textViewCurrentPxFallbackEnabled);
        assertFalse(plan.paintFallbackEnabled);
        assertFalse(plan.flutterSettingsEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
        assertFalse(plan.genericNativeFlutterEnabled);
        assertTrue("semantic-font-domain-plan".equals(plan.reason));
    }

    @Test
    public void inactiveFontScaleDisablesAllFontDomains() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(false, false);

        assertFalse(plan.resourcesFontEnabled);
        assertFalse(plan.webViewTextZoomEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.textViewSpRewriteEnabled);
        assertFalse(plan.textViewAbsoluteRewriteEnabled);
        assertFalse(plan.textViewCurrentPxFallbackEnabled);
        assertFalse(plan.paintFallbackEnabled);
        assertFalse(plan.flutterSettingsEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
        assertFalse(plan.genericNativeFlutterEnabled);
        assertTrue("font-scale-disabled".equals(plan.reason));
    }

    @Test
    public void hyperOsNativeFlutterIsArbitratedWithActiveFontDomains() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true, true);

        assertTrue(plan.resourcesFontEnabled);
        assertFalse(plan.flutterSettingsEnabled);
        assertTrue(plan.hyperOsNativeFlutterEnabled);
        assertFalse(plan.genericNativeFlutterEnabled);
    }

    @Test
    public void inactiveFontScaleSuppressesHyperOsNativeFlutterDomain() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(false, false, true);

        assertFalse(plan.flutterSettingsEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
        assertFalse(plan.genericNativeFlutterEnabled);
    }
}
