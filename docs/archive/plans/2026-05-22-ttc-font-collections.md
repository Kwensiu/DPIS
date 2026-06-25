# TTC Font Collections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add experimental TrueType Collection (`.ttc`) import support so users can select one or more TTC faces and use each face independently for per-app font replacement.

**Architecture:** The implementation introduces a small signature/parser layer for font classification, persists `ttcIndex` on `FontLibraryEntry`, extends `FontLibraryStore` with atomic batch registration for TTC faces, and centralizes imported typeface loading through a helper that honors `ttcIndex`. TTC import is gated by a Laboratory preference that blocks new imports only; existing TTC entries remain visible and usable.

**Tech Stack:** Java 17, Android SDK min 26, Material Components, JUnit4, Gradle flavor test tasks.

---

## Execution Preconditions

- Do not implement this plan on `main`.
- Before Task 1, use `superpowers:using-git-worktrees` or create/switch to an isolated feature branch/worktree.
- Keep the existing untracked design spec unless the user explicitly asks to delete or commit it:
  - `docs/superpowers/specs/2026-05-22-ttc-font-collections-design.md`
- Use `apply_patch` for manual edits.
- Run targeted tests after each task, then full debug unit tests at the end.

## File Map

- Create `app/src/main/java/com/dpis/module/FontFileKind.java`
  - Enum for detected font file kind: `TTF`, `OTF`, `TTC`, `UNSUPPORTED`.
- Create `app/src/main/java/com/dpis/module/FontFileInspector.java`
  - Reads the first bytes of an imported file, classifies font kind by signature, and delegates TTC parsing.
- Create `app/src/main/java/com/dpis/module/TtcFontCollectionParser.java`
  - Parses TTC header only: `ttcf`, version, face count, table directory offsets.
- Create `app/src/main/java/com/dpis/module/FontTypefaceLoader.java`
  - Loads imported font files and applies `ttcIndex` for TTC entries.
- Modify `app/src/main/java/com/dpis/module/FontLibraryEntry.java`
  - Add `ttcIndex`.
- Modify `app/src/main/java/com/dpis/module/FontLibraryStore.java`
  - Persist `ttcIndex`, add batch registration, use detected kind for extension and id strategy, and preserve shared files on delete.
- Modify `app/src/main/java/com/dpis/module/PublishedFontFileResolver.java`
  - Resolve `.ttc`.
- Modify `app/src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java`
  - Resolve entry metadata before loading imported fonts.
- Modify `app/src/main/java/com/dpis/module/AppConfigDialogBinder.java`
  - Preview imported TTC entries using `ttcIndex`.
- Modify `app/src/main/java/com/dpis/module/FontLibraryActivity.java`
  - Gate TTC import behind Laboratory preference, classify copied files by signature, and add the TTC face-selection dialog.
- Modify `app/src/main/java/com/dpis/module/DpiConfigStore.java`
  - Add `font.ttc_import_enabled`.
- Modify `app/src/main/java/com/dpis/module/ExperimentalSettingsActivity.java`
  - Replace empty-only page with the TTC switch row.
- Modify `app/src/main/res/layout/activity_experimental_settings.xml`
  - Add switch row container.
- Modify `app/src/main/res/values/strings.xml`
  - Add TTC switch and import dialog strings.
- Add/modify tests under `app/src/test/java/com/dpis/module/`.

---

### Task 1: Font Signature Classification And TTC Header Parser

**Files:**
- Create: `app/src/main/java/com/dpis/module/FontFileKind.java`
- Create: `app/src/main/java/com/dpis/module/TtcFontCollectionParser.java`
- Create: `app/src/main/java/com/dpis/module/FontFileInspector.java`
- Test: `app/src/test/java/com/dpis/module/TtcFontCollectionParserTest.java`

- [ ] **Step 1: Write parser tests**

Create `app/src/test/java/com/dpis/module/TtcFontCollectionParserTest.java`:

```java
package com.dpis.module;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TtcFontCollectionParserTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesValidTtcHeader() throws Exception {
        File file = writeTtc(0x00010000, 12, 48, 96);

        TtcFontCollectionParser.Result result = TtcFontCollectionParser.parse(file);

        assertTrue(result.valid);
        assertEquals(0x00010000, result.version);
        assertEquals(List.of(48L, 96L), result.offsets);
    }

    @Test
    public void rejectsNonTtcSignature() throws Exception {
        File file = temporaryFolder.newFile("Example.ttf");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {0x00, 0x01, 0x00, 0x00});
        }

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void rejectsZeroFaceCount() throws Exception {
        File file = writeRawHeader(0x74746366, 0x00010000, 0);

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void rejectsOversizedFaceCount() throws Exception {
        File file = writeRawHeader(0x74746366, 0x00010000, 129);

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void rejectsOffsetsOutsideFile() throws Exception {
        File file = writeTtc(0x00010000, 12, 4096);

        assertFalse(TtcFontCollectionParser.parse(file).valid);
    }

    @Test
    public void inspectorClassifiesRenamedTtcBySignature() throws Exception {
        File file = writeTtcNamed("Renamed.ttf", 0x00010000, 12, 48);

        FontFileInspector.Result result = FontFileInspector.inspect(file);

        assertEquals(FontFileKind.TTC, result.kind);
        assertTrue(result.ttc.valid);
        assertEquals(List.of(48L), result.ttc.offsets);
    }

    @Test
    public void inspectorClassifiesOpenTypeBySignature() throws Exception {
        File file = temporaryFolder.newFile("Example.bin");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {'O', 'T', 'T', 'O', 0, 0, 0, 0});
        }

        assertEquals(FontFileKind.OTF, FontFileInspector.inspect(file).kind);
    }

    private File writeTtc(int version, long headerOffset, long... offsets) throws Exception {
        return writeTtcNamed("Collection.ttc", version, headerOffset, offsets);
    }

    private File writeTtcNamed(String name, int version, long headerOffset, long... offsets) throws Exception {
        File file = temporaryFolder.newFile(name);
        int size = (int) Math.max(128, max(offsets) + 16);
        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x74746366);
        buffer.putInt(version);
        buffer.putInt(offsets.length);
        for (long offset : offsets) {
            buffer.putInt((int) offset);
        }
        buffer.position((int) headerOffset);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(buffer.array());
        }
        return file;
    }

    private File writeRawHeader(int signature, int version, int count) throws Exception {
        File file = temporaryFolder.newFile("Broken.ttc");
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(signature);
        buffer.putInt(version);
        buffer.putInt(count);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(buffer.array());
        }
        return file;
    }

    private static long max(long[] values) {
        long result = 0L;
        for (long value : values) {
            result = Math.max(result, value);
        }
        return result;
    }
}
```

- [ ] **Step 2: Run parser tests to verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.TtcFontCollectionParserTest
```

Expected: FAIL because `TtcFontCollectionParser`, `FontFileInspector`, and `FontFileKind` do not exist.

- [ ] **Step 3: Add font kind enum**

Create `app/src/main/java/com/dpis/module/FontFileKind.java`:

```java
package com.dpis.module;

enum FontFileKind {
    TTF(".ttf"),
    OTF(".otf"),
    TTC(".ttc"),
    UNSUPPORTED(".ttf");

    final String extension;

    FontFileKind(String extension) {
        this.extension = extension;
    }
}
```

- [ ] **Step 4: Add TTC parser**

Create `app/src/main/java/com/dpis/module/TtcFontCollectionParser.java`:

```java
package com.dpis.module;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TtcFontCollectionParser {
    private static final int SIGNATURE_TTC = 0x74746366; // ttcf
    private static final int MAX_FACE_COUNT = 128;
    private static final long HEADER_SIZE = 12L;

    private TtcFontCollectionParser() {
    }

    static Result parse(File file) {
        if (file == null || !file.isFile()) {
            return Result.invalid();
        }
        long fileLength = file.length();
        if (fileLength < HEADER_SIZE) {
            return Result.invalid();
        }
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            int signature = input.readInt();
            if (signature != SIGNATURE_TTC) {
                return Result.invalid();
            }
            int version = input.readInt();
            long count = Integer.toUnsignedLong(input.readInt());
            if (count <= 0 || count > MAX_FACE_COUNT) {
                return Result.invalid();
            }
            long offsetTableLength = HEADER_SIZE + count * 4L;
            if (offsetTableLength > fileLength) {
                return Result.invalid();
            }
            List<Long> offsets = new ArrayList<>((int) count);
            for (int index = 0; index < count; index++) {
                long offset = Integer.toUnsignedLong(input.readInt());
                if (offset < offsetTableLength || offset >= fileLength) {
                    return Result.invalid();
                }
                offsets.add(offset);
            }
            return new Result(true, version, Collections.unmodifiableList(offsets));
        } catch (IOException ignored) {
            return Result.invalid();
        }
    }

    static final class Result {
        final boolean valid;
        final int version;
        final List<Long> offsets;

        private Result(boolean valid, int version, List<Long> offsets) {
            this.valid = valid;
            this.version = version;
            this.offsets = offsets;
        }

        static Result invalid() {
            return new Result(false, 0, List.of());
        }
    }
}
```

- [ ] **Step 5: Add signature inspector**

Create `app/src/main/java/com/dpis/module/FontFileInspector.java`:

```java
package com.dpis.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

final class FontFileInspector {
    private static final int FONT_HEADER_TRUE_TYPE = 0x00010000;
    private static final int FONT_HEADER_OPEN_TYPE = 0x4F54544F; // OTTO
    private static final int FONT_HEADER_APPLE_TRUE_TYPE = 0x74727565; // true
    private static final int FONT_HEADER_TTC = 0x74746366; // ttcf

    private FontFileInspector() {
    }

    static Result inspect(File file) {
        int signature = readSignature(file);
        if (signature == FONT_HEADER_TTC) {
            TtcFontCollectionParser.Result ttc = TtcFontCollectionParser.parse(file);
            return new Result(ttc.valid ? FontFileKind.TTC : FontFileKind.UNSUPPORTED, ttc);
        }
        if (signature == FONT_HEADER_OPEN_TYPE) {
            return new Result(FontFileKind.OTF, TtcFontCollectionParser.Result.invalid());
        }
        if (signature == FONT_HEADER_TRUE_TYPE || signature == FONT_HEADER_APPLE_TRUE_TYPE) {
            return new Result(FontFileKind.TTF, TtcFontCollectionParser.Result.invalid());
        }
        return new Result(FontFileKind.UNSUPPORTED, TtcFontCollectionParser.Result.invalid());
    }

    private static int readSignature(File file) {
        if (file == null || !file.isFile()) {
            return 0;
        }
        try (InputStream input = new FileInputStream(file)) {
            byte[] header = new byte[4];
            if (input.read(header) != header.length) {
                return 0;
            }
            return ((header[0] & 0xFF) << 24)
                    | ((header[1] & 0xFF) << 16)
                    | ((header[2] & 0xFF) << 8)
                    | (header[3] & 0xFF);
        } catch (IOException ignored) {
            return 0;
        }
    }

    static final class Result {
        final FontFileKind kind;
        final TtcFontCollectionParser.Result ttc;

        Result(FontFileKind kind, TtcFontCollectionParser.Result ttc) {
            this.kind = kind;
            this.ttc = ttc;
        }
    }
}
```

- [ ] **Step 6: Run parser tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.TtcFontCollectionParserTest
```

Expected: PASS.

- [ ] **Step 7: Commit parser task**

Run:

```powershell
git add app/src/main/java/com/dpis/module/FontFileKind.java app/src/main/java/com/dpis/module/FontFileInspector.java app/src/main/java/com/dpis/module/TtcFontCollectionParser.java app/src/test/java/com/dpis/module/TtcFontCollectionParserTest.java
git commit -m "feat: parse TTC font collections"
```

---

### Task 2: Persist TTC Index And Batch Register Shared TTC Files

**Files:**
- Modify: `app/src/main/java/com/dpis/module/FontLibraryEntry.java`
- Modify: `app/src/main/java/com/dpis/module/FontLibraryStore.java`
- Test: `app/src/test/java/com/dpis/module/FontLibraryStoreTest.java`

- [ ] **Step 1: Write store tests for `ttcIndex` and shared-file deletion**

Append these tests to `FontLibraryStoreTest` before `writeFile`:

```java
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
    public void deletingOneTtcFaceKeepsSharedFileForOtherFaces() throws Exception {
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
                new DpiConfigStore(new FakePrefs()));

        assertSame(FontLibraryStore.DeleteResult.DELETED, result);
        assertNull(store.findById(entries.get(0).id));
        assertNotNull(store.findById(entries.get(1).id));
        assertTrue(sharedFile.isFile());
    }

    @Test
    public void deletingLastTtcFaceDeletesSharedFile() throws Exception {
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
                store.deleteFont(entries.get(0).id, new DpiConfigStore(new FakePrefs())));

        FontLibraryStore.DeleteResult result = store.deleteFont(
                entries.get(1).id,
                new DpiConfigStore(new FakePrefs()));

        assertSame(FontLibraryStore.DeleteResult.DELETED, result);
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
```

- [ ] **Step 2: Run store tests to verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontLibraryStoreTest
```

Expected: FAIL because `ttcIndex` and `registerCopiedFontFaces` do not exist.

- [ ] **Step 3: Add `ttcIndex` to entry**

Modify `FontLibraryEntry`:

```java
final int ttcIndex;

FontLibraryEntry(String id,
                 String displayName,
                 String sourceFileName,
                 String storedFileName,
                 String storedPath,
                 String sha256,
                 long importedAtEpochMs) {
    this(id, displayName, sourceFileName, storedFileName, storedPath, sha256, importedAtEpochMs, 0);
}

FontLibraryEntry(String id,
                 String displayName,
                 String sourceFileName,
                 String storedFileName,
                 String storedPath,
                 String sha256,
                 long importedAtEpochMs,
                 int ttcIndex) {
    this.id = id;
    this.displayName = displayName;
    this.sourceFileName = sourceFileName;
    this.storedFileName = storedFileName;
    this.storedPath = storedPath;
    this.sha256 = sha256;
    this.importedAtEpochMs = importedAtEpochMs;
    this.ttcIndex = Math.max(0, ttcIndex);
}
```

Update `equals` and `hashCode` to include `ttcIndex`.

- [ ] **Step 4: Extend metadata JSON in `FontLibraryStore`**

Add:

```java
private static final String JSON_TTC_INDEX = "ttcIndex";
```

In `parseEntry`, read optional index:

```java
int ttcIndex = 0;
if (object.containsKey(JSON_TTC_INDEX)) {
    try {
        ttcIndex = Math.max(0, Integer.parseInt(object.get(JSON_TTC_INDEX)));
    } catch (NumberFormatException ignored) {
        return null;
    }
}
return new FontLibraryEntry(
        id,
        displayName,
        sourceFileName,
        storedFileName,
        storedPath,
        sha256,
        importedAtEpochMs,
        ttcIndex);
```

In `toJson`, append:

```java
+ "," + quote(JSON_TTC_INDEX) + ":" + entry.ttcIndex
```

- [ ] **Step 5: Add batch registration API**

Add this public package-private method to `FontLibraryStore`:

```java
synchronized List<FontLibraryEntry> registerCopiedFontFaces(
        File sourceFile,
        String sourceFileName,
        String requestedDisplayName,
        FontFileKind kind,
        List<Integer> ttcIndexes,
        long importedAtEpochMs) throws IOException {
    if (kind != FontFileKind.TTC) {
        return List.of(registerCopiedFont(
                sourceFile,
                sourceFileName,
                requestedDisplayName,
                importedAtEpochMs,
                kind));
    }
    Objects.requireNonNull(ttcIndexes, "ttcIndexes");
    if (ttcIndexes.isEmpty()) {
        return List.of();
    }
    ensureFontDirectory();
    String extension = FontFileKind.TTC.extension;
    File tempFile = File.createTempFile("font_import_", extension, fontDirectory);
    String sha256 = copyAndDigest(sourceFile, tempFile);
    List<FontLibraryEntry> entries = readEntries();
    List<FontLibraryEntry> result = new ArrayList<>();
    List<Integer> missingIndexes = new ArrayList<>();
    for (Integer index : ttcIndexes) {
        if (index == null || index < 0) {
            continue;
        }
        FontLibraryEntry existing = findExistingTtcEntry(entries, sha256, index);
        if (existing != null) {
            result.add(existing);
        } else {
            missingIndexes.add(index);
        }
    }
    if (missingIndexes.isEmpty()) {
        tempFile.delete();
        return result;
    }
    String baseId = FONT_ID_PREFIX + sha256.substring(0, 16);
    File stagingFile = new File(fontDirectory, baseId + extension);
    if (!tempFile.renameTo(stagingFile)) {
        Files.copy(tempFile.toPath(), stagingFile.toPath());
        tempFile.delete();
    }
    stagingFile.setReadable(true, false);
    File targetFile = publishFontFile(stagingFile);
    List<FontLibraryEntry> originalEntries = new ArrayList<>(entries);
    for (Integer index : missingIndexes) {
        String id = baseId + "_ttc_" + index;
        FontLibraryEntry entry = new FontLibraryEntry(
                id,
                makeUniqueDisplayName(entries, requestedDisplayName + " (TTC " + index + ")", null),
                sourceFileName,
                targetFile.getName(),
                targetFile.getAbsolutePath(),
                sha256,
                importedAtEpochMs,
                index);
        entries.add(entry);
        result.add(entry);
    }
    if (!writeEntries(entries)) {
        deleteStoredFileIfUnreferenced(targetFile, originalEntries);
        if (!targetFile.equals(stagingFile)) {
            stagingFile.delete();
        }
        throw new IOException("Unable to persist font library metadata");
    }
    if (!targetFile.equals(stagingFile)) {
        stagingFile.delete();
    }
    return result;
}
```

Then refactor existing `registerCopiedFont` to call an overload that accepts `FontFileKind`:

```java
synchronized FontLibraryEntry registerCopiedFont(
        File sourceFile,
        String sourceFileName,
        String requestedDisplayName,
        long importedAtEpochMs) throws IOException {
    return registerCopiedFont(sourceFile, sourceFileName, requestedDisplayName, importedAtEpochMs, null);
}
```

In the overload, resolve extension from detected kind when available:

```java
String extension = kind == null ? resolveFontExtension(sourceFileName) : kind.extension;
```

- [ ] **Step 6: Add helper methods for directory, lookup, and shared delete**

Add:

```java
private void ensureFontDirectory() throws IOException {
    Objects.requireNonNull(fontDirectory, "fontDirectory");
    if (!fontDirectory.exists() && !fontDirectory.mkdirs()) {
        throw new IOException("Unable to create font directory: " + fontDirectory);
    }
    if (!fontDirectory.isDirectory()) {
        throw new IOException("Font directory is not a directory: " + fontDirectory);
    }
}

private static FontLibraryEntry findExistingTtcEntry(
        List<FontLibraryEntry> entries,
        String sha256,
        int ttcIndex) {
    for (FontLibraryEntry entry : entries) {
        if (sha256.equals(entry.sha256) && entry.ttcIndex == ttcIndex && resolveStoredFile(entry) != null) {
            return entry;
        }
    }
    return null;
}

private boolean hasRemainingPathReference(List<FontLibraryEntry> entries, String storedPath) {
    for (FontLibraryEntry entry : entries) {
        if (storedPath.equals(entry.storedPath)) {
            return true;
        }
    }
    return false;
}

private void deleteStoredFileIfUnreferenced(File file, List<FontLibraryEntry> entries) {
    if (file != null && file.exists() && !hasRemainingPathReference(entries, file.getAbsolutePath())) {
        deleteStoredFile(file);
    }
}
```

- [ ] **Step 7: Update delete logic to preserve shared files**

In `deleteFont`, after successful metadata write:

```java
File file = new File(entry.storedPath);
if (file.exists() && !hasRemainingPathReference(remainingEntries, entry.storedPath)
        && !deleteStoredFile(file)) {
    writeEntries(originalEntries);
    return DeleteResult.DELETE_FAILED;
}
return DeleteResult.DELETED;
```

- [ ] **Step 8: Run store tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontLibraryStoreTest
```

Expected: PASS.

- [ ] **Step 9: Commit store task**

Run:

```powershell
git add app/src/main/java/com/dpis/module/FontLibraryEntry.java app/src/main/java/com/dpis/module/FontLibraryStore.java app/src/test/java/com/dpis/module/FontLibraryStoreTest.java
git commit -m "feat: store TTC font faces"
```

---

### Task 3: Centralize Typeface Loading And Published TTC Resolution

**Files:**
- Create: `app/src/main/java/com/dpis/module/FontTypefaceLoader.java`
- Modify: `app/src/main/java/com/dpis/module/PublishedFontFileResolver.java`
- Modify: `app/src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/AppConfigDialogBinder.java`
- Modify: `app/src/main/java/com/dpis/module/FontLibraryActivity.java`
- Test: `app/src/test/java/com/dpis/module/PublishedFontFileResolverTest.java`
- Test: `app/src/test/java/com/dpis/module/TypefaceOverrideHookInstallerTest.java`

- [ ] **Step 1: Extend resolver tests**

Add to `PublishedFontFileResolverTest`:

```java
    @Test
    public void resolvesPublishedTtcWhenOtherFormatsMissing() throws Exception {
        File directory = temporaryFolder.newFolder("fonts");
        File font = new File(directory, "dpis_font_abcd1234_ttc_2.ttc");
        assertTrue(font.createNewFile());

        assertEquals(font, PublishedFontFileResolver.resolveInDirectory(directory, "font_abcd1234_ttc_2"));
    }
```

Add to `TypefaceOverrideHookInstallerTest.modernInstallerFallsBackToPublishedFontFile`:

```java
        assertTrue(source.contains("fontLibraryStore.findById(typefaceId)"));
        assertTrue(source.contains("FontTypefaceLoader.load(file, entry.ttcIndex)"));
```

- [ ] **Step 2: Run targeted tests to verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.PublishedFontFileResolverTest --tests com.dpis.module.TypefaceOverrideHookInstallerTest
```

Expected: FAIL because `.ttc` resolution and centralized loader are not wired.

- [ ] **Step 3: Add loader helper**

Create `app/src/main/java/com/dpis/module/FontTypefaceLoader.java`:

```java
package com.dpis.module;

import android.graphics.Typeface;

import java.io.File;
import java.util.Locale;

final class FontTypefaceLoader {
    private FontTypefaceLoader() {
    }

    static Typeface load(File file, int ttcIndex) {
        if (file == null || !file.canRead()) {
            return null;
        }
        try {
            if (isTtc(file) || ttcIndex > 0) {
                return new Typeface.Builder(file)
                        .setTtcIndex(Math.max(0, ttcIndex))
                        .build();
            }
            return Typeface.createFromFile(file);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isTtc(File file) {
        String name = file.getName();
        return name != null && name.toLowerCase(Locale.US).endsWith(".ttc");
    }
}
```

- [ ] **Step 4: Resolve `.ttc` files**

Update `PublishedFontFileResolver.resolveInDirectory`:

```java
File ttc = new File(directory, "dpis_" + typefaceId + ".ttc");
if (ttc.isFile()) {
    return ttc;
}
return null;
```

Keep `.ttf` first and `.otf` second to preserve existing behavior.

- [ ] **Step 5: Wire runtime replacement through entry metadata**

In `TypefaceOverrideHookInstaller`, replace imported file loading with:

```java
FontLibraryEntry entry = fontLibraryStore.findById(typefaceId);
File file = null;
int ttcIndex = 0;
if (entry != null) {
    file = fontLibraryStore.resolveFontFile(typefaceId);
    ttcIndex = entry.ttcIndex;
}
if (file == null) {
    file = PublishedFontFileResolver.resolve(typefaceId);
}
if (file == null || !file.canRead()) {
    logIfChanged(packageName + ":unreadable:" + typefaceId,
            LOG_PREFIX + "font file unreadable: package=" + packageName
                    + ", typefaceId=" + typefaceId);
    return null;
}
Typeface loaded = FontTypefaceLoader.load(file, ttcIndex);
if (loaded == null) {
    logIfChanged(packageName + ":load-failed:" + typefaceId,
            LOG_PREFIX + "font load failed: package=" + packageName
                    + ", typefaceId=" + typefaceId);
}
return loaded;
```

Remove the old `Typeface.createFromFile(file)` try/catch block.

- [ ] **Step 6: Wire previews**

In `FontLibraryActivity`, replace preview calls:

```java
Typeface previewTypeface = FontTypefaceLoader.load(fontFile, entry.ttcIndex);
```

Change `createFontPreview(File fontFile)` to:

```java
private View createFontPreview(File fontFile, int ttcIndex) {
    LinearLayout previewGroup = new LinearLayout(this);
    previewGroup.setOrientation(LinearLayout.VERTICAL);

    Typeface previewTypeface = FontTypefaceLoader.load(fontFile, ttcIndex);

    MaterialTextView primary = new MaterialTextView(this);
    primary.setText(FONT_PREVIEW_PRIMARY_TEXT);
    configureSingleLine(primary);
    primary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
    primary.setTextColor(MaterialColors.getColor(
            listView, com.google.android.material.R.attr.colorOnSurface));
    if (previewTypeface != null) {
        primary.setTypeface(previewTypeface);
    }
    previewGroup.addView(primary);

    MaterialTextView secondary = new MaterialTextView(this);
    secondary.setText(FONT_PREVIEW_SECONDARY_TEXT);
    configureSingleLine(secondary);
    secondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    secondary.setTextColor(MaterialColors.getColor(
            listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
    if (previewTypeface != null) {
        secondary.setTypeface(previewTypeface);
    }
    previewGroup.addView(secondary, topMarginParams(8));
    return previewGroup;
}
```

In `AppConfigDialogBinder.resolveTypefaceOptionPreview`, after resolving `FontLibraryEntry`, load with:

```java
return FontTypefaceLoader.load(fontFile, entry.ttcIndex);
```

- [ ] **Step 7: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.PublishedFontFileResolverTest --tests com.dpis.module.TypefaceOverrideHookInstallerTest
```

Expected: PASS.

- [ ] **Step 8: Commit loader task**

Run:

```powershell
git add app/src/main/java/com/dpis/module/FontTypefaceLoader.java app/src/main/java/com/dpis/module/PublishedFontFileResolver.java app/src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java app/src/main/java/com/dpis/module/AppConfigDialogBinder.java app/src/main/java/com/dpis/module/FontLibraryActivity.java app/src/test/java/com/dpis/module/PublishedFontFileResolverTest.java app/src/test/java/com/dpis/module/TypefaceOverrideHookInstallerTest.java
git commit -m "feat: load imported TTC faces by index"
```

---

### Task 4: Laboratory Gate For TTC Imports

**Files:**
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Modify: `app/src/main/java/com/dpis/module/ExperimentalSettingsActivity.java`
- Modify: `app/src/main/res/layout/activity_experimental_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
- Test: `app/src/test/java/com/dpis/module/ExperimentalSettingsActivitySourceSmokeTest.java`

- [ ] **Step 1: Add preference tests**

Append to `DpiConfigStoreTest`:

```java
    @Test
    public void ttcImportExperimentDefaultsOff() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertFalse(store.isTtcFontImportEnabled());
    }

    @Test
    public void ttcImportExperimentCanBeEnabledAndDisabled() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertTrue(store.setTtcFontImportEnabled(true));
        assertTrue(store.isTtcFontImportEnabled());
        assertTrue(store.setTtcFontImportEnabled(false));
        assertFalse(store.isTtcFontImportEnabled());
    }
```

Create `app/src/test/java/com/dpis/module/ExperimentalSettingsActivitySourceSmokeTest.java`:

```java
package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public final class ExperimentalSettingsActivitySourceSmokeTest {
    @Test
    public void laboratoryExposesTtcImportSwitch() throws IOException {
        String source = read("src/main/java/com/dpis/module/ExperimentalSettingsActivity.java");
        String layout = read("src/main/res/layout/activity_experimental_settings.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("isTtcFontImportEnabled()"));
        assertTrue(source.contains("setTtcFontImportEnabled("));
        assertTrue(layout.contains("experimental_ttc_import_switch"));
        assertTrue(strings.contains("settings_ttc_import_label"));
        assertTrue(strings.contains("settings_ttc_import_hint"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.DpiConfigStoreTest --tests com.dpis.module.ExperimentalSettingsActivitySourceSmokeTest
```

Expected: FAIL because the preference and switch UI are missing.

- [ ] **Step 3: Add `DpiConfigStore` preference**

Add key:

```java
static final String KEY_TTC_FONT_IMPORT_ENABLED = "font.ttc_import_enabled";
```

Add methods:

```java
boolean isTtcFontImportEnabled() {
    return getBoolean(KEY_TTC_FONT_IMPORT_ENABLED, false);
}

boolean setTtcFontImportEnabled(boolean enabled) {
    return commitBoth(editor -> editor.putBoolean(KEY_TTC_FONT_IMPORT_ENABLED, enabled));
}
```

- [ ] **Step 4: Replace experimental settings layout**

Replace `activity_experimental_settings.xml` with a vertical layout that keeps id `experimental_settings_content` and adds a switch row:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/experimental_settings_content"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:padding="24dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <LinearLayout
            android:id="@+id/experimental_ttc_import_row"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="?attr/selectableItemBackground"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center_vertical"
            android:minHeight="64dp"
            android:orientation="horizontal"
            android:paddingTop="10dp"
            android:paddingBottom="10dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <com.google.android.material.textview.MaterialTextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/settings_ttc_import_label"
                    android:textAppearance="@style/TextAppearance.Material3.TitleMedium" />

                <com.google.android.material.textview.MaterialTextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="2dp"
                    android:text="@string/settings_ttc_import_hint"
                    android:textAppearance="@style/TextAppearance.Material3.BodyMedium"
                    android:textColor="?attr/colorOnSurfaceVariant" />
            </LinearLayout>

            <com.google.android.material.materialswitch.MaterialSwitch
                android:id="@+id/experimental_ttc_import_switch"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 5: Bind switch in activity**

Update `ExperimentalSettingsActivity`:

```java
private DpiConfigStore configStore;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_experimental_settings);
    configStore = ConfigStoreFactory.createForModuleApp(
            this, DpisApplication.getXposedService());
    bindTtcImportSwitch();
    applyInsets();
}

private void bindTtcImportSwitch() {
    View row = findViewById(R.id.experimental_ttc_import_row);
    com.google.android.material.materialswitch.MaterialSwitch toggle =
            findViewById(R.id.experimental_ttc_import_switch);
    toggle.setChecked(configStore.isTtcFontImportEnabled());
    row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
    toggle.setOnCheckedChangeListener((button, checked) ->
            configStore.setTtcFontImportEnabled(checked));
}
```

- [ ] **Step 6: Add strings**

Add to `strings.xml` near the existing laboratory strings:

```xml
<string name="settings_ttc_import_label">TTC font collections</string>
<string name="settings_ttc_import_hint">Import TrueType Collection files as separate faces. Experimental.</string>
```

- [ ] **Step 7: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.DpiConfigStoreTest --tests com.dpis.module.ExperimentalSettingsActivitySourceSmokeTest
```

Expected: PASS.

- [ ] **Step 8: Commit laboratory task**

Run:

```powershell
git add app/src/main/java/com/dpis/module/DpiConfigStore.java app/src/main/java/com/dpis/module/ExperimentalSettingsActivity.java app/src/main/res/layout/activity_experimental_settings.xml app/src/main/res/values/strings.xml app/src/test/java/com/dpis/module/DpiConfigStoreTest.java app/src/test/java/com/dpis/module/ExperimentalSettingsActivitySourceSmokeTest.java
git commit -m "feat: gate TTC imports in laboratory"
```

---

### Task 5: TTC Import Flow And Face Selection Dialog

**Files:**
- Modify: `app/src/main/java/com/dpis/module/FontLibraryActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/dpis/module/FontLibraryActivitySourceSmokeTest.java`

- [ ] **Step 1: Add source smoke test for import flow**

Create or extend `FontLibraryActivitySourceSmokeTest` with:

```java
package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public final class FontLibraryActivitySourceSmokeTest {
    @Test
    public void ttcImportIsGatedAndUsesFaceSelectionDialog() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("configStore.isTtcFontImportEnabled()"));
        assertTrue(source.contains("FontFileInspector.inspect(tempFile)"));
        assertTrue(source.contains("showTtcFaceSelectionDialog("));
        assertTrue(source.contains("fontLibraryStore.registerCopiedFontFaces("));
        assertTrue(source.contains("font_library_ttc_select_title"));
        assertTrue(source.contains("font_library_ttc_select_all"));
        assertTrue(source.contains("font_library_ttc_deselect_all"));
        assertTrue(strings.contains("font_library_ttc_select_title"));
        assertTrue(strings.contains("font_library_ttc_failed_faces"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 2: Run source smoke test to verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontLibraryActivitySourceSmokeTest
```

Expected: FAIL because TTC UI is not implemented.

- [ ] **Step 3: Add import strings**

Add to `strings.xml`:

```xml
<string name="font_library_import_count_success">Imported %1$d faces</string>
<string name="font_library_ttc_select_title">选择导入 face</string>
<string name="font_library_ttc_selected_count">已选择 %1$d</string>
<string name="font_library_ttc_failed_faces">%1$d 个 face 无法导入</string>
<string name="font_library_ttc_select_all">Select all</string>
<string name="font_library_ttc_deselect_all">Deselect all</string>
```

- [ ] **Step 4: Accept TTC in picker only when enabled**

In `openFontImportPicker`, build MIME list dynamically:

```java
List<String> mimeTypes = new ArrayList<>();
mimeTypes.add("font/ttf");
mimeTypes.add("font/otf");
mimeTypes.add("application/x-font-ttf");
mimeTypes.add("application/vnd.ms-opentype");
if (configStore.isTtcFontImportEnabled()) {
    mimeTypes.add("font/collection");
    mimeTypes.add("font/ttc");
    mimeTypes.add("application/x-font-ttc");
}
intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
```

- [ ] **Step 5: Loosen initial input check and classify after copy**

Replace `isSupportedFontInput` with a method that lets unknown MIME through when the filename is plausible, but still does not claim TTC support until signature inspection:

```java
private boolean isPotentialFontInput(String displayName, String mimeType) {
    String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.US);
    if (lowerName.endsWith(".ttf") || lowerName.endsWith(".otf")) {
        return true;
    }
    if (configStore.isTtcFontImportEnabled() && lowerName.endsWith(".ttc")) {
        return true;
    }
    return "font/ttf".equals(mimeType)
            || "font/otf".equals(mimeType)
            || "application/x-font-ttf".equals(mimeType)
            || "application/vnd.ms-opentype".equals(mimeType)
            || (configStore.isTtcFontImportEnabled()
            && ("font/ttc".equals(mimeType)
            || "font/collection".equals(mimeType)
            || "application/x-font-ttc".equals(mimeType)));
}
```

Use this method in `promptImportName`.

- [ ] **Step 6: Split import path by inspected kind**

In `importFont`, after copying:

```java
FontFileInspector.Result inspection = FontFileInspector.inspect(tempFile);
if (inspection.kind == FontFileKind.TTC) {
    if (!configStore.isTtcFontImportEnabled()) {
        throw new IOException("TTC import disabled");
    }
    File finalTempFile = tempFile;
    runOnUiThread(() -> showTtcFaceSelectionDialog(
            finalTempFile,
            sourceName,
            displayName,
            inspection.ttc.offsets.size()));
    return;
}
if (inspection.kind == FontFileKind.UNSUPPORTED || !isSupportedSingleFontFile(tempFile, inspection.kind)) {
    throw new IOException("Unable to parse font");
}
importedEntry = fontLibraryStore.registerCopiedFont(
        tempFile,
        sourceName,
        displayName,
        System.currentTimeMillis(),
        inspection.kind);
```

For the TTC branch, do not delete `tempFile` in the current thread before dialog completion. Pass ownership to the dialog flow and delete it after cancellation or registration.

- [ ] **Step 7: Add single-font validation helper**

Replace `isSupportedFontFile` with:

```java
private static boolean isSupportedSingleFontFile(File file, FontFileKind kind) {
    if (file == null || !file.isFile() || kind == FontFileKind.TTC || kind == FontFileKind.UNSUPPORTED) {
        return false;
    }
    return FontTypefaceLoader.load(file, 0) != null;
}
```

- [ ] **Step 8: Add loadable face model and validator**

Add nested class:

```java
private static final class TtcFaceOption {
    final int index;
    final String label;

    TtcFaceOption(int index, String label) {
        this.index = index;
        this.label = label;
    }
}
```

Add helper:

```java
private List<TtcFaceOption> findLoadableTtcFaces(File file, String sourceName, int faceCount) {
    List<TtcFaceOption> result = new ArrayList<>();
    for (int index = 0; index < faceCount; index++) {
        if (FontTypefaceLoader.load(file, index) != null) {
            result.add(new TtcFaceOption(index, sourceName + " (TTC " + index + ")"));
        }
    }
    return result;
}
```

- [ ] **Step 9: Add face-selection dialog**

Implement:

```java
private void showTtcFaceSelectionDialog(
        File tempFile,
        String sourceName,
        String displayName,
        int faceCount) {
    List<TtcFaceOption> options = findLoadableTtcFaces(tempFile, sourceName, faceCount);
    int failedCount = Math.max(0, faceCount - options.size());
    if (options.isEmpty()) {
        tempFile.delete();
        showToast(R.string.font_library_import_failed);
        return;
    }

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    int padding = dp(20);
    content.setPadding(padding, dp(16), padding, 0);

    MaterialTextView subtitle = new MaterialTextView(this);
    subtitle.setText(sourceName);
    configureSingleLine(subtitle);
    subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    subtitle.setTextColor(MaterialColors.getColor(
            listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
    content.addView(subtitle);

    LinearLayout statusRow = new LinearLayout(this);
    statusRow.setOrientation(LinearLayout.HORIZONTAL);
    statusRow.setGravity(Gravity.CENTER_VERTICAL);
    MaterialTextView selectedBadge = createTtcStatusBadge(getString(R.string.font_library_ttc_selected_count, 0));
    MaterialTextView failedBadge = createTtcStatusBadge(getString(R.string.font_library_ttc_failed_faces, failedCount));
    statusRow.addView(selectedBadge);
    statusRow.addView(failedBadge);
    content.addView(statusRow, topMarginParams(10));

    LinearLayout actions = new LinearLayout(this);
    actions.setOrientation(LinearLayout.HORIZONTAL);
    actions.setGravity(Gravity.END);
    MaterialTextView selectAll = createTtcTextAction(R.string.font_library_ttc_select_all);
    MaterialTextView deselectAll = createTtcTextAction(R.string.font_library_ttc_deselect_all);
    actions.addView(selectAll);
    actions.addView(deselectAll);
    content.addView(actions, topMarginParams(8));

    boolean[] checked = new boolean[options.size()];
    LinearLayout rows = new LinearLayout(this);
    rows.setOrientation(LinearLayout.VERTICAL);
    content.addView(rows, topMarginParams(8));
    List<MaterialTextView> rowViews = new ArrayList<>();
    for (int i = 0; i < options.size(); i++) {
        int rowIndex = i;
        MaterialTextView row = createTtcFaceRow(options.get(i).label, false);
        row.setOnClickListener(v -> {
            checked[rowIndex] = !checked[rowIndex];
            updateTtcFaceRow(row, options.get(rowIndex).label, checked[rowIndex]);
            selectedBadge.setText(getString(R.string.font_library_ttc_selected_count, countSelected(checked)));
        });
        rowViews.add(row);
        rows.addView(row, topMarginParams(6));
    }

    androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.font_library_ttc_select_title)
            .setView(content)
            .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
            .setPositiveButton(R.string.font_library_import_action, null)
            .create();
    dialog.setOnCancelListener(unused -> tempFile.delete());
    dialog.setOnDismissListener(unused -> {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    });
    dialog.setOnShowListener(unused -> {
        bindDialogButtonHaptics(dialog);
        android.widget.Button importButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        importButton.setEnabled(false);
        selectAll.setOnClickListener(v -> {
            setAllTtcFaceRows(options, checked, rowViews, true);
            selectedBadge.setText(getString(R.string.font_library_ttc_selected_count, countSelected(checked)));
            importButton.setEnabled(true);
        });
        deselectAll.setOnClickListener(v -> {
            setAllTtcFaceRows(options, checked, rowViews, false);
            selectedBadge.setText(getString(R.string.font_library_ttc_selected_count, 0));
            importButton.setEnabled(false);
        });
        for (MaterialTextView row : rowViews) {
            row.setOnClickListener(v -> {
                int rowIndex = rowViews.indexOf(row);
                checked[rowIndex] = !checked[rowIndex];
                updateTtcFaceRow(row, options.get(rowIndex).label, checked[rowIndex]);
                int selected = countSelected(checked);
                selectedBadge.setText(getString(R.string.font_library_ttc_selected_count, selected));
                importButton.setEnabled(selected > 0);
            });
        }
        importButton.setOnClickListener(v -> importSelectedTtcFaces(
                dialog, tempFile, sourceName, displayName, options, checked));
    });
    dialog.show();
}
```

Add small UI helpers named exactly as used above:

```java
private MaterialTextView createTtcStatusBadge(String text) {
    MaterialTextView badge = new MaterialTextView(this);
    badge.setText(text);
    badge.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
    badge.setTextColor(MaterialColors.getColor(
            listView, com.google.android.material.R.attr.colorOnSecondaryContainer));
    badge.setGravity(Gravity.CENTER);
    badge.setPadding(dp(10), dp(5), dp(10), dp(5));
    android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
    background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
    background.setCornerRadius(dp(999));
    background.setColor(MaterialColors.getColor(
            listView, com.google.android.material.R.attr.colorSecondaryContainer));
    badge.setBackground(background);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    params.rightMargin = dp(8);
    badge.setLayoutParams(params);
    return badge;
}

private MaterialTextView createTtcTextAction(int textResId) {
    MaterialTextView action = new MaterialTextView(this);
    action.setText(textResId);
    action.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
    action.setTextColor(MaterialColors.getColor(
            listView, androidx.appcompat.R.attr.colorPrimary));
    action.setGravity(Gravity.CENTER);
    action.setPadding(dp(10), dp(6), dp(10), dp(6));
    action.setClickable(true);
    action.setFocusable(true);
    action.setBackgroundResource(resolveSelectableItemBackground());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    params.leftMargin = dp(8);
    action.setLayoutParams(params);
    return action;
}

private MaterialTextView createTtcFaceRow(String label, boolean selected) {
    MaterialTextView row = new MaterialTextView(this);
    row.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setMinHeight(dp(48));
    row.setPadding(dp(14), dp(10), dp(14), dp(10));
    row.setClickable(true);
    row.setFocusable(true);
    updateTtcFaceRow(row, label, selected);
    return row;
}

private void updateTtcFaceRow(MaterialTextView row, String label, boolean selected) {
    row.setText(selected ? label + "  ✓" : label);
    row.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
    android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
    background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
    background.setCornerRadius(dp(14));
    background.setColor(MaterialColors.getColor(
            listView,
            selected
                    ? com.google.android.material.R.attr.colorSecondaryContainer
                    : com.google.android.material.R.attr.colorSurfaceContainerHigh));
    background.setStroke(dp(1), MaterialColors.getColor(
            listView, com.google.android.material.R.attr.colorOutlineVariant));
    row.setBackground(background);
}

private void setAllTtcFaceRows(
        List<TtcFaceOption> options,
        boolean[] checked,
        List<MaterialTextView> rows,
        boolean selected) {
    for (int i = 0; i < checked.length; i++) {
        checked[i] = selected;
        updateTtcFaceRow(rows.get(i), options.get(i).label, selected);
    }
}

private int countSelected(boolean[] checked) {
    int count = 0;
    for (boolean selected : checked) {
        if (selected) {
            count++;
        }
    }
    return count;
}
```

Use existing `MaterialColors`, rounded `GradientDrawable`, and `configureSingleLine` patterns from this file. Keep badges compact with horizontal padding `dp(10)` and vertical padding `dp(5)`.

- [ ] **Step 10: Add selected-face registration**

Add:

```java
private void importSelectedTtcFaces(
        androidx.appcompat.app.AlertDialog dialog,
        File tempFile,
        String sourceName,
        String displayName,
        List<TtcFaceOption> options,
        boolean[] checked) {
    List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < checked.length; i++) {
        if (checked[i]) {
            indexes.add(options.get(i).index);
        }
    }
    if (indexes.isEmpty()) {
        return;
    }
    new Thread(() -> {
        List<FontLibraryEntry> imported = List.of();
        try {
            imported = fontLibraryStore.registerCopiedFontFaces(
                    tempFile,
                    sourceName,
                    displayName,
                    FontFileKind.TTC,
                    indexes,
                    System.currentTimeMillis());
        } catch (IOException | RuntimeException ignored) {
            imported = List.of();
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        List<FontLibraryEntry> finalImported = imported;
        runOnUiThread(() -> {
            if (finalImported.isEmpty()) {
                showToast(R.string.font_library_import_failed);
                return;
            }
            dialog.dismiss();
            showToast(R.string.font_library_import_count_success, finalImported.size());
            refreshFontList();
        });
    }, "dpis-ttc-font-import").start();
}
```

- [ ] **Step 11: Run source smoke test**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontLibraryActivitySourceSmokeTest
```

Expected: PASS.

- [ ] **Step 12: Commit import UI task**

Run:

```powershell
git add app/src/main/java/com/dpis/module/FontLibraryActivity.java app/src/main/res/values/strings.xml app/src/test/java/com/dpis/module/FontLibraryActivitySourceSmokeTest.java
git commit -m "feat: add TTC face import dialog"
```

---

### Task 6: Final Verification And Documentation Alignment

**Files:**
- Modify: `docs/superpowers/specs/2026-05-22-ttc-font-collections-design.md` only if implementation required design correction.

- [ ] **Step 1: Run focused TTC tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.TtcFontCollectionParserTest --tests com.dpis.module.FontLibraryStoreTest --tests com.dpis.module.PublishedFontFileResolverTest --tests com.dpis.module.TypefaceOverrideHookInstallerTest --tests com.dpis.module.ExperimentalSettingsActivitySourceSmokeTest --tests com.dpis.module.FontLibraryActivitySourceSmokeTest
```

Expected: PASS.

- [ ] **Step 2: Run all debug unit tests**

Run:

```powershell
./gradlew :app:testAllDebugUnitTests
```

Expected: PASS for modern101 and compat100 debug unit tests.

- [ ] **Step 3: Build debug APKs**

Run:

```powershell
./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug
```

Expected: both debug APKs build successfully.

- [ ] **Step 4: Manual validation on device**

If a device and a real TTC sample are available:

```powershell
adb install -r "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk"
```

Validate:

- Laboratory switch defaults off.
- With switch off, TTC import fails and `.ttf/.otf` import still works.
- With switch on, a TTC opens the face-selection dialog.
- Dialog defaults to zero selected faces and import is disabled.
- Select all and deselect all update the selected-count capsule.
- Failed loadable-face count is displayed when applicable.
- Importing multiple faces creates multiple font-library entries with fallback labels.
- Deleting one face does not delete the shared file while other faces remain.
- Applying two different TTC face entries to a target app produces different runtime load log ids.

- [ ] **Step 5: Check worktree status**

Run:

```powershell
git status --short --branch
```

Expected: only intended files changed; no generated APKs, logs, `.superpowers/brainstorm`, or `.debug-*` evidence files staged.

- [ ] **Step 6: Final commit if needed**

If Task 6 made doc or test adjustments:

```powershell
git add docs/superpowers/specs/2026-05-22-ttc-font-collections-design.md app/src/test/java/com/dpis/module
git commit -m "test: verify TTC font collection imports"
```

Skip this commit when there are no changes after verification.

---

## Plan Self-Review

Spec coverage:

- Experimental gate: Task 4.
- Signature-based classification and renamed TTC rejection: Tasks 1 and 5.
- TTC header parsing and face validation: Tasks 1 and 5.
- Default no selection, fixed top capsules, select all and deselect all: Task 5.
- `ttcIndex` persistence and old metadata default: Task 2.
- Distinct TTC ids and shared file storage: Task 2.
- Shared-file deletion safety: Task 2.
- Loader and runtime metadata resolution: Task 3.
- `.ttc` published resolver support: Task 3.
- Temp cleanup: Task 5.
- Focused and full verification: Task 6.

Known execution risks:

- `Typeface.Builder` behavior is difficult to unit-test without Android runtime fonts. The plan uses source smoke tests and manual device validation for runtime face differences.
- The face-selection dialog is built programmatically in an already-large activity. Keep helpers small and local; do not refactor unrelated font-library UI in this implementation.
- Process death between public-file publication and metadata commit is mitigated by cleanup but not fully atomic at filesystem level. The plan preserves the design requirement to avoid partial metadata and removes staged files on normal failures.
