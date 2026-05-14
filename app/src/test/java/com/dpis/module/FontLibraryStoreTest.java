package com.dpis.module;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class FontLibraryStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void registersImportedFontMetadataFromCopiedFile() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        File source = writeFile("Example.ttf", "fake-font-data");

        FontLibraryEntry entry = store.registerCopiedFontForTest(source, "Example.ttf", 1234L);

        assertNotNull(entry);
        assertTrue(entry.id.startsWith("font_"));
        assertEquals("Example.ttf", entry.displayName);
        assertEquals("Example.ttf", entry.sourceFileName);
        assertTrue(entry.storedFileName.startsWith(entry.id));
        assertTrue(entry.storedFileName.endsWith(".ttf"));
        assertEquals(new File(dir, entry.storedFileName).getAbsolutePath(), entry.storedPath);
        assertEquals(64, entry.sha256.length());
        assertEquals(1234L, entry.importedAtEpochMs);
        assertTrue(new File(entry.storedPath).isFile());

        List<FontLibraryEntry> entries = store.listFonts();
        assertEquals(1, entries.size());
        assertEquals(entry, entries.get(0));
    }

    @Test
    public void reusesExistingFontForDuplicateHash() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        File first = writeFile("First.ttf", "same-font-data");
        File second = writeFile("Second.otf", "same-font-data");

        FontLibraryEntry firstEntry = store.registerCopiedFontForTest(first, "First.ttf", 1000L);
        FontLibraryEntry secondEntry = store.registerCopiedFontForTest(second, "Second.otf", 2000L);

        assertEquals(firstEntry, secondEntry);
        assertEquals(1, store.listFonts().size());
    }

    @Test
    public void replacesStaleDuplicateHashWhenStoredFileIsMissing() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        File first = writeFile("First.ttf", "same-font-data");
        File second = writeFile("Second.otf", "same-font-data");
        FontLibraryEntry staleEntry = store.registerCopiedFontForTest(first, "First.ttf", 1000L);
        assertTrue(new File(staleEntry.storedPath).delete());

        FontLibraryEntry replacement = store.registerCopiedFontForTest(second, "Second.otf", 2000L);

        assertEquals(staleEntry.id, replacement.id);
        assertEquals("Second.otf", replacement.sourceFileName);
        assertTrue(replacement.storedFileName.endsWith(".otf"));
        assertTrue(new File(replacement.storedPath).isFile());
        assertEquals(1, store.listFonts().size());
        assertEquals(replacement, store.findById(replacement.id));
    }

    @Test
    public void deletesUnusedFont() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, new DpiConfigStore(new FakePrefs()));

        assertSame(FontLibraryStore.DeleteResult.DELETED, result);
        assertNull(store.findById(entry.id));
        assertFalse(new File(entry.storedPath).exists());
        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void deleteFailureToWriteMetadataKeepsFileAndEntry() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);
        prefs.setCommitResult(false);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, new DpiConfigStore(new FakePrefs()));

        assertSame(FontLibraryStore.DeleteResult.DELETE_FAILED, result);
        assertEquals(entry, store.findById(entry.id));
        assertTrue(new File(entry.storedPath).isFile());
    }

    @Test
    public void fileDeletionFailureRestoresMetadata() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);
        File storedFile = new File(entry.storedPath);
        assertTrue(storedFile.delete());
        assertTrue(storedFile.mkdir());
        assertTrue(new File(storedFile, "child").createNewFile());

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, new DpiConfigStore(new FakePrefs()));

        assertSame(FontLibraryStore.DeleteResult.DELETE_FAILED, result);
        assertEquals(entry, store.findById(entry.id));
        assertTrue(storedFile.isDirectory());
    }

    @Test
    public void importFailureToWriteMetadataDeletesCopiedFile() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        prefs.setCommitResult(false);

        try {
            store.registerCopiedFontForTest(writeFile("Example.ttf", "fake-font-data"), "Example.ttf", 1234L);
        } catch (java.io.IOException expected) {
            assertEquals(0, dir.listFiles().length);
            return;
        }

        throw new AssertionError("Expected metadata write failure");
    }

    @Test
    public void refusesDeleteWhenFontIsReferenced() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        configStore.setTargetTypefaceId("com.example.app", entry.id);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, configStore);

        assertSame(FontLibraryStore.DeleteResult.IN_USE, result);
        assertEquals(entry, store.findById(entry.id));
        assertTrue(new File(entry.storedPath).isFile());
    }

    @Test
    public void resolvesExistingFontFile() throws Exception {
        FontLibraryStore store = new FontLibraryStore(
                new FakePrefs(),
                temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.otf", "fake-font-data"),
                "Example.otf",
                1234L);

        File resolved = store.resolveFontFile(entry.id);

        assertNotNull(resolved);
        assertEquals(new File(entry.storedPath), resolved);
        assertTrue(resolved.isFile());
    }

    @Test
    public void malformedJsonReturnsEmptyList() throws Exception {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putString("font.library.entries", "{not-json").commit();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));

        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void invalidEntriesAreIgnored() throws Exception {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.library.entries", "[{\"id\":\"font_missing_fields\"}]")
                .commit();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));

        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void blankRequiredFieldEntriesAreIgnored() throws Exception {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.library.entries",
                        "[{\"id\":\"   \",\"displayName\":\"Example.ttf\","
                                + "\"sourceFileName\":\"Example.ttf\","
                                + "\"storedFileName\":\"font_bad.ttf\","
                                + "\"storedPath\":\"   \","
                                + "\"sha256\":\"abc\",\"importedAtEpochMs\":1234}]")
                .commit();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));

        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void quotedAndBackslashSourceFileNameRoundTrips() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        String sourceFileName = "Quote \"Mono\\One\".ttf";

        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Source.ttf", "fake-font-data"),
                sourceFileName,
                1234L);

        FontLibraryEntry roundTripped = store.findById(entry.id);
        assertNotNull(roundTripped);
        assertEquals(sourceFileName, roundTripped.displayName);
        assertEquals(sourceFileName, roundTripped.sourceFileName);
        assertEquals(entry, roundTripped);
    }

    @Test
    public void leadingAndTrailingSpacesInSourceFileNameRoundTrip() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        String sourceFileName = "  Display Font.ttf  ";

        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Source.ttf", "fake-font-data"),
                sourceFileName,
                1234L);

        FontLibraryEntry roundTripped = store.findById(entry.id);
        assertNotNull(roundTripped);
        assertEquals(sourceFileName, roundTripped.displayName);
        assertEquals(sourceFileName, roundTripped.sourceFileName);
    }

    @Test
    public void malformedEscapedMetadataReturnsEmptyList() throws Exception {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.library.entries",
                        "[{\"id\":\"font_bad\",\"displayName\":\"bad\\q\",\"sourceFileName\":\"bad.ttf\","
                                + "\"storedFileName\":\"font_bad.ttf\",\"storedPath\":\"missing.ttf\","
                                + "\"sha256\":\"abc\",\"importedAtEpochMs\":1234}]")
                .commit();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));

        assertTrue(store.listFonts().isEmpty());
    }

    private File writeFile(String name, String content) throws Exception {
        File file = temporaryFolder.newFile(name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}
