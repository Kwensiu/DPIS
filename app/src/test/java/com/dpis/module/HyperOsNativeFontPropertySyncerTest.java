package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HyperOsNativeFontPropertySyncerTest {
    @Test
    public void shellQuoteEscapesSingleQuotes() {
        assertEquals("'debug.dpis.forcefont.a55b5fe1'",
                HyperOsNativeFontPropertySyncer.shellQuoteForTest("debug.dpis.forcefont.a55b5fe1"));
        assertEquals("'a'\\''b'", HyperOsNativeFontPropertySyncer.shellQuoteForTest("a'b"));
        assertEquals("''", HyperOsNativeFontPropertySyncer.shellQuoteForTest(""));
    }
    @Test
    public void buildPublishCommandSetsForceFontPropertyOnly() {
        assertEquals("setprop 'debug.dpis.forcefont.a55b5fe1' '300'",
                HyperOsNativeFontPropertySyncer.buildPublishCommandForTest(
                        "debug.dpis.forcefont.a55b5fe1", 300));
    }
}
