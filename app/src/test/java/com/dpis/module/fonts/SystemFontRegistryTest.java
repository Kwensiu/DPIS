package com.dpis.module.fonts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public final class SystemFontRegistryTest {
    @Test
    public void buildsStableShortSystemFontIds() {
        String id = SystemFontRegistry.buildFontIdForTest("/system/fonts/Roboto-Regular.ttf", 2);

        assertTrue(SystemFontRegistry.isSystemFontId(id));
        assertEquals(id, SystemFontRegistry.buildFontIdForTest("/system/fonts/Roboto-Regular.ttf", 2));
        assertTrue(id.length() <= 32);
    }

    @Test
    public void rejectsInvalidSystemFontIds() {
        assertFalse(SystemFontRegistry.isSystemFontId("font_abcd"));
        assertNull(SystemFontRegistry.buildFontIdForTest(null, 0));
        assertNull(SystemFontRegistry.buildFontIdForTest("", 0));
        assertNull(SystemFontRegistry.buildFontIdForTest("/system/fonts/Roboto-Regular.ttf", -1));
    }

    @Test
    public void buildsSystemFamilyIds() {
        String id = SystemFontRegistry.buildFamilyIdForTest("sans-serif-condensed");

        assertEquals("system-family:sans-serif-condensed", id);
        assertTrue(SystemFontRegistry.isSystemFontId(id));
    }

    @Test
    public void recommendedFontsOnlyIncludeDeclaredFamiliesInOrder() {
        List<SystemFontEntry> entries = SystemFontRegistry.listRecommendedFontsForTest(
                new LinkedHashSet<>(Set.of("monospace", "sans-serif", "missing", "roboto-flex")));

        assertEquals(3, entries.size());
        assertEquals("system-family:sans-serif", entries.get(0).id());
        assertEquals("system-family:monospace", entries.get(1).id());
        assertEquals("system-family:roboto-flex", entries.get(2).id());
    }
}
