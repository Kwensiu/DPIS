package com.dpis.module.fonts

import android.content.SharedPreferences
import com.dpis.module.diagnostics.DpisLog.i
import com.dpis.module.fonts.FontCatalogCodec.decode
import com.dpis.module.fonts.FontCatalogCodec.encode
import com.dpis.module.fonts.FontFace.Companion.fromLegacyId
import com.dpis.module.fonts.FontFaceNameResolver.resolveTtcFaceLabel
import com.dpis.module.fonts.FontFileInspector.inspect
import com.dpis.module.fonts.FontPublicationStatus.Companion.fromStoredValue
import com.dpis.module.fonts.FontTypefaceLoader.load
import com.dpis.module.runtime.transport.SecureProcessLauncher.start
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.function.Predicate
import kotlin.math.max

class FontLibraryStore @JvmOverloads constructor(
    preferences: SharedPreferences?,
    private val fontDirectory: File?,
    private val publicFontDirectory: File? = null,
    private val legacyCatalogPreferences: SharedPreferences? = null,
    rootCommandExecutor: Predicate<String?>? = Predicate<String?> { command -> runRootCommand(command) }
) {
    private val preferences: SharedPreferences
    private val rootCommandExecutor: Predicate<String?>

    /** Creates a store with an explicit root executor for deterministic host-side validation.  */
    /**
     * Creates the local font catalog and migrates the pre-1.15 catalog once when supplied.
     * The legacy preference is intentionally only a migration source, never a live fallback.
     */
    init {
        this.preferences = requireNotNull(preferences) { "preferences" }
        this.rootCommandExecutor = requireNotNull(rootCommandExecutor) { "rootCommandExecutor" }
        migrateLegacyCatalogIfNecessary()
        migrateLegacyTtcFaceLabelsIfNecessary()
    }

    private fun migrateLegacyCatalogIfNecessary() {
        if (preferences.contains(KEY_ENTRIES)
            || legacyCatalogPreferences == null || legacyCatalogPreferences === preferences
        ) {
            return
        }
        val legacyCatalog = legacyCatalogPreferences.getString(KEY_ENTRIES, null)
        if (legacyCatalog == null || legacyCatalog.isBlank()) {
            return
        }
        if (preferences.edit().putString(KEY_ENTRIES, legacyCatalog).commit()) {
            legacyCatalogPreferences.edit().remove(KEY_ENTRIES).commit()
        }
    }

    private fun migrateLegacyTtcFaceLabelsIfNecessary() {
        val entries = readEntries()
        val updated: MutableList<FontLibraryEntry> = ArrayList<FontLibraryEntry>(entries.size)
        var changed = false
        for (entry in entries) {
            var replacement: FontLibraryEntry? = entry
            if (isLegacyAutomaticTtcLabel(entry)) {
                val file: File? = resolveStoredFile(entry)
                val resolved = FontFaceNameResolver.resolveTtcFaceLabel(
                    file, entry.ttcIndex, entry.displayName!!
                )
                if (resolved != entry.displayName) {
                    replacement = FontLibraryEntry(
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
                        entry.publicationStatus
                    )
                    changed = true
                }
            }
            updated.add(replacement!!)
        }
        if (changed) {
            writeEntries(updated)
        }
    }

    fun listFonts(): MutableList<FontLibraryEntry> {
        val entries = readEntries()
        entries.sortWith(Comparator { first, second ->
            val displayComparison = String.CASE_INSENSITIVE_ORDER.compare(
                first.displayName ?: "",
                second.displayName ?: "",
            )
            if (displayComparison != 0) displayComparison
            else (first.id ?: "").compareTo(second.id ?: "")
        })
        return entries
    }

    /**
     * Performs a read-only catalog health scan. It never invokes root or changes metadata.
     */
    @Synchronized
    fun inspectHealth(): HealthReport {
        val entries = readEntries()
        val knownPaths: MutableSet<kotlin.String?> = HashSet<kotlin.String?>()
        var missingPrivateFiles = 0
        var missingPublishedFallbacks = 0
        for (entry in entries) {
            knownPaths.add(entry.storedPath)
            if (resolveStoredFile(entry) == null) {
                missingPrivateFiles++
            }
            if (entry.publicationStatus == FontPublicationStatus.PUBLISHED
                && !isPublishedFallbackPresent(entry)
            ) {
                missingPublishedFallbacks++
            }
        }
        var orphanedPrivateFiles = 0
        if (fontDirectory != null && fontDirectory.isDirectory()) {
            val files = fontDirectory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (!knownPaths.contains(file.getAbsolutePath())) {
                        orphanedPrivateFiles++
                    }
                }
            }
        }
        return HealthReport(
            entries.size, missingPrivateFiles, missingPublishedFallbacks,
            orphanedPrivateFiles
        )
    }

    /**
     * Retries root publication for collections with a healthy private file. Call only from an
     * explicit user action; importing and health scanning must not unexpectedly request root.
     */
    @Synchronized
    fun retryPublishedFallbacks(): RepairResult {
        val entries = readEntries()
        val statusesByPath: MutableMap<kotlin.String?, FontPublicationStatus?> =
            LinkedHashMap<kotlin.String?, FontPublicationStatus?>()
        var attemptedCollections = 0
        var publishedCollections = 0
        for (entry in entries) {
            if (statusesByPath.containsKey(entry.storedPath) || resolveStoredFile(entry) == null) {
                continue
            }
            attemptedCollections++
            val status = publishFontFile(File(entry.storedPath))
            statusesByPath.put(entry.storedPath, status)
            if (status == FontPublicationStatus.PUBLISHED) {
                publishedCollections++
            }
        }
        if (statusesByPath.isEmpty()) {
            return RepairResult(0, 0, false)
        }
        val updated: MutableList<FontLibraryEntry> = ArrayList<FontLibraryEntry>(entries.size)
        for (entry in entries) {
            val status = statusesByPath.get(entry.storedPath)
            updated.add(if (status == null) entry else copyWithPublicationStatus(entry, status))
        }
        return RepairResult(attemptedCollections, publishedCollections, writeEntries(updated))
    }

    /**
     * Only removes import staging files. A missing or malformed catalog must never turn a
     * recoverable metadata problem into permanent user-font data loss.
     */
    fun purgeOrphanedFiles() {
        if (fontDirectory == null || !fontDirectory.isDirectory()) {
            return
        }
        val files = fontDirectory.listFiles()
        if (files == null) {
            return
        }
        for (file in files) {
            if (isImportStagingFile(file)) {
                file.delete()
            }
        }
    }

    fun findById(id: String?): FontLibraryEntry? {
        if (id.isNullOrBlank()) {
            return null
        }
        for (entry in readEntries()) {
            if (id == entry.id) {
                return entry
            }
        }
        return null
    }

    fun resolveFontFile(id: String?): File? {
        val entry = findById(id)
        if (entry == null) {
            return null
        }
        val file = File(entry.storedPath)
        return if (file.isFile()) file else null
    }

    @Synchronized
    fun deleteFont(id: String?, isFontReferenced: Predicate<String>?): DeleteResult {
        if (id.isNullOrBlank()) return DeleteResult.NOT_FOUND
        val entry = findById(id)
        if (entry == null) {
            return DeleteResult.NOT_FOUND
        }
        val originalEntries = readEntries()
        val collectionEntries: MutableList<FontLibraryEntry> = ArrayList<FontLibraryEntry>()
        for (candidate in originalEntries) {
            if (entry.collectionId == candidate.collectionId) {
                if (isFontReferenced != null && candidate.id != null && isFontReferenced.test(candidate.id)) {
                    return DeleteResult.IN_USE
                }
                collectionEntries.add(candidate)
            }
        }
        val remainingEntries: MutableList<FontLibraryEntry> =
            ArrayList<FontLibraryEntry>(originalEntries)
        remainingEntries.removeAll(collectionEntries)
        if (!writeEntries(remainingEntries)) {
            return DeleteResult.DELETE_FAILED
        }
        for (candidate in collectionEntries) {
            val file = File(candidate.storedPath)
            if (file.exists()
                && !hasRemainingPathReference(
                    remainingEntries,
                    candidate.storedPath!!
                ) && !deleteStoredFile(file)
            ) {
                writeEntries(originalEntries)
                return DeleteResult.DELETE_FAILED
            }
        }
        if (!removePublishedFallbacks(collectionEntries)) {
            // The private source and catalog are already gone. Do not make deletion depend on an
            // optional compatibility copy that may need a root grant no longer available.
            i("FONT_LIBRARY_AUDIT published fallback cleanup deferred after font deletion")
        }
        return DeleteResult.DELETED
    }

    private fun deleteStoredFile(file: File): Boolean {
        if (publicFontDirectory != null && isUnderPublicFontDirectory(file)) {
            return rootCommandExecutor.test("rm -f " + shellQuote(file.getAbsolutePath()))
        }
        return file.delete()
    }

    private fun isUnderPublicFontDirectory(file: File): Boolean {
        try {
            return file.getCanonicalPath().startsWith(
                publicFontDirectory!!.getCanonicalPath() + File.separator
            )
        } catch (ignored: IOException) {
            return false
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun registerCopiedFont(
        sourceFile: File?,
        sourceFileName: kotlin.String?,
        importedAtEpochMs: Long
    ): FontLibraryEntry {
        return registerCopiedFont(sourceFile, sourceFileName, sourceFileName, importedAtEpochMs)
    }

    @Synchronized
    @Throws(IOException::class)
    fun registerCopiedFont(
        sourceFile: File?,
        sourceFileName: kotlin.String?,
        requestedDisplayName: kotlin.String?,
        importedAtEpochMs: Long
    ): FontLibraryEntry {
        return registerCopiedFont(
            sourceFile,
            sourceFileName,
            requestedDisplayName,
            importedAtEpochMs,
            null
        )
    }

    @Synchronized
    @Throws(IOException::class)
    fun registerCopiedFont(
        sourceFile: File?,
        sourceFileName: kotlin.String?,
        requestedDisplayName: kotlin.String?,
        importedAtEpochMs: Long,
        kind: FontFileKind?
    ): FontLibraryEntry {
        requireNotNull(sourceFile) { "sourceFile" }
        ensureFontDirectory()

        val extension = if (kind == null) resolveFontExtension(sourceFileName) else kind.extension
        val tempFile = File.createTempFile("font_import_", extension, fontDirectory)
        val sha256: kotlin.String = copyAndDigest(sourceFile, tempFile)

        val entries = readEntries()
        entries.removeIf { entry: FontLibraryEntry? ->
            sha256 == entry!!.sha256 && Companion.resolveStoredFile(
                entry
            ) == null
        }
        for (entry in entries) {
            if (sha256 == entry.sha256) {
                tempFile.delete()
                return entry
            }
        }

        val id: kotlin.String = FONT_ID_PREFIX + sha256.substring(0, 16)
        val stagingFile = File(fontDirectory, id + extension)
        if (!tempFile.renameTo(stagingFile)) {
            Files.copy(tempFile.toPath(), stagingFile.toPath())
            tempFile.delete()
        }
        stagingFile.setReadable(true, false)
        val publicationStatus = publishFontFile(stagingFile)

        val displayName: kotlin.String = makeUniqueDisplayName(entries, requestedDisplayName, null)
        val entry = FontLibraryEntry(
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
            publicationStatus
        )
        entries.add(entry)
        if (!writeEntries(entries)) {
            stagingFile.delete()
            throw IOException("Unable to persist font library metadata")
        }
        return entry
    }

    @Synchronized
    @Throws(IOException::class)
    fun registerCopiedFontFaces(
        sourceFile: File?,
        sourceFileName: kotlin.String?,
        requestedDisplayName: kotlin.String?,
        kind: FontFileKind?,
        ttcIndexes: List<Int>,
        importedAtEpochMs: Long
    ): List<FontLibraryEntry> {
        if (kind != FontFileKind.TTC) {
            return listOf(
                registerCopiedFont(
                    sourceFile,
                    sourceFileName,
                    requestedDisplayName,
                    importedAtEpochMs,
                    kind
                )
            )
        }
        val inputFile = requireNotNull(sourceFile) { "sourceFile" }
        if (ttcIndexes.isEmpty()) {
            return emptyList()
        }
        ensureFontDirectory()
        val tempFile = File.createTempFile("font_import_", FontFileKind.TTC.extension, requireNotNull(fontDirectory))
        val sha256: String = copyAndDigest(inputFile, tempFile)
        val entries = readEntries()
        val result = ArrayList<FontLibraryEntry>()
        val missingIndexes: MutableList<Int> = ArrayList<Int>()
        for (index in ttcIndexes) {
            if (index < 0) {
                continue
            }
            val existing: FontLibraryEntry? = findExistingTtcEntry(entries, sha256, index)
            if (existing != null) {
                result.add(existing)
                continue
            }
            if (!missingIndexes.contains(index)) {
                missingIndexes.add(index)
            }
        }
        if (missingIndexes.isEmpty()) {
            tempFile.delete()
            return result
        }

        val baseId: kotlin.String = FONT_ID_PREFIX + sha256.substring(0, 16)
        var targetFile: File? = findExistingStoredFileForHash(entries, sha256)
        var stagingFile: File? = null
        var publicationStatus = FontPublicationStatus.PRIVATE
        if (targetFile == null) {
            stagingFile = File(fontDirectory, baseId + FontFileKind.TTC.extension)
            if (stagingFile.exists() && !stagingFile.delete()) {
                tempFile.delete()
                throw IOException("Unable to replace staging font file: " + stagingFile)
            }
            if (!tempFile.renameTo(stagingFile)) {
                Files.copy(tempFile.toPath(), stagingFile.toPath())
                tempFile.delete()
            }
            stagingFile.setReadable(true, false)
            targetFile = stagingFile
            publicationStatus = publishFontFile(stagingFile)
        } else {
            tempFile.delete()
            publicationStatus = publicationStatusForExistingFile(entries, targetFile)
        }

        val usesDefaultAlias = (normalizeDisplayName(requestedDisplayName)
                == normalizeDisplayName(sourceFileName))
        val requestedCollectionDisplayName: kotlin.String = if (usesDefaultAlias)
            defaultCollectionDisplayName(sourceFileName)
        else
            normalizeDisplayName(requestedDisplayName)
        val existingCollection: FontLibraryEntry? = findByCollectionId(entries, baseId)
        val collectionDisplayName = if (existingCollection != null)
            existingCollection.collectionDisplayName
        else
            makeUniqueCollectionDisplayName(entries, requestedCollectionDisplayName, baseId)
        val originalEntries: MutableList<FontLibraryEntry> = ArrayList<FontLibraryEntry>(entries)
        for (index in missingIndexes) {
            val id = baseId + "_ttc_" + index
            val fallbackLabel = requestedDisplayName + " (TTC " + index + ")"
            val faceLabel = resolveTtcFaceLabel(targetFile, index, fallbackLabel)
            val entry = FontLibraryEntry(
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
                publicationStatus
            )
            entries.add(entry)
            result.add(entry)
        }
        if (!writeEntries(entries)) {
            deleteStoredFileIfUnreferenced(targetFile, originalEntries)
            throw IOException("Unable to persist font library metadata")
        }
        return result
    }

    @Synchronized
    fun renameFont(id: kotlin.String?, requestedDisplayName: kotlin.String?): RenameResult {
        val displayName: kotlin.String? = sanitizeDisplayName(requestedDisplayName)
        if (displayName == null) {
            return RenameResult.INVALID_NAME
        }
        val entries = readEntries()
        var selected: FontLibraryEntry? = null
        for (entry in entries) {
            if (entry.id == id) {
                selected = entry
            }
        }
        if (selected == null) {
            return RenameResult.NOT_FOUND
        }
        for (entry in entries) {
            if (selected.collectionId != entry.collectionId && displayName.equals(
                    entry.collectionDisplayName!!.trim { it <= ' ' },
                    ignoreCase = true
                )
            ) {
                return RenameResult.DUPLICATE_NAME
            }
        }
        var faceCount = 0
        for (entry in entries) {
            if (selected.collectionId == entry.collectionId) {
                faceCount++
            }
        }
        val updatedEntries: MutableList<FontLibraryEntry> =
            ArrayList<FontLibraryEntry>(entries.size)
        for (entry in entries) {
            if (selected.collectionId == entry.collectionId) {
                updatedEntries.add(
                    FontLibraryEntry(
                        entry.id,
                        if (faceCount == 1) displayName else entry.displayName,
                        entry.sourceFileName,
                        entry.storedFileName,
                        entry.storedPath,
                        entry.sha256,
                        entry.importedAtEpochMs,
                        entry.ttcIndex,
                        entry.collectionId,
                        displayName,
                        entry.publicationStatus
                    )
                )
            } else {
                updatedEntries.add(entry)
            }
        }
        return if (writeEntries(updatedEntries)) RenameResult.RENAMED else RenameResult.WRITE_FAILED
    }

    private fun publishFontFile(stagingFile: File): FontPublicationStatus {
        if (publicFontDirectory == null) {
            return FontPublicationStatus.PRIVATE
        }
        val publicFile = File(publicFontDirectory, "dpis_" + stagingFile.getName())
        val publicTempFile = File(
            publicFontDirectory,
            "." + publicFile.getName() + ".tmp"
        )
        val publicParent = publicFontDirectory.getParentFile()
        val command = StringBuilder()
        command.append("mkdir -p ").append(shellQuote(publicFontDirectory.getAbsolutePath()))
        if (publicParent != null) {
            command.append(" && chmod 755 ").append(shellQuote(publicParent.getAbsolutePath()))
        }
        // Publish by rename so a target process never observes a partially copied font file.
        command.append(" && rm -f ").append(shellQuote(publicTempFile.getAbsolutePath()))
            .append(" && cp ").append(shellQuote(stagingFile.getAbsolutePath()))
            .append(" ").append(shellQuote(publicTempFile.getAbsolutePath()))
            .append(" && chmod 755 ").append(shellQuote(publicFontDirectory.getAbsolutePath()))
            .append(" && chmod 644 ").append(shellQuote(publicTempFile.getAbsolutePath()))
            .append(" && mv -f ").append(shellQuote(publicTempFile.getAbsolutePath()))
            .append(" ").append(shellQuote(publicFile.getAbsolutePath()))
        if (!rootCommandExecutor.test(command.toString())) {
            return FontPublicationStatus.PUBLISH_FAILED
        }
        return FontPublicationStatus.PUBLISHED
    }

    @Throws(IOException::class)
    private fun ensureFontDirectory() {
        requireNotNull(fontDirectory) { "fontDirectory" }
        if (!fontDirectory!!.exists() && !fontDirectory.mkdirs()) {
            throw IOException("Unable to create font directory: " + fontDirectory)
        }
        if (!fontDirectory.isDirectory()) {
            throw IOException("Font directory is not a directory: " + fontDirectory)
        }
    }

    /**
     * Removes optional published compatibility copies after their private authoritative catalog
     * entry is deleted. A revoked root grant must not make local font deletion unavailable.
     */
    private fun removePublishedFallbacks(entries: MutableList<FontLibraryEntry>): Boolean {
        if (publicFontDirectory == null) {
            return true
        }
        val removedNames: MutableSet<kotlin.String?> = HashSet<kotlin.String?>()
        for (entry in entries) {
            if (entry.publicationStatus != FontPublicationStatus.PUBLISHED || entry.storedFileName == null || !removedNames.add(
                    entry.storedFileName
                )
            ) {
                continue
            }
            val publicFile = File(publicFontDirectory, "dpis_" + entry.storedFileName)
            if (!rootCommandExecutor.test("rm -f " + shellQuote(publicFile.getAbsolutePath()))) {
                return false
            }
        }
        return true
    }

    /**
     * Rebuilds catalog records for authoritative private files that survived a historical catalog
     * overwrite. It only accepts files produced by DPIS's content-addressed naming scheme and
     * never deletes or replaces an existing catalog entry.
     */
    @Synchronized
    fun recoverMissingCatalogEntries(): RecoveryResult {
        if (fontDirectory == null || !fontDirectory.isDirectory()) {
            return RecoveryResult(0, false)
        }
        val entries = readEntries()
        val knownPaths: MutableSet<kotlin.String?> = HashSet<kotlin.String?>()
        val knownIds: MutableSet<kotlin.String?> = HashSet<kotlin.String?>()
        for (entry in entries) {
            knownPaths.add(entry.storedPath)
            knownIds.add(entry.id)
        }
        val files = fontDirectory.listFiles()
        if (files == null) {
            return RecoveryResult(0, false)
        }
        var recoveredEntries = 0
        for (file in files) {
            if (!isCatalogRecoveryCandidate(file) || knownPaths.contains(file.getAbsolutePath())) {
                continue
            }
            val inspection = inspect(file)
            if (inspection.kind == FontFileKind.UNSUPPORTED) {
                continue
            }
            val sha256: kotlin.String?
            try {
                sha256 = digestFile(file)
            } catch (ignored: IOException) {
                continue
            }
            val collectionId: kotlin.String = FONT_ID_PREFIX + sha256.substring(0, 16)
            val faceCount = if (inspection.kind == FontFileKind.TTC)
                inspection.ttc.offsets.size
            else
                1
            for (ttcIndex in 0..<faceCount) {
                if (inspection.kind == FontFileKind.TTC
                    && load(file, ttcIndex) == null
                ) {
                    continue
                }
                val id = if (inspection.kind == FontFileKind.TTC)
                    collectionId + "_ttc_" + ttcIndex
                else
                    collectionId
                if (!knownIds.add(id)) {
                    continue
                }
                val displayName = if (inspection.kind == FontFileKind.TTC)
                    resolveTtcFaceLabel(
                        file, ttcIndex, recoveredDisplayName(file, ttcIndex, faceCount)
                    )
                else
                    recoveredDisplayName(file, ttcIndex, faceCount)
                val recovered = FontLibraryEntry(
                    id,
                    makeUniqueDisplayName(entries, displayName, null),
                    file.getName(),
                    file.getName(),
                    file.getAbsolutePath(),
                    sha256,
                    max(0L, file.lastModified()),
                    ttcIndex,
                    collectionId,
                    defaultCollectionDisplayName(file.getName()),
                    FontPublicationStatus.PRIVATE
                )
                val publicationStatus = if (isPublishedFallbackPresent(recovered))
                    FontPublicationStatus.PUBLISHED
                else
                    FontPublicationStatus.PRIVATE
                entries.add(copyWithPublicationStatus(recovered, publicationStatus))
                recoveredEntries++
            }
            knownPaths.add(file.getAbsolutePath())
        }
        if (recoveredEntries == 0) {
            return RecoveryResult(0, false)
        }
        return RecoveryResult(recoveredEntries, writeEntries(entries))
    }

    private fun isPublishedFallbackPresent(entry: FontLibraryEntry?): Boolean {
        if (publicFontDirectory == null || entry == null || entry.storedFileName == null) {
            return false
        }
        val publicFile = File(publicFontDirectory, "dpis_" + entry.storedFileName)
        if (!publicFile.isFile() || entry.sha256 == null || entry.sha256.isBlank()) {
            return false
        }
        try {
            return entry.sha256.equals(digestFile(publicFile), ignoreCase = true)
        } catch (ignored: IOException) {
            return false
        }
    }

    private fun hasRemainingPathReference(
        entries: MutableList<FontLibraryEntry>,
        storedPath: kotlin.String
    ): Boolean {
        for (entry in entries) {
            if (storedPath == entry.storedPath) {
                return true
            }
        }
        return false
    }

    private fun deleteStoredFileIfUnreferenced(
        file: File?,
        entries: MutableList<FontLibraryEntry>
    ) {
        if (file != null && file.exists()
            && !hasRemainingPathReference(entries, file.getAbsolutePath())
        ) {
            deleteStoredFile(file)
        }
    }

    private fun readEntries(): MutableList<FontLibraryEntry> {
        val rawJson: kotlin.String = preferences.getString(KEY_ENTRIES, "[]")!!
        val objects = decode(rawJson)
        if (objects == null) {
            i("FONT_LIBRARY_AUDIT catalog unreadable; preserving all private font files")
            return ArrayList<FontLibraryEntry>()
        }

        val entries: MutableList<FontLibraryEntry> = ArrayList<FontLibraryEntry>()
        for (`object` in objects) {
            val entry: FontLibraryEntry? = parseEntry(`object`)
            if (entry != null) {
                entries.add(entry)
            }
        }
        return entries
    }

    private fun writeEntries(entries: MutableList<FontLibraryEntry>): Boolean {
        return preferences.edit()
            .putString(KEY_ENTRIES, encode(entries))
            .commit()
    }

    enum class DeleteResult {
        DELETED,
        NOT_FOUND,
        IN_USE,
        DELETE_FAILED
    }

    enum class RenameResult {
        RENAMED,
        NOT_FOUND,
        INVALID_NAME,
        DUPLICATE_NAME,
        WRITE_FAILED
    }

    class HealthReport internal constructor(
        @JvmField val catalogEntryCount: Int, @JvmField val missingPrivateFileCount: Int,
        @JvmField val missingPublishedFallbackCount: Int, @JvmField val orphanedPrivateFileCount: Int
    )

    class RepairResult internal constructor(
        @JvmField val attemptedCollectionCount: Int,
        @JvmField val publishedCollectionCount: Int,
        @JvmField val catalogUpdated: Boolean
    )

    class RecoveryResult internal constructor(
        @JvmField val recoveredEntryCount: Int,
        @JvmField val catalogUpdated: Boolean
    )

    companion object {
        private const val KEY_ENTRIES = "font.library.entries"
        private const val FONT_ID_PREFIX = "font_"

        private const val JSON_ID = "id"
        private const val JSON_DISPLAY_NAME = "displayName"
        private const val JSON_SOURCE_FILE_NAME = "sourceFileName"
        private const val JSON_STORED_FILE_NAME = "storedFileName"
        private const val JSON_STORED_PATH = "storedPath"
        private const val JSON_SHA256 = "sha256"
        private const val JSON_IMPORTED_AT_EPOCH_MS = "importedAtEpochMs"
        private const val JSON_TTC_INDEX = "ttcIndex"
        private const val JSON_COLLECTION_ID = "collectionId"
        private const val JSON_COLLECTION_DISPLAY_NAME = "collectionDisplayName"
        private const val JSON_PUBLICATION_STATUS = "publicationStatus"

        private fun isLegacyAutomaticTtcLabel(entry: FontLibraryEntry?): Boolean {
            if (entry == null || entry.ttcIndex < 0 || entry.sourceFileName == null) {
                return false
            }
            return entry.displayName == entry.sourceFileName + " (TTC " + entry.ttcIndex + ")"
        }

        private fun resolveStoredFile(entry: FontLibraryEntry): File? {
            val file = File(entry.storedPath)
            return if (file.isFile()) file else null
        }

        private fun findExistingTtcEntry(
            entries: MutableList<FontLibraryEntry>,
            sha256: kotlin.String,
            ttcIndex: Int
        ): FontLibraryEntry? {
            for (entry in entries) {
                if (sha256 == entry.sha256
                    && entry.ttcIndex == ttcIndex && resolveStoredFile(entry) != null
                ) {
                    return entry
                }
            }
            return null
        }

        private fun findExistingStoredFileForHash(
            entries: MutableList<FontLibraryEntry>,
            sha256: kotlin.String
        ): File? {
            for (entry in entries) {
                if (sha256 == entry.sha256) {
                    val file: File? = resolveStoredFile(entry)
                    if (file != null) {
                        return file
                    }
                }
            }
            return null
        }

        private fun isCatalogRecoveryCandidate(file: File?): Boolean {
            if (file == null || !file.isFile()) {
                return false
            }
            val name = file.getName()
            val extensionIndex = name.lastIndexOf('.')
            if (!name.startsWith(FONT_ID_PREFIX) || extensionIndex != FONT_ID_PREFIX.length + 16) {
                return false
            }
            for (index in FONT_ID_PREFIX.length..<extensionIndex) {
                if (name.get(index).digitToIntOrNull(16) ?: -1 < 0) {
                    return false
                }
            }
            val extension = name.substring(extensionIndex).lowercase()
            return FontFileKind.TTF.extension == extension
                    || FontFileKind.OTF.extension == extension
                    || FontFileKind.TTC.extension == extension
        }

        private fun recoveredDisplayName(file: File, ttcIndex: Int, faceCount: Int): kotlin.String {
            var baseName = file.getName()
            val extensionIndex = baseName.lastIndexOf('.')
            if (extensionIndex > 0) {
                baseName = baseName.substring(0, extensionIndex)
            }
            return if (faceCount > 1) baseName + " (TTC " + ttcIndex + ")" else baseName
        }

        private fun publicationStatusForExistingFile(
            entries: MutableList<FontLibraryEntry>, file: File?
        ): FontPublicationStatus {
            if (file == null) {
                return FontPublicationStatus.PUBLISH_FAILED
            }
            for (entry in entries) {
                if (file.getAbsolutePath() == entry.storedPath) {
                    return entry.publicationStatus
                }
            }
            return FontPublicationStatus.PRIVATE
        }

        private fun copyWithPublicationStatus(
            entry: FontLibraryEntry,
            publicationStatus: FontPublicationStatus?
        ): FontLibraryEntry {
            return FontLibraryEntry(
                entry.id, entry.displayName, entry.sourceFileName,
                entry.storedFileName, entry.storedPath, entry.sha256, entry.importedAtEpochMs,
                entry.ttcIndex, entry.collectionId, entry.collectionDisplayName, publicationStatus
            )
        }

        private fun isImportStagingFile(file: File?): Boolean {
            val name = if (file != null) file.getName() else ""
            return name.startsWith("font_import_") || name.startsWith(".font_import_")
        }

        private fun parseEntry(`object`: Map<String, String>): FontLibraryEntry? {
            val id: kotlin.String? = requiredString(`object`, JSON_ID)
            val displayName: kotlin.String? = requiredString(`object`, JSON_DISPLAY_NAME)
            val sourceFileName: kotlin.String? = requiredString(`object`, JSON_SOURCE_FILE_NAME)
            val storedFileName: kotlin.String? = requiredString(`object`, JSON_STORED_FILE_NAME)
            val storedPath: kotlin.String? = requiredString(`object`, JSON_STORED_PATH)
            val sha256: kotlin.String? = requiredString(`object`, JSON_SHA256)
            if (id == null || displayName == null || sourceFileName == null || storedFileName == null || storedPath == null || sha256 == null || !`object`.containsKey(
                    JSON_IMPORTED_AT_EPOCH_MS
                )
            ) {
                return null
            }
            val importedAtEpochMs: Long
            try {
                importedAtEpochMs = (`object`.get(JSON_IMPORTED_AT_EPOCH_MS) ?: return null).toLong()
            } catch (ignored: NumberFormatException) {
                return null
            }
            var ttcIndex = 0
            if (`object`.containsKey(JSON_TTC_INDEX)) {
                try {
                    ttcIndex = max(0, (`object`.get(JSON_TTC_INDEX) ?: return null).toInt())
                } catch (ignored: NumberFormatException) {
                    return null
                }
            }
            val legacyFace = fromLegacyId(id)
            var collectionId: kotlin.String? = requiredString(`object`, JSON_COLLECTION_ID)
            if (collectionId == null && legacyFace != null) {
                collectionId = legacyFace.collectionId
            }
            val publicationStatus = if (`object`.containsKey(JSON_PUBLICATION_STATUS))
                fromStoredValue(`object`.get(JSON_PUBLICATION_STATUS))
            else
                inferLegacyPublicationStatus(storedPath)
            var collectionDisplayName: kotlin.String? =
                requiredString(`object`, JSON_COLLECTION_DISPLAY_NAME)
            if (collectionDisplayName == null) {
                collectionDisplayName =
                    if (ttcIndex > 0 || (legacyFace != null && legacyFace.collectionFace))
                        defaultCollectionDisplayName(sourceFileName)
                    else
                        displayName
            }
            return FontLibraryEntry(
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
                publicationStatus
            )
        }

        private fun inferLegacyPublicationStatus(storedPath: kotlin.String?): FontPublicationStatus {
            return if (storedPath != null && storedPath.startsWith("/data/local/tmp/"))
                FontPublicationStatus.PUBLISHED
            else
                FontPublicationStatus.PRIVATE
        }

        private fun requiredString(
            `object`: Map<String, String>,
            key: String
        ): String? {
            val value = `object`.get(key)
            if (value == null) {
                return null
            }
            val trimmed = value.trim { it <= ' ' }
            return if (trimmed.isEmpty()) null else value
        }

        @JvmStatic
        fun normalizeDisplayName(sourceFileName: String?): String {
            val sanitized: kotlin.String? = sanitizeDisplayName(sourceFileName)
            if (sanitized == null) {
                return "Imported font"
            }
            return sanitized
        }

        private fun makeUniqueDisplayName(
            entries: MutableList<FontLibraryEntry>,
            requestedDisplayName: kotlin.String?,
            excludingId: kotlin.String?
        ): kotlin.String {
            val baseName: kotlin.String = normalizeDisplayName(requestedDisplayName)
            var candidate = baseName
            var suffix = 2
            while (containsDisplayName(entries, candidate, excludingId)) {
                candidate = baseName + " (" + suffix + ")"
                suffix++
            }
            return candidate
        }

        private fun makeUniqueCollectionDisplayName(
            entries: MutableList<FontLibraryEntry>,
            requestedDisplayName: kotlin.String?,
            collectionId: kotlin.String
        ): kotlin.String {
            val baseName: kotlin.String = normalizeDisplayName(requestedDisplayName)
            var candidate = baseName
            var suffix = 2
            while (containsCollectionDisplayName(entries, candidate, collectionId)) {
                candidate = baseName + " (" + suffix + ")"
                suffix++
            }
            return candidate
        }

        private fun containsDisplayName(
            entries: MutableList<FontLibraryEntry>,
            displayName: kotlin.String,
            excludingId: kotlin.String?
        ): Boolean {
            for (entry in entries) {
                if (entry.id == excludingId) {
                    continue
                }
                if (displayName.equals(entry.displayName!!.trim { it <= ' ' }, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        private fun containsCollectionDisplayName(
            entries: MutableList<FontLibraryEntry>,
            displayName: kotlin.String,
            collectionId: kotlin.String
        ): Boolean {
            for (entry in entries) {
                if (collectionId == entry.collectionId) {
                    continue
                }
                if (displayName.equals(
                        entry.collectionDisplayName!!.trim { it <= ' ' },
                        ignoreCase = true
                    )
                ) {
                    return true
                }
            }
            return false
        }

        private fun findByCollectionId(
            entries: MutableList<FontLibraryEntry>,
            collectionId: kotlin.String
        ): FontLibraryEntry? {
            for (entry in entries) {
                if (collectionId == entry.collectionId) {
                    return entry
                }
            }
            return null
        }

        private fun sanitizeDisplayName(displayName: kotlin.String?): kotlin.String? {
            if (displayName == null) {
                return null
            }
            val builder = StringBuilder(displayName.length)
            var previousWhitespace = false
            for (i in 0..<displayName.length) {
                val character = displayName.get(i)
                val whitespace =
                    Character.isWhitespace(character) || Character.isISOControl(character)
                if (whitespace) {
                    if (!previousWhitespace) {
                        builder.append(' ')
                        previousWhitespace = true
                    }
                    continue
                }
                builder.append(character)
                previousWhitespace = false
            }
            val sanitized = builder.toString().trim { it <= ' ' }
            if (sanitized.isEmpty()) {
                return null
            }
            return if (sanitized.length <= 80) sanitized else sanitized.substring(0, 80)
                .trim { it <= ' ' }
        }

        private fun resolveFontExtension(sourceFileName: kotlin.String?): kotlin.String {
            if (sourceFileName == null) {
                return ".ttf"
            }
            val lowerName = sourceFileName.lowercase()
            if (lowerName.endsWith(".otf")) {
                return ".otf"
            }
            return ".ttf"
        }

        @Throws(IOException::class)
        private fun copyAndDigest(source: File?, destination: File): kotlin.String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(source).use { `in` ->
                    Files.newOutputStream(destination.toPath()).use { out ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while ((`in`.read(buffer).also { read = it }) != -1) {
                            digest.update(buffer, 0, read)
                            out.write(buffer, 0, read)
                        }
                    }
                }
                val hashed = digest.digest()
                val builder = StringBuilder(hashed.size * 2)
                for (value in hashed) {
                    builder.append(kotlin.String.format(Locale.US, "%02x", value.toInt() and 0xff))
                }
                return builder.toString()
            } catch (exception: NoSuchAlgorithmException) {
                throw IllegalStateException("SHA-256 is unavailable", exception)
            }
        }

        private fun runRootCommand(command: kotlin.String?): Boolean {
            var process: Process? = null
            try {
                process = start("su", "-c", command)
                return process.waitFor() == 0
            } catch (ignored: IOException) {
                return false
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            } finally {
                if (process != null) {
                    process.destroy()
                }
            }
        }

        private fun shellQuote(value: kotlin.String?): kotlin.String {
            if (value == null || value.isEmpty()) {
                return "''"
            }
            return "'" + value.replace("'", "'\\''") + "'"
        }

        private fun defaultCollectionDisplayName(sourceFileName: kotlin.String?): kotlin.String {
            val normalized: kotlin.String = normalizeDisplayName(sourceFileName)
            val lower = normalized.lowercase()
            return if (lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc"))
                normalized.substring(0, normalized.length - 4)
            else
                normalized
        }

        @Throws(IOException::class)
        private fun digestFile(source: File?): kotlin.String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(source).use { `in` ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while ((`in`.read(buffer).also { read = it }) != -1) {
                        digest.update(buffer, 0, read)
                    }
                }
                val hashed = digest.digest()
                val builder = StringBuilder(hashed.size * 2)
                for (value in hashed) {
                    builder.append(kotlin.String.format(Locale.US, "%02x", value.toInt() and 0xff))
                }
                return builder.toString()
            } catch (exception: NoSuchAlgorithmException) {
                throw IllegalStateException("SHA-256 is unavailable", exception)
            }
        }
    }
}
