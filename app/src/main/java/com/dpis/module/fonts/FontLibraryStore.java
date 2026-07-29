package com.dpis.module.fonts;

import com.dpis.module.DpisLog;

import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class FontLibraryStore {
    private static final String KEY_ENTRIES = "font.library.entries";
    private static final String FONT_ID_PREFIX = "font_";

    private static final String JSON_ID = "id";
    private static final String JSON_DISPLAY_NAME = "displayName";
    private static final String JSON_SOURCE_FILE_NAME = "sourceFileName";
    private static final String JSON_STORED_FILE_NAME = "storedFileName";
    private static final String JSON_STORED_PATH = "storedPath";
    private static final String JSON_SHA256 = "sha256";
    private static final String JSON_IMPORTED_AT_EPOCH_MS = "importedAtEpochMs";
    private static final String JSON_TTC_INDEX = "ttcIndex";
    private static final String JSON_COLLECTION_ID = "collectionId";
    private static final String JSON_COLLECTION_DISPLAY_NAME = "collectionDisplayName";
    private static final String JSON_PUBLICATION_STATUS = "publicationStatus";

    private final SharedPreferences preferences;
    private final File fontDirectory;
    private final File publicFontDirectory;
    private final SharedPreferences legacyCatalogPreferences;
    private final Predicate<String> rootCommandExecutor;

    public FontLibraryStore(SharedPreferences preferences, File fontDirectory) {
        this(preferences, fontDirectory, null, null);
    }

    public FontLibraryStore(SharedPreferences preferences, File fontDirectory, File publicFontDirectory) {
        this(preferences, fontDirectory, publicFontDirectory, null);
    }

    /**
     * Creates the local font catalog and migrates the pre-1.15 catalog once when supplied.
     * The legacy preference is intentionally only a migration source, never a live fallback.
     */
    public FontLibraryStore(
            SharedPreferences preferences,
            File fontDirectory,
            File publicFontDirectory,
            SharedPreferences legacyCatalogPreferences
    ) {
        this(preferences, fontDirectory, publicFontDirectory, legacyCatalogPreferences,
                FontLibraryStore::runRootCommand);
    }

    /** Creates a store with an explicit root executor for deterministic host-side validation. */
    public FontLibraryStore(
            SharedPreferences preferences,
            File fontDirectory,
            File publicFontDirectory,
            SharedPreferences legacyCatalogPreferences,
            Predicate<String> rootCommandExecutor
    ) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.fontDirectory = fontDirectory;
        this.publicFontDirectory = publicFontDirectory;
        this.legacyCatalogPreferences = legacyCatalogPreferences;
        this.rootCommandExecutor = Objects.requireNonNull(rootCommandExecutor,
                "rootCommandExecutor");
        migrateLegacyCatalogIfNecessary();
        migrateLegacyTtcFaceLabelsIfNecessary();
    }

    private void migrateLegacyCatalogIfNecessary() {
        if (preferences.contains(KEY_ENTRIES)
                || legacyCatalogPreferences == null
                || legacyCatalogPreferences == preferences) {
            return;
        }
        String legacyCatalog = legacyCatalogPreferences.getString(KEY_ENTRIES, null);
        if (legacyCatalog == null || legacyCatalog.isBlank()) {
            return;
        }
        if (preferences.edit().putString(KEY_ENTRIES, legacyCatalog).commit()) {
            legacyCatalogPreferences.edit().remove(KEY_ENTRIES).commit();
        }
    }

    private void migrateLegacyTtcFaceLabelsIfNecessary() {
        List<FontLibraryEntry> entries = readEntries();
        List<FontLibraryEntry> updated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (FontLibraryEntry entry : entries) {
            FontLibraryEntry replacement = entry;
            if (isLegacyAutomaticTtcLabel(entry)) {
                File file = resolveStoredFile(entry);
                String resolved = FontFaceNameResolver.resolveTtcFaceLabel(
                        file, entry.ttcIndex, entry.displayName);
                if (!resolved.equals(entry.displayName)) {
                    replacement = new FontLibraryEntry(
                            entry.id,
                            resolved,
                            entry.sourceFileName,
                            entry.storedFileName,
                            entry.storedPath,
                            entry.sha256,
                            entry.importedAtEpochMs,
                            entry.ttcIndex,
                            entry.collectionId,
                            defaultCollectionDisplayName(entry.sourceFileName),
                            entry.publicationStatus);
                    changed = true;
                }
            }
            updated.add(replacement);
        }
        if (changed) {
            writeEntries(updated);
        }
    }

    private static boolean isLegacyAutomaticTtcLabel(FontLibraryEntry entry) {
        if (entry == null || entry.ttcIndex < 0 || entry.sourceFileName == null) {
            return false;
        }
        return entry.displayName.equals(entry.sourceFileName + " (TTC " + entry.ttcIndex + ")");
    }

    public List<FontLibraryEntry> listFonts() {
        List<FontLibraryEntry> entries = readEntries();
        entries.sort(Comparator
                .comparing((FontLibraryEntry entry) -> entry.displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.id));
        return entries;
    }

    /**
     * Performs a read-only catalog health scan. It never invokes root or changes metadata.
     */
    public synchronized HealthReport inspectHealth() {
        List<FontLibraryEntry> entries = readEntries();
        java.util.Set<String> knownPaths = new java.util.HashSet<>();
        int missingPrivateFiles = 0;
        int missingPublishedFallbacks = 0;
        for (FontLibraryEntry entry : entries) {
            knownPaths.add(entry.storedPath);
            if (resolveStoredFile(entry) == null) {
                missingPrivateFiles++;
            }
            if (entry.publicationStatus == FontPublicationStatus.PUBLISHED
                    && !isPublishedFallbackPresent(entry)) {
                missingPublishedFallbacks++;
            }
        }
        int orphanedPrivateFiles = 0;
        if (fontDirectory != null && fontDirectory.isDirectory()) {
            File[] files = fontDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!knownPaths.contains(file.getAbsolutePath())) {
                        orphanedPrivateFiles++;
                    }
                }
            }
        }
        return new HealthReport(entries.size(), missingPrivateFiles, missingPublishedFallbacks,
                orphanedPrivateFiles);
    }

    /**
     * Retries root publication for collections with a healthy private file. Call only from an
     * explicit user action; importing and health scanning must not unexpectedly request root.
     */
    public synchronized RepairResult retryPublishedFallbacks() {
        List<FontLibraryEntry> entries = readEntries();
        Map<String, FontPublicationStatus> statusesByPath = new LinkedHashMap<>();
        int attemptedCollections = 0;
        int publishedCollections = 0;
        for (FontLibraryEntry entry : entries) {
            if (statusesByPath.containsKey(entry.storedPath) || resolveStoredFile(entry) == null) {
                continue;
            }
            attemptedCollections++;
            FontPublicationStatus status = publishFontFile(new File(entry.storedPath));
            statusesByPath.put(entry.storedPath, status);
            if (status == FontPublicationStatus.PUBLISHED) {
                publishedCollections++;
            }
        }
        if (statusesByPath.isEmpty()) {
            return new RepairResult(0, 0, false);
        }
        List<FontLibraryEntry> updated = new ArrayList<>(entries.size());
        for (FontLibraryEntry entry : entries) {
            FontPublicationStatus status = statusesByPath.get(entry.storedPath);
            updated.add(status == null ? entry : copyWithPublicationStatus(entry, status));
        }
        return new RepairResult(attemptedCollections, publishedCollections, writeEntries(updated));
    }

    /**
     * Only removes import staging files. A missing or malformed catalog must never turn a
     * recoverable metadata problem into permanent user-font data loss.
     */
    public void purgeOrphanedFiles() {
        if (fontDirectory == null || !fontDirectory.isDirectory()) {
            return;
        }
        File[] files = fontDirectory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (isImportStagingFile(file)) {
                file.delete();
            }
        }
    }

    public FontLibraryEntry findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (FontLibraryEntry entry : readEntries()) {
            if (id.equals(entry.id)) {
                return entry;
            }
        }
        return null;
    }

    public File resolveFontFile(String id) {
        FontLibraryEntry entry = findById(id);
        if (entry == null) {
            return null;
        }
        File file = new File(entry.storedPath);
        return file.isFile() ? file : null;
    }

    public synchronized DeleteResult deleteFont(String id, Predicate<String> isFontReferenced) {
        FontLibraryEntry entry = findById(id);
        if (entry == null) {
            return DeleteResult.NOT_FOUND;
        }
        List<FontLibraryEntry> originalEntries = readEntries();
        List<FontLibraryEntry> collectionEntries = new ArrayList<>();
        for (FontLibraryEntry candidate : originalEntries) {
            if (entry.collectionId.equals(candidate.collectionId)) {
                if (isFontReferenced != null && isFontReferenced.test(candidate.id)) {
                    return DeleteResult.IN_USE;
                }
                collectionEntries.add(candidate);
            }
        }
        List<FontLibraryEntry> remainingEntries = new ArrayList<>(originalEntries);
        remainingEntries.removeAll(collectionEntries);
        if (!writeEntries(remainingEntries)) {
            return DeleteResult.DELETE_FAILED;
        }
        for (FontLibraryEntry candidate : collectionEntries) {
            File file = new File(candidate.storedPath);
            if (file.exists()
                    && !hasRemainingPathReference(remainingEntries, candidate.storedPath)
                    && !deleteStoredFile(file)) {
                writeEntries(originalEntries);
                return DeleteResult.DELETE_FAILED;
            }
        }
        if (!removePublishedFallbacks(collectionEntries)) {
            // The private source and catalog are already gone. Do not make deletion depend on an
            // optional compatibility copy that may need a root grant no longer available.
            DpisLog.i("FONT_LIBRARY_AUDIT published fallback cleanup deferred after font deletion");
        }
        return DeleteResult.DELETED;
    }

    private boolean deleteStoredFile(File file) {
        if (publicFontDirectory != null && isUnderPublicFontDirectory(file)) {
            return rootCommandExecutor.test("rm -f " + shellQuote(file.getAbsolutePath()));
        }
        return file.delete();
    }

    private boolean isUnderPublicFontDirectory(File file) {
        try {
            return file.getCanonicalPath().startsWith(
                    publicFontDirectory.getCanonicalPath() + File.separator);
        } catch (IOException ignored) {
            return false;
        }
    }

    public synchronized FontLibraryEntry registerCopiedFont(
            File sourceFile,
            String sourceFileName,
            long importedAtEpochMs) throws IOException {
        return registerCopiedFont(sourceFile, sourceFileName, sourceFileName, importedAtEpochMs);
    }

    public synchronized FontLibraryEntry registerCopiedFont(
            File sourceFile,
            String sourceFileName,
            String requestedDisplayName,
            long importedAtEpochMs) throws IOException {
        return registerCopiedFont(
                sourceFile,
                sourceFileName,
                requestedDisplayName,
                importedAtEpochMs,
                null);
    }

    public synchronized FontLibraryEntry registerCopiedFont(
            File sourceFile,
            String sourceFileName,
            String requestedDisplayName,
            long importedAtEpochMs,
            FontFileKind kind) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        ensureFontDirectory();

        String extension = kind == null ? resolveFontExtension(sourceFileName) : kind.extension;
        File tempFile = File.createTempFile("font_import_", extension, fontDirectory);
        String sha256 = copyAndDigest(sourceFile, tempFile);

        List<FontLibraryEntry> entries = readEntries();
        entries.removeIf(entry -> sha256.equals(entry.sha256) && resolveStoredFile(entry) == null);
        for (FontLibraryEntry entry : entries) {
            if (sha256.equals(entry.sha256)) {
                tempFile.delete();
                return entry;
            }
        }

        String id = FONT_ID_PREFIX + sha256.substring(0, 16);
        File stagingFile = new File(fontDirectory, id + extension);
        if (!tempFile.renameTo(stagingFile)) {
            Files.copy(tempFile.toPath(), stagingFile.toPath());
            tempFile.delete();
        }
        stagingFile.setReadable(true, false);
        FontPublicationStatus publicationStatus = publishFontFile(stagingFile);

        String displayName = makeUniqueDisplayName(entries, requestedDisplayName, null);
        FontLibraryEntry entry = new FontLibraryEntry(
                id,
                displayName,
                sourceFileName,
                stagingFile.getName(),
                stagingFile.getAbsolutePath(),
                sha256,
                importedAtEpochMs,
                0,
                id,
                displayName,
                publicationStatus);
        entries.add(entry);
        if (!writeEntries(entries)) {
            stagingFile.delete();
            throw new IOException("Unable to persist font library metadata");
        }
        return entry;
    }

    public synchronized List<FontLibraryEntry> registerCopiedFontFaces(
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
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(ttcIndexes, "ttcIndexes");
        if (ttcIndexes.isEmpty()) {
            return List.of();
        }
        ensureFontDirectory();
        File tempFile = File.createTempFile("font_import_", FontFileKind.TTC.extension, fontDirectory);
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
                continue;
            }
            if (!missingIndexes.contains(index)) {
                missingIndexes.add(index);
            }
        }
        if (missingIndexes.isEmpty()) {
            tempFile.delete();
            return result;
        }

        String baseId = FONT_ID_PREFIX + sha256.substring(0, 16);
        File targetFile = findExistingStoredFileForHash(entries, sha256);
        File stagingFile = null;
        FontPublicationStatus publicationStatus = FontPublicationStatus.PRIVATE;
        if (targetFile == null) {
            stagingFile = new File(fontDirectory, baseId + FontFileKind.TTC.extension);
            if (stagingFile.exists() && !stagingFile.delete()) {
                tempFile.delete();
                throw new IOException("Unable to replace staging font file: " + stagingFile);
            }
            if (!tempFile.renameTo(stagingFile)) {
                Files.copy(tempFile.toPath(), stagingFile.toPath());
                tempFile.delete();
            }
            stagingFile.setReadable(true, false);
            targetFile = stagingFile;
            publicationStatus = publishFontFile(stagingFile);
        } else {
            tempFile.delete();
            publicationStatus = publicationStatusForExistingFile(entries, targetFile);
        }

        boolean usesDefaultAlias = normalizeDisplayName(requestedDisplayName)
                .equals(normalizeDisplayName(sourceFileName));
        String requestedCollectionDisplayName = usesDefaultAlias
                ? defaultCollectionDisplayName(sourceFileName)
                : normalizeDisplayName(requestedDisplayName);
        FontLibraryEntry existingCollection = findByCollectionId(entries, baseId);
        String collectionDisplayName = existingCollection != null
                ? existingCollection.collectionDisplayName
                : makeUniqueCollectionDisplayName(entries, requestedCollectionDisplayName, baseId);
        List<FontLibraryEntry> originalEntries = new ArrayList<>(entries);
        for (Integer index : missingIndexes) {
            String id = baseId + "_ttc_" + index;
            String fallbackLabel = requestedDisplayName + " (TTC " + index + ")";
            String faceLabel = FontFaceNameResolver.resolveTtcFaceLabel(targetFile, index, fallbackLabel);
            FontLibraryEntry entry = new FontLibraryEntry(
                    id,
                    makeUniqueDisplayName(entries, faceLabel, null),
                    sourceFileName,
                    targetFile.getName(),
                    targetFile.getAbsolutePath(),
                    sha256,
                    importedAtEpochMs,
                    index,
                    baseId,
                    collectionDisplayName,
                    publicationStatus);
            entries.add(entry);
            result.add(entry);
        }
        if (!writeEntries(entries)) {
            deleteStoredFileIfUnreferenced(targetFile, originalEntries);
            throw new IOException("Unable to persist font library metadata");
        }
        return result;
    }

    public synchronized RenameResult renameFont(String id, String requestedDisplayName) {
        String displayName = sanitizeDisplayName(requestedDisplayName);
        if (displayName == null) {
            return RenameResult.INVALID_NAME;
        }
        List<FontLibraryEntry> entries = readEntries();
        FontLibraryEntry selected = null;
        for (FontLibraryEntry entry : entries) {
            if (entry.id.equals(id)) {
                selected = entry;
            }
        }
        if (selected == null) {
            return RenameResult.NOT_FOUND;
        }
        for (FontLibraryEntry entry : entries) {
            if (!selected.collectionId.equals(entry.collectionId)
                    && displayName.equalsIgnoreCase(entry.collectionDisplayName.trim())) {
                return RenameResult.DUPLICATE_NAME;
            }
        }
        int faceCount = 0;
        for (FontLibraryEntry entry : entries) {
            if (selected.collectionId.equals(entry.collectionId)) {
                faceCount++;
            }
        }
        List<FontLibraryEntry> updatedEntries = new ArrayList<>(entries.size());
        for (FontLibraryEntry entry : entries) {
            if (selected.collectionId.equals(entry.collectionId)) {
                updatedEntries.add(new FontLibraryEntry(
                        entry.id,
                        faceCount == 1 ? displayName : entry.displayName,
                        entry.sourceFileName,
                        entry.storedFileName,
                        entry.storedPath,
                        entry.sha256,
                        entry.importedAtEpochMs,
                        entry.ttcIndex,
                        entry.collectionId,
                        displayName,
                        entry.publicationStatus));
            } else {
                updatedEntries.add(entry);
            }
        }
        return writeEntries(updatedEntries) ? RenameResult.RENAMED : RenameResult.WRITE_FAILED;
    }

    private FontPublicationStatus publishFontFile(File stagingFile) {
        if (publicFontDirectory == null) {
            return FontPublicationStatus.PRIVATE;
        }
        File publicFile = new File(publicFontDirectory, "dpis_" + stagingFile.getName());
        File publicTempFile = new File(publicFontDirectory,
                "." + publicFile.getName() + ".tmp");
        File publicParent = publicFontDirectory.getParentFile();
        StringBuilder command = new StringBuilder();
        command.append("mkdir -p ").append(shellQuote(publicFontDirectory.getAbsolutePath()));
        if (publicParent != null) {
            command.append(" && chmod 755 ").append(shellQuote(publicParent.getAbsolutePath()));
        }
        // Publish by rename so a target process never observes a partially copied font file.
        command.append(" && rm -f ").append(shellQuote(publicTempFile.getAbsolutePath()))
                .append(" && cp ").append(shellQuote(stagingFile.getAbsolutePath()))
                .append(" ").append(shellQuote(publicTempFile.getAbsolutePath()))
                .append(" && chmod 755 ").append(shellQuote(publicFontDirectory.getAbsolutePath()))
                .append(" && chmod 644 ").append(shellQuote(publicTempFile.getAbsolutePath()))
                .append(" && mv -f ").append(shellQuote(publicTempFile.getAbsolutePath()))
                .append(" ").append(shellQuote(publicFile.getAbsolutePath()));
        if (!rootCommandExecutor.test(command.toString())) {
            return FontPublicationStatus.PUBLISH_FAILED;
        }
        return FontPublicationStatus.PUBLISHED;
    }

    private void ensureFontDirectory() throws IOException {
        Objects.requireNonNull(fontDirectory, "fontDirectory");
        if (!fontDirectory.exists() && !fontDirectory.mkdirs()) {
            throw new IOException("Unable to create font directory: " + fontDirectory);
        }
        if (!fontDirectory.isDirectory()) {
            throw new IOException("Font directory is not a directory: " + fontDirectory);
        }
    }

    private static File resolveStoredFile(FontLibraryEntry entry) {
        File file = new File(entry.storedPath);
        return file.isFile() ? file : null;
    }

    private static FontLibraryEntry findExistingTtcEntry(
            List<FontLibraryEntry> entries,
            String sha256,
            int ttcIndex) {
        for (FontLibraryEntry entry : entries) {
            if (sha256.equals(entry.sha256)
                    && entry.ttcIndex == ttcIndex
                    && resolveStoredFile(entry) != null) {
                return entry;
            }
        }
        return null;
    }

    private static File findExistingStoredFileForHash(List<FontLibraryEntry> entries, String sha256) {
        for (FontLibraryEntry entry : entries) {
            if (sha256.equals(entry.sha256)) {
                File file = resolveStoredFile(entry);
                if (file != null) {
                    return file;
                }
            }
        }
        return null;
    }

    /**
     * Removes optional published compatibility copies after their private authoritative catalog
     * entry is deleted. A revoked root grant must not make local font deletion unavailable.
     */
    private boolean removePublishedFallbacks(List<FontLibraryEntry> entries) {
        if (publicFontDirectory == null) {
            return true;
        }
        java.util.Set<String> removedNames = new java.util.HashSet<>();
        for (FontLibraryEntry entry : entries) {
            if (entry.publicationStatus != FontPublicationStatus.PUBLISHED
                    || entry.storedFileName == null
                    || !removedNames.add(entry.storedFileName)) {
                continue;
            }
            File publicFile = new File(publicFontDirectory, "dpis_" + entry.storedFileName);
            if (!rootCommandExecutor.test("rm -f " + shellQuote(publicFile.getAbsolutePath()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rebuilds catalog records for authoritative private files that survived a historical catalog
     * overwrite. It only accepts files produced by DPIS's content-addressed naming scheme and
     * never deletes or replaces an existing catalog entry.
     */
    public synchronized RecoveryResult recoverMissingCatalogEntries() {
        if (fontDirectory == null || !fontDirectory.isDirectory()) {
            return new RecoveryResult(0, false);
        }
        List<FontLibraryEntry> entries = readEntries();
        Set<String> knownPaths = new HashSet<>();
        Set<String> knownIds = new HashSet<>();
        for (FontLibraryEntry entry : entries) {
            knownPaths.add(entry.storedPath);
            knownIds.add(entry.id);
        }
        File[] files = fontDirectory.listFiles();
        if (files == null) {
            return new RecoveryResult(0, false);
        }
        int recoveredEntries = 0;
        for (File file : files) {
            if (!isCatalogRecoveryCandidate(file) || knownPaths.contains(file.getAbsolutePath())) {
                continue;
            }
            FontFileInspector.Result inspection = FontFileInspector.inspect(file);
            if (inspection.kind == FontFileKind.UNSUPPORTED) {
                continue;
            }
            String sha256;
            try {
                sha256 = digestFile(file);
            } catch (IOException ignored) {
                continue;
            }
            String collectionId = FONT_ID_PREFIX + sha256.substring(0, 16);
            int faceCount = inspection.kind == FontFileKind.TTC
                    ? inspection.ttc.offsets.size()
                    : 1;
            for (int ttcIndex = 0; ttcIndex < faceCount; ttcIndex++) {
                if (inspection.kind == FontFileKind.TTC
                        && FontTypefaceLoader.load(file, ttcIndex) == null) {
                    continue;
                }
                String id = inspection.kind == FontFileKind.TTC
                        ? collectionId + "_ttc_" + ttcIndex
                        : collectionId;
                if (!knownIds.add(id)) {
                    continue;
                }
                String displayName = inspection.kind == FontFileKind.TTC
                        ? FontFaceNameResolver.resolveTtcFaceLabel(
                                file, ttcIndex, recoveredDisplayName(file, ttcIndex, faceCount))
                        : recoveredDisplayName(file, ttcIndex, faceCount);
                FontLibraryEntry recovered = new FontLibraryEntry(
                        id,
                        makeUniqueDisplayName(entries, displayName, null),
                        file.getName(),
                        file.getName(),
                        file.getAbsolutePath(),
                        sha256,
                        Math.max(0L, file.lastModified()),
                        ttcIndex,
                        collectionId,
                        defaultCollectionDisplayName(file.getName()),
                        FontPublicationStatus.PRIVATE);
                FontPublicationStatus publicationStatus = isPublishedFallbackPresent(recovered)
                        ? FontPublicationStatus.PUBLISHED
                        : FontPublicationStatus.PRIVATE;
                entries.add(copyWithPublicationStatus(recovered, publicationStatus));
                recoveredEntries++;
            }
            knownPaths.add(file.getAbsolutePath());
        }
        if (recoveredEntries == 0) {
            return new RecoveryResult(0, false);
        }
        return new RecoveryResult(recoveredEntries, writeEntries(entries));
    }

    private static boolean isCatalogRecoveryCandidate(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String name = file.getName();
        int extensionIndex = name.lastIndexOf('.');
        if (!name.startsWith(FONT_ID_PREFIX) || extensionIndex != FONT_ID_PREFIX.length() + 16) {
            return false;
        }
        for (int index = FONT_ID_PREFIX.length(); index < extensionIndex; index++) {
            if (Character.digit(name.charAt(index), 16) < 0) {
                return false;
            }
        }
        String extension = name.substring(extensionIndex).toLowerCase(Locale.US);
        return FontFileKind.TTF.extension.equals(extension)
                || FontFileKind.OTF.extension.equals(extension)
                || FontFileKind.TTC.extension.equals(extension);
    }

    private static String recoveredDisplayName(File file, int ttcIndex, int faceCount) {
        String baseName = file.getName();
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }
        return faceCount > 1 ? baseName + " (TTC " + ttcIndex + ")" : baseName;
    }

    private boolean isPublishedFallbackPresent(FontLibraryEntry entry) {
        if (publicFontDirectory == null || entry == null || entry.storedFileName == null) {
            return false;
        }
        File publicFile = new File(publicFontDirectory, "dpis_" + entry.storedFileName);
        if (!publicFile.isFile() || entry.sha256 == null || entry.sha256.isBlank()) {
            return false;
        }
        try {
            return entry.sha256.equalsIgnoreCase(digestFile(publicFile));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static FontPublicationStatus publicationStatusForExistingFile(
            List<FontLibraryEntry> entries, File file) {
        if (file == null) {
            return FontPublicationStatus.PUBLISH_FAILED;
        }
        for (FontLibraryEntry entry : entries) {
            if (file.getAbsolutePath().equals(entry.storedPath)) {
                return entry.publicationStatus;
            }
        }
        return FontPublicationStatus.PRIVATE;
    }

    private static FontLibraryEntry copyWithPublicationStatus(FontLibraryEntry entry,
            FontPublicationStatus publicationStatus) {
        return new FontLibraryEntry(entry.id, entry.displayName, entry.sourceFileName,
                entry.storedFileName, entry.storedPath, entry.sha256, entry.importedAtEpochMs,
                entry.ttcIndex, entry.collectionId, entry.collectionDisplayName, publicationStatus);
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
        if (file != null
                && file.exists()
                && !hasRemainingPathReference(entries, file.getAbsolutePath())) {
            deleteStoredFile(file);
        }
    }

    private List<FontLibraryEntry> readEntries() {
        String rawJson = preferences.getString(KEY_ENTRIES, "[]");
        List<Map<String, String>> objects = parseJsonObjectArray(rawJson);
        if (objects == null) {
            DpisLog.i("FONT_LIBRARY_AUDIT catalog unreadable; preserving all private font files");
            return new ArrayList<>();
        }

        List<FontLibraryEntry> entries = new ArrayList<>();
        for (Map<String, String> object : objects) {
            FontLibraryEntry entry = parseEntry(object);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static boolean isImportStagingFile(File file) {
        String name = file != null ? file.getName() : "";
        return name.startsWith("font_import_") || name.startsWith(".font_import_");
    }

    private boolean writeEntries(List<FontLibraryEntry> entries) {
        StringBuilder array = new StringBuilder("[");
        for (FontLibraryEntry entry : entries) {
            if (array.length() > 1) {
                array.append(',');
            }
            array.append(toJson(entry));
        }
        array.append(']');
        return preferences.edit()
                .putString(KEY_ENTRIES, array.toString())
                .commit();
    }

    private static FontLibraryEntry parseEntry(Map<String, String> object) {
        String id = requiredString(object, JSON_ID);
        String displayName = requiredString(object, JSON_DISPLAY_NAME);
        String sourceFileName = requiredString(object, JSON_SOURCE_FILE_NAME);
        String storedFileName = requiredString(object, JSON_STORED_FILE_NAME);
        String storedPath = requiredString(object, JSON_STORED_PATH);
        String sha256 = requiredString(object, JSON_SHA256);
        if (id == null
                || displayName == null
                || sourceFileName == null
                || storedFileName == null
                || storedPath == null
                || sha256 == null
                || !object.containsKey(JSON_IMPORTED_AT_EPOCH_MS)) {
            return null;
        }
        long importedAtEpochMs;
        try {
            importedAtEpochMs = Long.parseLong(object.get(JSON_IMPORTED_AT_EPOCH_MS));
        } catch (NumberFormatException ignored) {
            return null;
        }
        int ttcIndex = 0;
        if (object.containsKey(JSON_TTC_INDEX)) {
            try {
                ttcIndex = Math.max(0, Integer.parseInt(object.get(JSON_TTC_INDEX)));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        FontFace legacyFace = FontFace.fromLegacyId(id);
        String collectionId = requiredString(object, JSON_COLLECTION_ID);
        if (collectionId == null && legacyFace != null) {
            collectionId = legacyFace.collectionId;
        }
        FontPublicationStatus publicationStatus = object.containsKey(JSON_PUBLICATION_STATUS)
                ? FontPublicationStatus.fromStoredValue(object.get(JSON_PUBLICATION_STATUS))
                : inferLegacyPublicationStatus(storedPath);
        String collectionDisplayName = requiredString(object, JSON_COLLECTION_DISPLAY_NAME);
        if (collectionDisplayName == null) {
            collectionDisplayName = ttcIndex > 0 || (legacyFace != null && legacyFace.collectionFace)
                    ? defaultCollectionDisplayName(sourceFileName)
                    : displayName;
        }
        return new FontLibraryEntry(
                id,
                displayName,
                sourceFileName,
                storedFileName,
                storedPath,
                sha256,
                importedAtEpochMs,
                ttcIndex,
                collectionId,
                collectionDisplayName,
                publicationStatus);
    }

    private static FontPublicationStatus inferLegacyPublicationStatus(String storedPath) {
        return storedPath != null && storedPath.startsWith("/data/local/tmp/")
                ? FontPublicationStatus.PUBLISHED
                : FontPublicationStatus.PRIVATE;
    }

    private static String toJson(FontLibraryEntry entry) {
        return "{"
                + jsonPair(JSON_ID, entry.id) + ","
                + jsonPair(JSON_DISPLAY_NAME, entry.displayName) + ","
                + jsonPair(JSON_SOURCE_FILE_NAME, entry.sourceFileName) + ","
                + jsonPair(JSON_STORED_FILE_NAME, entry.storedFileName) + ","
                + jsonPair(JSON_STORED_PATH, entry.storedPath) + ","
                + jsonPair(JSON_SHA256, entry.sha256) + ","
                + quote(JSON_IMPORTED_AT_EPOCH_MS) + ":" + entry.importedAtEpochMs + ","
                + quote(JSON_TTC_INDEX) + ":" + entry.ttcIndex + ","
                + jsonPair(JSON_COLLECTION_ID, entry.collectionId) + ","
                + jsonPair(JSON_COLLECTION_DISPLAY_NAME, entry.collectionDisplayName) + ","
                + jsonPair(JSON_PUBLICATION_STATUS, entry.publicationStatus.name())
                + "}";
    }

    private static String jsonPair(String key, String value) {
        return quote(key) + ":" + quote(value);
    }

    private static String requiredString(Map<String, String> object, String key) {
        String value = object.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : value;
    }

    private static List<Map<String, String>> parseJsonObjectArray(String rawJson) {
        if (rawJson == null) {
            return null;
        }
        JsonCursor cursor = new JsonCursor(rawJson);
        if (!cursor.consume('[')) {
            return null;
        }
        List<Map<String, String>> objects = new ArrayList<>();
        if (cursor.consume(']')) {
            return objects;
        }
        do {
            Map<String, String> object = parseJsonObject(cursor);
            if (object == null) {
                return null;
            }
            objects.add(object);
        } while (cursor.consume(','));
        return cursor.consume(']') && cursor.isExhausted() ? objects : null;
    }

    private static Map<String, String> parseJsonObject(JsonCursor cursor) {
        if (!cursor.consume('{')) {
            return null;
        }
        Map<String, String> object = new LinkedHashMap<>();
        if (cursor.consume('}')) {
            return object;
        }
        do {
            String key = cursor.readString();
            if (key == null || !cursor.consume(':')) {
                return null;
            }
            String value = cursor.readValueAsString();
            if (value == null) {
                return null;
            }
            object.put(key, value);
        } while (cursor.consume(','));
        return cursor.consume('}') ? object : null;
    }

    private static String quote(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format(Locale.US, "\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.append('"').toString();
    }

    public static String normalizeDisplayName(String sourceFileName) {
        String sanitized = sanitizeDisplayName(sourceFileName);
        if (sanitized == null) {
            return "Imported font";
        }
        return sanitized;
    }

    private static String makeUniqueDisplayName(
            List<FontLibraryEntry> entries,
            String requestedDisplayName,
            String excludingId) {
        String baseName = normalizeDisplayName(requestedDisplayName);
        String candidate = baseName;
        int suffix = 2;
        while (containsDisplayName(entries, candidate, excludingId)) {
            candidate = baseName + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    private static String makeUniqueCollectionDisplayName(
            List<FontLibraryEntry> entries,
            String requestedDisplayName,
            String collectionId) {
        String baseName = normalizeDisplayName(requestedDisplayName);
        String candidate = baseName;
        int suffix = 2;
        while (containsCollectionDisplayName(entries, candidate, collectionId)) {
            candidate = baseName + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    private static boolean containsDisplayName(
            List<FontLibraryEntry> entries,
            String displayName,
            String excludingId) {
        for (FontLibraryEntry entry : entries) {
            if (entry.id.equals(excludingId)) {
                continue;
            }
            if (displayName.equalsIgnoreCase(entry.displayName.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCollectionDisplayName(
            List<FontLibraryEntry> entries,
            String displayName,
            String collectionId) {
        for (FontLibraryEntry entry : entries) {
            if (collectionId.equals(entry.collectionId)) {
                continue;
            }
            if (displayName.equalsIgnoreCase(entry.collectionDisplayName.trim())) {
                return true;
            }
        }
        return false;
    }

    private static FontLibraryEntry findByCollectionId(
            List<FontLibraryEntry> entries,
            String collectionId) {
        for (FontLibraryEntry entry : entries) {
            if (collectionId.equals(entry.collectionId)) {
                return entry;
            }
        }
        return null;
    }

    private static String sanitizeDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(displayName.length());
        boolean previousWhitespace = false;
        for (int i = 0; i < displayName.length(); i++) {
            char character = displayName.charAt(i);
            boolean whitespace = Character.isWhitespace(character) || Character.isISOControl(character);
            if (whitespace) {
                if (!previousWhitespace) {
                    builder.append(' ');
                    previousWhitespace = true;
                }
                continue;
            }
            builder.append(character);
            previousWhitespace = false;
        }
        String sanitized = builder.toString().trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized.length() <= 80 ? sanitized : sanitized.substring(0, 80).trim();
    }

    private static String resolveFontExtension(String sourceFileName) {
        if (sourceFileName == null) {
            return ".ttf";
        }
        String lowerName = sourceFileName.toLowerCase(Locale.US);
        if (lowerName.endsWith(".otf")) {
            return ".otf";
        }
        return ".ttf";
    }

    private static String copyAndDigest(File source, File destination) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = Files.newOutputStream(destination.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }
            byte[] hashed = digest.digest();
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format(Locale.US, "%02x", value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean runRootCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
            return process.waitFor() == 0;
        } catch (IOException ignored) {
            return false;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public enum DeleteResult {
        DELETED,
        NOT_FOUND,
        IN_USE,
        DELETE_FAILED
    }

    public enum RenameResult {
        RENAMED,
        NOT_FOUND,
        INVALID_NAME,
        DUPLICATE_NAME,
        WRITE_FAILED
    }

    private static String defaultCollectionDisplayName(String sourceFileName) {
        String normalized = normalizeDisplayName(sourceFileName);
        String lower = normalized.toLowerCase(Locale.US);
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc")
                ? normalized.substring(0, normalized.length() - 4)
                : normalized;
    }

    private static String digestFile(File source) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(source)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hashed = digest.digest();
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format(Locale.US, "%02x", value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static final class HealthReport {
        public final int catalogEntryCount;
        public final int missingPrivateFileCount;
        public final int missingPublishedFallbackCount;
        public final int orphanedPrivateFileCount;

        HealthReport(int catalogEntryCount, int missingPrivateFileCount,
                int missingPublishedFallbackCount, int orphanedPrivateFileCount) {
            this.catalogEntryCount = catalogEntryCount;
            this.missingPrivateFileCount = missingPrivateFileCount;
            this.missingPublishedFallbackCount = missingPublishedFallbackCount;
            this.orphanedPrivateFileCount = orphanedPrivateFileCount;
        }
    }

    public static final class RepairResult {
        public final int attemptedCollectionCount;
        public final int publishedCollectionCount;
        public final boolean catalogUpdated;

        RepairResult(int attemptedCollectionCount, int publishedCollectionCount,
                boolean catalogUpdated) {
            this.attemptedCollectionCount = attemptedCollectionCount;
            this.publishedCollectionCount = publishedCollectionCount;
            this.catalogUpdated = catalogUpdated;
        }
    }

    public static final class RecoveryResult {
        public final int recoveredEntryCount;
        public final boolean catalogUpdated;

        RecoveryResult(int recoveredEntryCount, boolean catalogUpdated) {
            this.recoveredEntryCount = recoveredEntryCount;
            this.catalogUpdated = catalogUpdated;
        }
    }

    private static final class JsonCursor {
        private final String text;
        private int index;

        JsonCursor(String text) {
            this.text = text;
        }

        boolean consume(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                return false;
            }
            index++;
            return true;
        }

        boolean isExhausted() {
            skipWhitespace();
            return index == text.length();
        }

        String readValueAsString() {
            skipWhitespace();
            if (index >= text.length()) {
                return null;
            }
            if (text.charAt(index) == '"') {
                return readString();
            }
            int start = index;
            while (index < text.length()) {
                char character = text.charAt(index);
                if (character == ',' || character == '}' || Character.isWhitespace(character)) {
                    break;
                }
                index++;
            }
            return index > start ? text.substring(start, index) : null;
        }

        String readString() {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != '"') {
                return null;
            }
            index++;
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char character = text.charAt(index++);
                if (character == '"') {
                    return builder.toString();
                }
                if (character != '\\') {
                    builder.append(character);
                    continue;
                }
                if (index >= text.length()) {
                    return null;
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> builder.append(escaped);
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            return null;
                        }
                        try {
                            builder.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                        index += 4;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            return null;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
