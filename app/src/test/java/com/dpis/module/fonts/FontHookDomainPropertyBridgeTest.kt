package com.dpis.module

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertyBridge
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontHookDomainPropertyBridgeTest {
    @Test
    fun maskRoundTripsCustomizableDomainsInStableOrder() {
        val domains = setOf(
            FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
            FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
            FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER,
        )

        val mask = FontHookDomainPropertyBridge.encodeMask(domains)

        assertEquals(148, mask)
        assertEquals(domains, FontHookDomainPropertyBridge.decodeMask(mask))
    }

    @Test
    fun legacyV2MaskValuesDoNotShiftIntoNewActivityThreadDomain() {
        val parsed = FontHookDomainPropertyBridge.parseOverrideValueForTest(
            TEST_PACKAGE_NAME,
            "v2:a44631ddc484:149",
        )

        assertTrue(parsed.customPathEnabled)
        assertEquals(
            setOf(
                FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
                FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER,
            ),
            parsed.enabledKnownDomains,
        )
        assertFalse(parsed.enabledKnownDomains.contains(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT))
    }

    @Test
    fun systemModeDomainsAreNotEncodedAsCompatCustomMaskBits() {
        val mask = FontHookDomainPropertyBridge.encodeMask(
            setOf(
                FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT,
                FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT,
            ),
        )

        assertEquals(0, mask)
        assertTrue(FontHookDomainPropertyBridge.decodeMask(mask).isEmpty())
    }

    @Test
    fun propertyNamesUseStablePackageHash() {
        assertEquals(
            "debug.dpis.hookdomains.0b666619",
            FontHookDomainPropertyBridge.propertyNameForPackage(TEST_PACKAGE_NAME),
        )
        assertEquals(
            "persist.debug.dpis.hookdomains.0b666619",
            FontHookDomainPropertyBridge.persistentPropertyNameForPackage(TEST_PACKAGE_NAME),
        )
    }

    @Test
    fun parseDistinguishesAutomaticFromEmptyCustomPath() {
        val automatic = FontHookDomainPropertyBridge.parseOverrideValueForTest("0")
        val emptyCustom = FontHookDomainPropertyBridge.parseOverrideValueForTest("1")

        assertFalse(automatic.customPathEnabled)
        assertTrue(emptyCustom.customPathEnabled)
        assertTrue(emptyCustom.enabledKnownDomains.isEmpty())
    }

    @Test
    fun v2ValueRequiresMatchingPackageCheck() {
        val value = FontHookDomainPropertyBridge.encodeOverrideValue(
            TEST_PACKAGE_NAME,
            setOf(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK),
        )

        val matching = FontHookDomainPropertyBridge.parseOverrideValueForTest(
            TEST_PACKAGE_NAME,
            value,
        )
        val mismatched = FontHookDomainPropertyBridge.parseOverrideValueForTest(
            TEST_PACKAGE_NAME,
            "v2:24473df468cb:17",
        )

        assertTrue(value.startsWith("v2:a44631ddc484:"))
        assertTrue(matching.customPathEnabled)
        assertEquals(
            setOf(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK),
            matching.enabledKnownDomains,
        )
        assertFalse(mismatched.customPathEnabled)
    }

    private companion object {
        const val TEST_PACKAGE_NAME = "com.example.dpis.test"
    }
}
