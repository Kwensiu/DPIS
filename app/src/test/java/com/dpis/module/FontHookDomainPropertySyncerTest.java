package com.dpis.module;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

public class FontHookDomainPropertySyncerTest {
    @Test
    public void publishWritesRuntimeAndPersistentMask() {
        assertEquals("setprop 'debug.dpis.hookdomains.8eeb79b0' 'v2:c9d161436703:25'; "
                        + "setprop 'persist.debug.dpis.hookdomains.8eeb79b0' "
                        + "'v2:c9d161436703:25'",
                FontHookDomainPropertySyncer.buildPublishCommandForTest(
                        "org.telegram.messenger",
                        Set.of(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK,
                                FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK)));
    }

    @Test
    public void emptyCustomPathPublishesDistinctNonAutomaticValue() {
        assertEquals("setprop 'debug.dpis.hookdomains.8eeb79b0' 'v2:c9d161436703:1'; "
                        + "setprop 'persist.debug.dpis.hookdomains.8eeb79b0' "
                        + "'v2:c9d161436703:1'",
                FontHookDomainPropertySyncer.buildPublishCommandForTest(
                        "org.telegram.messenger",
                        Set.of()));
    }

    @Test
    public void clearWritesAutomaticMarker() {
        assertEquals("setprop 'debug.dpis.hookdomains.8eeb79b0' '0'; "
                        + "setprop 'persist.debug.dpis.hookdomains.8eeb79b0' '0'",
                FontHookDomainPropertySyncer.buildClearCommandForTest(
                        "org.telegram.messenger"));
    }
}
