package com.dpis.module.fonts

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.R
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.ui.compose.FontDetailDialog
import com.dpis.module.ui.compose.FontDetailPresentation
import com.dpis.module.ui.compose.FontDetailUiState
import com.dpis.module.ui.compose.FontReferenceUiItem
import com.dpis.module.ui.compose.SupportActivityContent

class FontDetailActivity : LocalizedActivity() {
    private lateinit var fontLibraryStore: FontLibraryStore
    private lateinit var configStore: FontLibraryConfigStore
    private lateinit var fontId: String
    private lateinit var presentation: FontDetailPresentation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extraId = intent.getStringExtra(EXTRA_FONT_ID)
        if (extraId.isNullOrBlank()) {
            finish()
            return
        }
        fontId = extraId
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
            this,
            DpisApplication.xposedService,
        )
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
            this,
            DpisApplication.xposedService,
        )
        presentation = FontDetailPresentation()
        SupportActivityContent.installFontDetail(
            this,
            presentation,
            ::showFallbackExplanationDialog,
            ::promptRename,
            ::confirmDeleteForCurrentEntry,
            ::confirmClearAppTypefaceByPackage,
            ::onRenameSubmit,
            ::onFallbackRetry,
            ::onDeleteConfirm,
            ::onRestoreConfirm,
        )
        refreshDetails()
    }

    override fun onResume() {
        super.onResume()
        if (::presentation.isInitialized) {
            refreshDetails()
        }
    }

    private fun refreshDetails() {
        val entry = fontLibraryStore.findById(fontId)
        if (entry == null) {
            finish()
            return
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
    }

    private fun confirmClearAppTypefaceByPackage(packageName: String) {
        val entry = fontLibraryStore.findById(fontId)
        if (entry == null) {
            finish()
            return
        }
        val reference = findReferences(entry).firstOrNull { it.packageName == packageName } ?: return
        presentation.show(FontDetailDialog.Restore(reference.packageName, reference.label))
    }

    private fun confirmDeleteForCurrentEntry() {
        val entry = fontLibraryStore.findById(fontId)
        if (entry == null) {
            finish()
            return
        }
        val references = findReferences(entry)
        val title = getString(R.string.font_library_delete_title)
        val confirm = getString(R.string.font_library_delete_action)
        val message = if (references.isEmpty()) {
            getString(R.string.font_library_delete_message, entry.collectionDisplayName)
        } else {
            getString(
                R.string.font_library_delete_in_use_message,
                entry.collectionDisplayName,
                references.size,
            )
        }
        presentation.show(FontDetailDialog.Delete(title, message, confirm))
    }

    private fun showFallbackExplanationDialog() {
        presentation.show(FontDetailDialog.Fallback)
    }

    private fun promptRename() {
        val entry = fontLibraryStore.findById(fontId) ?: return
        presentation.show(FontDetailDialog.Rename(entry.collectionDisplayName.orEmpty()))
    }

    private fun onRenameSubmit(name: String): Boolean {
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
        TypefaceCatalogCache.invalidate(this)
        refreshDetails()
        return true
    }

    private fun onDeleteConfirm() {
        val entry = fontLibraryStore.findById(fontId)
        presentation.dismiss()
        if (entry == null) {
            finish()
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
            TypefaceCatalogCache.invalidate(this)
            finish()
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

    private fun onRestoreConfirm() {
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

    private fun onFallbackRetry() {
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
            val info = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(info)
            if (label.isNotEmpty()) return label.toString()
        } catch (_: PackageManager.NameNotFoundException) {
        } catch (_: RuntimeException) {
        }
        return packageName
    }

    private fun showToast(id: Int) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
    }

    private class FontReference(
        val packageName: String,
        val label: String,
        val typefaceId: String,
    )

    companion object {
        const val EXTRA_FONT_ID = "com.dpis.module.fonts.extra.FONT_ID"
    }
}
