package com.dpis.module.fonts.presentation

import android.content.pm.PackageManager
import android.widget.Toast
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontLibraryConfigStore
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.FontPublicationStatus
import com.dpis.module.fonts.FontTypefaceLoader
import com.dpis.module.fonts.TypefaceCatalogCache
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.ui.compose.FontDetailDialog
import com.dpis.module.ui.compose.FontDetailPresentation
import com.dpis.module.ui.compose.FontDetailUiState
import com.dpis.module.ui.compose.FontReferenceUiItem

/**
 * Owns a single font collection's detail, rename, delete, and reference restore.
 * The Activity hosts [FontDetailContent].
 */
class FontDetailSession(
    private val activity: LocalizedActivity,
    private val fontId: String,
    private val onClose: () -> Unit,
) {
    private lateinit var fontLibraryStore: FontLibraryStore
    private lateinit var configStore: FontLibraryConfigStore
    val presentation = FontDetailPresentation()

    fun start(): Boolean {
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
            activity,
            DpisApplication.xposedService,
        )
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
            activity,
            DpisApplication.xposedService,
        )
        return refreshDetails()
    }

    fun onResume() {
        if (::fontLibraryStore.isInitialized) {
            refreshDetails()
        }
    }

    private fun refreshDetails(): Boolean {
        val entry = fontLibraryStore.findById(fontId)
        if (entry == null) {
            onClose()
            return false
        }
        val references = findReferences(entry)
        val file = fontLibraryStore.resolveFontFile(entry.id)
        presentation.show(
            FontDetailUiState(
                entry.collectionDisplayName.orEmpty(),
                entry.sourceFileName.orEmpty(),
                references.isNotEmpty(),
                entry.publicationStatus == FontPublicationStatus.PUBLISH_FAILED,
                if (file != null) FontTypefaceLoader.load(file, entry.ttcIndex) else null,
                references.map { FontReferenceUiItem(it.packageName, it.label) },
            ),
        )
        return true
    }

    fun confirmClearAppTypefaceByPackage(packageName: String) {
        val entry = fontLibraryStore.findById(fontId)
        if (entry == null) {
            onClose()
            return
        }
        val reference = findReferences(entry).firstOrNull { it.packageName == packageName } ?: return
        presentation.show(FontDetailDialog.Restore(reference.packageName, reference.label))
    }

    fun confirmDeleteForCurrentEntry() {
        val entry = fontLibraryStore.findById(fontId)
        if (entry == null) {
            onClose()
            return
        }
        val references = findReferences(entry)
        val title = activity.getString(R.string.font_library_delete_title)
        val confirm = activity.getString(R.string.font_library_delete_action)
        val message = if (references.isEmpty()) {
            activity.getString(R.string.font_library_delete_message, entry.collectionDisplayName)
        } else {
            activity.getString(
                R.string.font_library_delete_in_use_message,
                entry.collectionDisplayName,
                references.size,
            )
        }
        presentation.show(FontDetailDialog.Delete(title, message, confirm))
    }

    fun showFallbackExplanationDialog() {
        presentation.show(FontDetailDialog.Fallback)
    }

    fun promptRename() {
        val entry = fontLibraryStore.findById(fontId) ?: return
        presentation.show(FontDetailDialog.Rename(entry.collectionDisplayName.orEmpty()))
    }

    fun onRenameSubmit(name: String): Boolean {
        val result = fontLibraryStore.renameFont(fontId, name)
        if (result != FontLibraryStore.RenameResult.RENAMED) {
            showToast(
                if (result == FontLibraryStore.RenameResult.DUPLICATE_NAME) {
                    R.string.font_library_name_duplicate
                } else {
                    R.string.font_library_name_invalid
                },
            )
            return false
        }
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        TypefaceCatalogCache.invalidate(activity)
        refreshDetails()
        return true
    }

    fun onDeleteConfirm() {
        val entry = fontLibraryStore.findById(fontId)
        presentation.dismiss()
        if (entry == null) {
            onClose()
            return
        }
        val references = findReferences(entry)
        val result = if (references.isEmpty()) {
            fontLibraryStore.deleteFont(entry.id, ::isFontReferenced)
        } else {
            forceDeleteFont(entry, references)
        }
        handleDeleteResult(result)
    }

    private fun handleDeleteResult(result: FontLibraryStore.DeleteResult) {
        if (result == FontLibraryStore.DeleteResult.DELETED) {
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
            TypefaceCatalogCache.invalidate(activity)
            onClose()
        } else {
            showToast(
                if (result == FontLibraryStore.DeleteResult.IN_USE) {
                    R.string.font_library_delete_in_use
                } else {
                    R.string.font_library_delete_failed
                },
            )
        }
    }

    private fun forceDeleteFont(
        entry: FontLibraryEntry,
        references: List<FontReference>,
    ): FontLibraryStore.DeleteResult {
        val cleared = mutableListOf<FontReference>()
        for (reference in references) {
            if (!configStore.clearTargetTypefaceId(reference.packageName)) {
                if (!restoreTypefaceReferences(cleared)) {
                    DpisLog.i(
                        "FONT_LIBRARY_AUDIT unable to restore all typeface references after "
                            + "force-delete setup failed",
                    )
                }
                return FontLibraryStore.DeleteResult.DELETE_FAILED
            }
            cleared.add(reference)
        }
        val result = fontLibraryStore.deleteFont(entry.id) { false }
        if (result != FontLibraryStore.DeleteResult.DELETED) {
            if (restoreTypefaceReferences(cleared)) {
                publishTypefaceReferences(cleared, restoreOriginal = true)
            } else {
                DpisLog.i(
                    "FONT_LIBRARY_AUDIT typeface reference rollback incomplete; "
                        + "runtime state was not restored",
                )
            }
        } else {
            publishTypefaceReferences(cleared, restoreOriginal = false)
        }
        return result
    }

    fun onRestoreConfirm() {
        val dialog = presentation.dialog as? FontDetailDialog.Restore ?: return
        presentation.dismiss()
        if (!configStore.clearTargetTypefaceId(dialog.packageName)) {
            showToast(R.string.font_library_restore_app_font_failed)
            return
        }
        FontRuntimePropertySyncer.publishTypefaceTargetAsync(dialog.packageName, null)
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        refreshDetails()
    }

    fun onFallbackRetry() {
        presentation.dismiss()
        retryPublishedFallbacks()
    }

    private fun retryPublishedFallbacks() {
        Thread({
            val result = fontLibraryStore.retryPublishedFallbacks()
            activity.runOnUiThread {
                if (result.catalogUpdated) {
                    TypefaceCatalogCache.invalidate(activity)
                }
                refreshDetails()
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

    private fun findReferences(selected: FontLibraryEntry): List<FontReference> {
        val collectionFaceIds = fontLibraryStore.listFonts()
            .filter { it.collectionId == selected.collectionId }
            .map { it.id }
        val references = configStore.configuredPackages.mapNotNull { packageName ->
            val selectedId = configStore.getTargetTypefaceId(packageName)
            if (collectionFaceIds.contains(selectedId)) {
                FontReference(packageName, resolveAppLabel(packageName), selectedId)
            } else {
                null
            }
        }.toMutableList()
        references.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        return references
    }

    private fun isFontReferenced(id: String): Boolean {
        val entry = fontLibraryStore.findById(id)
        return entry != null && findReferences(entry).isNotEmpty()
    }

    private fun restoreTypefaceReferences(references: List<FontReference>): Boolean {
        var restored = true
        for (reference in references) {
            restored = restored && configStore.setTargetTypefaceId(
                reference.packageName,
                reference.typefaceId,
            )
        }
        return restored
    }

    private fun publishTypefaceReferences(
        references: List<FontReference>,
        restoreOriginal: Boolean,
    ) {
        for (reference in references) {
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(
                reference.packageName,
                if (restoreOriginal) reference.typefaceId else null,
            )
        }
    }

    private fun resolveAppLabel(packageName: String): String {
        try {
            val info = activity.packageManager.getApplicationInfo(packageName, 0)
            val label = activity.packageManager.getApplicationLabel(info)
            if (label.isNotEmpty()) return label.toString()
        } catch (_: PackageManager.NameNotFoundException) {
        } catch (_: RuntimeException) {
        }
        return packageName
    }

    private fun showToast(id: Int) {
        Toast.makeText(activity, id, Toast.LENGTH_SHORT).show()
    }

    private class FontReference(
        val packageName: String,
        val label: String,
        val typefaceId: String,
    )
}
