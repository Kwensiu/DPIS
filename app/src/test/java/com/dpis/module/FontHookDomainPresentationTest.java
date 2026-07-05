package com.dpis.module;

import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FontHookDomainPresentationTest {
    @Test
    public void recommendedTemplateRawDisplaysAsAutomatic() {
        String raw = HookDomainOverrideStore.formatCsv(
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                Set.of());

        FontHookDomainPresentation presentation =
                FontHookDomainPresentation.forRecommendedTemplateRaw(raw);

        assertTrue(presentation.displaysAsAutomatic());
        assertNull(presentation.normalizedRawOrNull());
    }

    @Test
    public void unknownDomainsKeepCustomPresentation() {
        String raw = "removed_domain,"
                + HookDomainOverrideStore.formatCsv(
                        FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                        Set.of());

        FontHookDomainPresentation presentation =
                FontHookDomainPresentation.forRecommendedTemplateRaw(raw);

        assertFalse(presentation.displaysAsAutomatic());
        assertEquals(
                HookDomainOverrideStore.formatCsv(
                        FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                        orderedSet("removed_domain")),
                presentation.normalizedRawOrNull());
    }

    @Test
    public void emptyRawRemainsExplicitCustomOptOut() {
        FontHookDomainPresentation presentation =
                FontHookDomainPresentation.forRecommendedTemplateRaw("");

        assertFalse(presentation.displaysAsAutomatic());
        assertEquals("", presentation.normalizedRawOrNull());
    }

    private static LinkedHashSet<String> orderedSet(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }
}
