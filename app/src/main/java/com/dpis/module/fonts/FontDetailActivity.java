package com.dpis.module.fonts;

import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisApplication;
import com.dpis.module.DpisLog;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;
import com.dpis.module.ui.DialogWindowSizer;
import com.dpis.module.ui.TouchFeedbackBinder;
import com.dpis.module.ui.compose.FontDetailPresentation;
import com.dpis.module.ui.compose.FontDetailUiState;
import com.dpis.module.ui.compose.FontReferenceUiItem;
import com.dpis.module.ui.compose.SupportActivityContent;
import com.dpis.module.ui.compose.ComposeConfirmDialog;
import com.dpis.module.ui.compose.ComposeTextInputDialog;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Full-screen management view for one imported font collection. */
public final class FontDetailActivity extends LocalizedActivity {
    public static final String EXTRA_FONT_ID = "com.dpis.module.fonts.extra.FONT_ID";
    private FontLibraryStore fontLibraryStore;
    private FontLibraryConfigStore configStore;
    private String fontId;
    private FontDetailPresentation presentation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fontId = getIntent().getStringExtra(EXTRA_FONT_ID);
        if (fontId == null || fontId.isBlank()) {
            finish();
            return;
        }
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
                this, DpisApplication.getXposedService());
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
                this, DpisApplication.getXposedService());
        presentation = new FontDetailPresentation();
        SupportActivityContent.installFontDetail(
                this,
                presentation,
                this::showFallbackExplanationDialog,
                () -> {
            FontLibraryEntry entry = fontLibraryStore.findById(fontId);
            if (entry != null) {
                promptRename(entry);
            }
                },
                this::confirmDeleteForCurrentEntry,
                this::confirmClearAppTypefaceByPackage);
        refreshDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presentation != null) {
            refreshDetails();
        }
    }

    private void refreshDetails() {
        FontLibraryEntry entry = fontLibraryStore.findById(fontId);
        if (entry == null) {
            finish();
            return;
        }
        List<FontReference> references = findReferences(entry);
        File file = fontLibraryStore.resolveFontFile(entry.id);
        List<FontReferenceUiItem> referenceItems = new ArrayList<>();
        for (FontReference reference : references) {
            referenceItems.add(new FontReferenceUiItem(reference.packageName, reference.label));
        }
        presentation.show(new FontDetailUiState(
                resolveFontTitle(entry),
                entry.sourceFileName == null ? "" : entry.sourceFileName,
                !references.isEmpty(),
                entry.publicationStatus == FontPublicationStatus.PUBLISHED,
                entry.publicationStatus == FontPublicationStatus.PUBLISH_FAILED,
                file != null ? FontTypefaceLoader.load(file, entry.ttcIndex) : null,
                referenceItems));
    }

    private void confirmClearAppTypefaceByPackage(String packageName) {
        FontLibraryEntry entry = fontLibraryStore.findById(fontId);
        if (entry == null) {
            finish();
            return;
        }
        for (FontReference reference : findReferences(entry)) {
            if (reference.packageName.equals(packageName)) {
                confirmClearAppTypeface(entry, reference);
                return;
            }
        }
    }

    private void confirmDeleteForCurrentEntry() {
        FontLibraryEntry entry = fontLibraryStore.findById(fontId);
        if (entry == null) {
            finish();
            return;
        }
        List<FontReference> references = findReferences(entry);
        if (references.isEmpty()) {
            confirmDelete(entry);
        } else {
            confirmForceDelete(entry, references);
        }
    }

    private void showFallbackExplanationDialog() {
        ComposeConfirmDialog.showWithLabels(this,
                getString(R.string.font_library_fallback_dialog_title),
                getString(R.string.font_library_fallback_dialog_message),
                getString(R.string.dialog_close_button),
                getString(R.string.font_library_publication_retry_action),
                this::retryPublishedFallbacks, () -> {});
    }

    private void promptRename(FontLibraryEntry entry) {
        ComposeTextInputDialog.show(this,
                getString(R.string.font_library_name_title),
                getString(R.string.font_library_name_hint),
                entry.collectionDisplayName,
                name -> {
                FontLibraryStore.RenameResult result = fontLibraryStore.renameFont(entry.id, name);
                if (result != FontLibraryStore.RenameResult.RENAMED) {
                    showToast(result == FontLibraryStore.RenameResult.DUPLICATE_NAME
                            ? R.string.font_library_name_duplicate : R.string.font_library_name_invalid);
                    return false;
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                TypefaceCatalogCache.invalidate(this);
                refreshDetails();
                return true;
                });
    }

    private void confirmDelete(FontLibraryEntry entry) {
        showFontConfirmation(R.string.font_library_delete_title,
                getString(R.string.font_library_delete_message, resolveFontTitle(entry)),
                R.string.font_library_delete_action,
                () -> handleDeleteResult(fontLibraryStore.deleteFont(entry.id, this::isFontReferenced)));
    }

    private void confirmForceDelete(FontLibraryEntry entry, List<FontReference> references) {
        showFontConfirmation(R.string.font_library_delete_title,
                getString(R.string.font_library_delete_in_use_message,
                        resolveFontTitle(entry), references.size()),
                R.string.font_library_delete_action,
                () -> handleDeleteResult(forceDeleteFont(entry, references)));
    }

    private void handleDeleteResult(FontLibraryStore.DeleteResult result) {
        if (result == FontLibraryStore.DeleteResult.DELETED) {
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
            TypefaceCatalogCache.invalidate(this);
            finish();
        } else {
            showToast(result == FontLibraryStore.DeleteResult.IN_USE
                    ? R.string.font_library_delete_in_use : R.string.font_library_delete_failed);
        }
    }

    private FontLibraryStore.DeleteResult forceDeleteFont(FontLibraryEntry entry, List<FontReference> references) {
        List<FontReference> cleared = new ArrayList<>();
        for (FontReference reference : references) {
            if (!configStore.clearTargetTypefaceId(reference.packageName)) {
                if (!restoreTypefaceReferences(cleared)) {
                    DpisLog.i("FONT_LIBRARY_AUDIT unable to restore all typeface references after "
                            + "force-delete setup failed");
                }
                return FontLibraryStore.DeleteResult.DELETE_FAILED;
            }
            cleared.add(reference);
        }
        FontLibraryStore.DeleteResult result = fontLibraryStore.deleteFont(entry.id, unused -> false);
        if (result != FontLibraryStore.DeleteResult.DELETED) {
            if (restoreTypefaceReferences(cleared)) {
                publishTypefaceReferences(cleared, true);
            } else {
                DpisLog.i("FONT_LIBRARY_AUDIT typeface reference rollback incomplete; "
                        + "runtime state was not restored");
            }
        } else {
            publishTypefaceReferences(cleared, false);
        }
        return result;
    }

    private void confirmClearAppTypeface(FontLibraryEntry entry, FontReference reference) {
        showFontConfirmation(R.string.font_library_restore_app_font_title,
                getString(R.string.font_library_restore_app_font_message, reference.label),
                R.string.font_library_restore_default_action, () -> {
                    if (!configStore.clearTargetTypefaceId(reference.packageName)) {
                        showToast(R.string.font_library_restore_app_font_failed);
                        return;
                    }
                    FontRuntimePropertySyncer.publishTypefaceTargetAsync(reference.packageName, null);
                    RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                    refreshDetails();
                });
    }

    private void retryPublishedFallbacks() {
        new Thread(() -> {
            FontLibraryStore.RepairResult result = fontLibraryStore.retryPublishedFallbacks();
            runOnUiThread(() -> {
                if (result.catalogUpdated) {
                    TypefaceCatalogCache.invalidate(this);
                }
                refreshDetails();
                showToast(result.catalogUpdated && result.publishedCollectionCount > 0
                        ? R.string.font_library_publication_retry_success
                        : R.string.font_library_publication_retry_failed);
            });
        }, "dpis-font-library-publish-retry").start();
    }

    private List<FontReference> findReferences(FontLibraryEntry selected) {
        List<FontReference> references = new ArrayList<>();
        List<String> collectionFaceIds = new ArrayList<>();
        for (FontLibraryEntry candidate : fontLibraryStore.listFonts()) {
            if (selected.collectionId.equals(candidate.collectionId)) collectionFaceIds.add(candidate.id);
        }
        for (String packageName : configStore.getConfiguredPackages()) {
            String selectedId = configStore.getTargetTypefaceId(packageName);
            if (collectionFaceIds.contains(selectedId)) {
                references.add(new FontReference(packageName, resolveAppLabel(packageName), selectedId));
            }
        }
        references.sort((left, right) -> left.label.compareToIgnoreCase(right.label));
        return references;
    }

    private boolean isFontReferenced(String id) {
        FontLibraryEntry entry = fontLibraryStore.findById(id);
        return entry != null && !findReferences(entry).isEmpty();
    }

    private boolean restoreTypefaceReferences(List<FontReference> references) {
        boolean restored = true;
        for (FontReference reference : references) {
            restored &= configStore.setTargetTypefaceId(reference.packageName, reference.typefaceId);
        }
        return restored;
    }

    private void publishTypefaceReferences(List<FontReference> references, boolean restoreOriginal) {
        for (FontReference reference : references) {
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(reference.packageName,
                    restoreOriginal ? reference.typefaceId : null);
        }
    }

    private String resolveAppLabel(String packageName) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            CharSequence label = getPackageManager().getApplicationLabel(info);
            if (label != null && label.length() > 0) return label.toString();
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            // Fall back to package name when the target is absent or hidden.
        }
        return packageName;
    }

    private void showFontConfirmation(int titleResId, String message, int confirmResId,
            Runnable onConfirm) {
        ComposeConfirmDialog.showWithLabels(this, getString(titleResId), message,
                getString(R.string.dialog_process_action_confirm_negative),
                getString(confirmResId), onConfirm, () -> {});
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void showToast(int id) { Toast.makeText(this, id, Toast.LENGTH_SHORT).show(); }

    private void bindDialogButtonHaptics(androidx.appcompat.app.AlertDialog dialog) {
        TouchFeedbackBinder.bindPressHaptic(dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE));
        TouchFeedbackBinder.bindPressHaptic(dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE));
    }

    private String resolveFontTitle(FontLibraryEntry entry) {
        return entry.collectionDisplayName;
    }

    private static String stripFontExtension(String value) {
        if (value == null || value.isBlank()) return "Imported font";
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        return lower.endsWith(".ttf") || lower.endsWith(".otf")
                ? trimmed.substring(0, trimmed.length() - 4) : trimmed;
    }

    private static final class FontReference {
        final String packageName;
        final String label;
        final String typefaceId;
        FontReference(String packageName, String label, String typefaceId) {
            this.packageName = packageName;
            this.label = label;
            this.typefaceId = typefaceId;
        }
    }
}
