package com.dpis.module.fonts

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import com.dpis.module.config.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.R
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.ui.compose.FontLibraryDialog
import com.dpis.module.ui.compose.FontLibraryPresentation
import com.dpis.module.ui.compose.FontLibraryUiItem
import com.dpis.module.ui.compose.SupportActivityContent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FontLibraryActivity : LocalizedActivity() {
    private lateinit var fontLibraryStore: FontLibraryStore
    private lateinit var configStore: FontLibraryConfigStore
    private lateinit var presentation: FontLibraryPresentation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
            this,
            DpisApplication.xposedService,
        )
        fontLibraryStore.purgeOrphanedFiles()
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
            this,
            DpisApplication.xposedService,
        )
        presentation = FontLibraryPresentation()
        SupportActivityContent.installFontLibrary(
            this,
            presentation,
            ::openFontImportPicker,
            ::openFontLibraryExportPicker,
            ::openFontLibraryImportPicker,
            ::openFontDetails,
            ::onNameSubmit,
            ::onLargeConfirm,
            ::onRepairConfirm,
        )
        refreshFontList()
        recoverMissingFontCatalogAsync()
        runFontHealthScan()
    }

    override fun onResume() {
        super.onResume()
        refreshFontList()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) {
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
                    getString(
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

    private fun openFontDetails(fontId: String) {
        startActivity(
            Intent(this, FontDetailActivity::class.java)
                .putExtra(FontDetailActivity.EXTRA_FONT_ID, fontId),
        )
    }

    private fun recoverMissingFontCatalogAsync() {
        Thread({
            val result = fontLibraryStore.recoverMissingCatalogEntries()
            if (!result.catalogUpdated || result.recoveredEntryCount == 0) {
                return@Thread
            }
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
            runOnUiThread {
                TypefaceCatalogCache.invalidate(this)
                refreshFontList()
            }
        }, "dpis-font-library-catalog-recovery").start()
    }

    @Suppress("DEPRECATION")
    private fun openFontImportPicker() {
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
            startActivityForResult(intent, REQUEST_IMPORT_FONT)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.font_library_picker_failed)
        }
    }

    private fun promptImportName(uri: Uri) {
        val sourceName: String
        val mimeType: String?
        try {
            sourceName = resolveDisplayName(uri)
            mimeType = contentResolver.getType(uri)
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

    private fun onNameSubmit(name: String) {
        val dialog = presentation.dialog as? FontLibraryDialog.Name ?: return
        presentation.dismiss()
        confirmLargeFontImport(dialog.uri, dialog.sourceName, dialog.mimeType, name)
    }

    @Suppress("DEPRECATION")
    private fun openFontLibraryExportPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(FontLibraryArchiveCodec.MIME_TYPE)
            .putExtra(Intent.EXTRA_TITLE, getString(R.string.font_library_archive_file_name))
        try {
            startActivityForResult(intent, REQUEST_EXPORT_FONT_LIBRARY)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.font_library_archive_export_failed)
        }
    }

    @Suppress("DEPRECATION")
    private fun openFontLibraryImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(FontLibraryArchiveCodec.MIME_TYPE)
        try {
            startActivityForResult(intent, REQUEST_IMPORT_FONT_LIBRARY)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.font_library_archive_import_failed)
        }
    }

    private fun exportFontLibrary(uri: Uri) {
        Thread({
            var result: FontLibraryArchiveCodec.ExportResult? = null
            try {
                contentResolver.openOutputStream(uri)?.use { output ->
                    result = FontLibraryArchiveCodec.writeArchive(output, fontLibraryStore)
                }
            } catch (_: IOException) {
                result = null
            } catch (_: RuntimeException) {
                result = null
            }
            val finalResult = result
            runOnUiThread {
                when {
                    finalResult == null -> showToast(R.string.font_library_archive_export_failed)
                    finalResult.skippedCollectionCount > 0 -> showToast(
                        R.string.font_library_archive_export_partial,
                        finalResult.collectionCount,
                        finalResult.skippedCollectionCount,
                    )
                    else -> showToast(R.string.font_library_archive_export_success)
                }
            }
        }, "dpis-font-library-export").start()
    }

    private fun importFontLibrary(uri: Uri) {
        Thread({
            var result: FontLibraryArchiveCodec.RestoreResult? = null
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    result = FontLibraryArchiveCodec.restoreArchive(input, fontLibraryStore, cacheDir)
                }
            } catch (_: IOException) {
                result = null
            } catch (_: RuntimeException) {
                result = null
            }
            val finalResult = result
            runOnUiThread {
                if (finalResult == null) {
                    showToast(R.string.font_library_archive_import_failed)
                    return@runOnUiThread
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
                TypefaceCatalogCache.invalidate(this)
                refreshFontList()
                if (finalResult.failureCount > 0) {
                    showToast(
                        R.string.font_library_archive_import_partial,
                        finalResult.collectionCount,
                        finalResult.failureCount,
                    )
                } else {
                    showToast(
                        R.string.font_library_archive_import_success,
                        finalResult.collectionCount,
                    )
                }
            }
        }, "dpis-font-library-import").start()
    }

    private fun confirmLargeFontImport(
        uri: Uri,
        sourceName: String,
        mimeType: String?,
        displayName: String,
    ) {
        val sizeBytes = resolveDocumentSize(uri)
        if (!FontImport.hasSpace(sizeBytes, cacheDir.usableSpace, filesDir.usableSpace)) {
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

    private fun onLargeConfirm() {
        val dialog = presentation.dialog as? FontLibraryDialog.Large ?: return
        presentation.dismiss()
        importFont(dialog.uri, dialog.sourceName, dialog.mimeType, dialog.displayName)
    }

    private fun resolveDocumentSize(uri: Uri): Long {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null).use { cursor ->
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
                    cacheDir,
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
            runOnUiThread {
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
                TypefaceCatalogCache.invalidate(this)
                refreshFontList()
            }
        }, "dpis-font-import").start()
    }

    private fun findLoadableTtcFaceIndexes(file: File, faceCount: Int): List<Int> {
        return (0 until faceCount).filter { FontTypefaceLoader.load(file, it) != null }
    }

    private fun copyUriToFile(uri: Uri, targetFile: File) {
        contentResolver.openInputStream(uri).use { input ->
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
        Thread({
            val report = fontLibraryStore.inspectHealth()
            if (report.missingPublishedFallbackCount <= 0 || isFinishing) {
                return@Thread
            }
            runOnUiThread { showPublishedFallbackRepairPrompt(report) }
        }, "dpis-font-library-health").start()
    }

    private fun showPublishedFallbackRepairPrompt(report: FontLibraryStore.HealthReport) {
        presentation.show(FontLibraryDialog.Repair(report.missingPublishedFallbackCount))
    }

    private fun onRepairConfirm() {
        presentation.dismiss()
        retryPublishedFallbacks()
    }

    private fun retryPublishedFallbacks() {
        Thread({
            val result = fontLibraryStore.retryPublishedFallbacks()
            runOnUiThread {
                if (result.catalogUpdated) {
                    TypefaceCatalogCache.invalidate(this)
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
        }, "dpis-font-library-publish-retry").start()
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
            contentResolver.query(
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
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(messageResId: Int, vararg args: Any?) {
        Toast.makeText(this, getString(messageResId, *args), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_IMPORT_FONT = 2001
        private const val REQUEST_EXPORT_FONT_LIBRARY = 2002
        private const val REQUEST_IMPORT_FONT_LIBRARY = 2003
    }
}
