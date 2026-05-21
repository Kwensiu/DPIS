package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class PublishedFontFileResolverTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesPublishedTtfByTypefaceId() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");
        File font = new File(directory, "dpis_font_abcd1234.ttf");
        assertTrue(font.createNewFile());

        assertEquals(font, PublishedFontFileResolver.resolveInDirectory(directory, "font_abcd1234"));
    }

    @Test
    public void resolvesPublishedOtfWhenTtfMissing() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");
        File font = new File(directory, "dpis_font_abcd1234.otf");
        assertTrue(font.createNewFile());

        assertEquals(font, PublishedFontFileResolver.resolveInDirectory(directory, "font_abcd1234"));
    }

    @Test
    public void resolvesPublishedTtcWhenOtherFormatsMissing() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");
        File font = new File(directory, "dpis_font_abcd1234.ttc");
        assertTrue(font.createNewFile());

        assertEquals(font, PublishedFontFileResolver.resolveInDirectory(directory, "font_abcd1234"));
    }

    @Test
    public void resolvesSharedPublishedTtcForFaceTypefaceId() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");
        File font = new File(directory, "dpis_font_abcd1234.ttc");
        assertTrue(font.createNewFile());

        assertEquals(font, PublishedFontFileResolver.resolveInDirectory(directory, "font_abcd1234_ttc_2"));
    }

    @Test
    public void returnsNullForMissingOrBlankTypefaceId() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");

        assertNull(PublishedFontFileResolver.resolveInDirectory(directory, null));
        assertNull(PublishedFontFileResolver.resolveInDirectory(directory, " "));
        assertNull(PublishedFontFileResolver.resolveInDirectory(directory, "font_missing"));
    }
}
