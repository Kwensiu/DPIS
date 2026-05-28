package com.dpis.module;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class AppConfigDialogBinder {
    private static final long MODE_TOGGLE_ANIM_DURATION_MS = 200L;

    enum ProcessAction {
        START,
        RESTART,
        STOP
    }

    interface Host {
        void clearDialogInputFocus(View fallbackFocusView,
                TextInputEditText viewportInputView,
                TextInputEditText fontInputView);

        void toggleScope(AppListItem item,
                boolean currentlyInScope,
                Runnable onTurnedInScope,
                Runnable onTurnedOutScope);

        boolean requestScope(AppListItem item,
                Runnable onTurnedInScope,
                Runnable onRequestFinished);

        void executeProcessAction(AppListItem item, ProcessAction action);

        void applyHyperOsNativeProxy(AppListItem item, Runnable onFinished);

        void unmountHyperOsNativeProxy(AppListItem item, Runnable onFinished);

        boolean setDpisEnabled(String packageName, boolean enabled);

        void showFontHookDomains(AppListItem item,
                AppConfigDialogState state,
                Runnable onStateChanged);

        String getFontHookDomainsButtonText(AppListItem item,
                boolean previewFromGlobalPrefill,
                String previewFontHookDomainsRaw);

        void openTypefaceLibrary();

        int[] saveAppConfig(AppListItem item,
                TextInputEditText viewportInput,
                TextInputEditText fontScaleInput,
                String viewportMode,
                String viewportApplyMode,
                String fontMode,
                String selectedTypefaceId,
                String previewFontHookDomainsRaw,
                String viewportScaleInput,
                String viewportAbsoluteInput);

        DpiConfigStore getConfigStore();

        void requestAppsLoad();

        void showToast(int messageResId);
    }

    private final Activity activity;
    private final Host host;

    AppConfigDialogBinder(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    void bind(View dialogView, AppListItem item, boolean systemHooksEnabled) {
        AppConfigDialogViews views = initDialogViews(dialogView);
        AppConfigDialogState state = bindDialogInitialState(item, views);
        WechatTargetFieldSheetBinder.bind(dialogView, item,
                () -> updateSaveButtonState(dialogView, views));
        updateSaveButtonState(dialogView, views);
        AppConfigDialogActionStyle style = resolveDialogActionStyle(views.scopeButton);
        refreshDialogState(views, state, style, systemHooksEnabled, item);
        new AppConfigSheetInteractions(this, host)
                .bind(dialogView, item, views, state, style, systemHooksEnabled);
    }

    private AppConfigDialogViews initDialogViews(View dialogView) {
        return new AppConfigDialogViews(
                dialogView.findViewById(R.id.dialog_app_icon),
                dialogView.findViewById(R.id.dialog_title),
                dialogView.findViewById(R.id.dialog_package),
                dialogView.findViewById(R.id.dialog_status),
                dialogView.findViewById(R.id.dialog_preview_status),
                dialogView.findViewById(R.id.dialog_viewport_input_layout),
                dialogView.findViewById(R.id.dialog_viewport_input),
                dialogView.findViewById(R.id.dialog_font_scale_input_layout),
                dialogView.findViewById(R.id.dialog_font_scale_input),
                new ModeToggle(
                        dialogView.findViewById(R.id.dialog_viewport_mode_toggle_button),
                        dialogView.findViewById(R.id.dialog_viewport_mode_toggle_thumb),
                        dialogView.findViewById(R.id.dialog_viewport_mode_system_label),
                        dialogView.findViewById(R.id.dialog_viewport_mode_compat_label)),
                new ModeToggle(
                        dialogView.findViewById(R.id.dialog_font_mode_toggle_button),
                        dialogView.findViewById(R.id.dialog_font_mode_toggle_thumb),
                        dialogView.findViewById(R.id.dialog_font_mode_system_label),
                        dialogView.findViewById(R.id.dialog_font_mode_compat_label)),
                dialogView.findViewById(R.id.dialog_typeface_selector_button),
                dialogView.findViewById(R.id.dialog_scope_button),
                dialogView.findViewById(R.id.dialog_start_button),
                dialogView.findViewById(R.id.dialog_restart_button),
                dialogView.findViewById(R.id.dialog_stop_button),
                dialogView.findViewById(R.id.dialog_dpis_toggle_button),
                dialogView.findViewById(R.id.dialog_font_hook_domains_button),
                dialogView.findViewById(R.id.dialog_disable_button),
                dialogView.findViewById(R.id.dialog_save_button));
    }

    private AppConfigDialogState bindDialogInitialState(AppListItem item, AppConfigDialogViews views) {
        views.iconView.setImageDrawable(item.icon);
        views.titleView.setText(item.label);
        views.packageView.setText(item.packageName);
        String initialViewportInput = formatViewportInput(item.viewportTargetSpec);
        views.viewportInputView.setText(initialViewportInput);
        String initialViewportScaleInput = item.viewportScalePermille != null
                ? String.valueOf(item.viewportScalePermille / 10)
                : (item.viewportTargetSpec.isRelativeScale() ? initialViewportInput : "");
        String initialViewportAbsoluteInput = item.viewportWidthDp != null
                ? String.valueOf(item.viewportWidthDp)
                : (item.viewportTargetSpec.isAbsoluteDp() ? initialViewportInput : "");
        views.fontInputView.setText(item.fontScalePercent != null
                ? String.valueOf(item.fontScalePercent)
                : "");
        String initialViewportType = initialViewportTargetType(item.viewportTargetSpec);
        bindViewportModeToggle(views.viewportModeToggle, initialViewportType, false);
        bindViewportInputHint(views.viewportInputLayout, initialViewportType);
        bindFontModeToggle(views.fontModeToggle, initialFontMode(item.fontMode), false);
        String selectedTypefaceId = normalizeTypefaceId(item.typefaceId);
        bindTypefaceSelector(views.typefaceSelectorButton, selectedTypefaceId);
        updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputLayout, views.fontInputView, views.saveButton);
        return new AppConfigDialogState(item.inScope, item.scopeKnown, item.dpisEnabled,
                item.previewFromGlobalPrefill,
                item.previewFontHookDomainsRaw,
                item.viewportMode,
                selectedTypefaceId,
                initialViewportType,
                initialViewportInput,
                initialViewportScaleInput,
                initialViewportAbsoluteInput);
    }

    private AppConfigDialogActionStyle resolveDialogActionStyle(MaterialButton baseButton) {
        ColorStateList defaultActionBgTint = baseButton.getBackgroundTintList();
        int defaultActionStrokeWidth = baseButton.getStrokeWidth();
        int defaultActionTextColor = MaterialColors.getColor(
                baseButton, androidx.appcompat.R.attr.colorPrimary);
        return new AppConfigDialogActionStyle(defaultActionBgTint,
                defaultActionStrokeWidth, defaultActionTextColor);
    }

    void refreshDialogState(AppConfigDialogViews views,
            AppConfigDialogState state,
            AppConfigDialogActionStyle style,
            boolean systemHooksEnabled,
            AppListItem item) {
        updateDialogStatus(
                views.statusView,
                state.scopeSelected,
                state.scopeKnown,
                state.dpisEnabled,
                views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputView,
                views.fontModeToggle,
                state.selectedTypefaceId,
                systemHooksEnabled,
                item.packageName,
                state.viewportApplyMode);
        bindPreviewStatus(views.previewStatusView, state.previewFromGlobalPrefill);
        bindScopeButton(views.scopeButton, state.scopeSelected, state.scopeKnown,
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor);
        bindDpisToggleButton(views.dpisToggleButton, state.dpisEnabled,
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor);
        bindFontHookDomainsButton(
                views.fontHookDomainsButton,
                item,
                state.previewFromGlobalPrefill,
                state.previewFontHookDomainsRaw);
    }

    private void bindPreviewStatus(MaterialTextView previewStatusView, boolean previewFromGlobalPrefill) {
        previewStatusView.setVisibility(previewFromGlobalPrefill ? View.VISIBLE : View.GONE);
        if (previewFromGlobalPrefill) {
            previewStatusView.setText(R.string.dialog_global_prefill_preview_status);
        }
    }

    void bindTypefaceSelector(MaterialButton selectorButton, String selectedTypefaceId) {
        configureTypefaceSelectorMarquee(selectorButton);
        selectorButton.setText(formatTypefaceSelectorText(resolveTypefaceDisplayText(
                selectedTypefaceId, listFontLibraryEntries())));
    }

    private void configureTypefaceSelectorMarquee(MaterialButton selectorButton) {
        selectorButton.setSingleLine(true);
        selectorButton.setHorizontallyScrolling(true);
        selectorButton.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        selectorButton.setMarqueeRepeatLimit(-1);
        selectorButton.setSelected(true);
    }

    void showTypefaceSelector(MaterialButton selectorButton,
            AppConfigDialogState state,
            Runnable onSelectionChanged) {
        boolean selectedImported = state.selectedTypefaceId != null
                && !state.selectedTypefaceId.isBlank()
                && !SystemFontRegistry.isSystemFontId(state.selectedTypefaceId);
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_typeface_selection, null, false);
        TabLayout tabs = root.findViewById(R.id.typeface_tabs);
        LinearLayout listView = root.findViewById(R.id.typeface_options_container);
        MaterialButton importButton = root.findViewById(R.id.typeface_import_button);
        MaterialButton doneButton = root.findViewById(R.id.typeface_dialog_done_button);
        tabs.addTab(tabs.newTab().setText(R.string.dialog_typeface_tab_system));
        tabs.addTab(tabs.newTab().setText(R.string.dialog_typeface_tab_imported));
        AlertDialog[] dialogHolder = new AlertDialog[1];
        Runnable showSystemFonts = () -> bindTypefaceOptionRows(
                listView,
                buildSystemTypefaceOptions(SystemFontRegistry.listRecommendedFonts(), state.selectedTypefaceId),
                selectorButton,
                state,
                onSelectionChanged,
                dialogHolder,
                false);
        Runnable showImportedFonts = () -> bindTypefaceOptionRows(
                listView,
                buildImportedTypefaceOptions(listFontLibraryEntries(), state.selectedTypefaceId),
                selectorButton,
                state,
                onSelectionChanged,
                dialogHolder,
                true);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    showImportedFonts.run();
                    return;
                }
                showSystemFonts.run();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        importButton.setOnClickListener(v -> {
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
            host.openTypefaceLibrary();
        });
        dialogHolder[0] = new MaterialAlertDialogBuilder(activity)
                .setView(root)
                .create();
        doneButton.setOnClickListener(v -> {
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
        });
        dialogHolder[0].setCanceledOnTouchOutside(true);
        dialogHolder[0].show();
        TabLayout.Tab initialTab = tabs.getTabAt(selectedImported ? 1 : 0);
        if (initialTab != null) {
            initialTab.select();
        }
        if (tabs.getSelectedTabPosition() == (selectedImported ? 1 : 0)) {
            if (selectedImported) {
                showImportedFonts.run();
                return;
            }
            showSystemFonts.run();
        }
    }

    private String resolveTypefaceDisplayText(String selectedTypefaceId, List<FontLibraryEntry> entries) {
        if (selectedTypefaceId == null || selectedTypefaceId.isBlank()) {
            return activity.getString(R.string.dialog_typeface_default);
        }
        for (SystemFontEntry entry : SystemFontRegistry.listRecommendedFonts()) {
            if (selectedTypefaceId.equals(entry.id)) {
                return entry.displayName;
            }
        }
        for (FontLibraryEntry entry : entries) {
            if (selectedTypefaceId.equals(entry.id)) {
                return entry.displayName;
            }
        }
        return activity.getString(R.string.dialog_typeface_missing);
    }

    private List<TypefaceOption> buildSystemTypefaceOptions(
            List<SystemFontEntry> entries,
            String selectedTypefaceId) {
        List<TypefaceOption> options = new ArrayList<>(entries.size() + 2);
        options.add(new TypefaceOption(null, activity.getString(R.string.dialog_typeface_default)));
        if (SystemFontRegistry.isSystemFontId(selectedTypefaceId)
                && !containsSystemTypeface(entries, selectedTypefaceId)) {
            options.add(new TypefaceOption(
                    selectedTypefaceId,
                    activity.getString(R.string.dialog_typeface_missing)));
        }
        for (SystemFontEntry entry : entries) {
            options.add(new TypefaceOption(entry.id, entry.displayName));
        }
        return options;
    }

    private List<TypefaceOption> buildImportedTypefaceOptions(
            List<FontLibraryEntry> entries,
            String selectedTypefaceId) {
        List<TypefaceOption> options = new ArrayList<>(entries.size() + 2);
        options.add(new TypefaceOption(null, activity.getString(R.string.dialog_typeface_default)));
        if (selectedTypefaceId != null
                && !selectedTypefaceId.isBlank()
                && !SystemFontRegistry.isSystemFontId(selectedTypefaceId)
                && !containsImportedTypeface(entries, selectedTypefaceId)) {
            options.add(new TypefaceOption(
                    selectedTypefaceId,
                    activity.getString(R.string.dialog_typeface_missing)));
        }
        if (entries.isEmpty()) {
            options.add(new TypefaceOption(
                    TypefaceOption.DISABLED_ID,
                    activity.getString(R.string.dialog_typeface_imported_empty)));
        }
        for (FontLibraryEntry entry : entries) {
            options.add(new TypefaceOption(entry.id, resolveFontOptionLabel(entry)));
        }
        return options;
    }

    private void bindTypefaceOptionRows(LinearLayout listView,
            List<TypefaceOption> options,
            MaterialButton selectorButton,
            AppConfigDialogState state,
            Runnable onSelectionChanged,
            AlertDialog[] dialogHolder,
            boolean editableImportedRows) {
        listView.removeAllViews();
        FontLibraryStore fontLibraryStore = editableImportedRows ? createFontLibraryStore() : null;
        for (TypefaceOption option : options) {
            View row = createTypefaceOptionRow(
                    listView,
                    option,
                    resolveTypefaceOptionPreview(option, fontLibraryStore),
                    option.matches(state.selectedTypefaceId),
                    () -> {
                if (option.isDisabled()) {
                    return;
                }
                state.selectedTypefaceId = option.id;
                selectorButton.setText(formatTypefaceSelectorText(option.label));
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
                bindTypefaceOptionRows(
                        listView,
                        editableImportedRows
                                ? buildImportedTypefaceOptions(listFontLibraryEntries(), state.selectedTypefaceId)
                                : buildSystemTypefaceOptions(
                                        SystemFontRegistry.listRecommendedFonts(), state.selectedTypefaceId),
                        selectorButton,
                        state,
                        onSelectionChanged,
                        dialogHolder,
                        editableImportedRows);
                    });
            listView.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private View createTypefaceOptionRow(ViewGroup parent,
            TypefaceOption option,
            Typeface previewTypeface,
            boolean selected,
            Runnable onSelect) {
        FrameLayout row = new FrameLayout(activity);
        int rowPaddingVertical = dimenPx(R.dimen.dialog_typeface_option_row_padding_vertical);
        row.setPadding(0, rowPaddingVertical, 0, rowPaddingVertical);
        MaterialButton optionButton = createTypefaceOptionButton(
                parent, option.label, previewTypeface, selected);
        optionButton.setEnabled(!option.isDisabled());
        optionButton.setOnClickListener(v -> onSelect.run());
        row.addView(optionButton, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dimenPx(R.dimen.dialog_typeface_option_min_height),
                Gravity.CENTER_VERTICAL));
        return row;
    }

    private MaterialButton createTypefaceOptionButton(
            ViewGroup parent,
            String text,
            Typeface previewTypeface,
            boolean selected) {
        MaterialButton button = new MaterialButton(activity);
        button.setText(text);
        if (previewTypeface != null) {
            button.setTypeface(previewTypeface);
        }
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setMinWidth(0);
        button.setMinHeight(dimenPx(R.dimen.dialog_typeface_option_min_height));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        int paddingHorizontal = dimenPx(R.dimen.dialog_typeface_option_padding_horizontal);
        button.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        button.setCornerRadius(dimenPx(R.dimen.dialog_typeface_option_corner_radius));
        button.setStrokeWidth(0);
        int backgroundColor = selected
                ? MaterialColors.getColor(parent, com.google.android.material.R.attr.colorSecondaryContainer)
                : MaterialColors.getColor(parent, com.google.android.material.R.attr.colorSurfaceVariant);
        int textColor = MaterialColors.getColor(
                parent,
                selected ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurface);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
        return button;
    }

    private Typeface resolveTypefaceOptionPreview(TypefaceOption option, FontLibraryStore fontLibraryStore) {
        if (option == null || option.id == null || option.id.isBlank() || option.isDisabled()) {
            return null;
        }
        if (SystemFontRegistry.isSystemFontId(option.id)) {
            return SystemFontRegistry.loadTypeface(option.id);
        }
        if (fontLibraryStore == null) {
            return null;
        }
        FontLibraryEntry entry = fontLibraryStore.findById(option.id);
        if (entry == null) {
            return null;
        }
        File fontFile = fontLibraryStore.resolveFontFile(option.id);
        if (fontFile == null) {
            return null;
        }
        return FontTypefaceLoader.load(fontFile, entry.ttcIndex);
    }

    private static String resolveFontOptionLabel(FontLibraryEntry entry) {
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
        String lower = trimmed.toLowerCase(java.util.Locale.US);
        if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
            return trimmed.substring(0, trimmed.length() - 4);
        }
        return trimmed;
    }

    private String formatTypefaceSelectorText(String displayText) {
        return activity.getString(R.string.dialog_typeface_selector_value, displayText);
    }

    private static String formatViewportInput(ViewportTargetSpec spec) {
        return AppConfigInputValidation.formatViewportInput(spec);
    }

    private static boolean containsSystemTypeface(List<SystemFontEntry> entries, String selectedTypefaceId) {
        for (SystemFontEntry entry : entries) {
            if (entry.id.equals(selectedTypefaceId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsImportedTypeface(List<FontLibraryEntry> entries, String selectedTypefaceId) {
        for (FontLibraryEntry entry : entries) {
            if (entry.id.equals(selectedTypefaceId)) {
                return true;
            }
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private int dimenPx(int resId) {
        return activity.getResources().getDimensionPixelSize(resId);
    }

    private List<FontLibraryEntry> listFontLibraryEntries() {
        return createFontLibraryStore().listFonts();
    }

    private FontLibraryStore createFontLibraryStore() {
        return ConfigStoreFactory.createFontLibraryForModuleApp(
                activity, DpisApplication.getXposedService());
    }

    private static String normalizeTypefaceId(String typefaceId) {
        return typefaceId != null && !typefaceId.isBlank() ? typefaceId : null;
    }

    static void showSaveButtonFeedback(MaterialButton saveButton) {
        if (saveButton == null) {
            return;
        }
        CharSequence restoreText;
        Object[] tag = saveButton.getTag() instanceof Object[] ? (Object[]) saveButton.getTag() : null;
        if (tag != null && tag[0] instanceof CharSequence) {
            restoreText = (CharSequence) tag[0];
            if (tag[1] instanceof Runnable) {
                saveButton.removeCallbacks((Runnable) tag[1]);
            }
        } else {
            restoreText = saveButton.getText();
        }
        saveButton.setText(R.string.status_save_success_inline);
        Runnable restore = () -> {
            if (saveButton.isAttachedToWindow()) {
                saveButton.setText(restoreText);
            }
        };
        saveButton.setTag(new Object[] { restoreText, restore });
        saveButton.postDelayed(restore, 1500);
    }

    static boolean updateSaveButtonState(TextInputLayout viewportInputLayout,
            TextInputEditText viewportInputView,
            ModeToggle viewportModeToggle,
            TextInputLayout fontInputLayout,
            TextInputEditText fontInputView,
            MaterialButton saveButton) {
        String viewportRaw = viewportInputView.getText() != null
                ? viewportInputView.getText().toString()
                : "";
        String fontRaw = fontInputView.getText() != null
                ? fontInputView.getText().toString()
                : "";
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                viewportRaw, resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(fontRaw);
        int defaultStrokeColor = MaterialColors.getColor(
                viewportInputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                viewportInputLayout, androidx.appcompat.R.attr.colorError);
        viewportInputLayout.setError(null);
        fontInputLayout.setError(null);
        viewportInputLayout.setErrorEnabled(false);
        fontInputLayout.setErrorEnabled(false);
        viewportInputLayout.setBoxStrokeColor(viewportValid ? defaultStrokeColor : errorStrokeColor);
        fontInputLayout.setBoxStrokeColor(fontValid ? defaultStrokeColor : errorStrokeColor);
        boolean valid = viewportValid && fontValid;
        saveButton.setEnabled(valid);
        return valid;
    }

    static boolean updateSaveButtonState(View dialogView, AppConfigDialogViews views) {
        boolean genericValid = updateSaveButtonState(
                views.viewportInputLayout,
                views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputLayout,
                views.fontInputView,
                views.saveButton);
        boolean valid = genericValid && WechatTargetFieldSheetBinder.isInputValid(dialogView);
        views.saveButton.setEnabled(valid);
        return valid;
    }

    private void updateDialogStatus(MaterialTextView statusView,
            boolean inScope,
            boolean scopeKnown,
            boolean dpisEnabled,
            TextInputEditText viewportInputView,
            ModeToggle viewportModeToggle,
            TextInputEditText fontInputView,
            ModeToggle fontModeToggle,
            String selectedTypefaceId,
            boolean systemHooksEnabled,
            String packageName,
            String currentViewportApplyMode) {
        ViewportTargetSpec viewportTargetSpec = parseViewportTargetSpecOrNullSafe(
                viewportInputView, resolveViewportMode(viewportModeToggle));
        Integer fontScalePercent = parseFontScalePercentOrNullSafe(fontInputView);
        String fontMode = fontScalePercent == null ? FontApplyMode.OFF : resolveFontMode(fontModeToggle);
        String viewportApplyMode = viewportTargetSpec.isEnabled()
                ? ViewportApplyMode.normalize(currentViewportApplyMode)
                : ViewportApplyMode.OFF;
        String dialogStatusText = AppStatusFormatter.formatCompact(
                activity.getResources(), inScope, scopeKnown, viewportTargetSpec,
                viewportApplyMode,
                fontScalePercent, fontMode, selectedTypefaceId, dpisEnabled);
        boolean warnViewport = scopeKnown && AppStatusFormatter.shouldWarnViewportEmulation(
                viewportTargetSpec, viewportApplyMode, systemHooksEnabled, dpisEnabled);
        boolean warnFont = scopeKnown && AppStatusFormatter.shouldWarnFontEmulation(
                fontScalePercent, fontMode, systemHooksEnabled, dpisEnabled);
        if (warnViewport || warnFont) {
            int warnColor = MaterialColors.getColor(statusView, androidx.appcompat.R.attr.colorError);
            statusView.setText(AppStatusFormatter.applyConfigSegmentsWarnStyle(
                    dialogStatusText, warnColor, warnViewport, warnFont));
            return;
        }
        statusView.setText(dialogStatusText);
    }

    void syncHyperOsNativeProxyAfterSave(
            AppListItem item, AppConfigDialogViews views, AppConfigDialogState state) {
        if (!item.hyperOsNativeProxyCandidate) {
            return;
        }
        setSaveAndResetButtonsEnabled(views, false);
        Runnable onFinished = () -> {
            setSaveAndResetButtonsEnabled(views, true);
        };
        if (state.dpisEnabled && hasActiveDialogConfig(views, state)) {
            host.applyHyperOsNativeProxy(item, onFinished);
            return;
        }
        host.unmountHyperOsNativeProxy(item, onFinished);
    }

    // State lifecycle is bound to the dialog instance; pending flag cleanup is
    // best-effort since the state object is discarded when the dialog closes.
    void requestScopeAfterSuccessfulSave(View dialogView,
            AppListItem item,
            AppConfigDialogViews views,
            AppConfigDialogState state,
            AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        if (!state.scopeKnown || state.scopeSelected || state.scopeRequestPending) {
            return;
        }
        state.scopeRequestPending = true;
        boolean requestStarted = host.requestScope(item,
                () -> {
                    if (!dialogView.isAttachedToWindow()) {
                        return;
                    }
                    state.scopeSelected = true;
                    refreshDialogState(views, state, style, systemHooksEnabled, item);
                },
                () -> state.scopeRequestPending = false);
        if (requestStarted) {
            host.showToast(R.string.save_scope_request_notice);
        } else {
            state.scopeRequestPending = false;
        }
    }

    private static boolean hasActiveDialogConfig(AppConfigDialogViews views, AppConfigDialogState state) {
        return parseViewportTargetSpecOrNullSafe(
                views.viewportInputView,
                resolveViewportMode(views.viewportModeToggle)).isEnabled()
                || parsePositiveIntOrNullSafe(views.fontInputView) != null
                || (state.selectedTypefaceId != null && !state.selectedTypefaceId.isBlank());
    }

    private static void setSaveAndResetButtonsEnabled(AppConfigDialogViews views, boolean enabled) {
        views.saveButton.setEnabled(enabled);
        views.disableButton.setEnabled(enabled);
    }

    private static Integer parsePositiveIntOrNullSafe(TextInputEditText inputView) {
        String raw = inputView.getText() != null ? inputView.getText().toString() : "";
        return AppConfigInputValidation.parsePositiveIntOrNull(raw);
    }

    private static ViewportTargetSpec parseViewportTargetSpecOrNullSafe(
            TextInputEditText inputView,
            String viewportTargetType) {
        String raw = inputView.getText() != null ? inputView.getText().toString() : "";
        return AppConfigInputValidation.parseViewportTargetSpec(raw, viewportTargetType);
    }

    private static Integer parseFontScalePercentOrNullSafe(TextInputEditText inputView) {
        String raw = inputView.getText() != null ? inputView.getText().toString() : "";
        return AppConfigInputValidation.parseFontScalePercentOrNull(raw);
    }

    static String resolveFontMode(ModeToggle fontModeToggle) {
        Object modeTag = fontModeToggle.container.getTag();
        if (FontApplyMode.SYSTEM_EMULATION.equals(modeTag)) {
            return FontApplyMode.SYSTEM_EMULATION;
        }
        return FontApplyMode.FIELD_REWRITE;
    }

    private static String initialFontMode(String fontMode) {
        return AppConfigInputValidation.initialFontMode(fontMode);
    }

    static String resolveViewportMode(ModeToggle viewportModeToggle) {
        Object modeTag = viewportModeToggle.container.getTag();
        if (ViewportTargetType.ABSOLUTE_DP.equals(modeTag)) {
            return ViewportTargetType.ABSOLUTE_DP;
        }
        return ViewportTargetType.RELATIVE_SCALE;
    }

    private static String initialViewportTargetType(ViewportTargetSpec spec) {
        return AppConfigInputValidation.initialViewportTargetType(spec);
    }

    static void bindFontModeToggle(ModeToggle fontModeToggle,
            String fontMode,
            boolean animate) {
        String resolved = FontApplyMode.SYSTEM_EMULATION.equals(fontMode)
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        fontModeToggle.container.setTag(resolved);
        updateModeToggleVisual(fontModeToggle, FontApplyMode.SYSTEM_EMULATION.equals(resolved), animate);
    }

    static void toggleFontMode(ModeToggle fontModeToggle) {
        String nextMode = FontApplyMode.FIELD_REWRITE.equals(resolveFontMode(fontModeToggle))
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        bindFontModeToggle(fontModeToggle, nextMode, true);
    }

    static void bindViewportModeToggle(ModeToggle viewportModeToggle,
            String viewportTargetType,
            boolean animate) {
        String resolved = ViewportTargetType.ABSOLUTE_DP.equals(
                ViewportTargetType.normalize(viewportTargetType))
                        ? ViewportTargetType.ABSOLUTE_DP
                        : ViewportTargetType.RELATIVE_SCALE;
        viewportModeToggle.container.setTag(resolved);
        updateModeToggleVisual(viewportModeToggle,
                ViewportTargetType.RELATIVE_SCALE.equals(resolved), animate);
    }

    void bindViewportInputHint(TextInputLayout viewportInputLayout, String viewportTargetType) {
        if (viewportInputLayout == null) {
            return;
        }
        viewportInputLayout.setHint(ViewportTargetType.RELATIVE_SCALE.equals(
                ViewportTargetType.normalize(viewportTargetType))
                        ? R.string.dialog_viewport_hint_scale
                        : R.string.dialog_viewport_hint_absolute);
    }

    static void toggleViewportMode(ModeToggle viewportModeToggle,
            TextInputEditText viewportInputView,
            AppConfigDialogState state) {
        String nextMode = ViewportTargetType.ABSOLUTE_DP.equals(
                resolveViewportMode(viewportModeToggle))
                        ? ViewportTargetType.RELATIVE_SCALE
                        : ViewportTargetType.ABSOLUTE_DP;
        switchViewportTargetType(viewportModeToggle, viewportInputView, state, nextMode, true);
    }

    static void switchViewportTargetType(ModeToggle viewportModeToggle,
            TextInputEditText viewportInputView,
            AppConfigDialogState state,
            String nextType,
            boolean animate) {
        state.updateViewportInput(resolveViewportMode(viewportModeToggle),
                viewportInputView.getText());
        bindViewportModeToggle(viewportModeToggle, nextType, animate);
        viewportInputView.setText(state.viewportInputFor(nextType));
    }

    private static void updateModeToggleVisual(ModeToggle toggle,
            boolean emulationActive,
            boolean animate) {
        int activeTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSecondaryContainer);
        int inactiveTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSurface);
        toggle.emulationLabel.setTextColor(emulationActive ? activeTextColor : inactiveTextColor);
        toggle.replaceLabel.setTextColor(emulationActive ? inactiveTextColor : activeTextColor);
        toggle.emulationLabel.setAlpha(emulationActive ? 1f : 0.66f);
        toggle.replaceLabel.setAlpha(emulationActive ? 0.66f : 1f);
        toggle.emulationLabel.setTypeface(Typeface.DEFAULT,
                emulationActive ? Typeface.BOLD : Typeface.NORMAL);
        toggle.replaceLabel.setTypeface(Typeface.DEFAULT,
                emulationActive ? Typeface.NORMAL : Typeface.BOLD);
        toggle.emulationLabel.setScaleX(emulationActive ? 1.04f : 1f);
        toggle.emulationLabel.setScaleY(emulationActive ? 1.04f : 1f);
        toggle.replaceLabel.setScaleX(emulationActive ? 1f : 1.04f);
        toggle.replaceLabel.setScaleY(emulationActive ? 1f : 1.04f);
        toggle.container.post(() -> {
            int availableWidth = toggle.container.getWidth()
                    - toggle.container.getPaddingLeft()
                    - toggle.container.getPaddingRight();
            if (availableWidth <= 0) {
                return;
            }
            int half = availableWidth / 2;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) toggle.thumb.getLayoutParams();
            if (params.width != half || params.height != FrameLayout.LayoutParams.MATCH_PARENT) {
                params.width = half;
                params.height = FrameLayout.LayoutParams.MATCH_PARENT;
                toggle.thumb.setLayoutParams(params);
            }
            float target = emulationActive ? 0f : half;
            if (animate) {
                toggle.thumb.animate().cancel();
                toggle.thumb.animate()
                        .translationX(target)
                        .translationY(0f)
                        .setDuration(MODE_TOGGLE_ANIM_DURATION_MS)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            } else {
                toggle.thumb.setTranslationX(target);
                toggle.thumb.setTranslationY(0f);
            }
        });
    }

    private void bindScopeButton(MaterialButton scopeButton,
            boolean inScope,
            boolean scopeKnown,
            ColorStateList defaultBgTint,
            int defaultStrokeWidth,
            int defaultTextColor) {
        int activeBgColor = MaterialColors.getColor(
                scopeButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                scopeButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        scopeButton.setIcon(null);
        int scopeTextRes = scopeKnown
                ? (inScope ? R.string.scope_remove_button : R.string.scope_add_button)
                : R.string.scope_add_button;
        scopeButton.setText(scopeTextRes);
        boolean activeScopeStyle = scopeKnown && inScope;
        scopeButton.setBackgroundTintList(activeScopeStyle
                ? ColorStateList.valueOf(activeBgColor)
                : defaultBgTint);
        scopeButton.setTextColor(activeScopeStyle ? activeFgColor : defaultTextColor);
        scopeButton.setStrokeWidth(activeScopeStyle ? 0 : defaultStrokeWidth);
        scopeButton.setContentDescription(activity.getString(scopeTextRes));
        scopeButton.setEnabled(scopeKnown);
        scopeButton.setAlpha(scopeKnown ? 1f : 0.6f);
    }

    private void bindDpisToggleButton(MaterialButton dpisToggleButton,
            boolean dpisEnabled,
            ColorStateList defaultBgTint,
            int defaultStrokeWidth,
            int defaultTextColor) {
        String buttonText = activity.getString(
                dpisEnabled ? R.string.dialog_dpis_disable_button : R.string.dialog_dpis_enable_button);
        dpisToggleButton.setText(buttonText);
        dpisToggleButton.setIcon(null);
        int activeBgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        boolean enabledActive = dpisEnabled;
        dpisToggleButton.setBackgroundTintList(
                enabledActive ? ColorStateList.valueOf(activeBgColor) : defaultBgTint);
        dpisToggleButton.setTextColor(enabledActive ? activeFgColor : defaultTextColor);
        dpisToggleButton.setStrokeWidth(enabledActive ? 0 : defaultStrokeWidth);
        dpisToggleButton.setContentDescription(buttonText);
    }

    void bindFontHookDomainsButton(MaterialButton button,
            AppListItem item,
            boolean previewFromGlobalPrefill,
            String previewFontHookDomainsRaw) {
        String buttonText = host.getFontHookDomainsButtonText(
                item, previewFromGlobalPrefill, previewFontHookDomainsRaw);
        button.setText(buttonText);
        button.setIcon(null);
        button.setContentDescription(buttonText);
    }

    static final class ModeToggle {
        final View container;
        final View thumb;
        final MaterialTextView emulationLabel;
        final MaterialTextView replaceLabel;

        ModeToggle(View container, View thumb, MaterialTextView emulationLabel,
                MaterialTextView replaceLabel) {
            this.container = container;
            this.thumb = thumb;
            this.emulationLabel = emulationLabel;
            this.replaceLabel = replaceLabel;
        }
    }

    static final class AppConfigDialogViews {
        final android.widget.ImageView iconView;
        final MaterialTextView titleView;
        final MaterialTextView packageView;
        final MaterialTextView statusView;
        final MaterialTextView previewStatusView;
        final TextInputLayout viewportInputLayout;
        final TextInputEditText viewportInputView;
        final TextInputLayout fontInputLayout;
        final TextInputEditText fontInputView;
        final ModeToggle viewportModeToggle;
        final ModeToggle fontModeToggle;
        final MaterialButton typefaceSelectorButton;
        final MaterialButton scopeButton;
        final MaterialButton startButton;
        final MaterialButton restartButton;
        final MaterialButton stopButton;
        final MaterialButton dpisToggleButton;
        final MaterialButton fontHookDomainsButton;
        final MaterialButton disableButton;
        final MaterialButton saveButton;

        AppConfigDialogViews(android.widget.ImageView iconView,
                MaterialTextView titleView,
                MaterialTextView packageView,
                MaterialTextView statusView,
                MaterialTextView previewStatusView,
                TextInputLayout viewportInputLayout,
                TextInputEditText viewportInputView,
                TextInputLayout fontInputLayout,
                TextInputEditText fontInputView,
                ModeToggle viewportModeToggle,
                ModeToggle fontModeToggle,
                MaterialButton typefaceSelectorButton,
                MaterialButton scopeButton,
                MaterialButton startButton,
                MaterialButton restartButton,
                MaterialButton stopButton,
                MaterialButton dpisToggleButton,
                MaterialButton fontHookDomainsButton,
                MaterialButton disableButton,
                MaterialButton saveButton) {
            this.iconView = iconView;
            this.titleView = titleView;
            this.packageView = packageView;
            this.statusView = statusView;
            this.previewStatusView = previewStatusView;
            this.viewportInputLayout = viewportInputLayout;
            this.viewportInputView = viewportInputView;
            this.fontInputLayout = fontInputLayout;
            this.fontInputView = fontInputView;
            this.viewportModeToggle = viewportModeToggle;
            this.fontModeToggle = fontModeToggle;
            this.typefaceSelectorButton = typefaceSelectorButton;
            this.scopeButton = scopeButton;
            this.startButton = startButton;
            this.restartButton = restartButton;
            this.stopButton = stopButton;
            this.dpisToggleButton = dpisToggleButton;
            this.fontHookDomainsButton = fontHookDomainsButton;
            this.disableButton = disableButton;
            this.saveButton = saveButton;
        }
    }

    static final class AppConfigDialogState {
        boolean scopeSelected;
        boolean scopeKnown;
        boolean scopeRequestPending;
        boolean dpisEnabled;
        boolean previewFromGlobalPrefill;
        String previewFontHookDomainsRaw;
        String viewportApplyMode;
        String selectedTypefaceId;
        String viewportScaleInput = "";
        String viewportAbsoluteInput = "";

        AppConfigDialogState(boolean scopeSelected,
                boolean scopeKnown,
                boolean dpisEnabled,
                boolean previewFromGlobalPrefill,
                String previewFontHookDomainsRaw,
                String viewportApplyMode,
                String selectedTypefaceId,
                String initialViewportType,
                String initialViewportInput,
                String initialViewportScaleInput,
                String initialViewportAbsoluteInput) {
            this.scopeSelected = scopeSelected;
            this.scopeKnown = scopeKnown;
            this.dpisEnabled = dpisEnabled;
            this.previewFromGlobalPrefill = previewFromGlobalPrefill;
            this.previewFontHookDomainsRaw = previewFontHookDomainsRaw;
            this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
            this.selectedTypefaceId = selectedTypefaceId;
            this.viewportScaleInput = valueOrEmpty(initialViewportScaleInput);
            this.viewportAbsoluteInput = valueOrEmpty(initialViewportAbsoluteInput);
            if (ViewportTargetType.ABSOLUTE_DP.equals(
                    ViewportTargetType.normalize(initialViewportType))) {
                this.viewportAbsoluteInput = valueOrEmpty(initialViewportInput);
            } else {
                this.viewportScaleInput = valueOrEmpty(initialViewportInput);
            }
        }

        void updateViewportInput(String viewportTargetType, CharSequence input) {
            String normalized = ViewportTargetType.normalize(viewportTargetType);
            String value = input != null ? input.toString() : "";
            if (ViewportTargetType.ABSOLUTE_DP.equals(normalized)) {
                viewportAbsoluteInput = value;
                return;
            }
            viewportScaleInput = value;
        }

        String viewportInputFor(String viewportTargetType) {
            return ViewportTargetType.ABSOLUTE_DP.equals(
                    ViewportTargetType.normalize(viewportTargetType))
                            ? viewportAbsoluteInput
                            : viewportScaleInput;
        }

        void clearViewportInputs() {
            viewportScaleInput = "";
            viewportAbsoluteInput = "";
        }

        private static String valueOrEmpty(String value) {
            return value != null ? value : "";
        }
    }

    static final class AppConfigDialogActionStyle {
        final ColorStateList defaultActionBgTint;
        final int defaultActionStrokeWidth;
        final int defaultActionTextColor;

        AppConfigDialogActionStyle(ColorStateList defaultActionBgTint,
                int defaultActionStrokeWidth,
                int defaultActionTextColor) {
            this.defaultActionBgTint = defaultActionBgTint;
            this.defaultActionStrokeWidth = defaultActionStrokeWidth;
            this.defaultActionTextColor = defaultActionTextColor;
        }
    }

    private static final class TypefaceOption {
        static final String DISABLED_ID = "__disabled__";

        final String id;
        final String label;

        TypefaceOption(String id, String label) {
            this.id = id;
            this.label = label;
        }

        boolean isDisabled() {
            return DISABLED_ID.equals(id);
        }

        boolean matches(String selectedTypefaceId) {
            if (id == null) {
                return selectedTypefaceId == null || selectedTypefaceId.isBlank();
            }
            return id.equals(selectedTypefaceId);
        }
    }
}
