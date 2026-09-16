package com.dpis.module.fonts.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.fonts.FontFileInspector
import com.dpis.module.fonts.FontFileKind
import com.dpis.module.fonts.FontImport
import com.dpis.module.fonts.FontLibraryArchiveCodec
import com.dpis.module.fonts.FontLibraryConfigStore
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.FontTypefaceLoader
import com.dpis.module.fonts.TypefaceCatalogCache
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.ui.compose.FontLibraryDialog
import com.dpis.module.ui.compose.FontLibraryPresentation
import com.dpis.module.ui.compose.FontLibraryUiItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Owns font-library catalog, import/export, and SAF results.
 * The Activity forwards lifecycle and hosts [FontLibraryContent].
 */
class FontLibrarySession(
    private val activity: LocalizedActivity,
    private val onOpenFontDetails: (String) -> Unit,
) {
    private lateinit var fontLibraryStore: FontLibraryStore
    private lateinit var configStore: FontLibraryConfigStore
    private lateinit var archiveWorkflow: FontLibraryArchiveWorkflow
    private lateinit var healthWorkflow: FontLibraryHealthWorkflow
    val presentation = FontLibraryPresentation()

    fun start() {
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
            activity,
            DpisApplication.xposedService,
        )
        fontLibraryStore.purgeOrphanedFiles()
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
            activity,
            DpisApplication.xposedService,
        )
        archiveWorkflow = FontLibraryArchiveWorkflow(
            activity,
            fontLibraryStore,
            ::onArchiveExported,
            ::onArchiveImported,
        )
        healthWorkflow = FontLibraryHealthWorkflow(
            activity,
            fontLibraryStore,
            ::showPublishedFallbackRepairPrompt,
            ::onRepairFinished,
        )
        refreshFontList()
        recoverMissingFontCatalogAsync()
        runFontHealthScan()
    }

    fun onResume() {
        if (::fontLibraryStore.isInitialized) {
            refreshFontList()
        }
    }

    @Suppress("DEPRECATION")
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            return
        }
        val uri = data.data ?: return
        when (requestCode) {
            REQUEST_IMPORT_FONT -> promptImportName(uri)
            REQUEST_EXPORT_FONT_LIBRARY -> exportFontLibrary(uri)
            REQUEST_IMPORT_FONT_LIBRARY -> importFontLibrary(uri)
        }
    }

    private fun refreshFontList() {
        val collections = linkedMapOf<String, MutableList<FontLibraryEntry>>()
        for (entry in fontLibraryStore.listFonts()) {
            collections.getOrPut(entry.collectionId.orEmpty()) { mutableListOf() }.add(entry)
        }
        val items = collections.values.map { faces ->
            val entry = faces[0]
            val fontFile = fontLibraryStore.resolveFontFile(entry.id)
            FontLibraryUiItem(
                entry.id.orEmpty(),
                if (faces.size > 1) {
                    activity.getString(
                        R.string.font_library_collection_label,
                        entry.collectionDisplayName.orEmpty(),
                        faces.size,
                    )
                } else {
                    entry.collectionDisplayName.orEmpty()
                },
                entry.sourceFileName.orEmpty(),
                isCollectionInUse(entry),
                if (fontFile != null) FontTypefaceLoader.load(fontFile, entry.ttcIndex) else null,
            )
        }
        presentation.show(items)
    }

    fun openFontDetails(fontId: String) {
        onOpenFontDetails(fontId)
    }

    private fun recoverMissingFontCatalogAsync() {
        Thread({
            val result = fontLibraryStore.recoverMissingCatalogEntries()
            if (!result.catalogUpdated || result.recoveredEntryCount == 0) {
                return@Thread
            }
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
            activity.runOnUiThread {
                TypefaceCatalogCache.invalidate(activity)
                refreshFontList()
            }
        }, "dpis-font-library-catalog-recovery").start()
    }

    @Suppress("DEPRECATION")
    fun openFontImportPicker() {
        val mimeTypes = arrayOf(
            "font/ttf",
            "font/otf",
            "application/x-font-ttf",
            "application/vnd.ms-opentype",
            "font/collection",
            "font/ttc",
            "application/x-font-ttc",
        )
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        try {
            activity.startActivityForResult(intent, REQUEST_IMPORT_FONT)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.font_library_picker_failed)
        }
    }

    private fun promptImportName(uri: Uri) {
        val sourceName: String
        val mimeType: String?
        try {
            sourceName = resolveDisplayName(uri)
            mimeType = activity.contentResolver.getType(uri)
        } catch (_: RuntimeException) {
            showToast(R.string.font_library_import_failed)
            return
        }
        if (!FontImport.isPotentialInput(sourceName, mimeType)) {
            showToast(R.string.font_library_import_failed)
            return
        }
        presentation.show(
            FontLibraryDialog.Name(
                uri,
                sourceName,
                mimeType,
                FontLibraryStore.normalizeDisplayName(sourceName),
            ),
        )
    }

    fun onNameSubmit(name: String) {
        val dialog = presentation.dialog as? FontLibraryDialog.Name ?: return
        presentation.dismiss()
        confirmLargeFontImport(dialog.uri, dialog.sourceName, dialog.mimeType, name)
    }

    @Suppress("DEPRECATION")
    fun openFontLibraryExportPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(FontLibraryArchiveCodec.MIME_TYPE)
            .putExtra(Intent.EXTRA_TITLE, activity.getString(R.string.font_library_archive_file_name))
        try {
            activity.startActivityForResult(intent, REQUEST_EXPORT_FONT_LIBRARY)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.font_library_archive_export_failed)
        }
    }

    @Suppress("DEPRECATION")
    fun openFontLibraryImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(FontLibraryArchiveCodec.MIME_TYPE)
        try {
            activity.startActivityForResult(intent, REQUEST_IMPORT_FONT_LIBRARY)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.font_library_archive_import_failed)
        }
    }

    private fun exportFontLibrary(uri: Uri) {
        archiveWorkflow.export(uri)
    }

    private fun importFontLibrary(uri: Uri) {
        archiveWorkflow.import(uri)
    }

    private fun onArchiveExported(result: FontLibraryArchiveCodec.ExportResult?) {
        when {
            result == null -> showToast(R.string.font_library_archive_export_failed)
            result.skippedCollectionCount > 0 -> showToast(
                R.string.font_library_archive_export_partial,
                result.collectionCount,
                result.skippedCollectionCount,
            )
            else -> showToast(R.string.font_library_archive_export_success)
        }
    }

    private fun onArchiveImported(result: FontLibraryArchiveCodec.RestoreResult?) {
        if (result == null) {
            showToast(R.string.font_library_archive_import_failed)
            return
        }
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        TypefaceCatalogCache.invalidate(activity)
        refreshFontList()
        if (result.failureCount > 0) {
            showToast(R.string.font_library_archive_import_partial, result.collectionCount, result.failureCount)
        } else {
            showToast(R.string.font_library_archive_import_success, result.collectionCount)
        }
    }

    private fun confirmLargeFontImport(
        uri: Uri,
        sourceName: String,
        mimeType: String?,
        displayName: String,
    ) {
        val sizeBytes = resolveDocumentSize(uri)
        if (!FontImport.hasSpace(sizeBytes, activity.cacheDir.usableSpace, activity.filesDir.usableSpace)) {
            showToast(R.string.font_library_import_insufficient_space)
            return
        }
        if (sizeBytes < FontImport.LARGE_WARNING_BYTES) {
            importFont(uri, sourceName, mimeType, displayName)
            return
        }
        val sizeMiB = (sizeBytes + 1024L * 1024L - 1L) / (1024L * 1024L)
        presentation.show(
            FontLibraryDialog.Large(uri, sourceName, mimeType, displayName, sizeMiB),
        )
    }

    fun onLargeConfirm() {
        val dialog = presentation.dialog as? FontLibraryDialog.Large ?: return
        presentation.dismiss()
        importFont(dialog.uri, dialog.sourceName, dialog.mimeType, dialog.displayName)
    }

    private fun resolveDocumentSize(uri: Uri): Long {
        try {
            activity.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null).use { cursor ->
                if (cursor == null || !cursor.moveToFirst()) {
                    return -1L
                }
                val column = cursor.getColumnIndex(OpenableColumns.SIZE)
                return if (column >= 0 && !cursor.isNull(column)) cursor.getLong(column) else -1L
            }
        } catch (_: RuntimeException) {
            return -1L
        }
    }

    private fun importFont(uri: Uri, sourceName: String, mimeType: String?, displayName: String) {
        Thread({
            var tempFile: File? = null
            var importedEntry: FontLibraryEntry? = null
            var importedFaceCount = 0
            try {
                val created = File.createTempFile(
                    "dpis-font-import-",
                    FontImport.tempExtension(sourceName, mimeType),
                    activity.cacheDir,
                )
                tempFile = created
                copyUriToFile(uri, created)
                val inspection = FontFileInspector.inspect(created)
                if (inspection.kind == FontFileKind.TTC) {
                    val loadableIndexes = findLoadableTtcFaceIndexes(
                        created,
                        inspection.ttc.offsets.size,
                    )
                    val importedFaces = fontLibraryStore.registerCopiedFontFaces(
                        created,
                        sourceName,
                        displayName,
                        FontFileKind.TTC,
                        loadableIndexes,
                        System.currentTimeMillis(),
                    )
                    if (importedFaces.isEmpty()) {
                        throw IOException("No TTC face could be loaded")
                    }
                    importedFaceCount = importedFaces.size
                } else if (!isSupportedSingleFontFile(created, inspection.kind)) {
                    throw IOException("Unable to parse font")
                } else {
                    importedEntry = fontLibraryStore.registerCopiedFont(
                        created,
                        sourceName,
                        displayName,
                        System.currentTimeMillis(),
                        inspection.kind,
                    )
                }
            } catch (_: IOException) {
                importedEntry = null
                importedFaceCount = 0
            } catch (_: RuntimeException) {
                importedEntry = null
                importedFaceCount = 0
            } finally {
                tempFile?.takeIf { it.exists() }?.delete()
            }
            val finalImportedEntry = importedEntry
            val finalImportedFaceCount = importedFaceCount
            activity.runOnUiThread {
                if (finalImportedEntry == null && finalImportedFaceCount == 0) {
                    showToast(R.string.font_library_import_failed)
                    return@runOnUiThread
                }
                if (finalImportedFaceCount > 0) {
                    showToast(R.string.font_library_import_count_success, finalImportedFaceCount)
                } else {
                    showToast(R.string.font_library_import_success, finalImportedEntry!!.displayName)
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
                TypefaceCatalogCache.invalidate(activity)
                refreshFontList()
            }
        }, "dpis-font-import").start()
    }

    private fun findLoadableTtcFaceIndexes(file: File, faceCount: Int): List<Int> {
        return (0 until faceCount).filter { FontTypefaceLoader.load(file, it) != null }
    }

    private fun copyUriToFile(uri: Uri, targetFile: File) {
        activity.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(targetFile).use { output ->
                if (input == null) {
                    throw IOException("Unable to open font input stream")
                }
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    private fun runFontHealthScan() {
        healthWorkflow.scan()
    }

    private fun showPublishedFallbackRepairPrompt(report: FontLibraryStore.HealthReport) {
        presentation.show(FontLibraryDialog.Repair(report.missingPublishedFallbackCount))
    }

    fun onRepairConfirm() {
        presentation.dismiss()
        retryPublishedFallbacks()
    }

    private fun retryPublishedFallbacks() {
        healthWorkflow.repair()
    }

    private fun onRepairFinished(result: FontLibraryStore.RepairResult) {
        if (result.catalogUpdated) {
            TypefaceCatalogCache.invalidate(activity)
        }
        refreshFontList()
        showToast(
            if (result.catalogUpdated && result.publishedCollectionCount > 0) {
                R.string.font_library_publication_retry_success
            } else {
                R.string.font_library_publication_retry_failed
            },
        )
    }

    private fun isCollectionInUse(selected: FontLibraryEntry): Boolean {
        for (packageName in configStore.configuredPackages) {
            val selectedTypefaceId = configStore.getTargetTypefaceId(packageName)
            val configured = fontLibraryStore.findById(selectedTypefaceId)
            if (configured != null && selected.collectionId == configured.collectionId) {
                return true
            }
        }
        return false
    }

    private fun resolveDisplayName(uri: Uri): String {
        var displayName: String? = null
        try {
            activity.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        displayName = cursor.getString(index)
                    }
                }
            }
        } catch (_: RuntimeException) {
            displayName = null
        }
        if (!displayName.isNullOrBlank()) {
            return displayName
        }
        val path = uri.lastPathSegment
        return if (path.isNullOrBlank()) "Imported font" else path
    }

    private fun isSupportedSingleFontFile(file: File?, kind: FontFileKind): Boolean {
        if (file == null || !file.isFile || kind == FontFileKind.TTC || kind == FontFileKind.UNSUPPORTED) {
            return false
        }
        return FontTypefaceLoader.load(file, 0) != null
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(messageResId: Int, vararg args: Any?) {
        Toast.makeText(activity, activity.getString(messageResId, *args), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_IMPORT_FONT = 2001
        private const val REQUEST_EXPORT_FONT_LIBRARY = 2002
        private const val REQUEST_IMPORT_FONT_LIBRARY = 2003
    }
}
