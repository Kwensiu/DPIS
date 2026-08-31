package com.dpis.module

import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FontHookDomainPresentationTest {
    @Test
    fun automaticDomainsRawDisplaysAsAutomatic() {
        val raw = HookDomainOverrideStore.formatCsv(
            FontHookDomainRegistry.automaticCustomizableDomains(),
            emptySet(),
        )

        val presentation = FontHookDomainPresentation.forAutomaticDomainsRaw(raw)

        assertTrue(presentation.displaysAsAutomatic())
        assertNull(presentation.normalizedRawOrNull())
    }

    @Test
    fun unknownDomainsKeepCustomPresentation() {
        val raw = "removed_domain," + HookDomainOverrideStore.formatCsv(
            FontHookDomainRegistry.automaticCustomizableDomains(),
            emptySet(),
        )

        val presentation = FontHookDomainPresentation.forAutomaticDomainsRaw(raw)

        assertFalse(presentation.displaysAsAutomatic())
        assertEquals(
            HookDomainOverrideStore.formatCsv(
                FontHookDomainRegistry.automaticCustomizableDomains(),
                linkedSetOf("removed_domain"),
            ),
            presentation.normalizedRawOrNull(),
        )
    }

    @Test
    fun emptyRawRemainsExplicitCustomOptOut() {
        val presentation = FontHookDomainPresentation.forAutomaticDomainsRaw("")

        assertFalse(presentation.displaysAsAutomatic())
        assertEquals("", presentation.normalizedRawOrNull())
    }
}
