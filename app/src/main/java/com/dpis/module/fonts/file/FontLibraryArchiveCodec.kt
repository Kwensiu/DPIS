package com.dpis.module.fonts

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Font files and catalog metadata use a separate archive from app configuration. */
object FontLibraryArchiveCodec {
    const val MIME_TYPE = "application/zip"

    private const val MAX_ARCHIVE_FILE_ENTRIES = 512
    private const val MAX_ARCHIVE_UNCOMPRESSED_BYTES = 2L * 1024L * 1024L * 1024L
    private const val MAX_MANIFEST_UNCOMPRESSED_BYTES = 1024 * 1024
    private const val RESTORE_FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L
    private const val MANIFEST_ENTRY = "font-library.tsv"
    private const val FONT_DIRECTORY = "fonts/"
    private const val FORMAT_HEADER_V1 = "dpis-font-library\t1"
    private const val FORMAT_HEADER_V2 = "dpis-font-library\t2"

    @JvmStatic
    @Throws(IOException::class)
    fun writeArchive(output: OutputStream?, store: FontLibraryStore?): ExportResult {
        if (output == null || store == null) throw IOException("Font archive output and store are required")
        val collections = groupCollections(store.listFonts())
        var exportedCollections = 0
        var skippedCollections = 0
        ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
            val manifest = StringBuilder(FORMAT_HEADER_V2).append('\n')
            for (entries in collections.values) {
                val first = entries.first()
                val file = store.resolveFontFile(first.id)
                if (file == null || !file.isFile) {
                    skippedCollections++
                    continue
                }
                val archiveName = FONT_DIRECTORY + safeCollectionFileName(first)
                writeFile(zip, archiveName, file)
                manifest.append(first.collectionId).append('\t')
                    .append(encode(first.sourceFileName)).append('\t')
                    .append(encode(first.collectionDisplayName)).append('\t')
                    .append(archiveName).append('\t')
                    .append(faceIndexes(entries)).append('\n')
                exportedCollections++
            }
            writeBytes(zip, MANIFEST_ENTRY, manifest.toString().toByteArray(StandardCharsets.UTF_8))
        }
        return ExportResult(exportedCollections, skippedCollections)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun restoreArchive(
        input: InputStream?,
        store: FontLibraryStore?,
        temporaryDirectory: File?
    ): RestoreResult =
        restoreArchive(input, store, temporaryDirectory) { file, index ->
            FontTypefaceLoader.load(
                file,
                index
            ) != null
        }

    @JvmStatic
    @Throws(IOException::class)
    fun restoreArchive(
        input: InputStream?,
        store: FontLibraryStore?,
        temporaryDirectory: File?,
        facePreflight: FacePreflight?
    ): RestoreResult {
        if (input == null || store == null || temporaryDirectory == null || facePreflight == null) {
            throw IOException("Font archive input, store, temporary directory, and preflight are required")
        }
        if (!temporaryDirectory.exists() && !temporaryDirectory.mkdirs()) {
            throw IOException("Unable to create font archive temporary directory")
        }
        val files = linkedMapOf<String, File>()
        var manifest: String? = null
        var extractedBytes = 0L
        var fileEntries = 0
        try {
            ZipInputStream(input, StandardCharsets.UTF_8).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val entryName = entry.name ?: ""
                    if (entryName == MANIFEST_ENTRY) {
                        if (manifest != null) throw IOException("Font archive contains multiple manifests")
                        manifest = String(
                            readBounded(zip, MAX_MANIFEST_UNCOMPRESSED_BYTES),
                            StandardCharsets.UTF_8
                        )
                        continue
                    }
                    if (!entryName.startsWith(FONT_DIRECTORY) || entryName.contains("..")) continue
                    if (++fileEntries > MAX_ARCHIVE_FILE_ENTRIES) throw IOException("Font archive has too many files")
                    if (files.containsKey(entryName)) throw IOException("Font archive contains duplicate font entries")
                    val tempFile =
                        File.createTempFile("dpis-font-archive-", ".font", temporaryDirectory)
                    try {
                        FileOutputStream(tempFile).use { output ->
                            extractedBytes = copyWithArchiveLimit(
                                zip,
                                output,
                                extractedBytes,
                                temporaryDirectory
                            )
                        }
                    } catch (error: IOException) {
                        tempFile.delete()
                        throw error
                    }
                    files[entryName] = tempFile
                }
            }
        } catch (error: IOException) {
            deleteFiles(files.values)
            throw error
        } catch (error: RuntimeException) {
            deleteFiles(files.values)
            throw error
        }
        try {
            val manifestText = manifest ?: throw IOException("Font archive manifest is missing")
            if (!hasRestoreSpace(temporaryDirectory, extractedBytes)) {
                throw IOException("Not enough available storage to restore font archive")
            }
            var collections = 0
            var faces = 0
            var failures = 0
            for (collection in parseManifest(manifestText)) {
                val file = files[collection.archiveName]
                if (file == null) {
                    failures++
                    continue
                }
                val inspection = FontFileInspector.inspect(file)
                if (inspection.kind == FontFileKind.UNSUPPORTED ||
                    !isCollectionLoadable(file, inspection, collection, facePreflight)
                ) {
                    failures++
                    continue
                }
                if (inspection.kind == FontFileKind.TTC) {
                    val imported = store.registerCopiedFontFaces(
                        file, collection.sourceFileName, collection.displayName, inspection.kind,
                        collection.faceIndexes, System.currentTimeMillis()
                    )
                    collections++
                    faces += imported.size
                } else {
                    store.registerCopiedFont(
                        file, collection.sourceFileName, collection.displayName,
                        System.currentTimeMillis(), inspection.kind
                    )
                    collections++
                    faces++
                }
            }
            return RestoreResult(collections, faces, failures)
        } finally {
            deleteFiles(files.values)
        }
    }

    private fun groupCollections(entries: List<FontLibraryEntry>): Map<String, List<FontLibraryEntry>> =
        entries.groupByTo(linkedMapOf()) { it.collectionId.orEmpty() }

    private fun safeCollectionFileName(entry: FontLibraryEntry): String {
        val name = entry.storedFileName
        val extension =
            name?.lastIndexOf('.')?.takeIf { it >= 0 }?.let { name.substring(it) } ?: ".font"
        return entry.collectionId.orEmpty().replace(Regex("[^A-Za-z0-9_.-]"), "_") + extension
    }

    private fun faceIndexes(entries: List<FontLibraryEntry>): String =
        entries.joinToString(",") { it.ttcIndex.toString() }

    private fun parseManifest(manifest: String): List<ArchiveCollection> {
        val lines = manifest.split("\n")
        if (lines[0] != FORMAT_HEADER_V1 && lines[0] != FORMAT_HEADER_V2) {
            throw IOException("Unsupported font archive format")
        }
        val legacy = lines[0] == FORMAT_HEADER_V1
        return lines.drop(1).filter(String::isNotBlank).map { line ->
            val fields = line.split('\t', limit = 5)
            if (fields.size != 5 || fields[0].isBlank() || !fields[3].startsWith(FONT_DIRECTORY)) {
                throw IOException("Invalid font archive manifest entry")
            }
            val sourceName = decode(fields[1])
            val displayName = if (legacy) sourceName else decode(fields[2])
            ArchiveCollection(sourceName, displayName, fields[3], parseIndexes(fields[4]))
        }
    }

    private fun parseIndexes(value: String): List<Int> = value.split(',').map { part ->
        val index = part.toIntOrNull() ?: throw IOException("Invalid TTC face index")
        if (index < 0) throw IOException("Invalid TTC face index")
        index
    }

    private fun isCollectionLoadable(
        file: File,
        inspection: FontFileInspector.Result,
        collection: ArchiveCollection,
        facePreflight: FacePreflight
    ): Boolean {
        if (inspection.kind == FontFileKind.TTC) {
            val faceCount = inspection.ttc.offsets.size
            return collection.faceIndexes.isNotEmpty() && collection.faceIndexes.all { index ->
                index >= 0 && index < faceCount && facePreflight.isLoadable(file, index)
            }
        }
        return collection.faceIndexes.size == 1 && collection.faceIndexes[0] == 0 &&
                facePreflight.isLoadable(file, 0)
    }

    private fun copyWithArchiveLimit(
        input: InputStream,
        output: OutputStream,
        copied: Long,
        directory: File
    ): Long {
        val buffer = ByteArray(8192)
        var total = copied
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            if (total > MAX_ARCHIVE_UNCOMPRESSED_BYTES - read) throw IOException("Font archive is too large when extracted")
            if (directory.usableSpace < read) throw IOException("Not enough temporary storage for font archive")
            output.write(buffer, 0, read)
            total += read
        }
        return total
    }

    private fun hasRestoreSpace(directory: File, extractedBytes: Long): Boolean {
        if (extractedBytes < 0L) return false
        val required =
            if (extractedBytes > (Long.MAX_VALUE - RESTORE_FREE_SPACE_MARGIN_BYTES) / 2L) {
                Long.MAX_VALUE
            } else extractedBytes * 2L + RESTORE_FREE_SPACE_MARGIN_BYTES
        return directory.usableSpace >= required
    }

    private fun encode(value: String?): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString((value ?: "").toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (error: IllegalArgumentException) {
        throw IOException("Invalid font archive text", error)
    }

    private fun writeFile(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                zip.write(buffer, 0, count)
            }
        }
        zip.closeEntry()
    }

    private fun writeBytes(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun readBounded(input: InputStream, maximumBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            if (read > maximumBytes - total) throw IOException("Font archive manifest is too large")
            output.write(buffer, 0, read)
            total += read
        }
        return output.toByteArray()
    }

    private fun deleteFiles(files: Iterable<File>) = files.forEach { it.delete() }

    class RestoreResult(
        @JvmField val collectionCount: Int,
        @JvmField val faceCount: Int,
        @JvmField val failureCount: Int
    )

    class ExportResult(
        @JvmField val collectionCount: Int,
        @JvmField val skippedCollectionCount: Int
    )

    fun interface FacePreflight {
        fun isLoadable(file: File, ttcIndex: Int): Boolean
    }

    private data class ArchiveCollection(
        val sourceFileName: String,
        val displayName: String,
        val archiveName: String,
        val faceIndexes: List<Int>
    )
}
