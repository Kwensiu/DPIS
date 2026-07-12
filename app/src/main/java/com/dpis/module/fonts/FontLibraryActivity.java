package com.dpis.module.fonts;

import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisApplication;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.ui.DialogWindowSizer;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

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
    private static final String FONT_PREVIEW_PRIMARY_TEXT = "AaBbCc 你好世界 123";
    private static final String FONT_PREVIEW_SECONDARY_TEXT = "The quick brown fox jumps over the lazy dog";

    private LinearLayout listView;
    private MaterialTextView emptyView;
    private FontLibraryStore fontLibraryStore;
    private FontLibraryConfigStore configStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_library);
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
                this, DpisApplication.getXposedService());
        fontLibraryStore.purgeOrphanedFiles();
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
                this, DpisApplication.getXposedService());
        listView = findViewById(R.id.font_library_list);
        emptyView = findViewById(R.id.font_library_empty);
        AppCompatImageButton backButton = findViewById(R.id.font_library_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finish());
        AppCompatImageButton archiveMenuButton = findViewById(
                R.id.font_library_archive_menu_button);
        TouchFeedbackBinder.bindPressHaptic(archiveMenuButton);
        archiveMenuButton.setOnClickListener(this::showFontLibraryArchiveMenu);
        FloatingActionButton importFab = findViewById(R.id.font_library_import_fab);
        TouchFeedbackBinder.bindPressScaleAndHaptic(importFab);
        importFab.setOnClickListener(v -> openFontImportPicker());
        applyInsets();
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

    private void applyInsets() {
        View toolbar = findViewById(R.id.font_library_toolbar);
        final int baseTopPadding = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        View importFab = findViewById(R.id.font_library_import_fab);
        ViewCompat.setOnApplyWindowInsetsListener(importFab, (view, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.bottomMargin = getResources().getDimensionPixelSize(
                    R.dimen.floating_actions_search_margin_bottom) + navigationBars.bottom;
            view.setLayoutParams(params);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
        ViewCompat.requestApplyInsets(importFab);
    }

    private void refreshFontList() {
        List<FontLibraryEntry> entries = fontLibraryStore.listFonts();
        listView.removeAllViews();
        boolean empty = entries.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        ((ScrollView) listView.getParent()).setVisibility(empty ? View.GONE : View.VISIBLE);
        java.util.LinkedHashMap<String, List<FontLibraryEntry>> collections =
                new java.util.LinkedHashMap<>();
        for (FontLibraryEntry entry : entries) {
            collections.computeIfAbsent(entry.collectionId, unused -> new ArrayList<>()).add(entry);
        }
        for (List<FontLibraryEntry> faces : collections.values()) {
            listView.addView(createFontRow(faces.get(0), faces.size()));
        }
    }

    private void recoverMissingFontCatalogAsync() {
        new Thread(() -> {
            FontLibraryStore.RecoveryResult result = fontLibraryStore.recoverMissingCatalogEntries();
            if (!result.catalogUpdated || result.recoveredEntryCount == 0) {
                return;
            }
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
            runOnUiThread(this::refreshFontList);
        }, "dpis-font-library-catalog-recovery").start();
    }

    private View createFontRow(FontLibraryEntry entry, int faceCount) {
        boolean inUse = isCollectionInUse(entry);
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        card.setStrokeColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOutlineVariant));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(20));
        card.setClickable(true);
        card.setFocusable(true);
        TouchFeedbackBinder.bindPressHaptic(card);
        card.setOnClickListener(v -> startActivity(new Intent(this, FontDetailActivity.class)
                .putExtra(FontDetailActivity.EXTRA_FONT_ID, entry.id)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(content);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        content.addView(textGroup, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        textGroup.addView(titleRow);

        MaterialTextView title = new MaterialTextView(this);
        title.setText(faceCount > 1
                ? getString(R.string.font_library_collection_label, resolveFontTitle(entry), faceCount)
                : resolveFontTitle(entry));
        configureSingleLine(title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (inUse) {
            MaterialTextView badge = createUsedBadge();
            titleRow.addView(badge);
        }

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(resolveFontSubtitle(entry));
        configureSingleLine(subtitle);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        textGroup.addView(subtitle);

        File fontFile = fontLibraryStore.resolveFontFile(entry.id);
        if (fontFile != null) {
            textGroup.addView(createDivider(8));

            MaterialTextView preview = new MaterialTextView(this);
            preview.setText(FONT_PREVIEW_PRIMARY_TEXT);
            preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            Typeface previewTypeface = FontTypefaceLoader.load(fontFile, entry.ttcIndex);
            if (previewTypeface != null) {
                preview.setTypeface(previewTypeface);
            }
            configureSingleLine(preview);
            preview.setTextColor(MaterialColors.getColor(
                    listView, com.google.android.material.R.attr.colorOnSurface));
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            previewParams.topMargin = dp(8);
            textGroup.addView(preview, previewParams);
        }

        return card;
    }

    private MaterialTextView createUsedBadge() {
        MaterialTextView badge = new MaterialTextView(this);
        badge.setText(R.string.font_library_used_badge);
        badge.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
        badge.setTextColor(MaterialColors.getColor(
                listView, androidx.appcompat.R.attr.colorPrimary));
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(999));
        background.setColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorSecondaryContainer));
        badge.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(8);
        badge.setLayoutParams(params);
        return badge;
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
        if (configStore.isTtcFontImportEnabled()) {
            mimeTypes.add("font/collection");
            mimeTypes.add("font/ttc");
            mimeTypes.add("application/x-font-ttc");
        }
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
        TextInputLayout inputLayout = createNameInput(FontLibraryStore.normalizeDisplayName(sourceName));
        TextInputEditText input = (TextInputEditText) inputLayout.getEditText();
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_name_title)
                .setView(inputLayout)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.dialog_confirm_button, (unusedDialog, which) -> {
                    String displayName = input != null && input.getText() != null
                            ? input.getText().toString()
                            : "";
                    confirmLargeFontImport(uri, sourceName, mimeType, displayName);
                })
                .create();
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, this);
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

    private void showFontLibraryArchiveMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.font_library_export_archive_action);
        menu.getMenu().add(0, 2, 1, R.string.font_library_import_archive_action);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                openFontLibraryExportPicker();
                return true;
            }
            if (item.getItemId() == 2) {
                openFontLibraryImportPicker();
                return true;
            }
            return false;
        });
        menu.show();
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
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_large_import_title)
                .setMessage(getString(R.string.font_library_large_import_message, sizeMiB))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_large_import_continue,
                        (unusedDialog, which) -> importFont(uri, sourceName, mimeType, displayName))
                .create();
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private TextInputLayout createNameInput(String initialName) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(getString(R.string.font_library_name_hint));
        int padding = dp(20);
        inputLayout.setPadding(padding, 0, padding, 0);
        TextInputEditText input = new TextInputEditText(inputLayout.getContext());
        input.setSingleLine(true);
        input.setText(initialName);
        input.selectAll();
        inputLayout.addView(input);
        return inputLayout;
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
            try {
                tempFile = File.createTempFile(
                        "dpis-font-import-",
                        resolveFontTempExtension(sourceName, mimeType),
                        getCacheDir());
                copyUriToFile(uri, tempFile);
                FontFileInspector.Result inspection = FontFileInspector.inspect(tempFile);
                if (inspection.kind == FontFileKind.TTC) {
                    if (!configStore.isTtcFontImportEnabled()) {
                        throw new IOException("TTC import disabled");
                    }
                    File ttcTempFile = tempFile;
                    tempFile = null;
                    int faceCount = inspection.ttc.offsets.size();
                    List<TtcFaceOption> options = findLoadableTtcFaces(
                            ttcTempFile, sourceName, faceCount);
                    runOnUiThread(() -> showTtcFaceSelectionDialog(
                            ttcTempFile,
                            sourceName,
                            displayName,
                            options,
                            faceCount));
                    return;
                }
                if (!isSupportedSingleFontFile(tempFile, inspection.kind)) {
                    throw new IOException("Unable to parse font");
                }
                importedEntry = fontLibraryStore.registerCopiedFont(
                        tempFile,
                        sourceName,
                        displayName,
                        System.currentTimeMillis(),
                        inspection.kind);
            } catch (IOException | RuntimeException error) {
                importedEntry = null;
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
            FontLibraryEntry finalImportedEntry = importedEntry;
            runOnUiThread(() -> {
                if (finalImportedEntry == null) {
                    showToast(R.string.font_library_import_failed);
                    return;
                }
                showToast(R.string.font_library_import_success, finalImportedEntry.displayName);
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                refreshFontList();
            });
        }, "dpis-font-import").start();
    }

    private void showTtcFaceSelectionDialog(
            File tempFile,
            String sourceName,
            String displayName,
            List<TtcFaceOption> options,
            int faceCount) {
        int failedCount = Math.max(0, faceCount - options.size());
        if (options.isEmpty()) {
            tempFile.delete();
            showToast(R.string.font_library_import_failed);
            return;
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(16), padding, 0);

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(sourceName);
        configureSingleLine(subtitle);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        content.addView(subtitle);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        MaterialTextView selectedBadge = createTtcStatusBadge(
                getString(R.string.font_library_ttc_selected_count, 0));
        MaterialTextView failedBadge = createTtcStatusBadge(
                getString(R.string.font_library_ttc_failed_faces, failedCount));
        statusRow.addView(selectedBadge);
        statusRow.addView(failedBadge);
        content.addView(statusRow, topMarginParams(10));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        MaterialTextView selectAll = createTtcTextAction(R.string.font_library_ttc_select_all);
        MaterialTextView deselectAll = createTtcTextAction(R.string.font_library_ttc_deselect_all);
        actions.addView(selectAll);
        actions.addView(deselectAll);
        content.addView(actions, topMarginParams(8));

        boolean[] checked = new boolean[options.size()];
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        content.addView(rows, topMarginParams(8));
        List<MaterialTextView> rowViews = new ArrayList<>();
        for (TtcFaceOption option : options) {
            MaterialTextView row = createTtcFaceRow(option.label, false);
            rowViews.add(row);
            rows.addView(row, topMarginParams(6));
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_ttc_select_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_import_action, null)
                .create();
        dialog.setOnCancelListener(unused -> tempFile.delete());
        dialog.setOnDismissListener(unused -> {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        });
        dialog.setOnShowListener(unused -> {
            bindDialogButtonHaptics(dialog);
            android.widget.Button importButton =
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            importButton.setEnabled(false);
            selectAll.setOnClickListener(v -> {
                setAllTtcFaceRows(options, checked, rowViews, true);
                selectedBadge.setText(getString(
                        R.string.font_library_ttc_selected_count,
                        countSelected(checked)));
                importButton.setEnabled(true);
            });
            deselectAll.setOnClickListener(v -> {
                setAllTtcFaceRows(options, checked, rowViews, false);
                selectedBadge.setText(getString(R.string.font_library_ttc_selected_count, 0));
                importButton.setEnabled(false);
            });
            for (int i = 0; i < rowViews.size(); i++) {
                int rowIndex = i;
                MaterialTextView row = rowViews.get(i);
                row.setOnClickListener(v -> {
                    checked[rowIndex] = !checked[rowIndex];
                    updateTtcFaceRow(row, options.get(rowIndex).label, checked[rowIndex]);
                    int selected = countSelected(checked);
                    selectedBadge.setText(getString(
                            R.string.font_library_ttc_selected_count,
                            selected));
                    importButton.setEnabled(selected > 0);
                });
            }
            importButton.setOnClickListener(v -> importSelectedTtcFaces(
                    dialog, tempFile, sourceName, displayName, options, checked));
        });
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, this);
    }

    private void importSelectedTtcFaces(
            androidx.appcompat.app.AlertDialog dialog,
            File tempFile,
            String sourceName,
            String displayName,
            List<TtcFaceOption> options,
            boolean[] checked) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < checked.length; i++) {
            if (checked[i]) {
                indexes.add(options.get(i).index);
            }
        }
        if (indexes.isEmpty()) {
            return;
        }
        new Thread(() -> {
            List<FontLibraryEntry> imported = List.of();
            try {
                imported = fontLibraryStore.registerCopiedFontFaces(
                        tempFile,
                        sourceName,
                        displayName,
                        FontFileKind.TTC,
                        indexes,
                        System.currentTimeMillis());
            } catch (IOException | RuntimeException ignored) {
                imported = List.of();
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            List<FontLibraryEntry> finalImported = imported;
            runOnUiThread(() -> {
                if (finalImported.isEmpty()) {
                    showToast(R.string.font_library_import_failed);
                    return;
                }
                dialog.dismiss();
                showToast(R.string.font_library_import_count_success, finalImported.size());
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                refreshFontList();
            });
        }, "dpis-ttc-font-import").start();
    }

    private List<TtcFaceOption> findLoadableTtcFaces(File file, String sourceName, int faceCount) {
        List<TtcFaceOption> result = new ArrayList<>();
        for (int index = 0; index < faceCount; index++) {
            if (FontTypefaceLoader.load(file, index) != null) {
                result.add(new TtcFaceOption(index, sourceName + " (TTC " + index + ")"));
            }
        }
        return result;
    }

    private MaterialTextView createTtcStatusBadge(String text) {
        MaterialTextView badge = new MaterialTextView(this);
        badge.setText(text);
        badge.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
        badge.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSecondaryContainer));
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(999));
        background.setColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorSecondaryContainer));
        badge.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(8);
        badge.setLayoutParams(params);
        return badge;
    }

    private MaterialTextView createTtcTextAction(int textResId) {
        MaterialTextView action = new MaterialTextView(this);
        action.setText(textResId);
        action.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        action.setTextColor(MaterialColors.getColor(
                listView, androidx.appcompat.R.attr.colorPrimary));
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(10), dp(6), dp(10), dp(6));
        action.setClickable(true);
        action.setFocusable(true);
        action.setBackgroundResource(resolveSelectableItemBackground());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(8);
        action.setLayoutParams(params);
        return action;
    }

    private MaterialTextView createTtcFaceRow(String label, boolean selected) {
        MaterialTextView row = new MaterialTextView(this);
        row.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(dp(48));
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setClickable(true);
        row.setFocusable(true);
        updateTtcFaceRow(row, label, selected);
        return row;
    }

    private void updateTtcFaceRow(MaterialTextView row, String label, boolean selected) {
        row.setText(selected ? label + "  ✓" : label);
        row.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(14));
        background.setColor(MaterialColors.getColor(
                listView,
                selected
                        ? com.google.android.material.R.attr.colorSecondaryContainer
                        : com.google.android.material.R.attr.colorSurfaceContainerHigh));

        background.setStroke(dp(1), MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOutlineVariant));
        row.setBackground(background);
    }

    private void setAllTtcFaceRows(
            List<TtcFaceOption> options,
            boolean[] checked,
            List<MaterialTextView> rows,
            boolean selected) {
        for (int i = 0; i < checked.length; i++) {
            checked[i] = selected;
            updateTtcFaceRow(rows.get(i), options.get(i).label, selected);
        }
    }

    private int countSelected(boolean[] checked) {
        int count = 0;
        for (boolean selected : checked) {
            if (selected) {
                count++;
            }
        }
        return count;
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
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_publication_repair_title)
                .setMessage(getString(R.string.font_library_publication_repair_message,
                        report.missingPublishedFallbackCount))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_publication_retry_action,
                        (unused, which) -> retryPublishedFallbacks())
                .create();
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void retryPublishedFallbacks() {
        new Thread(() -> {
            FontLibraryStore.RepairResult result = fontLibraryStore.retryPublishedFallbacks();
            runOnUiThread(() -> {
                refreshFontList();
                showToast(result.catalogUpdated && result.publishedCollectionCount > 0
                        ? R.string.font_library_publication_retry_success
                        : R.string.font_library_publication_retry_failed);
            });
        }, "dpis-font-library-publish-retry").start();
    }

    private View createDivider(int topMarginDp) {
        View divider = new View(this);
        divider.setBackgroundColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOutlineVariant));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        params.topMargin = dp(topMarginDp);
        divider.setLayoutParams(params);
        return divider;
    }

    private void configureSingleLine(MaterialTextView textView) {
        textView.setSingleLine(true);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }

    private LinearLayout.LayoutParams topMarginParams(int marginTopDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(marginTopDp);
        return params;
    }

    private int resolveSelectableItemBackground() {
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return outValue.resourceId;
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
        String source = entry.sourceFileName != null ? entry.sourceFileName : "";
        int publicationLabel = switch (entry.publicationStatus) {
            case PUBLISHED -> R.string.font_library_publication_published;
            case PUBLISH_FAILED -> R.string.font_library_publication_fallback_failed;
            case PRIVATE -> R.string.font_library_publication_private;
        };
        return source + " · " + getString(publicationLabel);
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
        if (configStore.isTtcFontImportEnabled() && lowerName.endsWith(".ttc")) {
            return true;
        }
        return "font/ttf".equals(mimeType)
                || "font/otf".equals(mimeType)
                || "application/x-font-ttf".equals(mimeType)
                || "application/vnd.ms-opentype".equals(mimeType)
                || (configStore.isTtcFontImportEnabled()
                && ("font/ttc".equals(mimeType)
                || "font/collection".equals(mimeType)
                || "application/x-font-ttc".equals(mimeType)));
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

    private static final class TtcFaceOption {
        final int index;
        final String label;

        TtcFaceOption(int index, String label) {
            this.index = index;
            this.label = label;
        }
    }
}
