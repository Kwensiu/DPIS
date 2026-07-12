package com.dpis.module.fonts;

import com.dpis.module.FakePrefs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class FontLibraryArchiveCodecTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void roundTripsFontLibraryWithoutPerAppConfig() throws Exception {
        File source = temporaryFolder.newFile("Example.ttf");
        try (FileOutputStream output = new FileOutputStream(source)) {
            output.write(new byte[] { 0, 1, 0, 0 });
        }
        FontLibraryStore sourceStore = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("source-fonts"));
        sourceStore.registerCopiedFont(source, "Example.ttf", "Example", 1234L,
                FontFileKind.TTF);

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        FontLibraryArchiveCodec.writeArchive(archive, sourceStore);

        FontLibraryStore restoredStore = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("restored-fonts"));
        FontLibraryArchiveCodec.RestoreResult result = FontLibraryArchiveCodec.restoreArchive(
                new ByteArrayInputStream(archive.toByteArray()), restoredStore,
                temporaryFolder.newFolder("temporary"), (file, index) -> true);

        assertEquals(1, result.collectionCount);
        assertEquals(1, result.faceCount);
        assertEquals(0, result.failureCount);
        assertEquals(1, restoredStore.listFonts().size());
        assertEquals("Example", restoredStore.listFonts().get(0).collectionDisplayName);
        assertTrue(restoredStore.resolveFontFile(restoredStore.listFonts().get(0).id).isFile());
    }

    @Test
    public void skipsCollectionWhenFacePreflightFails() throws Exception {
        File source = temporaryFolder.newFile("Example.ttf");
        try (FileOutputStream output = new FileOutputStream(source)) {
            output.write(new byte[] { 0, 1, 0, 0 });
        }
        FontLibraryStore sourceStore = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("source-fonts"));
        sourceStore.registerCopiedFont(source, "Example.ttf", "Example", 1234L,
                FontFileKind.TTF);
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        FontLibraryArchiveCodec.writeArchive(archive, sourceStore);

        FontLibraryStore restoredStore = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("restored-fonts"));
        FontLibraryArchiveCodec.RestoreResult result = FontLibraryArchiveCodec.restoreArchive(
                new ByteArrayInputStream(archive.toByteArray()), restoredStore,
                temporaryFolder.newFolder("temporary"), (file, index) -> false);

        assertEquals(0, result.collectionCount);
        assertEquals(0, result.faceCount);
        assertEquals(1, result.failureCount);
        assertTrue(restoredStore.listFonts().isEmpty());
    }

    @Test
    public void rejectsOversizedManifestBeforeItCanExhaustMemory() throws Exception {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("font-library.tsv"));
            zip.write(new byte[1024 * 1024 + 1]);
            zip.closeEntry();
        }

        FontLibraryStore restoredStore = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("restored-fonts"));
        try {
            FontLibraryArchiveCodec.restoreArchive(new ByteArrayInputStream(archive.toByteArray()),
                    restoredStore, temporaryFolder.newFolder("temporary"), (file, index) -> true);
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("manifest is too large"));
            return;
        }
        throw new AssertionError("Expected oversized manifest rejection");
    }
}
