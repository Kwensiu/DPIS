package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class FontHookDomainPropertyBridgeTest {
    @Test
    public void maskRoundTripsCustomizableDomainsInStableOrder() {
        int mask = FontHookDomainPropertyBridge.encodeMask(Set.of(
                FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT,
                FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
                FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER));

        assertEquals(298, mask);
        assertEquals(Set.of(
                        FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT,
                        FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                        FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
                        FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER),
                FontHookDomainPropertyBridge.decodeMask(mask));
    }

    @Test
    public void systemServerFontDomainUsesAppendedMaskBit() {
        int mask = FontHookDomainPropertyBridge.encodeMask(Set.of(
                FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT));

        assertEquals(512, mask);
        assertEquals(Set.of(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT),
                FontHookDomainPropertyBridge.decodeMask(mask));
    }

    @Test
    public void propertyNamesUseStablePackageHash() {
        assertEquals("debug.dpis.hookdomains.8eeb79b0",
                FontHookDomainPropertyBridge.propertyNameForPackage("org.telegram.messenger"));
        assertEquals("persist.debug.dpis.hookdomains.8eeb79b0",
                FontHookDomainPropertyBridge.persistentPropertyNameForPackage(
                        "org.telegram.messenger"));
    }

    @Test
    public void parseDistinguishesAutomaticFromEmptyCustomPath() {
        HookDomainOverride automatic = FontHookDomainPropertyBridge.parseOverrideValueForTest("0");
        HookDomainOverride emptyCustom = FontHookDomainPropertyBridge.parseOverrideValueForTest("1");

        assertFalse(automatic.customPathEnabled);
        assertTrue(emptyCustom.customPathEnabled);
        assertTrue(emptyCustom.enabledKnownDomains.isEmpty());
    }

    @Test
    public void v2ValueRequiresMatchingPackageCheck() {
        String value = FontHookDomainPropertyBridge.encodeOverrideValue(
                "org.telegram.messenger",
                Set.of(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK));

        HookDomainOverride matching =
                FontHookDomainPropertyBridge.parseOverrideValueForTest(value);
        HookDomainOverride mismatched =
                FontHookDomainPropertyBridge.parseOverrideValueForTest("v2:24473df468cb:17");

        assertTrue(value.startsWith("v2:c9d161436703:"));
        assertTrue(matching.customPathEnabled);
        assertEquals(Set.of(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK),
                matching.enabledKnownDomains);
        assertFalse(mismatched.customPathEnabled);
    }
}
