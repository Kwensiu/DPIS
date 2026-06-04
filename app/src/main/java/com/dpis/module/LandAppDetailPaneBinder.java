package com.dpis.module;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

final class LandAppDetailPaneBinder {
    interface Actions {
        void saveDraft(AppListItem item,
                Integer viewportValue,
                String viewportTargetType,
                Integer fontPercent,
                String fontMode,
                String selectedTypefaceId,
                String previewFontHookDomainsRaw,
                String viewportApplyMode,
                String viewportScaleInput,
                String viewportAbsoluteInput,
                boolean dpisEnabled,
                View root,
                MaterialButton saveButton);

        void showTypefaceSelector(AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                Runnable onChanged);

        void showHookDomains(AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                Runnable onChanged);

        void toggleScope(AppListItem item);

        boolean setDpisEnabled(String packageName, boolean enabled);

        void executeProcessAction(AppListItem item, AppConfigDialogBinder.ProcessAction action);

        String getFontHookDomainsButtonText(AppListItem item,
                boolean previewFromGlobalPrefill,
                String previewFontHookDomainsRaw);
    }

    private final Activity activity;
    private final Actions actions;

    LandAppDetailPaneBinder(Activity activity, Actions actions) {
        this.activity = activity;
        this.actions = actions;
    }

    void bind(View root, AppListItem item, boolean systemHooksEnabled) {
        ImageView iconView = root.findViewById(R.id.land_detail_app_icon);
        MaterialTextView titleView = root.findViewById(R.id.land_detail_title);
        MaterialTextView packageView = root.findViewById(R.id.land_detail_package);
        MaterialTextView statusView = root.findViewById(R.id.land_detail_status);
        MaterialTextView unsavedBadge = root.findViewById(R.id.land_detail_unsaved_badge);
        MaterialButton saveButton = root.findViewById(R.id.land_detail_save_button);

        iconView.setImageDrawable(item.icon);
        titleView.setText(item.label);
        packageView.setText(item.packageName);
        statusView.setText(formatStatus(item, systemHooksEnabled));
        unsavedBadge.setVisibility(item.previewFromGlobalPrefill ? View.VISIBLE : View.GONE);
        AppConfigDialogBinder.AppConfigDialogState state =
                AppConfigDialogBinder.AppConfigDialogState.fromItem(item);

        bindViewportEditor(root, item, state);
        bindFontEditor(root, item);
        WechatTargetFieldSheetBinder.bind(root, item, () -> updateSaveButtonState(root, saveButton));
        MaterialTextView typefaceValue = root.findViewById(R.id.land_detail_typeface_value);
        typefaceValue.setText(formatTypefaceValue(state.selectedTypefaceId));
        bindEditorRow(root, R.id.land_detail_typeface_row, () -> actions.showTypefaceSelector(item, state, () -> {
            typefaceValue.setText(formatTypefaceValue(state.selectedTypefaceId));
            updateSaveButtonState(root, saveButton);
        }));
        MaterialTextView hookValue = root.findViewById(R.id.land_detail_hook_chain_value);
        hookValue.setText(actions.getFontHookDomainsButtonText(
                item, state.previewFromGlobalPrefill, state.previewFontHookDomainsRaw));
        bindValueRow(root,
                R.id.land_detail_hook_chain_row,
                R.id.land_detail_hook_chain_value,
                hookValue.getText().toString(),
                () -> actions.showHookDomains(item, state, () -> hookValue.setText(
                        actions.getFontHookDomainsButtonText(
                                item, state.previewFromGlobalPrefill, state.previewFontHookDomainsRaw))));

        bindAdvancedButton(root,
                R.id.land_detail_scope_row,
                item.scopeKnown && item.inScope ? R.string.scope_remove_button : R.string.scope_add_button,
                () -> actions.toggleScope(item));
        bindAdvancedButton(root,
                R.id.land_detail_dpis_toggle_row,
                item.dpisEnabled ? R.string.dialog_dpis_disable_button : R.string.dialog_dpis_enable_button,
                () -> {
                    boolean nextEnabled = !state.dpisEnabled;
                    if (actions.setDpisEnabled(item.packageName, nextEnabled)) {
                        state.dpisEnabled = nextEnabled;
                        MaterialButton button = root.findViewById(R.id.land_detail_dpis_toggle_row);
                        button.setText(nextEnabled
                                ? R.string.dialog_dpis_disable_button
                                : R.string.dialog_dpis_enable_button);
                    }
                });
        bindAdvancedButton(root,
                R.id.land_detail_reset_row,
                R.string.dialog_disable_button,
                () -> resetDraft(root, item, state, typefaceValue, hookValue, saveButton));

        bindProcessButton(root, R.id.land_detail_start_button, item, AppConfigDialogBinder.ProcessAction.START);
        bindProcessButton(root, R.id.land_detail_restart_button, item, AppConfigDialogBinder.ProcessAction.RESTART);
        bindProcessButton(root, R.id.land_detail_stop_button, item, AppConfigDialogBinder.ProcessAction.STOP);
        bindSaveButton(root, item, state, saveButton);
        updateSaveButtonState(root, saveButton);
    }

    private void bindViewportEditor(View root,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state) {
        TextInputLayout inputLayout = root.findViewById(R.id.land_detail_viewport_input_layout);
        TextInputEditText input = root.findViewById(R.id.land_detail_viewport_input);
        AppConfigDialogBinder.ModeToggle toggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                root.findViewById(R.id.land_detail_viewport_mode_width_label));
        String initialType = AppConfigInputValidation.initialViewportTargetType(item.viewportTargetSpec);
        AppConfigDialogBinder.bindViewportModeToggle(toggle, initialType, false);
        bindViewportInputHint(inputLayout, initialType);
        String initialText = AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec);
        input.setText(initialText);
        bindViewportInput(inputLayout, input, toggle, state);
        bindViewportToggle(inputLayout, input, toggle, item, state);
    }

    private void bindFontEditor(View root, AppListItem item) {
        TextInputLayout inputLayout = root.findViewById(R.id.land_detail_font_scale_input_layout);
        TextInputEditText input = root.findViewById(R.id.land_detail_font_scale_input);
        AppConfigDialogBinder.ModeToggle toggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.land_detail_font_mode_toggle_button),
                root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                root.findViewById(R.id.land_detail_font_mode_system_label),
                root.findViewById(R.id.land_detail_font_mode_compat_label));
        AppConfigDialogBinder.bindFontModeToggle(
                toggle, AppConfigInputValidation.initialFontMode(item.fontMode), false);
        String initialText = FontApplyMode.isEnabled(item.fontMode) && item.fontScalePercent != null
                ? String.valueOf(item.fontScalePercent)
                : "";
        input.setText(initialText);
        bindFontInput(inputLayout, input, toggle, item);
        bindFontToggle(inputLayout, input, toggle, item);
    }

    private void bindViewportInput(TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppConfigDialogBinder.AppConfigDialogState state) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String raw = editable != null ? editable.toString() : "";
                String type = AppConfigDialogBinder.resolveViewportMode(toggle);
                state.updateViewportInput(type, raw);
                if (raw.trim().isEmpty()) {
                    inputLayout.setError(null);
                    updateSaveButtonState((View) inputLayout.getRootView(), null);
                    return;
                }
                if (!AppConfigInputValidation.isViewportInputValid(raw, type)) {
                    inputLayout.setError(activity.getString(R.string.status_save_invalid));
                    updateSaveButtonState((View) inputLayout.getRootView(), null);
                    return;
                }
                inputLayout.setError(null);
                updateSaveButtonState((View) inputLayout.getRootView(), null);
            }
        });
    }

    private void bindViewportToggle(TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state) {
        View.OnClickListener switcher = clicked -> {
            AppConfigDialogBinder.toggleViewportMode(toggle, input, state);
            String type = AppConfigDialogBinder.resolveViewportMode(toggle);
            bindViewportInputHint(inputLayout, type);
            String raw = input.getText() != null ? input.getText().toString() : "";
            if (AppConfigInputValidation.isViewportInputValid(raw, type)) {
                inputLayout.setError(null);
            } else {
                inputLayout.setError(activity.getString(R.string.status_save_invalid));
            }
            updateSaveButtonState((View) inputLayout.getRootView(), null);
        };
        toggle.container.setOnClickListener(switcher);
        toggle.emulationLabel.setOnClickListener(clicked -> switchViewportMode(
                inputLayout, input, toggle, item, state, ViewportTargetType.RELATIVE_SCALE));
        toggle.replaceLabel.setOnClickListener(clicked -> switchViewportMode(
                inputLayout, input, toggle, item, state, ViewportTargetType.ABSOLUTE_DP));
        TouchFeedbackBinder.bindPressHaptic(toggle.container);
    }

    private void switchViewportMode(TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            String targetType) {
        AppConfigDialogBinder.switchViewportTargetType(toggle, input, state, targetType, true);
        bindViewportInputHint(inputLayout, targetType);
        String raw = input.getText() != null ? input.getText().toString() : "";
        if (AppConfigInputValidation.isViewportInputValid(raw, targetType)) {
            inputLayout.setError(null);
        } else {
            inputLayout.setError(activity.getString(R.string.status_save_invalid));
        }
        updateSaveButtonState((View) inputLayout.getRootView(), null);
    }

    private void bindFontInput(TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String raw = editable != null ? editable.toString() : "";
                if (raw.trim().isEmpty()) {
                    inputLayout.setError(null);
                    updateSaveButtonState((View) inputLayout.getRootView(), null);
                    return;
                }
                if (!AppConfigInputValidation.isFontScaleInputValid(raw)) {
                    inputLayout.setError(activity.getString(R.string.status_save_invalid));
                    updateSaveButtonState((View) inputLayout.getRootView(), null);
                    return;
                }
                inputLayout.setError(null);
                updateSaveButtonState((View) inputLayout.getRootView(), null);
            }
        });
    }

    private void bindFontToggle(TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item) {
        View.OnClickListener switcher = clicked -> {
            AppConfigDialogBinder.toggleFontMode(toggle);
            saveFontModeIfValid(inputLayout, input, toggle, item);
        };
        toggle.container.setOnClickListener(switcher);
        toggle.emulationLabel.setOnClickListener(clicked -> {
            AppConfigDialogBinder.bindFontModeToggle(toggle, FontApplyMode.SYSTEM_EMULATION, true);
            saveFontModeIfValid(inputLayout, input, toggle, item);
        });
        toggle.replaceLabel.setOnClickListener(clicked -> {
            AppConfigDialogBinder.bindFontModeToggle(toggle, FontApplyMode.FIELD_REWRITE, true);
            saveFontModeIfValid(inputLayout, input, toggle, item);
        });
        TouchFeedbackBinder.bindPressHaptic(toggle.container);
    }

    private void saveFontModeIfValid(TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item) {
        String raw = input.getText() != null ? input.getText().toString() : "";
        if (raw.trim().isEmpty()) {
            inputLayout.setError(null);
            updateSaveButtonState((View) inputLayout.getRootView(), null);
            return;
        }
        if (!AppConfigInputValidation.isFontScaleInputValid(raw)) {
            inputLayout.setError(activity.getString(R.string.status_save_invalid));
            updateSaveButtonState((View) inputLayout.getRootView(), null);
            return;
        }
        inputLayout.setError(null);
        updateSaveButtonState((View) inputLayout.getRootView(), null);
    }

    private void bindViewportInputHint(TextInputLayout inputLayout, String viewportTargetType) {
        inputLayout.setHint(ViewportTargetType.RELATIVE_SCALE.equals(
                ViewportTargetType.normalize(viewportTargetType))
                        ? R.string.dialog_viewport_hint_scale
                        : R.string.dialog_viewport_hint_absolute);
    }

    private Integer parsePercentOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void bindValueRow(View root, int rowId, int valueId, String value, Runnable action) {
        View row = root.findViewById(rowId);
        MaterialTextView valueView = root.findViewById(valueId);
        valueView.setText(value);
        bindEditorRow(row, action);
    }

    private void bindAdvancedButton(View root,
            int buttonId,
            int textResId,
            Runnable action) {
        MaterialButton button = root.findViewById(buttonId);
        button.setText(textResId);
        bindEditorRow(button, action);
    }

    private void bindEditorRow(View root, int rowId, Runnable action) {
        bindEditorRow(root.findViewById(rowId), action);
    }

    private void bindEditorRow(View row, Runnable action) {
        if (row == null) {
            return;
        }
        row.setOnClickListener(clicked -> action.run());
        TouchFeedbackBinder.bindPressHaptic(row);
    }

    private void bindProcessButton(View root,
            int buttonId,
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action) {
        MaterialButton button = root.findViewById(buttonId);
        button.setOnClickListener(clicked -> actions.executeProcessAction(item, action));
        TouchFeedbackBinder.bindPressHaptic(button);
    }

    private void bindSaveButton(View root,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            MaterialButton saveButton) {
        if (saveButton == null) {
            return;
        }
        saveButton.setOnClickListener(clicked -> {
            if (!updateSaveButtonState(root, saveButton)) {
                return;
            }
            TextInputEditText viewportInput = root.findViewById(R.id.land_detail_viewport_input);
            TextInputEditText fontInput = root.findViewById(R.id.land_detail_font_scale_input);
            AppConfigDialogBinder.ModeToggle viewportToggle = new AppConfigDialogBinder.ModeToggle(
                    root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                    root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                    root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                    root.findViewById(R.id.land_detail_viewport_mode_width_label));
            AppConfigDialogBinder.ModeToggle fontToggle = new AppConfigDialogBinder.ModeToggle(
                    root.findViewById(R.id.land_detail_font_mode_toggle_button),
                    root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                    root.findViewById(R.id.land_detail_font_mode_system_label),
                    root.findViewById(R.id.land_detail_font_mode_compat_label));
            Integer viewportValue = parsePercentOrNull(textOf(viewportInput));
            Integer fontPercent = parsePercentOrNull(textOf(fontInput));
            actions.saveDraft(
                    item,
                    viewportValue,
                    AppConfigDialogBinder.resolveViewportMode(viewportToggle),
                    fontPercent,
                    fontPercent == null ? FontApplyMode.OFF : AppConfigDialogBinder.resolveFontMode(fontToggle),
                    state.selectedTypefaceId,
                    state.previewFontHookDomainsRaw,
                    state.viewportApplyMode,
                    state.viewportScaleInput,
                    state.viewportAbsoluteInput,
                    state.dpisEnabled,
                    root,
                    saveButton);
        });
        TouchFeedbackBinder.bindPressHaptic(saveButton);
    }

    private boolean updateSaveButtonState(View root, MaterialButton saveButton) {
        if (root == null) {
            return true;
        }
        MaterialButton resolvedSaveButton = saveButton != null
                ? saveButton
                : root.findViewById(R.id.land_detail_save_button);
        TextInputLayout viewportInputLayout = root.findViewById(R.id.land_detail_viewport_input_layout);
        TextInputEditText viewportInput = root.findViewById(R.id.land_detail_viewport_input);
        TextInputLayout fontInputLayout = root.findViewById(R.id.land_detail_font_scale_input_layout);
        TextInputEditText fontInput = root.findViewById(R.id.land_detail_font_scale_input);
        if (viewportInputLayout == null || viewportInput == null
                || fontInputLayout == null || fontInput == null || resolvedSaveButton == null) {
            return true;
        }
        AppConfigDialogBinder.ModeToggle viewportToggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                root.findViewById(R.id.land_detail_viewport_mode_width_label));
        AppConfigDialogBinder.ModeToggle fontToggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.land_detail_font_mode_toggle_button),
                root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                root.findViewById(R.id.land_detail_font_mode_system_label),
                root.findViewById(R.id.land_detail_font_mode_compat_label));
        boolean valid = AppConfigDialogBinder.updateSaveButtonState(
                viewportInputLayout,
                viewportInput,
                viewportToggle,
                fontInputLayout,
                fontInput,
                resolvedSaveButton);
        valid = valid && WechatTargetFieldSheetBinder.isInputValid(root);
        resolvedSaveButton.setEnabled(valid);
        return valid;
    }

    private String textOf(TextInputEditText input) {
        return input != null && input.getText() != null ? input.getText().toString() : "";
    }

    private void resetDraft(View root,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            MaterialTextView typefaceValue,
            MaterialTextView hookValue,
            MaterialButton saveButton) {
        TextInputEditText viewportInput = root.findViewById(R.id.land_detail_viewport_input);
        TextInputEditText fontInput = root.findViewById(R.id.land_detail_font_scale_input);
        TextInputLayout viewportInputLayout = root.findViewById(R.id.land_detail_viewport_input_layout);
        AppConfigDialogBinder.ModeToggle viewportToggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                root.findViewById(R.id.land_detail_viewport_mode_width_label));
        AppConfigDialogBinder.ModeToggle fontToggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.land_detail_font_mode_toggle_button),
                root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                root.findViewById(R.id.land_detail_font_mode_system_label),
                root.findViewById(R.id.land_detail_font_mode_compat_label));
        viewportInput.setText("");
        fontInput.setText("");
        WechatTargetFieldSheetBinder.clearDraft(root);
        state.selectedTypefaceId = null;
        state.clearViewportInputs();
        state.clearPreviewOnlyStateForReset();
        typefaceValue.setText(formatTypefaceValue(state.selectedTypefaceId));
        hookValue.setText(actions.getFontHookDomainsButtonText(
                item, state.previewFromGlobalPrefill, state.previewFontHookDomainsRaw));
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportToggle, ViewportTargetType.RELATIVE_SCALE, true);
        bindViewportInputHint(viewportInputLayout, ViewportTargetType.RELATIVE_SCALE);
        AppConfigDialogBinder.bindFontModeToggle(
                fontToggle, FontApplyMode.SYSTEM_EMULATION, true);
        updateSaveButtonState(root, saveButton);
    }

    private CharSequence formatStatus(AppListItem item, boolean systemHooksEnabled) {
        String status = AppStatusFormatter.formatCompact(
                activity.getResources(),
                item.inScope,
                item.scopeKnown,
                item.viewportTargetSpec,
                item.viewportMode,
                item.fontScalePercent,
                item.fontMode,
                item.typefaceId,
                item.dpisEnabled,
                item.hasAppSpecificConfig());
        int warnColor = com.google.android.material.color.MaterialColors.getColor(
                activity.findViewById(android.R.id.content),
                androidx.appcompat.R.attr.colorError);
        return AppStatusFormatter.applyConfigSegmentsWarnStyle(
                status,
                warnColor,
                AppStatusFormatter.shouldWarnViewportEmulation(
                        item.viewportTargetSpec, item.viewportMode, systemHooksEnabled, item.dpisEnabled),
                AppStatusFormatter.shouldWarnFontEmulation(
                        item.fontScalePercent, item.fontMode, systemHooksEnabled, item.dpisEnabled));
    }
    private String formatTypefaceValue(String selectedTypefaceId) {
        String typefaceId = selectedTypefaceId != null && !selectedTypefaceId.isBlank()
                ? selectedTypefaceId
                : null;
        if (typefaceId == null) {
            return activity.getString(R.string.dialog_typeface_default);
        }
        for (SystemFontEntry entry : SystemFontRegistry.listRecommendedFonts()) {
            if (typefaceId.equals(entry.id)) {
                return entry.displayName;
            }
        }
        FontLibraryEntry imported = ConfigStoreFactory
                .createFontLibraryForModuleApp(activity, DpisApplication.getXposedService())
                .findById(typefaceId);
        if (imported != null) {
            return imported.displayName;
        }
        return activity.getString(R.string.dialog_typeface_missing_named, typefaceId);
    }

}
