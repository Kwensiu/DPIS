package com.dpis.module.fonts;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    private final SharedPreferences preferences;
    private final File fontDirectory;
    private final File publicFontDirectory;

    public FontLibraryStore(SharedPreferences preferences, File fontDirectory) {
        this(preferences, fontDirectory, null);
    }

    public FontLibraryStore(SharedPreferences preferences, File fontDirectory, File publicFontDirectory) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.fontDirectory = fontDirectory;
        this.publicFontDirectory = publicFontDirectory;
    }

    public List<FontLibraryEntry> listFonts() {
        List<FontLibraryEntry> entries = readEntries();
        entries.sort(Comparator
                .comparing((FontLibraryEntry entry) -> entry.displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.id));
        return entries;
    }

    public void purgeOrphanedFiles() {
        if (fontDirectory == null || !fontDirectory.isDirectory()) {
            return;
        }
        File[] files = fontDirectory.listFiles();
        if (files == null) {
            return;
        }
        List<FontLibraryEntry> entries = readEntries();
        java.util.Set<String> knownPaths = new java.util.HashSet<>();
        for (FontLibraryEntry entry : entries) {
            knownPaths.add(entry.storedPath);
        }
        for (File file : files) {
            if (!knownPaths.contains(file.getAbsolutePath())) {
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
        if (isFontReferenced != null && isFontReferenced.test(entry.id)) {
            return DeleteResult.IN_USE;
        }
        List<FontLibraryEntry> originalEntries = readEntries();
        List<FontLibraryEntry> remainingEntries = new ArrayList<>(originalEntries);
        remainingEntries.removeIf(candidate -> entry.id.equals(candidate.id));
        if (!writeEntries(remainingEntries)) {
            return DeleteResult.DELETE_FAILED;
        }
        File file = new File(entry.storedPath);
        if (file.exists()
                && !hasRemainingPathReference(remainingEntries, entry.storedPath)
                && !deleteStoredFile(file)) {
            writeEntries(originalEntries);
            return DeleteResult.DELETE_FAILED;
        }
        return DeleteResult.DELETED;
    }

    private boolean deleteStoredFile(File file) {
        if (publicFontDirectory != null && isUnderPublicFontDirectory(file)) {
            return runRootCommand("rm -f " + shellQuote(file.getAbsolutePath()));
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
        File targetFile = publishFontFile(stagingFile);

        FontLibraryEntry entry = new FontLibraryEntry(
                id,
                makeUniqueDisplayName(entries, requestedDisplayName, null),
                sourceFileName,
                targetFile.getName(),
                targetFile.getAbsolutePath(),
                sha256,
                importedAtEpochMs);
        entries.add(entry);
        if (!writeEntries(entries)) {
            targetFile.delete();
            if (!targetFile.equals(stagingFile)) {
                stagingFile.delete();
            }
            throw new IOException("Unable to persist font library metadata");
        }
        if (!targetFile.equals(stagingFile)) {
            stagingFile.delete();
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
            targetFile = publishFontFile(stagingFile);
        } else {
            tempFile.delete();
        }

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
            if (stagingFile != null && !targetFile.equals(stagingFile)) {
                stagingFile.delete();
            }
            throw new IOException("Unable to persist font library metadata");
        }
        if (stagingFile != null && !targetFile.equals(stagingFile)) {
            stagingFile.delete();
        }
        return result;
    }

    public synchronized RenameResult renameFont(String id, String requestedDisplayName) {
        String displayName = sanitizeDisplayName(requestedDisplayName);
        if (displayName == null) {
            return RenameResult.INVALID_NAME;
        }
        List<FontLibraryEntry> entries = readEntries();
        boolean found = false;
        for (FontLibraryEntry entry : entries) {
            if (!entry.id.equals(id) && displayName.equalsIgnoreCase(entry.displayName.trim())) {
                return RenameResult.DUPLICATE_NAME;
            }
        }
        List<FontLibraryEntry> updatedEntries = new ArrayList<>(entries.size());
        for (FontLibraryEntry entry : entries) {
            if (entry.id.equals(id)) {
                found = true;
                updatedEntries.add(new FontLibraryEntry(
                        entry.id,
                        displayName,
                        entry.sourceFileName,
                        entry.storedFileName,
                        entry.storedPath,
                        entry.sha256,
                        entry.importedAtEpochMs,
                        entry.ttcIndex));
            } else {
                updatedEntries.add(entry);
            }
        }
        if (!found) {
            return RenameResult.NOT_FOUND;
        }
        return writeEntries(updatedEntries) ? RenameResult.RENAMED : RenameResult.WRITE_FAILED;
    }

    private File publishFontFile(File stagingFile) throws IOException {
        if (publicFontDirectory == null) {
            return stagingFile;
        }
        File publicFile = new File(publicFontDirectory, "dpis_" + stagingFile.getName());
        File publicParent = publicFontDirectory.getParentFile();
        StringBuilder command = new StringBuilder();
        command.append("mkdir -p ").append(shellQuote(publicFontDirectory.getAbsolutePath()));
        if (publicParent != null) {
            command.append(" && chmod 755 ").append(shellQuote(publicParent.getAbsolutePath()));
        }
        command.append(" && cp ").append(shellQuote(stagingFile.getAbsolutePath()))
                .append(" ").append(shellQuote(publicFile.getAbsolutePath()))
                .append(" && chmod 755 ").append(shellQuote(publicFontDirectory.getAbsolutePath()))
                .append(" && chmod 644 ").append(shellQuote(publicFile.getAbsolutePath()));
        if (!runRootCommand(command.toString())) {
            throw new IOException("Unable to publish font file: " + publicFile);
        }
        return publicFile;
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
        return new FontLibraryEntry(
                id,
                displayName,
                sourceFileName,
                storedFileName,
                storedPath,
                sha256,
                importedAtEpochMs,
                ttcIndex);
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
                + quote(JSON_TTC_INDEX) + ":" + entry.ttcIndex
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
