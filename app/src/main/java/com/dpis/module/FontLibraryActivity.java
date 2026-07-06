package com.dpis.module;

import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.fonts.FontLibraryEntry;
import com.dpis.module.fonts.FontLibraryStore;

import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.ui.DialogWindowSizer;

import com.dpis.module.fonts.FontTypefaceLoader;
import com.dpis.module.ui.MaxHeightNestedScrollView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dpis.module.fonts.FontFileInspector;
import com.dpis.module.fonts.FontFileKind;
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
    private static final String FONT_PREVIEW_PRIMARY_TEXT = "AaBbCc 你好世界 123";
    private static final String FONT_PREVIEW_SECONDARY_TEXT = "The quick brown fox jumps over the lazy dog";
    private static final float FONT_DETAIL_DIALOG_MAX_HEIGHT_FRACTION = 0.72f;

    private LinearLayout listView;
    private MaterialTextView emptyView;
    private FontLibraryStore fontLibraryStore;
    private DpisConfigStore configStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_library);
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
                this, DpisApplication.getXposedService());
        fontLibraryStore.purgeOrphanedFiles();
        configStore = ConfigStoreFactory.createLocalUiModuleConfigStore(
                this, DpisApplication.getXposedService());
        listView = findViewById(R.id.font_library_list);
        emptyView = findViewById(R.id.font_library_empty);
        AppCompatImageButton backButton = findViewById(R.id.font_library_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finish());
        FloatingActionButton importFab = findViewById(R.id.font_library_import_fab);
        TouchFeedbackBinder.bindPressScaleAndHaptic(importFab);
        importFab.setOnClickListener(v -> openFontImportPicker());
        applyInsets();
        refreshFontList();
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
        if (requestCode != REQUEST_IMPORT_FONT
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }
        promptImportName(data.getData());
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
        for (FontLibraryEntry entry : entries) {
            listView.addView(createFontRow(entry));
        }
    }

    private View createFontRow(FontLibraryEntry entry) {
        List<FontReference> references = findReferences(entry.id);
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
        card.setOnClickListener(v -> showFontDetails(entry));
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
        title.setText(resolveFontTitle(entry));
        configureSingleLine(title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!references.isEmpty()) {
            MaterialTextView badge = createUsedBadge();
            titleRow.addView(badge);
        }

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(entry.sourceFileName);
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

    private void showFontDetails(FontLibraryEntry entry) {
        List<FontReference> references = findReferences(entry.id);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, dp(20), padding, 0);
        MaxHeightNestedScrollView scrollView = new MaxHeightNestedScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setMaxHeightFraction(FONT_DETAIL_DIALOG_MAX_HEIGHT_FRACTION);

        content.addView(createFontDetailHeader(entry, !references.isEmpty()));

        File detailFontFile = fontLibraryStore.resolveFontFile(entry.id);
        if (detailFontFile != null) {
            content.addView(createDivider(18));
            content.addView(createFontPreview(detailFontFile, entry.ttcIndex), topMarginParams(14));
        }

        content.addView(createDivider(18));
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        content.addView(createReferenceSection(entry, references, dialogHolder), topMarginParams(14));
        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        dialogHolder[0] = new MaterialAlertDialogBuilder(this)
                .setView(scrollView)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setNeutralButton(R.string.font_library_rename_action, null)
                .setPositiveButton(R.string.font_library_delete_action, null)
                .create();
        androidx.appcompat.app.AlertDialog dialog = dialogHolder[0];
        dialog.setOnShowListener(d -> {
            bindDialogButtonHaptics(dialog);
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(v -> promptRename(entry, dialog));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        if (!references.isEmpty()) {
                            confirmForceDelete(entry, references, dialog);
                            return;
                        }
                        confirmDelete(entry, dialog);
                    });
        });
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, this);
    }

    private View createFontDetailHeader(FontLibraryEntry entry, boolean inUse) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        header.setBaselineAligned(false);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);

        MaterialTextView title = new MaterialTextView(this);
        title.setText(resolveFontTitle(entry));
        configureSingleLine(title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textGroup.addView(title);

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(entry.sourceFileName);
        configureSingleLine(subtitle);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        textGroup.addView(subtitle);

        header.addView(textGroup, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (inUse) {
            header.addView(createUsedBadge());
        }
        return header;
    }

    private View createFontPreview(File fontFile, int ttcIndex) {
        LinearLayout previewGroup = new LinearLayout(this);
        previewGroup.setOrientation(LinearLayout.VERTICAL);

        Typeface previewTypeface = FontTypefaceLoader.load(fontFile, ttcIndex);

        MaterialTextView primary = new MaterialTextView(this);
        primary.setText(FONT_PREVIEW_PRIMARY_TEXT);
        configureSingleLine(primary);
        primary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        primary.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurface));
        if (previewTypeface != null) {
            primary.setTypeface(previewTypeface);
        }
        previewGroup.addView(primary);

        MaterialTextView secondary = new MaterialTextView(this);
        secondary.setText(FONT_PREVIEW_SECONDARY_TEXT);
        configureSingleLine(secondary);
        secondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        secondary.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        if (previewTypeface != null) {
            secondary.setTypeface(previewTypeface);
        }
        previewGroup.addView(secondary, topMarginParams(8));
        return previewGroup;
    }

    private View createReferenceSection(FontLibraryEntry entry,
            List<FontReference> references,
            androidx.appcompat.app.AlertDialog[] dialogHolder) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);

        MaterialTextView title = new MaterialTextView(this);
        title.setText(getString(R.string.font_library_used_by_title) + " · " + references.size());
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        section.addView(title);

        if (references.isEmpty()) {
            MaterialTextView empty = new MaterialTextView(this);
            empty.setText(R.string.font_library_unused);
            empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            empty.setTextColor(MaterialColors.getColor(
                    listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
            section.addView(empty, topMarginParams(8));
            return section;
        }

        for (FontReference reference : references) {
            section.addView(createReferenceRow(entry, reference, dialogHolder), topMarginParams(8));
        }
        return section;
    }

    private View createReferenceRow(FontLibraryEntry entry,
            FontReference reference,
            androidx.appcompat.app.AlertDialog[] dialogHolder) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackgroundResource(resolveSelectableItemBackground());
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        TouchFeedbackBinder.bindPressHaptic(row);
        row.setOnClickListener(v -> confirmClearAppTypeface(entry, reference, dialogHolder[0]));

        MaterialTextView label = new MaterialTextView(this);
        label.setText(reference.label);
        configureSingleLine(label);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(label);

        MaterialTextView packageName = new MaterialTextView(this);
        packageName.setText(reference.packageName);
        configureSingleLine(packageName);
        packageName.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        packageName.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        row.addView(packageName, topMarginParams(2));
        return row;
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

    private int resolveSelectableItemBackground() {
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return outValue.resourceId;
    }

    private LinearLayout.LayoutParams topMarginParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private void bindDialogButtonHaptics(androidx.appcompat.app.AlertDialog dialog) {
        TouchFeedbackBinder.bindPressHaptic(
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE));
        TouchFeedbackBinder.bindPressHaptic(
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL));
        TouchFeedbackBinder.bindPressHaptic(
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE));
    }

    private void promptRename(FontLibraryEntry entry, androidx.appcompat.app.AlertDialog parentDialog) {
        TextInputLayout inputLayout = createNameInput(entry.displayName);
        TextInputEditText input = (TextInputEditText) inputLayout.getEditText();
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_name_title)
                .setView(inputLayout)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.dialog_confirm_button, null)
                .create();
        dialog.setOnShowListener(d -> {
            bindDialogButtonHaptics(dialog);
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String displayName = input != null && input.getText() != null
                            ? input.getText().toString()
                            : "";
                    FontLibraryStore.RenameResult result = fontLibraryStore.renameFont(entry.id, displayName);
                    if (result != FontLibraryStore.RenameResult.RENAMED) {
                        handleRenameResult(result);
                        return;
                    }
                    RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                    parentDialog.dismiss();
                    dialog.dismiss();
                    refreshFontList();
                });
        });
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void confirmDelete(FontLibraryEntry entry, androidx.appcompat.app.AlertDialog parentDialog) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_delete_title)
                .setMessage(getString(R.string.font_library_delete_message, resolveFontTitle(entry)))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action, (unusedDialog, which) -> {
                    FontLibraryStore.DeleteResult result =
                            fontLibraryStore.deleteFont(entry.id, this::isFontReferenced);
                    handleDeleteResult(result);
                    if (result == FontLibraryStore.DeleteResult.DELETED) {
                        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                        parentDialog.dismiss();
                        refreshFontList();
                    }
                })
                .create();
        dialog.setOnShowListener(d -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void confirmForceDelete(FontLibraryEntry entry,
            List<FontReference> references,
            androidx.appcompat.app.AlertDialog parentDialog) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_delete_title)
                .setMessage(getString(
                        R.string.font_library_delete_in_use_message,
                        resolveFontTitle(entry),
                        references.size()))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action, (unusedDialog, which) -> {
                    FontLibraryStore.DeleteResult result = forceDeleteFont(entry, references);
                    handleDeleteResult(result);
                    if (result == FontLibraryStore.DeleteResult.DELETED) {
                        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                        parentDialog.dismiss();
                        refreshFontList();
                    }
                })
                .create();
        dialog.setOnShowListener(d -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private FontLibraryStore.DeleteResult forceDeleteFont(FontLibraryEntry entry, List<FontReference> references) {
        List<String> clearedPackages = new ArrayList<>();
        for (FontReference reference : references) {
            if (!configStore.clearTargetTypefaceId(reference.packageName)) {
                restoreTypefaceReferences(clearedPackages, entry.id);
                return FontLibraryStore.DeleteResult.DELETE_FAILED;
            }
            clearedPackages.add(reference.packageName);
        }

        FontLibraryStore.DeleteResult result = fontLibraryStore.deleteFont(entry.id, unused -> false);
        if (result != FontLibraryStore.DeleteResult.DELETED) {
            restoreTypefaceReferences(clearedPackages, entry.id);
            publishTypefaceReferences(clearedPackages, entry.id);
        } else {
            publishTypefaceReferences(clearedPackages, null);
        }
        return result;
    }

    private void restoreTypefaceReferences(List<String> packageNames, String typefaceId) {
        for (String packageName : packageNames) {
            configStore.setTargetTypefaceId(packageName, typefaceId);
        }
    }

    private void publishTypefaceReferences(List<String> packageNames, String typefaceId) {
        for (String packageName : packageNames) {
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(packageName, typefaceId);
        }
    }

    private void confirmClearAppTypeface(FontLibraryEntry entry,
            FontReference reference,
            androidx.appcompat.app.AlertDialog parentDialog) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_restore_app_font_title)
                .setMessage(getString(R.string.font_library_restore_app_font_message, reference.label))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_restore_default_action, (unusedDialog, which) -> {
                    if (!configStore.clearTargetTypefaceId(reference.packageName)) {
                        showToast(R.string.font_library_restore_app_font_failed);
                        return;
                    }
                    FontRuntimePropertySyncer.publishTypefaceTargetAsync(reference.packageName, null);
                    RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                    refreshFontList();
                    if (parentDialog != null) {
                        parentDialog.dismiss();
                    }
                    showFontDetails(entry);
                })
                .create();
        dialog.setOnShowListener(d -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
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
                    importFont(uri, sourceName, mimeType, displayName);
                })
                .create();
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, this);
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

    private List<FontReference> findReferences(String fontId) {
        List<FontReference> references = new ArrayList<>();
        for (String packageName : configStore.getConfiguredPackages()) {
            if (!fontId.equals(configStore.getTargetTypefaceId(packageName))) {
                continue;
            }
            references.add(new FontReference(packageName, resolveAppLabel(packageName)));
        }
        references.sort((left, right) -> left.label.compareToIgnoreCase(right.label));
        return references;
    }

    private boolean isFontReferenced(String fontId) {
        return !findReferences(fontId).isEmpty();
    }

    private String resolveAppLabel(String packageName) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            CharSequence label = getPackageManager().getApplicationLabel(info);
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            // Fall back to package name for uninstalled or hidden apps.
        }
        return packageName;
    }

    private void handleRenameResult(FontLibraryStore.RenameResult result) {
        if (result == FontLibraryStore.RenameResult.RENAMED) {
            return;
        }
        showToast(result == FontLibraryStore.RenameResult.DUPLICATE_NAME
                ? R.string.font_library_name_duplicate
                : R.string.font_library_name_invalid);
    }

    private void handleDeleteResult(FontLibraryStore.DeleteResult result) {
        if (result == FontLibraryStore.DeleteResult.DELETED) {
            return;
        }
        showToast(result == FontLibraryStore.DeleteResult.IN_USE
                ? R.string.font_library_delete_in_use
                : R.string.font_library_delete_failed);
    }

    private String resolveFontTitle(FontLibraryEntry entry) {
        String source = entry.sourceFileName != null ? entry.sourceFileName : "";
        String display = entry.displayName != null ? entry.displayName.trim() : "";
        if (!display.isEmpty() && !display.equals(source.trim())) {
            return display;
        }
        return stripFontExtension(source);
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

    private static final class FontReference {
        final String packageName;
        final String label;

        FontReference(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
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
