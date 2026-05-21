package com.dpis.module;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

    private LinearLayout listView;
    private MaterialTextView emptyView;
    private FontLibraryStore fontLibraryStore;
    private DpiConfigStore configStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_library);
        fontLibraryStore = ConfigStoreFactory.createFontLibraryForModuleApp(
                this, DpisApplication.getXposedService());
        configStore = ConfigStoreFactory.createForModuleApp(
                this, DpisApplication.getXposedService());
        listView = findViewById(R.id.font_library_list);
        emptyView = findViewById(R.id.font_library_empty);
        AppCompatImageButton backButton = findViewById(R.id.font_library_back_button);
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
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        textGroup.addView(subtitle);

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
        content.setPadding(padding, dp(8), padding, 0);

        MaterialTextView title = new MaterialTextView(this);
        title.setText(resolveFontTitle(entry));
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(entry.sourceFileName);
        subtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        subtitle.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        content.addView(subtitle);

        MaterialTextView referencesTitle = new MaterialTextView(this);
        referencesTitle.setText(R.string.font_library_used_by_title);
        referencesTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        LinearLayout.LayoutParams refTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        refTitleParams.topMargin = dp(16);
        content.addView(referencesTitle, refTitleParams);

        MaterialTextView referencesText = new MaterialTextView(this);
        referencesText.setText(formatReferenceList(references));
        referencesText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        referencesText.setTextColor(MaterialColors.getColor(
                listView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        content.addView(referencesText);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_detail_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setNeutralButton(R.string.font_library_rename_action, null)
                .setPositiveButton(R.string.font_library_delete_action, null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(v -> promptRename(entry, dialog));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        if (!references.isEmpty()) {
                            showToast(R.string.font_library_delete_in_use);
                            return;
                        }
                        confirmDelete(entry, dialog);
                    });
        });
        dialog.show();
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
        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String displayName = input != null && input.getText() != null
                            ? input.getText().toString()
                            : "";
                    FontLibraryStore.RenameResult result = fontLibraryStore.renameFont(entry.id, displayName);
                    if (result != FontLibraryStore.RenameResult.RENAMED) {
                        handleRenameResult(result);
                        return;
                    }
                    parentDialog.dismiss();
                    dialog.dismiss();
                    refreshFontList();
                }));
        dialog.show();
    }

    private void confirmDelete(FontLibraryEntry entry, androidx.appcompat.app.AlertDialog parentDialog) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_delete_title)
                .setMessage(getString(R.string.font_library_delete_message, resolveFontTitle(entry)))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action, (dialog, which) -> {
                    FontLibraryStore.DeleteResult result = fontLibraryStore.deleteFont(entry.id, configStore);
                    handleDeleteResult(result);
                    parentDialog.dismiss();
                    refreshFontList();
                })
                .show();
    }

    @SuppressWarnings("deprecation")
    private void openFontImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/vnd.ms-opentype"
                });
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
        if (!isSupportedFontInput(sourceName, mimeType)) {
            showToast(R.string.font_library_import_failed);
            return;
        }
        TextInputLayout inputLayout = createNameInput(FontLibraryStore.normalizeDisplayName(sourceName));
        TextInputEditText input = (TextInputEditText) inputLayout.getEditText();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font_library_name_title)
                .setView(inputLayout)
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.dialog_confirm_button, (dialog, which) -> {
                    String displayName = input != null && input.getText() != null
                            ? input.getText().toString()
                            : "";
                    importFont(uri, sourceName, mimeType, displayName);
                })
                .show();
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
                Typeface typeface = Typeface.createFromFile(tempFile);
                if (typeface == null) {
                    throw new IOException("Unable to parse font");
                }
                importedEntry = fontLibraryStore.registerCopiedFont(
                        tempFile,
                        sourceName,
                        displayName,
                        System.currentTimeMillis());
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
                refreshFontList();
            });
        }, "dpis-font-import").start();
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

    private String formatReferenceList(List<FontReference> references) {
        if (references.isEmpty()) {
            return getString(R.string.font_library_unused);
        }
        StringBuilder builder = new StringBuilder();
        for (FontReference reference : references) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(reference.label)
                    .append(" (")
                    .append(reference.packageName)
                    .append(')');
        }
        return builder.toString();
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

    private static boolean isSupportedFontInput(String displayName, String mimeType) {
        String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.US);
        return lowerName.endsWith(".ttf")
                || lowerName.endsWith(".otf")
                || "font/ttf".equals(mimeType)
                || "font/otf".equals(mimeType)
                || "application/x-font-ttf".equals(mimeType)
                || "application/vnd.ms-opentype".equals(mimeType);
    }

    private static String resolveFontTempExtension(String displayName, String mimeType) {
        String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.US);
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
}
