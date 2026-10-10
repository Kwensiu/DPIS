package com.dpis.module

import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HookDomainPlanTest {
    @Test
    fun derivesCapabilitiesFromEnabledDomains() {
        val domains = linkedSetOf(
            FontHookDomainRegistry.ID_RESOURCES_FONT,
            FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE,
            FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM,
        )
        val plan = HookDomainPlan(domains, emptySet(), emptySet(), "auto", "test")

        assertTrue(plan.hasResourcesFont())
        assertTrue(plan.hasTextViewHooks())
        assertTrue(plan.hasTextViewSpRewrite())
        assertFalse(plan.hasTextViewAbsoluteRewrite())
        assertTrue(plan.hasWebViewTextZoom())
        assertFalse(plan.hasFlutterSettings())
        assertFalse(plan.hasHyperOsNativeFlutter())
        assertFalse(plan.hasActivityThreadFont())
        assertFalse(plan.hasSystemServerFont())
    }

    @Test
    fun csvOutputMatchesRegistryOrder() {
        val domains = linkedSetOf(
            FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM,
            FontHookDomainRegistry.ID_RESOURCES_FONT,
            FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT,
        )
        val plan = HookDomainPlan(domains, emptySet(), emptySet(), "auto", "test")

        assertTrue(plan.hasSystemServerFont())
        assertEquals(
            "resources_font,system_server_font,webview_text_zoom",
            plan.enabledDomainsCsv()
        )
    }

    @Test
    fun registryCustomizableDomainsStayCompatOnly() {
        assertEquals(
            FontHookDomainRegistry.ID_RESOURCES_FONT,
            FontHookDomainRegistry.orderedCustomizableDisplayIdsList()[0]
        )
        assertFalse(
            FontHookDomainRegistry.automaticCustomizableDomains()
                .contains(FontHookDomainRegistry.ID_RESOURCES_FONT)
        )
        assertFalse(
            FontHookDomainRegistry.orderedCustomizableDisplayIdsList()
                .contains(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT)
        )
        assertFalse(
            FontHookDomainRegistry.orderedCustomizableDisplayIdsList()
                .contains(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT)
        )
        assertFalse(
            FontHookDomainRegistry.automaticCustomizableDomains()
                .contains(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT)
        )
        assertFalse(
            FontHookDomainRegistry.automaticCustomizableDomains()
                .contains(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT)
        )
    }

    @Test
    fun toFontDomainPlanRoundTrips() {
        val plan = HookDomainPlan(
            linkedSetOf(
                FontHookDomainRegistry.ID_RESOURCES_FONT,
                FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE,
                FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK,
                FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
                FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM,
                FontHookDomainRegistry.ID_FLUTTER_SETTINGS,
            ),
            emptySet(),
            emptySet(),
            "auto",
            "field-rewrite",
        )

        val fontPlan: FontHookArbitration.FontDomainPlan = plan.toFontDomainPlan()
        assertTrue(fontPlan.resourcesFontEnabled)
        assertTrue(fontPlan.textViewHooksEnabled)
        assertTrue(fontPlan.textViewSpRewriteEnabled)
        assertTrue(fontPlan.textViewAbsoluteRewriteEnabled)
        assertTrue(fontPlan.textViewCurrentPxFallbackEnabled)
        assertTrue(fontPlan.paintFallbackEnabled)
        assertTrue(fontPlan.webViewTextZoomEnabled)
        assertTrue(fontPlan.flutterSettingsEnabled)
        assertFalse(fontPlan.hyperOsNativeFlutterEnabled)
        assertEquals("field-rewrite", fontPlan.reason)
    }

    @Test
    fun emptyPlanDisablesAll() {
        val plan = HookDomainPlan(emptySet(), emptySet(), emptySet(), "auto", "off")

        assertFalse(plan.hasResourcesFont())
        assertFalse(plan.hasTextViewHooks())
        assertFalse(plan.hasWebViewTextZoom())
        assertFalse(plan.hasFlutterSettings())
        assertFalse(plan.hasHyperOsNativeFlutter())
        assertEquals("", plan.enabledDomainsCsv())
    }

    @Test
    fun unknownDomainsPreserved() {
        val plan =
            HookDomainPlan(emptySet(), emptySet(), setOf("custom_domain_x"), "custom", "test")

        assertEquals("custom_domain_x", plan.unknownDomainsCsv())
    }

    @Test
    fun knownDomainsAreIgnoredInUnknownCsv() {
        val plan = HookDomainPlan(
            emptySet(),
            emptySet(),
            linkedSetOf(FontHookDomainRegistry.ID_RESOURCES_FONT, "custom_domain_x"),
            "custom",
            "test",
        )

        assertEquals("custom_domain_x", plan.unknownDomainsCsv())
    }

    @Test
    fun domainSetsAreImmutable() {
        val plan = HookDomainPlan(
            setOf(FontHookDomainRegistry.ID_RESOURCES_FONT),
            setOf(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM),
            setOf("custom_domain_x"),
            "auto",
            "test",
        )

        assertSetImmutable(plan.enabledDomains)
        assertSetImmutable(plan.builtinDomains)
        assertSetImmutable(plan.unknownCustomDomains)
    }

    private fun assertSetImmutable(domains: Set<String>) {
        try {
            (domains as MutableSet).add("new_domain")
            fail("Expected immutable domain set")
        } catch (_: UnsupportedOperationException) {
            // Execution-plan state must not be mutated by callers.
        }
    }
}
