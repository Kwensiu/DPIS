package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

public class HookDomainPlanTest {

    @Test
    public void derivesCapabilitiesFromEnabledDomains() {
        Set<String> domains = new LinkedHashSet<>(Set.of(
                FontHookDomainRegistry.ID_RESOURCES_FONT,
                FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE,
                FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM));
        HookDomainPlan plan = new HookDomainPlan(domains, Set.of(), Set.of(), "auto", "test");

        assertTrue(plan.hasResourcesFont());
        assertTrue(plan.hasTextViewHooks());
        assertTrue(plan.hasTextViewSpRewrite());
        assertFalse(plan.hasTextViewAbsoluteRewrite());
        assertTrue(plan.hasWebViewTextZoom());
        assertFalse(plan.hasFlutterSettings());
        assertFalse(plan.hasHyperOsNativeFlutter());
        assertFalse(plan.hasActivityThreadFont());
        assertFalse(plan.hasSystemServerFont());
    }

    @Test
    public void csvOutputMatchesRegistryOrder() {
        Set<String> domains = new LinkedHashSet<>(Set.of(
                FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM,
                FontHookDomainRegistry.ID_RESOURCES_FONT,
                FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT));
        HookDomainPlan plan = new HookDomainPlan(domains, Set.of(), Set.of(), "auto", "test");

        assertTrue(plan.hasSystemServerFont());
        assertEquals("resources_font,system_server_font,webview_text_zoom",
                plan.enabledDomainsCsv());
    }

    @Test
    public void registryDisplaysSystemServerFontNearResourceFontButKeepsMaskBitAppended() {
        assertEquals(FontHookDomainRegistry.ID_RESOURCES_FONT,
                FontHookDomainRegistry.orderedCustomizableDisplayIdsList().get(0));
        assertEquals(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT,
                FontHookDomainRegistry.orderedCustomizableDisplayIdsList().get(1));
        assertEquals(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT,
                FontHookDomainRegistry.orderedCustomizableIdsList().get(
                        FontHookDomainRegistry.orderedCustomizableIdsList().size() - 1));
    }

    @Test
    public void toFontDomainPlanRoundTrips() {
        Set<String> domains = new LinkedHashSet<>(Set.of(
                FontHookDomainRegistry.ID_RESOURCES_FONT,
                FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE,
                FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK,
                FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
                FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM,
                FontHookDomainRegistry.ID_FLUTTER_SETTINGS));
        HookDomainPlan plan = new HookDomainPlan(domains, Set.of(), Set.of(), "auto", "field-rewrite");

        FontHookArbitration.FontDomainPlan fontPlan = plan.toFontDomainPlan();
        assertTrue(fontPlan.resourcesFontEnabled);
        assertTrue(fontPlan.textViewHooksEnabled);
        assertTrue(fontPlan.textViewSpRewriteEnabled);
        assertTrue(fontPlan.textViewAbsoluteRewriteEnabled);
        assertTrue(fontPlan.textViewCurrentPxFallbackEnabled);
        assertTrue(fontPlan.paintFallbackEnabled);
        assertTrue(fontPlan.webViewTextZoomEnabled);
        assertTrue(fontPlan.flutterSettingsEnabled);
        assertFalse(fontPlan.hyperOsNativeFlutterEnabled);
        assertEquals("field-rewrite", fontPlan.reason);
    }

    @Test
    public void emptyPlanDisablesAll() {
        HookDomainPlan plan = new HookDomainPlan(Set.of(), Set.of(), Set.of(), "auto", "off");

        assertFalse(plan.hasResourcesFont());
        assertFalse(plan.hasTextViewHooks());
        assertFalse(plan.hasWebViewTextZoom());
        assertFalse(plan.hasFlutterSettings());
        assertFalse(plan.hasHyperOsNativeFlutter());
        assertEquals("", plan.enabledDomainsCsv());
    }

    @Test
    public void unknownDomainsPreserved() {
        Set<String> unknown = Set.of("custom_domain_x");
        HookDomainPlan plan = new HookDomainPlan(Set.of(), Set.of(), unknown, "custom", "test");

        assertEquals("custom_domain_x", plan.unknownDomainsCsv());
    }

    @Test
    public void knownDomainsAreIgnoredInUnknownCsv() {
        Set<String> unknown = new LinkedHashSet<>(Set.of(
                FontHookDomainRegistry.ID_RESOURCES_FONT,
                "custom_domain_x"));
        HookDomainPlan plan = new HookDomainPlan(Set.of(), Set.of(), unknown, "custom", "test");

        assertEquals("custom_domain_x", plan.unknownDomainsCsv());
    }

    @Test
    public void domainSetsAreImmutable() {
        HookDomainPlan plan = new HookDomainPlan(
                Set.of(FontHookDomainRegistry.ID_RESOURCES_FONT),
                Set.of(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM),
                Set.of("custom_domain_x"),
                "auto",
                "test");

        assertSetImmutable(plan.enabledDomains);
        assertSetImmutable(plan.builtinDomains);
        assertSetImmutable(plan.unknownCustomDomains);
    }

    private static void assertSetImmutable(Set<String> domains) {
        try {
            domains.add("new_domain");
            fail("Expected immutable domain set");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable execution-plan state.
        }
    }
}
