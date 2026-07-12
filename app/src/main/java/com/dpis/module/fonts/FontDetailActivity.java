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
import com.dpis.module.ui.WindowInsetsBinder;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Full-screen management view for one imported font collection. */
public final class FontDetailActivity extends LocalizedActivity {
    public static final String EXTRA_FONT_ID = "com.dpis.module.fonts.extra.FONT_ID";
    private static final String FONT_PREVIEW_PRIMARY_TEXT = "AaBbCc 你好世界 123";
    private static final String FONT_PREVIEW_SECONDARY_TEXT = "The quick brown fox jumps over the lazy dog";

    private LinearLayout content;
    private FontLibraryStore fontLibraryStore;
    private FontLibraryConfigStore configStore;
    private String fontId;
    private MaterialButton fallbackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_detail);
        fontId = getIntent().getStringExtra(EXTRA_FONT_ID);
        if (fontId == null || fontId.isBlank()) {
            finish();
            return;
        }
        fontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
                this, DpisApplication.getXposedService());
        configStore = ConfigStoreFactory.createFontLibraryConfigStore(
                this, DpisApplication.getXposedService());
        content = findViewById(R.id.font_detail_content);
        AppCompatImageButton back = findViewById(R.id.font_detail_back_button);
        TouchFeedbackBinder.bindPressHaptic(back);
        back.setOnClickListener(v -> finish());
        fallbackButton = findViewById(R.id.font_detail_fallback_button);
        bindToolbarIcon(fallbackButton, unused -> showFallbackExplanationDialog());
        MaterialButton renameButton = findViewById(R.id.font_detail_rename_button);
        bindToolbarIcon(renameButton, unused -> {
            FontLibraryEntry entry = fontLibraryStore.findById(fontId);
            if (entry != null) {
                promptRename(entry);
            }
        });
        MaterialButton deleteButton = findViewById(R.id.font_detail_delete_button);
        bindToolbarIcon(deleteButton, unused -> confirmDeleteForCurrentEntry());
        WindowInsetsBinder.applySafeDrawingPadding(
                findViewById(R.id.font_detail_toolbar), false, true, false, false);
        WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);
        refreshDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) {
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
        fallbackButton.setVisibility(entry.publicationStatus == FontPublicationStatus.PUBLISH_FAILED
                ? View.VISIBLE : View.GONE);
        content.removeAllViews();
        content.addView(createHeader(entry, !references.isEmpty()));
        File file = fontLibraryStore.resolveFontFile(entry.id);
        if (file != null) {
            content.addView(createPreviewSection(file, entry.ttcIndex), topMarginParams(20));
        }
        content.addView(createReferenceSection(entry, references), topMarginParams(28));
    }

    private View createHeader(FontLibraryEntry entry, boolean inUse) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        MaterialTextView title = new MaterialTextView(this);
        title.setText(resolveFontTitle(entry));
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        group.addView(title);
        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(entry.sourceFileName == null ? "" : entry.sourceFileName);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        group.addView(subtitle, topMarginParams(8));
        LinearLayout badges = new LinearLayout(this);
        badges.setGravity(Gravity.CENTER_VERTICAL);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        if (inUse) {
            badges.addView(createUsedBadge());
        }
        badges.addView(createPublicationBadge(entry));
        group.addView(badges, topMarginParams(6));
        return group;
    }

    private View createPreviewSection(File fontFile, int ttcIndex) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.addView(createSectionTitle(R.string.font_library_preview_title));
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainer));
        card.setCardElevation(0f);
        card.setRadius(dp(12));
        int padding = dp(16);
        card.setContentPadding(padding, padding, padding, padding);
        card.addView(createFontPreview(fontFile, ttcIndex));
        section.addView(card, topMarginParams(10));
        return section;
    }

    private View createFontPreview(File fontFile, int ttcIndex) {
        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        Typeface typeface = FontTypefaceLoader.load(fontFile, ttcIndex);
        MaterialTextView primary = new MaterialTextView(this);
        primary.setText(FONT_PREVIEW_PRIMARY_TEXT);
        primary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        primary.setSingleLine(true);
        primary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (typeface != null) primary.setTypeface(typeface);
        preview.addView(primary);
        MaterialTextView secondary = new MaterialTextView(this);
        secondary.setText(FONT_PREVIEW_SECONDARY_TEXT);
        secondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        secondary.setSingleLine(true);
        secondary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        secondary.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        if (typeface != null) secondary.setTypeface(typeface);
        preview.addView(secondary, topMarginParams(8));
        return preview;
    }

    private View createReferenceSection(FontLibraryEntry entry, List<FontReference> references) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.addView(createSectionTitle(R.string.font_library_active_apps_title));
        if (references.isEmpty()) {
            MaterialTextView empty = new MaterialTextView(this);
            empty.setText(R.string.font_library_unused);
            empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            empty.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
            section.addView(empty, topMarginParams(8));
            return section;
        }
        for (int index = 0; index < references.size(); index++) {
            View row = createReferenceRow(entry, references.get(index), index, references.size());
            if (index == 0) {
                section.addView(row, topMarginParams(10));
            } else {
                section.addView(row, topMarginParams(2));
            }
        }
        return section;
    }

    private View createReferenceRow(FontLibraryEntry entry, FontReference reference,
            int index, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // Match the connected sibling surfaces used by the home page's basic-information rows.
        row.setBackgroundResource(referenceRowBackground(index, count));
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        MaterialTextView label = new MaterialTextView(this);
        label.setText(reference.label);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(label);
        MaterialTextView packageName = new MaterialTextView(this);
        packageName.setText(reference.packageName);
        packageName.setSingleLine(true);
        packageName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        packageName.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        packageName.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        labels.addView(packageName, topMarginParams(2));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialTextView remove = createReferenceRemoveAction();
        remove.setOnClickListener(v -> confirmClearAppTypeface(entry, reference));
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        removeParams.leftMargin = dp(8);
        removeParams.gravity = Gravity.CENTER_VERTICAL;
        row.addView(remove, removeParams);
        return row;
    }

    private static int referenceRowBackground(int index, int count) {
        if (count <= 1) {
            return R.drawable.bg_font_detail_reference_row_single;
        }
        if (index == 0) {
            return R.drawable.bg_home_info_row_top;
        }
        return index == count - 1
                ? R.drawable.bg_home_info_row_bottom
                : R.drawable.bg_home_info_row_middle;
    }

    private void bindToolbarIcon(MaterialButton button, View.OnClickListener listener) {
        TouchFeedbackBinder.bindPressHaptic(button);
        button.setOnClickListener(listener);
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
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_fallback_dialog_title)
                .setMessage(R.string.font_library_fallback_dialog_message)
                .setNegativeButton(R.string.dialog_close_button, null)
                .setPositiveButton(R.string.font_library_publication_retry_action,
                        (unused, which) -> retryPublishedFallbacks())
                .create();
        dialog.setOnShowListener(ignored -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void promptRename(FontLibraryEntry entry) {
        TextInputLayout inputLayout = createNameInput(entry.collectionDisplayName);
        TextInputEditText input = (TextInputEditText) inputLayout.getEditText();
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_name_title).setView(inputLayout)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.dialog_confirm_button, null).create();
        dialog.setOnShowListener(ignored -> {
            bindDialogButtonHaptics(dialog);
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = input != null && input.getText() != null ? input.getText().toString() : "";
                FontLibraryStore.RenameResult result = fontLibraryStore.renameFont(entry.id, name);
                if (result != FontLibraryStore.RenameResult.RENAMED) {
                    showToast(result == FontLibraryStore.RenameResult.DUPLICATE_NAME
                            ? R.string.font_library_name_duplicate : R.string.font_library_name_invalid);
                    return;
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                dialog.dismiss();
                refreshDetails();
            });
        });
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void confirmDelete(FontLibraryEntry entry) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_delete_title)
                .setMessage(getString(R.string.font_library_delete_message, resolveFontTitle(entry)))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action, (unused, which) -> {
                    handleDeleteResult(fontLibraryStore.deleteFont(entry.id, this::isFontReferenced));
                }).create();
        dialog.setOnShowListener(ignored -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void confirmForceDelete(FontLibraryEntry entry, List<FontReference> references) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_delete_title)
                .setMessage(getString(R.string.font_library_delete_in_use_message,
                        resolveFontTitle(entry), references.size()))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action, (unused, which) -> {
                    handleDeleteResult(forceDeleteFont(entry, references));
                }).create();
        dialog.setOnShowListener(ignored -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void handleDeleteResult(FontLibraryStore.DeleteResult result) {
        if (result == FontLibraryStore.DeleteResult.DELETED) {
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
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
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_restore_app_font_title)
                .setMessage(getString(R.string.font_library_restore_app_font_message, reference.label))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_restore_default_action, (unused, which) -> {
                    if (!configStore.clearTargetTypefaceId(reference.packageName)) {
                        showToast(R.string.font_library_restore_app_font_failed);
                        return;
                    }
                    FontRuntimePropertySyncer.publishTypefaceTargetAsync(reference.packageName, null);
                    RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                    refreshDetails();
                }).create();
        dialog.setOnShowListener(ignored -> bindDialogButtonHaptics(dialog));
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void retryPublishedFallbacks() {
        new Thread(() -> {
            FontLibraryStore.RepairResult result = fontLibraryStore.retryPublishedFallbacks();
            runOnUiThread(() -> {
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

    private TextInputLayout createNameInput(String name) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(R.string.font_library_name_hint);
        int padding = dp(20);
        layout.setPadding(padding, 0, padding, 0);
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setSingleLine(true);
        input.setText(name);
        input.selectAll();
        layout.addView(input);
        return layout;
    }

    private MaterialTextView createUsedBadge() {
        return createBadge(
                R.string.font_library_used_badge,
                com.google.android.material.R.attr.colorSecondaryContainer,
                androidx.appcompat.R.attr.colorPrimary);
    }

    private MaterialTextView createPublicationBadge(FontLibraryEntry entry) {
        boolean isPublic = entry.publicationStatus == FontPublicationStatus.PUBLISHED;
        return createBadge(
                isPublic
                        ? R.string.font_library_public_badge
                        : R.string.font_library_private_badge,
                isPublic
                        ? com.google.android.material.R.attr.colorPrimaryContainer
                        : com.google.android.material.R.attr.colorTertiaryContainer,
                isPublic
                        ? com.google.android.material.R.attr.colorOnPrimaryContainer
                        : com.google.android.material.R.attr.colorOnTertiaryContainer);
    }

    private MaterialTextView createBadge(int textResId, int backgroundAttribute, int textAttribute) {
        MaterialTextView badge = new MaterialTextView(this);
        badge.setText(textResId);
        badge.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
        badge.setTextColor(color(textAttribute));
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(999));
        background.setColor(color(backgroundAttribute));
        badge.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(8);
        badge.setLayoutParams(params);
        return badge;
    }

    private MaterialTextView createSectionTitle(int textResId) {
        MaterialTextView title = new MaterialTextView(this);
        title.setText(textResId);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        return title;
    }

    private MaterialTextView createReferenceRemoveAction() {
        MaterialTextView action = new MaterialTextView(this);
        action.setText(R.string.font_library_remove_app_action);
        action.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        action.setTextColor(color(com.google.android.material.R.attr.colorOnSecondaryContainer));
        action.setGravity(Gravity.CENTER);
        action.setMinWidth(dp(48));
        action.setMinHeight(dp(28));
        action.setIncludeFontPadding(false);
        action.setPadding(dp(8), 0, dp(8), 0);
        action.setClickable(true);
        action.setFocusable(true);
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(999));
        background.setColor(color(com.google.android.material.R.attr.colorSecondaryContainer));
        action.setBackground(background);
        int selectableBackground = resolveSelectableItemBackground();
        if (selectableBackground != 0) {
            action.setForeground(getDrawable(selectableBackground));
        }
        TouchFeedbackBinder.bindPressHaptic(action);
        return action;
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

    private int color(int attribute) { return MaterialColors.getColor(content, attribute); }
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
