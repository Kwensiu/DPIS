package com.dpis.module;

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
        assertNull(presentation.normalizedRawOrNull(raw));
    }

    @Test
    public void unknownDomainsKeepCustomPresentation() {
        String raw = HookDomainOverrideStore.formatCsv(
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                orderedSet("removed_domain"));

        FontHookDomainPresentation presentation =
                FontHookDomainPresentation.forRecommendedTemplateRaw(raw);

        assertFalse(presentation.displaysAsAutomatic());
        assertEquals(raw, presentation.normalizedRawOrNull(raw));
    }

    private static LinkedHashSet<String> orderedSet(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }
}
