package com.dpis.module;

import com.dpis.module.fonts.FontFileKind;
import com.dpis.module.fonts.FontFace;
import com.dpis.module.fonts.FontLibraryEntry;
import com.dpis.module.fonts.FontLibraryStore;
import com.dpis.module.fonts.FontPublicationStatus;

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

        FontLibraryEntry entry = store.registerCopiedFont(source, "Example.ttf", 1234L);

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

        FontLibraryEntry firstEntry = store.registerCopiedFont(first, "First.ttf", 1000L);
        FontLibraryEntry secondEntry = store.registerCopiedFont(second, "Second.otf", 2000L);

        assertEquals(firstEntry, secondEntry);
        assertEquals(1, store.listFonts().size());
    }

    @Test
    public void keepsPrivateFontWhenRootPublicationFails() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File privateDirectory = temporaryFolder.newFolder("private-fonts");
        File unavailablePublicDirectory = new File(privateDirectory, "missing/public-fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, privateDirectory,
                unavailablePublicDirectory, null, unused -> false);

        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "private-font-data"), "Example.ttf", 1234L);

        assertSame(FontPublicationStatus.PUBLISH_FAILED, entry.publicationStatus);
        assertEquals(entry.id, entry.collectionId);
        assertTrue(new File(entry.storedPath).isFile());
        assertEquals(entry.storedPath, store.resolveFontFile(entry.id).getAbsolutePath());
    }

    @Test
    public void rebuildsCatalogForAuthoritativePrivateFontWhenCatalogWasOverwritten()
            throws Exception {
        FakePrefs prefs = new FakePrefs();
        File directory = temporaryFolder.newFolder("fonts");
        FontLibraryStore originalStore = new FontLibraryStore(prefs, directory);
        File source = temporaryFolder.newFile("Example.ttf");
        try (FileOutputStream output = new FileOutputStream(source)) {
            output.write(new byte[] { 0, 1, 0, 0 });
        }
        FontLibraryEntry original = originalStore.registerCopiedFont(
                source, "Example.ttf", 1234L);
        prefs.edit().remove("font.library.entries").commit();

        FontLibraryStore.RecoveryResult result = new FontLibraryStore(prefs, directory)
                .recoverMissingCatalogEntries();

        assertTrue(result.catalogUpdated);
        assertEquals(1, result.recoveredEntryCount);
        FontLibraryEntry recovered = new FontLibraryStore(prefs, directory).findById(original.id);
        assertNotNull(recovered);
        assertEquals(original.storedPath, recovered.storedPath);
        assertEquals(original.sha256, recovered.sha256);
    }

    @Test
    public void migratesLegacyCatalogIntoDedicatedLocalPreferencesExactlyOnce() throws Exception {
        FakePrefs dedicatedPrefs = new FakePrefs();
        FakePrefs legacyPrefs = new FakePrefs();
        legacyPrefs.edit().putString("font.library.entries", "[]").commit();

        new FontLibraryStore(dedicatedPrefs, temporaryFolder.newFolder("fonts"), null, legacyPrefs);

        assertEquals("[]", dedicatedPrefs.getString("font.library.entries", null));
        assertFalse(legacyPrefs.contains("font.library.entries"));
    }

    @Test
    public void writesPublicationStatusWithCatalogEntry() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));

        store.registerCopiedFont(writeFile("Example.ttf", "font-data"), "Example.ttf", 1234L);

        assertTrue(prefs.getString("font.library.entries", "").contains("publicationStatus"));
        assertTrue(prefs.getString("font.library.entries", "").contains("collectionId"));
    }

    @Test
    public void derivesCollectionAndFaceFromLegacyTtcId() {
        FontFace face = FontFace.fromLegacyId("font_abcdef_ttc_7");

        assertNotNull(face);
        assertEquals("font_abcdef", face.collectionId);
        assertEquals(7, face.ttcIndex);
        assertEquals("font_abcdef_ttc_7", face.toLegacyId());
    }

    @Test
    public void preservesLegacyTtcFaceZeroId() {
        FontFace face = FontFace.fromLegacyId("font_abcdef_ttc_0");

        assertNotNull(face);
        assertEquals(0, face.ttcIndex);
        assertTrue(face.collectionFace);
        assertEquals("font_abcdef_ttc_0", face.toLegacyId());
    }

    @Test
    public void deletesAllFacesWhenDeletingOneTtcFace() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        List<FontLibraryEntry> faces = store.registerCopiedFontFaces(
                writeFile("Collection.ttc", "ttc-data"), "Collection.ttc", "Collection",
                FontFileKind.TTC, List.of(0, 1), 1234L);

        assertSame(FontLibraryStore.DeleteResult.DELETED,
                store.deleteFont(faces.get(0).id, unused -> false));
        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void healthReportSeparatesMissingAndOrphanedPrivateFiles() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(new FakePrefs(), directory);
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "font-data"), "Example.ttf", 1234L);
        assertTrue(new File(entry.storedPath).delete());
        writeFileIn(directory, "orphan.ttf", "orphan-data");

        FontLibraryStore.HealthReport health = store.inspectHealth();

        assertEquals(1, health.catalogEntryCount);
        assertEquals(1, health.missingPrivateFileCount);
        assertEquals(0, health.missingPublishedFallbackCount);
        assertEquals(1, health.orphanedPrivateFileCount);
    }

    @Test
    public void healthReportTreatsHashMismatchedPublishedFallbackAsMissing() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File privateDirectory = temporaryFolder.newFolder("private-fonts");
        File publicDirectory = temporaryFolder.newFolder("public-fonts");
        FontLibraryStore store = new FontLibraryStore(
                prefs, privateDirectory, publicDirectory, null, unused -> false);
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "private-font-data"), "Example.ttf", 1234L);
        writeFileIn(publicDirectory, "dpis_" + entry.storedFileName, "different-public-data");
        String catalog = prefs.getString("font.library.entries", "")
                .replace("PUBLISH_FAILED", "PUBLISHED")
                .replace("PRIVATE", "PUBLISHED");
        prefs.edit().putString("font.library.entries", catalog).commit();

        FontLibraryStore.HealthReport health = store.inspectHealth();

        assertEquals(1, health.missingPublishedFallbackCount);
    }

    @Test
    public void replacesStaleDuplicateHashWhenStoredFileIsMissing() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        File first = writeFile("First.ttf", "same-font-data");
        File second = writeFile("Second.otf", "same-font-data");
        FontLibraryEntry staleEntry = store.registerCopiedFont(first, "First.ttf", 1000L);
        assertTrue(new File(staleEntry.storedPath).delete());

        FontLibraryEntry replacement = store.registerCopiedFont(second, "Second.otf", 2000L);

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
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, unused -> false);

        assertSame(FontLibraryStore.DeleteResult.DELETED, result);
        assertNull(store.findById(entry.id));
        assertFalse(new File(entry.storedPath).exists());
        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void deleteFailureToWriteMetadataKeepsFileAndEntry() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);
        prefs.setCommitResult(false);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, unused -> false);

        assertSame(FontLibraryStore.DeleteResult.DELETE_FAILED, result);
        assertEquals(entry, store.findById(entry.id));
        assertTrue(new File(entry.storedPath).isFile());
    }

    @Test
    public void fileDeletionFailureRestoresMetadata() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);
        File storedFile = new File(entry.storedPath);
        assertTrue(storedFile.delete());
        assertTrue(storedFile.mkdir());
        assertTrue(new File(storedFile, "child").createNewFile());

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, unused -> false);

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
            store.registerCopiedFont(writeFile("Example.ttf", "fake-font-data"), "Example.ttf", 1234L);
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
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);
        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, fontId -> fontId.equals(entry.id));

        assertSame(FontLibraryStore.DeleteResult.IN_USE, result);
        assertEquals(entry, store.findById(entry.id));
        assertTrue(new File(entry.storedPath).isFile());
    }

    @Test
    public void resolvesExistingFontFile() throws Exception {
        FontLibraryStore store = new FontLibraryStore(
                new FakePrefs(),
                temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFont(
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
    public void deletesPrivateFontWhenPublishedFallbackCleanupCannotUseRoot() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File privateDirectory = temporaryFolder.newFolder("private-fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, privateDirectory,
                temporaryFolder.newFolder("public-fonts"), null, unused -> false);
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"), "Example.ttf", 1234L);
        String catalog = prefs.getString("font.library.entries", "")
                .replace("PUBLISH_FAILED", "PUBLISHED");
        prefs.edit().putString("font.library.entries", catalog).commit();

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, unused -> false);

        assertSame(FontLibraryStore.DeleteResult.DELETED, result);
        assertFalse(new File(entry.storedPath).exists());
        assertTrue(store.listFonts().isEmpty());
    }

    @Test
    public void purgeKeepsFontFilesWhenCatalogIsUnreadable() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File directory = temporaryFolder.newFolder("fonts");
        File fontFile = writeFileIn(directory, "font_preserve.ttf", "font-data");
        File stagingFile = writeFileIn(directory, "font_import_stale.ttf", "temporary-data");
        prefs.edit().putString("font.library.entries", "{not-json").commit();
        FontLibraryStore store = new FontLibraryStore(prefs, directory);

        store.purgeOrphanedFiles();

        assertTrue(fontFile.isFile());
        assertFalse(stagingFile.exists());
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

        FontLibraryEntry entry = store.registerCopiedFont(
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

        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Source.ttf", "fake-font-data"),
                sourceFileName,
                1234L);

        FontLibraryEntry roundTripped = store.findById(entry.id);
        assertNotNull(roundTripped);
        assertEquals("Display Font.ttf", roundTripped.displayName);
        assertEquals(sourceFileName, roundTripped.sourceFileName);
    }

    @Test
    public void registersImportedFontWithCustomDisplayName() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));

        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Source.ttf", "fake-font-data"),
                "Source.ttf",
                "Friendly Name",
                1234L);

        assertEquals("Friendly Name", entry.displayName);
        assertEquals("Source.ttf", entry.sourceFileName);
    }

    @Test
    public void importedDisplayNamesAreUnique() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        store.registerCopiedFont(writeFile("One.ttf", "one"), "One.ttf", "Friendly", 1L);

        FontLibraryEntry second = store.registerCopiedFont(
                writeFile("Two.ttf", "two"),
                "Two.ttf",
                "friendly",
                2L);

        assertEquals("friendly (2)", second.displayName);
    }

    @Test
    public void renamesImportedFontWhenNameIsUnique() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);

        FontLibraryStore.RenameResult result = store.renameFont(entry.id, "Display Name");

        assertSame(FontLibraryStore.RenameResult.RENAMED, result);
        assertEquals("Display Name", store.findById(entry.id).displayName);
        assertEquals("Display Name", store.findById(entry.id).collectionDisplayName);
    }

    @Test
    public void renamesTtcCollectionWithoutOverwritingFaceLabels() throws Exception {
        FontLibraryStore store = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("fonts"));
        List<FontLibraryEntry> faces = store.registerCopiedFontFaces(
                writeFile("Collection.ttc", "same-ttc-data"),
                "Collection.ttc",
                "Collection.ttc",
                FontFileKind.TTC,
                List.of(0, 1),
                1234L);
        String firstFaceLabel = faces.get(0).displayName;
        String secondFaceLabel = faces.get(1).displayName;

        assertSame(FontLibraryStore.RenameResult.RENAMED,
                store.renameFont(faces.get(0).id, "Collection Alias"));

        assertEquals(firstFaceLabel, store.findById(faces.get(0).id).displayName);
        assertEquals(secondFaceLabel, store.findById(faces.get(1).id).displayName);
        assertEquals("Collection Alias", store.findById(faces.get(0).id).collectionDisplayName);
        assertEquals("Collection Alias", store.findById(faces.get(1).id).collectionDisplayName);
    }

    @Test
    public void disambiguatesTtcCollectionAliasesAcrossDifferentFiles() throws Exception {
        FontLibraryStore store = new FontLibraryStore(new FakePrefs(),
                temporaryFolder.newFolder("fonts"));
        List<FontLibraryEntry> first = store.registerCopiedFontFaces(
                writeFile("First.ttc", "first-ttc-data"),
                "First.ttc",
                "Shared collection",
                FontFileKind.TTC,
                List.of(0),
                1234L);
        List<FontLibraryEntry> second = store.registerCopiedFontFaces(
                writeFile("Second.ttc", "second-ttc-data"),
                "Second.ttc",
                "Shared collection",
                FontFileKind.TTC,
                List.of(0),
                1235L);

        assertEquals("Shared collection", first.get(0).collectionDisplayName);
        assertEquals("Shared collection (2)", second.get(0).collectionDisplayName);
    }

    @Test
    public void renameRejectsDuplicateDisplayName() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        store.registerCopiedFont(writeFile("One.ttf", "one"), "One.ttf", "One", 1L);
        FontLibraryEntry second = store.registerCopiedFont(
                writeFile("Two.ttf", "two"),
                "Two.ttf",
                "Two",
                2L);

        FontLibraryStore.RenameResult result = store.renameFont(second.id, "one");

        assertSame(FontLibraryStore.RenameResult.DUPLICATE_NAME, result);
        assertEquals("Two", store.findById(second.id).displayName);
    }

    @Test
    public void renameRejectsBlankDisplayName() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFont(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);

        FontLibraryStore.RenameResult result = store.renameFont(entry.id, " \n ");

        assertSame(FontLibraryStore.RenameResult.INVALID_NAME, result);
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

    @Test
    public void oldMetadataDefaultsTtcIndexToZero() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        File stored = new File(dir, "font_old.ttf");
        assertTrue(stored.createNewFile());
        prefs.edit()
                .putString("font.library.entries",
                        "[{\"id\":\"font_old\",\"displayName\":\"Old\",\"sourceFileName\":\"Old.ttf\","
                                + "\"storedFileName\":\"font_old.ttf\",\"storedPath\":\""
                                + stored.getAbsolutePath().replace("\\", "\\\\")
                                + "\",\"sha256\":\"abcdef\",\"importedAtEpochMs\":1234}]")
                .commit();

        FontLibraryStore store = new FontLibraryStore(prefs, dir);

        assertEquals(0, store.findById("font_old").ttcIndex);
    }

    @Test
    public void registersMultipleTtcFacesAgainstOneStoredFile() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);

        List<FontLibraryEntry> entries = store.registerCopiedFontFaces(
                writeFile("Collection.ttc", "same-ttc-data"),
                "Collection.ttc",
                "Collection",
                FontFileKind.TTC,
                List.of(0, 2),
                1234L);

        assertEquals(2, entries.size());
        assertTrue(entries.get(0).id.endsWith("_ttc_0"));
        assertTrue(entries.get(1).id.endsWith("_ttc_2"));
        assertEquals(0, entries.get(0).ttcIndex);
        assertEquals(2, entries.get(1).ttcIndex);
        assertEquals(entries.get(0).storedPath, entries.get(1).storedPath);
        assertTrue(entries.get(0).storedFileName.endsWith(".ttc"));
        assertEquals(2, store.listFonts().size());
    }

    @Test
    public void reusesExistingTtcFaceByHashAndIndex() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        File first = writeFile("First.ttc", "same-ttc-data");
        File second = writeFile("Second.ttc", "same-ttc-data");

        List<FontLibraryEntry> firstEntries = store.registerCopiedFontFaces(
                first, "First.ttc", "First", FontFileKind.TTC, List.of(1), 1000L);
        List<FontLibraryEntry> secondEntries = store.registerCopiedFontFaces(
                second, "Second.ttc", "Second", FontFileKind.TTC, List.of(1), 2000L);

        assertEquals(firstEntries.get(0), secondEntries.get(0));
        assertEquals(1, store.listFonts().size());
    }

    @Test
    public void deletingOneTtcFaceDeletesTheWholeCollection() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        List<FontLibraryEntry> entries = store.registerCopiedFontFaces(
                writeFile("Collection.ttc", "same-ttc-data"),
                "Collection.ttc",
                "Collection",
                FontFileKind.TTC,
                List.of(0, 1),
                1234L);
        File sharedFile = new File(entries.get(0).storedPath);

        FontLibraryStore.DeleteResult result = store.deleteFont(
                entries.get(0).id,
                unused -> false);

        assertSame(FontLibraryStore.DeleteResult.DELETED, result);
        assertNull(store.findById(entries.get(0).id));
        assertNull(store.findById(entries.get(1).id));
        assertFalse(sharedFile.exists());
    }

    @Test
    public void deletingACollectionTwiceReportsNotFound() throws Exception {
        FakePrefs prefs = new FakePrefs();
        FontLibraryStore store = new FontLibraryStore(prefs, temporaryFolder.newFolder("fonts"));
        List<FontLibraryEntry> entries = store.registerCopiedFontFaces(
                writeFile("Collection.ttc", "same-ttc-data"),
                "Collection.ttc",
                "Collection",
                FontFileKind.TTC,
                List.of(0, 1),
                1234L);
        File sharedFile = new File(entries.get(0).storedPath);
        assertSame(FontLibraryStore.DeleteResult.DELETED,
                store.deleteFont(entries.get(0).id, unused -> false));

        FontLibraryStore.DeleteResult result = store.deleteFont(
                entries.get(1).id,
                unused -> false);

        assertSame(FontLibraryStore.DeleteResult.NOT_FOUND, result);
        assertFalse(sharedFile.exists());
    }

    @Test
    public void ttcBatchCommitFailureLeavesNoStoredFiles() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        prefs.setCommitResult(false);

        try {
            store.registerCopiedFontFaces(
                    writeFile("Collection.ttc", "same-ttc-data"),
                    "Collection.ttc",
                    "Collection",
                    FontFileKind.TTC,
                    List.of(0, 1),
                    1234L);
        } catch (java.io.IOException expected) {
            assertEquals(0, dir.listFiles().length);
            return;
        }

        throw new AssertionError("Expected metadata write failure");
    }

    private File writeFile(String name, String content) throws Exception {
        File file = temporaryFolder.newFile(name);
        return writeFile(file, content);
    }

    private File writeFileIn(File directory, String name, String content) throws Exception {
        return writeFile(new File(directory, name), content);
    }

    private static File writeFile(File file, String content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}
