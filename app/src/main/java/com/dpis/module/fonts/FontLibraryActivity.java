package com.dpis.module.fonts;

import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisApplication;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;
import com.dpis.module.ui.compose.FontLibraryPresentation;
import com.dpis.module.ui.compose.FontLibraryUiItem;
import com.dpis.module.ui.dialog.ConfirmDialog;
import com.dpis.module.ui.compose.ComposeTextInputDialog;
import com.dpis.module.ui.compose.SupportActivityContent;

import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.ui.DialogWindowSizer;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FontLibraryActivity extends LocalizedActivity {
    private static final int REQUEST_IMPORT_FONT = 2001;
    private static final int REQUEST_EXPORT_FONT_LIBRARY = 2002;
    private static final int REQUEST_IMPORT_FONT_LIBRARY = 2003;
    private static final long LARGE_FONT_WARNING_BYTES = 256L * 1024L * 1024L;
    private static final long IMPORT_FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L;
    private FontLibraryStore fontLibraryStore;
    private FontLibraryConfigStore configStore;
    private FontLibraryPresentation presentation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
                this, DpisApplication.getXposedService());
        fontLibraryStore.purgeOrphanedFiles();
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
                this, DpisApplication.getXposedService());
        presentation = new FontLibraryPresentation();
        SupportActivityContent.installFontLibrary(
                this,
                presentation,
                this::openFontImportPicker,
                this::openFontLibraryExportPicker,
                this::openFontLibraryImportPicker,
                this::openFontDetails);
        refreshFontList();
        recoverMissingFontCatalogAsync();
        runFontHealthScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFontList();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_IMPORT_FONT) {
            promptImportName(uri);
        } else if (requestCode == REQUEST_EXPORT_FONT_LIBRARY) {
            exportFontLibrary(uri);
        } else if (requestCode == REQUEST_IMPORT_FONT_LIBRARY) {
            importFontLibrary(uri);
        }
    }

    private void refreshFontList() {
        List<FontLibraryEntry> entries = fontLibraryStore.listFonts();
        java.util.LinkedHashMap<String, List<FontLibraryEntry>> collections =
                new java.util.LinkedHashMap<>();
        for (FontLibraryEntry entry : entries) {
            collections.computeIfAbsent(entry.collectionId, unused -> new ArrayList<>()).add(entry);
        }
        List<FontLibraryUiItem> items = new ArrayList<>();
        for (List<FontLibraryEntry> faces : collections.values()) {
            FontLibraryEntry entry = faces.get(0);
            File fontFile = fontLibraryStore.resolveFontFile(entry.id);
            items.add(new FontLibraryUiItem(
                    entry.id,
                    faces.size() > 1
                            ? getString(R.string.font_library_collection_label,
                                    entry.collectionDisplayName, faces.size())
                            : entry.collectionDisplayName,
                    resolveFontSubtitle(entry),
                    isCollectionInUse(entry),
                    fontFile != null ? FontTypefaceLoader.load(fontFile, entry.ttcIndex) : null));
        }
        presentation.show(items);
    }

    private void openFontDetails(String fontId) {
        startActivity(new Intent(this, FontDetailActivity.class)
                .putExtra(FontDetailActivity.EXTRA_FONT_ID, fontId));
    }

    private void recoverMissingFontCatalogAsync() {
        new Thread(() -> {
            FontLibraryStore.RecoveryResult result = fontLibraryStore.recoverMissingCatalogEntries();
            if (!result.catalogUpdated || result.recoveredEntryCount == 0) {
                return;
            }
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
            runOnUiThread(() -> {
                TypefaceCatalogCache.invalidate(this);
                refreshFontList();
            });
        }, "dpis-font-library-catalog-recovery").start();
    }

    private void bindDialogButtonHaptics(androidx.appcompat.app.AlertDialog dialog) {
        TouchFeedbackBinder.bindPressHaptic(
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE));
        TouchFeedbackBinder.bindPressHaptic(
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL));
        TouchFeedbackBinder.bindPressHaptic(
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE));
    }

    @SuppressWarnings("deprecation")
    private void openFontImportPicker() {
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add("font/ttf");
        mimeTypes.add("font/otf");
        mimeTypes.add("application/x-font-ttf");
        mimeTypes.add("application/vnd.ms-opentype");
        mimeTypes.add("font/collection");
        mimeTypes.add("font/ttc");
        mimeTypes.add("application/x-font-ttc");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
        try {
            startActivityForResult(intent, REQUEST_IMPORT_FONT);
        } catch (ActivityNotFoundException error) {
            showToast(R.string.font_library_picker_failed);
        }
    }

    private void promptImportName(Uri uri) {
        String sourceName;
        String mimeType;
        try {
            sourceName = resolveDisplayName(uri);
            mimeType = getContentResolver().getType(uri);
        } catch (RuntimeException error) {
            showToast(R.string.font_library_import_failed);
            return;
        }
        if (!isPotentialFontInput(sourceName, mimeType)) {
            showToast(R.string.font_library_import_failed);
            return;
        }
        ComposeTextInputDialog.showLarge(this,
                getString(R.string.font_library_name_title),
                getString(R.string.font_library_name_hint),
                FontLibraryStore.normalizeDisplayName(sourceName),
                displayName -> {
                    confirmLargeFontImport(uri, sourceName, mimeType, displayName);
                    return true;
                });
    }

    @SuppressWarnings("deprecation")
    private void openFontLibraryExportPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(FontLibraryArchiveCodec.MIME_TYPE)
                .putExtra(Intent.EXTRA_TITLE, getString(R.string.font_library_archive_file_name));
        try {
            startActivityForResult(intent, REQUEST_EXPORT_FONT_LIBRARY);
        } catch (ActivityNotFoundException error) {
            showToast(R.string.font_library_archive_export_failed);
        }
    }

    @SuppressWarnings("deprecation")
    private void openFontLibraryImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(FontLibraryArchiveCodec.MIME_TYPE);
        try {
            startActivityForResult(intent, REQUEST_IMPORT_FONT_LIBRARY);
        } catch (ActivityNotFoundException error) {
            showToast(R.string.font_library_archive_import_failed);
        }
    }

    private void exportFontLibrary(Uri uri) {
        new Thread(() -> {
            FontLibraryArchiveCodec.ExportResult result = null;
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output != null) {
                    result = FontLibraryArchiveCodec.writeArchive(output, fontLibraryStore);
                }
            } catch (IOException | RuntimeException ignored) {
                result = null;
            }
            FontLibraryArchiveCodec.ExportResult finalResult = result;
            runOnUiThread(() -> {
                if (finalResult == null) {
                    showToast(R.string.font_library_archive_export_failed);
                } else if (finalResult.skippedCollectionCount > 0) {
                    showToast(R.string.font_library_archive_export_partial,
                            finalResult.collectionCount, finalResult.skippedCollectionCount);
                } else {
                    showToast(R.string.font_library_archive_export_success);
                }
            });
        }, "dpis-font-library-export").start();
    }

    private void importFontLibrary(Uri uri) {
        new Thread(() -> {
            FontLibraryArchiveCodec.RestoreResult result = null;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input != null) {
                    result = FontLibraryArchiveCodec.restoreArchive(input, fontLibraryStore,
                            getCacheDir());
                }
            } catch (IOException | RuntimeException ignored) {
                result = null;
            }
            FontLibraryArchiveCodec.RestoreResult finalResult = result;
            runOnUiThread(() -> {
                if (finalResult == null) {
                    showToast(R.string.font_library_archive_import_failed);
                    return;
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                TypefaceCatalogCache.invalidate(this);
                refreshFontList();
                if (finalResult.failureCount > 0) {
                    showToast(R.string.font_library_archive_import_partial,
                            finalResult.collectionCount, finalResult.failureCount);
                } else {
                    showToast(R.string.font_library_archive_import_success,
                            finalResult.collectionCount);
                }
            });
        }, "dpis-font-library-import").start();
    }

    private void confirmLargeFontImport(Uri uri, String sourceName, String mimeType,
            String displayName) {
        long sizeBytes = resolveDocumentSize(uri);
        if (!hasImportSpace(sizeBytes)) {
            showToast(R.string.font_library_import_insufficient_space);
            return;
        }
        if (sizeBytes < LARGE_FONT_WARNING_BYTES) {
            importFont(uri, sourceName, mimeType, displayName);
            return;
        }
        long sizeMiB = (sizeBytes + 1024L * 1024L - 1L) / (1024L * 1024L);
        ConfirmDialog.showWithLabels(this,
                getString(R.string.font_library_large_import_title),
                getString(R.string.font_library_large_import_message, sizeMiB),
                getString(R.string.dialog_process_action_confirm_negative),
                getString(R.string.font_library_large_import_continue),
                () -> importFont(uri, sourceName, mimeType, displayName), () -> {});
    }

    private long resolveDocumentSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[] { OpenableColumns.SIZE }, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return -1L;
            }
            int column = cursor.getColumnIndex(OpenableColumns.SIZE);
            return column >= 0 && !cursor.isNull(column) ? cursor.getLong(column) : -1L;
        } catch (RuntimeException ignored) {
            return -1L;
        }
    }

    private boolean hasImportSpace(long sourceSizeBytes) {
        if (sourceSizeBytes < 0L) {
            // Providers do not always expose SIZE. Preserve the existing import path in that case.
            return true;
        }
        long required = sourceSizeBytes > (Long.MAX_VALUE - IMPORT_FREE_SPACE_MARGIN_BYTES) / 3L
                ? Long.MAX_VALUE
                : sourceSizeBytes * 3L + IMPORT_FREE_SPACE_MARGIN_BYTES;
        return getCacheDir().getUsableSpace() >= required
                && getFilesDir().getUsableSpace() >= required;
    }

    private void importFont(Uri uri, String sourceName, String mimeType, String displayName) {
        new Thread(() -> {
            File tempFile = null;
            FontLibraryEntry importedEntry = null;
            int importedFaceCount = 0;
            try {
                tempFile = File.createTempFile(
                        "dpis-font-import-",
                        resolveFontTempExtension(sourceName, mimeType),
                        getCacheDir());
                copyUriToFile(uri, tempFile);
                FontFileInspector.Result inspection = FontFileInspector.inspect(tempFile);
                if (inspection.kind == FontFileKind.TTC) {
                    List<Integer> loadableIndexes = findLoadableTtcFaceIndexes(
                            tempFile, inspection.ttc.offsets.size());
                    List<FontLibraryEntry> importedFaces = fontLibraryStore.registerCopiedFontFaces(
                            tempFile,
                            sourceName,
                            displayName,
                            FontFileKind.TTC,
                            loadableIndexes,
                            System.currentTimeMillis());
                    if (importedFaces.isEmpty()) {
                        throw new IOException("No TTC face could be loaded");
                    }
                    importedFaceCount = importedFaces.size();
                } else if (!isSupportedSingleFontFile(tempFile, inspection.kind)) {
                    throw new IOException("Unable to parse font");
                } else {
                    importedEntry = fontLibraryStore.registerCopiedFont(
                            tempFile,
                            sourceName,
                            displayName,
                            System.currentTimeMillis(),
                            inspection.kind);
                }
            } catch (IOException | RuntimeException error) {
                importedEntry = null;
                importedFaceCount = 0;
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
            FontLibraryEntry finalImportedEntry = importedEntry;
            int finalImportedFaceCount = importedFaceCount;
            runOnUiThread(() -> {
                if (finalImportedEntry == null && finalImportedFaceCount == 0) {
                    showToast(R.string.font_library_import_failed);
                    return;
                }
                if (finalImportedFaceCount > 0) {
                    showToast(R.string.font_library_import_count_success, finalImportedFaceCount);
                } else {
                    showToast(R.string.font_library_import_success, finalImportedEntry.displayName);
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                TypefaceCatalogCache.invalidate(this);
                refreshFontList();
            });
        }, "dpis-font-import").start();
    }

    private List<Integer> findLoadableTtcFaceIndexes(File file, int faceCount) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < faceCount; index++) {
            if (FontTypefaceLoader.load(file, index) != null) {
                result.add(index);
            }
        }
        return result;
    }

    private void copyUriToFile(Uri uri, File targetFile) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri);
                OutputStream output = new FileOutputStream(targetFile)) {
            if (input == null) {
                throw new IOException("Unable to open font input stream");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    /** Health checks are read-only; root publication remains an explicit user decision. */
    private void runFontHealthScan() {
        new Thread(() -> {
            FontLibraryStore.HealthReport report = fontLibraryStore.inspectHealth();
            if (report.missingPublishedFallbackCount <= 0 || isFinishing()) {
                return;
            }
            runOnUiThread(() -> showPublishedFallbackRepairPrompt(report));
        }, "dpis-font-library-health").start();
    }

    private void showPublishedFallbackRepairPrompt(FontLibraryStore.HealthReport report) {
        ConfirmDialog.showWithLabels(this,
                getString(R.string.font_library_publication_repair_title),
                getString(R.string.font_library_publication_repair_message,
                        report.missingPublishedFallbackCount),
                getString(R.string.dialog_process_action_confirm_negative),
                getString(R.string.font_library_publication_retry_action),
                this::retryPublishedFallbacks, () -> {});
    }

    private void retryPublishedFallbacks() {
        new Thread(() -> {
            FontLibraryStore.RepairResult result = fontLibraryStore.retryPublishedFallbacks();
            runOnUiThread(() -> {
                if (result.catalogUpdated) {
                    TypefaceCatalogCache.invalidate(this);
                }
                refreshFontList();
                showToast(result.catalogUpdated && result.publishedCollectionCount > 0
                        ? R.string.font_library_publication_retry_success
                        : R.string.font_library_publication_retry_failed);
            });
        }, "dpis-font-library-publish-retry").start();
    }

    private String resolveFontTitle(FontLibraryEntry entry) {
        String source = entry.sourceFileName != null ? entry.sourceFileName : "";
        String display = entry.displayName != null ? entry.displayName.trim() : "";
        if (!display.isEmpty() && !display.equals(source.trim())) {
            return display;
        }
        return stripFontExtension(source);
    }

    private boolean isCollectionInUse(FontLibraryEntry selected) {
        for (String packageName : configStore.getConfiguredPackages()) {
            String selectedTypefaceId = configStore.getTargetTypefaceId(packageName);
            FontLibraryEntry configured = fontLibraryStore.findById(selectedTypefaceId);
            if (configured != null && selected.collectionId.equals(configured.collectionId)) {
                return true;
            }
        }
        return false;
    }

    private String resolveFontSubtitle(FontLibraryEntry entry) {
        return entry.sourceFileName != null ? entry.sourceFileName : "";
    }

    private static String stripFontExtension(String sourceFileName) {
        if (sourceFileName == null || sourceFileName.isBlank()) {
            return "Imported font";
        }
        String trimmed = sourceFileName.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
            return trimmed.substring(0, trimmed.length() - 4);
        }
        return trimmed;
    }

    private String resolveDisplayName(Uri uri) {
        String displayName = null;
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[] { OpenableColumns.DISPLAY_NAME },
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    displayName = cursor.getString(index);
                }
            }
        } catch (RuntimeException ignored) {
            displayName = null;
        }
        if (displayName == null || displayName.isBlank()) {
            String path = uri.getLastPathSegment();
            displayName = path == null || path.isBlank() ? "Imported font" : path;
        }
        return displayName;
    }

    private boolean isPotentialFontInput(String displayName, String mimeType) {
        String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.US);
        if (lowerName.endsWith(".ttf") || lowerName.endsWith(".otf")) {
            return true;
        }
        if (lowerName.endsWith(".ttc")) {
            return true;
        }
        return "font/ttf".equals(mimeType)
                || "font/otf".equals(mimeType)
                || "application/x-font-ttf".equals(mimeType)
                || "application/vnd.ms-opentype".equals(mimeType)
                || ("font/ttc".equals(mimeType)
                || "font/collection".equals(mimeType)
                || "application/x-font-ttc".equals(mimeType));
    }

    private static boolean isSupportedSingleFontFile(File file, FontFileKind kind) {
        if (file == null
                || !file.isFile()
                || kind == FontFileKind.TTC
                || kind == FontFileKind.UNSUPPORTED) {
            return false;
        }
        return FontTypefaceLoader.load(file, 0) != null;
    }

    private static String resolveFontTempExtension(String displayName, String mimeType) {
        String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.US);
        if (lowerName.endsWith(".ttc") || "font/ttc".equals(mimeType)
                || "font/collection".equals(mimeType)
                || "application/x-font-ttc".equals(mimeType)) {
            return ".ttc";
        }
        if (lowerName.endsWith(".otf") || "font/otf".equals(mimeType)
                || "application/vnd.ms-opentype".equals(mimeType)) {
            return ".otf";
        }
        return ".ttf";
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showToast(int messageResId, Object... args) {
        Toast.makeText(this, getString(messageResId, args), Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
