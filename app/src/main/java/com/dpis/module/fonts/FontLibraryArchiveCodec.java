package com.dpis.module.fonts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Separate archive format for font files and face catalog metadata. Per-app configuration is
 * intentionally excluded so restoring an archive never changes an app's selected Typeface.
 */
public final class FontLibraryArchiveCodec {
    public static final String MIME_TYPE = "application/zip";
    // A safety boundary for untrusted document-provider ZIPs, not a font-library product quota.
    private static final int MAX_ARCHIVE_FILE_ENTRIES = 512;
    private static final long MAX_ARCHIVE_UNCOMPRESSED_BYTES = 2L * 1024L * 1024L * 1024L;
    private static final int MAX_MANIFEST_UNCOMPRESSED_BYTES = 1024 * 1024;
    private static final long RESTORE_FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L;
    private static final String MANIFEST_ENTRY = "font-library.tsv";
    private static final String FONT_DIRECTORY = "fonts/";
    private static final String FORMAT_HEADER = "dpis-font-library\t1";

    private FontLibraryArchiveCodec() {
    }

    public static ExportResult writeArchive(OutputStream output, FontLibraryStore store) throws IOException {
        if (output == null || store == null) {
            throw new IOException("Font archive output and store are required");
        }
        Map<String, List<FontLibraryEntry>> collections = groupCollections(store.listFonts());
        int exportedCollections = 0;
        int skippedCollections = 0;
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            StringBuilder manifest = new StringBuilder(FORMAT_HEADER).append('\n');
            for (Map.Entry<String, List<FontLibraryEntry>> collection : collections.entrySet()) {
                FontLibraryEntry first = collection.getValue().get(0);
                File file = store.resolveFontFile(first.id);
                if (file == null || !file.isFile()) {
                    skippedCollections++;
                    continue;
                }
                String archiveName = FONT_DIRECTORY + safeCollectionFileName(first);
                writeFile(zip, archiveName, file);
                manifest.append(first.collectionId).append('\t')
                        .append(encode(first.sourceFileName)).append('\t')
                        .append(encode(first.displayName)).append('\t')
                        .append(archiveName).append('\t')
                        .append(faceIndexes(collection.getValue())).append('\n');
                exportedCollections++;
            }
            writeBytes(zip, MANIFEST_ENTRY, manifest.toString().getBytes(StandardCharsets.UTF_8));
        }
        return new ExportResult(exportedCollections, skippedCollections);
    }

    public static RestoreResult restoreArchive(InputStream input, FontLibraryStore store, File temporaryDirectory)
            throws IOException {
        return restoreArchive(input, store, temporaryDirectory,
                (file, ttcIndex) -> FontTypefaceLoader.load(file, ttcIndex) != null);
    }

    public static RestoreResult restoreArchive(
            InputStream input,
            FontLibraryStore store,
            File temporaryDirectory,
            FacePreflight facePreflight
    )
            throws IOException {
        if (input == null || store == null || temporaryDirectory == null || facePreflight == null) {
            throw new IOException("Font archive input, store, temporary directory, and preflight are required");
        }
        if (!temporaryDirectory.exists() && !temporaryDirectory.mkdirs()) {
            throw new IOException("Unable to create font archive temporary directory");
        }
        Map<String, File> files = new LinkedHashMap<>();
        String manifest = null;
        long extractedBytes = 0L;
        int fileEntries = 0;
        try (ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (MANIFEST_ENTRY.equals(entry.getName())) {
                    if (manifest != null) {
                        throw new IOException("Font archive contains multiple manifests");
                    }
                    manifest = new String(readBounded(zip, MAX_MANIFEST_UNCOMPRESSED_BYTES),
                            StandardCharsets.UTF_8);
                    continue;
                }
                if (!entry.getName().startsWith(FONT_DIRECTORY) || entry.getName().contains("..")) {
                    continue;
                }
                if (++fileEntries > MAX_ARCHIVE_FILE_ENTRIES) {
                    throw new IOException("Font archive has too many files");
                }
                if (files.containsKey(entry.getName())) {
                    throw new IOException("Font archive contains duplicate font entries");
                }
                File tempFile = File.createTempFile("dpis-font-archive-", ".font", temporaryDirectory);
                try (OutputStream output = new FileOutputStream(tempFile)) {
                    extractedBytes = copyWithArchiveLimit(
                            zip, output, extractedBytes, temporaryDirectory);
                } catch (IOException error) {
                    tempFile.delete();
                    throw error;
                }
                files.put(entry.getName(), tempFile);
            }
        } catch (IOException | RuntimeException error) {
            deleteFiles(files.values());
            throw error;
        }
        try {
            if (manifest == null) {
                throw new IOException("Font archive manifest is missing");
            }
            if (!hasRestoreSpace(temporaryDirectory, extractedBytes)) {
                throw new IOException("Not enough available storage to restore font archive");
            }
            List<ArchiveCollection> manifestCollections = parseManifest(manifest);
            int collections = 0;
            int faces = 0;
            int failures = 0;
            for (ArchiveCollection collection : manifestCollections) {
                File file = files.get(collection.archiveName);
                if (file == null) {
                    failures++;
                    continue;
                }
                FontFileInspector.Result inspection = FontFileInspector.inspect(file);
                if (inspection.kind == FontFileKind.UNSUPPORTED) {
                    failures++;
                    continue;
                }
                if (!isCollectionLoadable(file, inspection, collection, facePreflight)) {
                    failures++;
                    continue;
                }
                if (inspection.kind == FontFileKind.TTC) {
                    List<FontLibraryEntry> imported = store.registerCopiedFontFaces(file,
                            collection.sourceFileName, collection.displayName, inspection.kind,
                            collection.faceIndexes, System.currentTimeMillis());
                    collections++;
                    faces += imported.size();
                } else {
                    store.registerCopiedFont(file, collection.sourceFileName, collection.displayName,
                            System.currentTimeMillis(), inspection.kind);
                    collections++;
                    faces++;
                }
            }
            return new RestoreResult(collections, faces, failures);
        } finally {
            deleteFiles(files.values());
        }
    }

    private static Map<String, List<FontLibraryEntry>> groupCollections(List<FontLibraryEntry> entries) {
        Map<String, List<FontLibraryEntry>> collections = new LinkedHashMap<>();
        for (FontLibraryEntry entry : entries) {
            collections.computeIfAbsent(entry.collectionId, unused -> new ArrayList<>()).add(entry);
        }
        return collections;
    }

    private static String safeCollectionFileName(FontLibraryEntry entry) {
        String extension = entry.storedFileName != null && entry.storedFileName.lastIndexOf('.') >= 0
                ? entry.storedFileName.substring(entry.storedFileName.lastIndexOf('.'))
                : ".font";
        return entry.collectionId.replaceAll("[^A-Za-z0-9_.-]", "_") + extension;
    }

    private static String faceIndexes(List<FontLibraryEntry> entries) {
        StringBuilder indexes = new StringBuilder();
        for (FontLibraryEntry entry : entries) {
            if (indexes.length() > 0) {
                indexes.append(',');
            }
            indexes.append(entry.ttcIndex);
        }
        return indexes.toString();
    }

    private static List<ArchiveCollection> parseManifest(String manifest) throws IOException {
        String[] lines = manifest.split("\\n");
        if (lines.length == 0 || !FORMAT_HEADER.equals(lines[0])) {
            throw new IOException("Unsupported font archive format");
        }
        List<ArchiveCollection> collections = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            String[] fields = lines[index].split("\\t", -1);
            if (fields.length != 5 || fields[0].isBlank() || !fields[3].startsWith(FONT_DIRECTORY)) {
                throw new IOException("Invalid font archive manifest entry");
            }
            collections.add(new ArchiveCollection(decode(fields[1]), decode(fields[2]), fields[3],
                    parseIndexes(fields[4])));
        }
        return collections;
    }

    private static List<Integer> parseIndexes(String value) throws IOException {
        List<Integer> indexes = new ArrayList<>();
        for (String part : value.split(",")) {
            try {
                int index = Integer.parseInt(part);
                if (index < 0) {
                    throw new IOException("Invalid TTC face index");
                }
                indexes.add(index);
            } catch (NumberFormatException error) {
                throw new IOException("Invalid TTC face index", error);
            }
        }
        return indexes;
    }

    private static boolean isCollectionLoadable(
            File file,
            FontFileInspector.Result inspection,
            ArchiveCollection collection,
            FacePreflight facePreflight
    ) {
        if (inspection.kind == FontFileKind.TTC) {
            int faceCount = inspection.ttc.offsets.size();
            if (collection.faceIndexes.isEmpty()) {
                return false;
            }
            for (Integer index : collection.faceIndexes) {
                if (index == null || index < 0 || index >= faceCount
                        || !facePreflight.isLoadable(file, index)) {
                    return false;
                }
            }
            return true;
        }
        return collection.faceIndexes.size() == 1
                && collection.faceIndexes.get(0) == 0
                && facePreflight.isLoadable(file, 0);
    }

    private static long copyWithArchiveLimit(
            InputStream input,
            OutputStream output,
            long copied,
            File temporaryDirectory
    )
            throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        long total = copied;
        while ((read = input.read(buffer)) != -1) {
            if (total > MAX_ARCHIVE_UNCOMPRESSED_BYTES - read) {
                throw new IOException("Font archive is too large when extracted");
            }
            if (temporaryDirectory.getUsableSpace() < read) {
                throw new IOException("Not enough temporary storage for font archive");
            }
            output.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

    private static boolean hasRestoreSpace(File temporaryDirectory, long extractedBytes) {
        if (extractedBytes < 0L) {
            return false;
        }
        // Temporary files already occupy one copy. The remaining two copies cover the private
        // authoritative file and an optional root-published fallback before cleanup completes.
        long required = extractedBytes > (Long.MAX_VALUE - RESTORE_FREE_SPACE_MARGIN_BYTES) / 2L
                ? Long.MAX_VALUE
                : extractedBytes * 2L + RESTORE_FREE_SPACE_MARGIN_BYTES;
        return temporaryDirectory.getUsableSpace() >= required;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value != null ? value : "").getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) throws IOException {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid font archive text", error);
        }
    }

    private static void writeFile(ZipOutputStream zip, String name, File file) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream input = new java.io.FileInputStream(file)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }

    private static void writeBytes(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static byte[] readBounded(InputStream input, int maximumBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = input.read(buffer)) != -1) {
            if (read > maximumBytes - total) {
                throw new IOException("Font archive manifest is too large");
            }
            output.write(buffer, 0, read);
            total += read;
        }
        return output.toByteArray();
    }

    private static void deleteFiles(Iterable<File> files) {
        for (File file : files) {
            if (file != null) {
                file.delete();
            }
        }
    }

    public static final class RestoreResult {
        public final int collectionCount;
        public final int faceCount;
        public final int failureCount;

        RestoreResult(int collectionCount, int faceCount, int failureCount) {
            this.collectionCount = collectionCount;
            this.faceCount = faceCount;
            this.failureCount = failureCount;
        }
    }

    public static final class ExportResult {
        public final int collectionCount;
        public final int skippedCollectionCount;

        ExportResult(int collectionCount, int skippedCollectionCount) {
            this.collectionCount = collectionCount;
            this.skippedCollectionCount = skippedCollectionCount;
        }
    }

    public interface FacePreflight {
        boolean isLoadable(File file, int ttcIndex);
    }

    private static final class ArchiveCollection {
        final String sourceFileName;
        final String displayName;
        final String archiveName;
        final List<Integer> faceIndexes;

        ArchiveCollection(String sourceFileName, String displayName, String archiveName,
                List<Integer> faceIndexes) {
            this.sourceFileName = sourceFileName;
            this.displayName = displayName;
            this.archiveName = archiveName;
            this.faceIndexes = faceIndexes;
        }
    }
}
