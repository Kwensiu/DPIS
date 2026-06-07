package com.dpis.module;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;

final class LandAppDetailPaneBinder {

    interface Actions {

        void saveDraft(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                Integer viewportValue,
                String viewportTargetType,
                Integer fontPercent,
                String fontMode,
                String selectedTypefaceId,
                String draftFontHookDomainsRaw,
                String viewportApplyMode,
                boolean viewportApplyModeResetRequested,
                boolean fontHookDomainsResetRequested,
                String viewportScaleInput,
                String viewportAbsoluteInput,
                boolean dpisEnabled,
                View root,
                MaterialButton saveButton
        );

        void showTypefaceSelector(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                Runnable onChanged
        );

        void showHookDomains(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                Runnable onChanged
        );

        void toggleScope(
                AppListItem item,
                boolean currentlyInScope,
                Runnable onTurnedInScope,
                Runnable onTurnedOutScope
        );

        boolean setDpisEnabled(String packageName, boolean enabled);

        void executeProcessAction(
                AppListItem item,
                AppConfigDialogBinder.ProcessAction action
        );

        void onDraftStateChanged(AppConfigDialogBinder.AppConfigDialogState state);

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
        MaterialTextView packageView = root.findViewById(
                R.id.land_detail_package
        );
        MaterialTextView statusView = root.findViewById(
                R.id.land_detail_status
        );
        MaterialTextView unsavedBadge = root.findViewById(
                R.id.land_detail_unsaved_badge
        );
        MaterialButton saveButton = root.findViewById(
                R.id.land_detail_save_button
        );
        MaterialButton scopeButton = root.findViewById(
                R.id.land_detail_scope_row
        );
        MaterialButton dpisToggleButton = root.findViewById(
                R.id.land_detail_dpis_toggle_row
        );

        iconView.setImageDrawable(item.icon);
        titleView.setText(item.label);
        packageView.setText(item.packageName);
        statusView.setText(formatStatus(item, systemHooksEnabled));
        unsavedBadge.setVisibility(
                item.previewFromGlobalPrefill ? View.VISIBLE : View.GONE
        );
        AppConfigDialogBinder.AppConfigDialogState state
                = AppConfigDialogBinder.AppConfigDialogState.fromItem(item);
        root.setTag(R.id.land_detail_hook_chain_row, state);

        bindViewportEditor(root, item, state);
        bindFontEditor(root, item);
        WechatTargetFieldSheetBinder.bind(root, item, ()
                -> updateSaveButtonState(root, saveButton)
        );
        FormInputFocusBinder.bindDismissOnOutsideTouch(
                root.findViewById(R.id.land_detail_scroll),
                root,
                root.findViewById(R.id.land_detail_viewport_input),
                root.findViewById(R.id.land_detail_font_scale_input),
                root.findViewById(R.id.dialog_wechat_target_field_input)
        );
        MaterialTextView typefaceValue = root.findViewById(
                R.id.land_detail_typeface_value
        );
        typefaceValue.setText(formatTypefaceValue(state.selectedTypefaceId));
        bindEditorRow(root, R.id.land_detail_typeface_row, ()
                -> actions.showTypefaceSelector(item, state, () -> {
                    typefaceValue.setText(
                            formatTypefaceValue(state.selectedTypefaceId)
                    );
                    actions.onDraftStateChanged(state);
                    updateSaveButtonState(root, saveButton);
                })
        );
        MaterialTextView hookValue = root.findViewById(
                R.id.land_detail_hook_chain_value
        );
        hookValue.setText(formatHookChainValue(item, state));
        bindEditorRow(root, R.id.land_detail_hook_chain_row, ()
                -> actions.showHookDomains(currentFontConfigItem(root, item), state, () -> {
                    hookValue.setText(formatHookChainValue(item, state));
                    actions.onDraftStateChanged(state);
                    updateSaveButtonState(root, saveButton);
                })
        );

        ActionButtonStyle scopeStyle = ActionButtonStyle.capture(scopeButton);
        ActionButtonStyle dpisStyle = ActionButtonStyle.capture(
                dpisToggleButton
        );
        refreshScopeButton(scopeButton, state, scopeStyle);
        refreshDpisToggleButton(dpisToggleButton, state, dpisStyle);
        bindAdvancedButton(root, R.id.land_detail_scope_row, () -> {
            clearLandDetailInputFocus(root);
            actions.toggleScope(
                    item,
                    state.scopeSelected,
                    () -> {
                        state.scopeSelected = true;
                        refreshScopeButton(scopeButton, state, scopeStyle);
                    },
                    () -> {
                        state.scopeSelected = false;
                        refreshScopeButton(scopeButton, state, scopeStyle);
                    }
            );
        });
        bindAdvancedButton(root, R.id.land_detail_dpis_toggle_row, () -> {
            clearLandDetailInputFocus(root);
            if (state.previewFromGlobalPrefill) {
                return;
            }
            boolean nextEnabled = !state.dpisEnabled;
            if (actions.setDpisEnabled(item.packageName, nextEnabled)) {
                state.dpisEnabled = nextEnabled;
                refreshDpisToggleButton(dpisToggleButton, state, dpisStyle);
                statusView.setText(formatStatus(item, systemHooksEnabled));
            }
        });
        bindAdvancedButton(
                root,
                R.id.land_detail_reset_row,
                R.string.dialog_disable_button,
                ()
                -> resetDraft(
                        root,
                        item,
                        state,
                        typefaceValue,
                        hookValue,
                        saveButton
                )
        );
        bindAdaptiveAdvancedActions(root);

        bindProcessButton(
                root,
                R.id.land_detail_start_button,
                item,
                AppConfigDialogBinder.ProcessAction.START
        );
        bindProcessButton(
                root,
                R.id.land_detail_restart_button,
                item,
                AppConfigDialogBinder.ProcessAction.RESTART
        );
        bindProcessButton(
                root,
                R.id.land_detail_stop_button,
                item,
                AppConfigDialogBinder.ProcessAction.STOP
        );
        bindSaveButton(root, item, state, saveButton);
        bindAdaptiveActionDock(root, saveButton);
        setCleanStateSignature(
                root,
                item.previewFromGlobalPrefill
                        ? emptyStateSignature()
                        : buildStateSignature(root)
        );
        updateUnsavedBadge(root);
        updateSaveButtonState(root, saveButton);
    }

    private void bindViewportEditor(
            View root,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        TextInputLayout inputLayout = root.findViewById(
                R.id.land_detail_viewport_input_layout
        );
        TextInputEditText input = root.findViewById(
                R.id.land_detail_viewport_input
        );
        AppConfigDialogBinder.ModeToggle toggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                        root.findViewById(R.id.land_detail_viewport_mode_width_label)
                );
        String initialType = AppConfigInputValidation.initialViewportTargetType(
                item.viewportTargetSpec
        );
        AppConfigDialogBinder.bindViewportModeToggle(
                toggle,
                initialType,
                false
        );
        bindViewportInputHint(inputLayout, initialType);
        String initialText = AppConfigInputValidation.formatViewportInput(
                item.viewportTargetSpec
        );
        input.setText(initialText);
        bindViewportInput(inputLayout, input, toggle, state);
        bindViewportToggle(inputLayout, input, toggle, item, state);
    }

    private void bindFontEditor(View root, AppListItem item) {
        TextInputLayout inputLayout = root.findViewById(
                R.id.land_detail_font_scale_input_layout
        );
        TextInputEditText input = root.findViewById(
                R.id.land_detail_font_scale_input
        );
        AppConfigDialogBinder.ModeToggle toggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_font_mode_toggle_button),
                        root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_font_mode_system_label),
                        root.findViewById(R.id.land_detail_font_mode_compat_label)
                );
        AppConfigDialogBinder.bindFontModeToggle(
                toggle,
                AppConfigInputValidation.initialFontMode(item.fontMode),
                false
        );
        String initialText
                = FontApplyMode.isEnabled(item.fontMode)
                && item.fontScalePercent != null
                        ? String.valueOf(item.fontScalePercent)
                        : "";
        input.setText(initialText);
        bindFontInput(inputLayout, input, toggle, item);
        bindFontToggle(inputLayout, input, toggle, item);
    }

    private void bindViewportInput(
            TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        input.addTextChangedListener(
                new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String raw = editable != null ? editable.toString() : "";
                String type = AppConfigDialogBinder.resolveViewportMode(
                        toggle
                );
                state.updateViewportInput(type, raw);
                updateSaveButtonState(
                        (View) inputLayout.getRootView(),
                        null
                );
            }
        }
        );
    }

    private void bindViewportToggle(
            TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        View.OnClickListener switcher = clicked -> {
            FormInputFocusBinder.clearFocusAndHideIme(
                    (View) inputLayout.getRootView(),
                    input
            );
            AppConfigDialogBinder.toggleViewportMode(toggle, input, state);
            String type = AppConfigDialogBinder.resolveViewportMode(toggle);
            bindViewportInputHint(inputLayout, type);
            updateSaveButtonState((View) inputLayout.getRootView(), null);
        };
        toggle.container.setOnClickListener(switcher);
        toggle.emulationLabel.setOnClickListener(clicked
                -> switchViewportMode(
                        inputLayout,
                        input,
                        toggle,
                        item,
                        state,
                        ViewportTargetType.RELATIVE_SCALE
                )
        );
        toggle.replaceLabel.setOnClickListener(clicked
                -> switchViewportMode(
                        inputLayout,
                        input,
                        toggle,
                        item,
                        state,
                        ViewportTargetType.ABSOLUTE_DP
                )
        );
        TouchFeedbackBinder.bindPressHaptic(toggle.container);
    }

    private void switchViewportMode(
            TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            String targetType
    ) {
        FormInputFocusBinder.clearFocusAndHideIme(
                (View) inputLayout.getRootView(),
                input
        );
        AppConfigDialogBinder.switchViewportTargetType(
                toggle,
                input,
                state,
                targetType,
                true
        );
        bindViewportInputHint(inputLayout, targetType);
        updateSaveButtonState((View) inputLayout.getRootView(), null);
    }

    private void bindFontInput(
            TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item
    ) {
        input.addTextChangedListener(
                new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateSaveButtonState(
                        (View) inputLayout.getRootView(),
                        null
                );
            }
        }
        );
    }

    private void bindFontToggle(
            TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item
    ) {
        View.OnClickListener switcher = clicked -> {
            FormInputFocusBinder.clearFocusAndHideIme(
                    (View) inputLayout.getRootView(),
                    input
            );
            AppConfigDialogBinder.toggleFontMode(toggle);
            saveFontModeIfValid(inputLayout, input, toggle, item);
        };
        toggle.container.setOnClickListener(switcher);
        toggle.emulationLabel.setOnClickListener(clicked -> {
            FormInputFocusBinder.clearFocusAndHideIme(
                    (View) inputLayout.getRootView(),
                    input
            );
            AppConfigDialogBinder.bindFontModeToggle(
                    toggle,
                    FontApplyMode.SYSTEM_EMULATION,
                    true
            );
            saveFontModeIfValid(inputLayout, input, toggle, item);
        });
        toggle.replaceLabel.setOnClickListener(clicked -> {
            FormInputFocusBinder.clearFocusAndHideIme(
                    (View) inputLayout.getRootView(),
                    input
            );
            AppConfigDialogBinder.bindFontModeToggle(
                    toggle,
                    FontApplyMode.FIELD_REWRITE,
                    true
            );
            saveFontModeIfValid(inputLayout, input, toggle, item);
        });
        TouchFeedbackBinder.bindPressHaptic(toggle.container);
    }

    private void saveFontModeIfValid(
            TextInputLayout inputLayout,
            TextInputEditText input,
            AppConfigDialogBinder.ModeToggle toggle,
            AppListItem item
    ) {
        updateSaveButtonState((View) inputLayout.getRootView(), null);
    }

    private void bindViewportInputHint(
            TextInputLayout inputLayout,
            String viewportTargetType
    ) {
        inputLayout.setHint(
                ViewportTargetType.RELATIVE_SCALE.equals(
                        ViewportTargetType.normalize(viewportTargetType)
                )
                ? R.string.dialog_viewport_hint_scale
                : R.string.dialog_viewport_hint_absolute
        );
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

    private void bindAdvancedButton(
            View root,
            int buttonId,
            int textResId,
            Runnable action
    ) {
        MaterialButton button = root.findViewById(buttonId);
        button.setText(textResId);
        bindEditorRow(button, action);
    }

    private void bindAdvancedButton(View root, int buttonId, Runnable action) {
        bindEditorRow(root.findViewById(buttonId), action);
    }

    private void refreshScopeButton(
            MaterialButton scopeButton,
            AppConfigDialogBinder.AppConfigDialogState state,
            ActionButtonStyle style
    ) {
        if (scopeButton == null || state == null || style == null) {
            return;
        }
        int activeBgColor = MaterialColors.getColor(
                scopeButton,
                com.google.android.material.R.attr.colorSecondaryContainer
        );
        int activeFgColor = MaterialColors.getColor(
                scopeButton,
                com.google.android.material.R.attr.colorOnSecondaryContainer
        );
        int scopeTextRes = state.scopeKnown
                ? (state.scopeSelected
                        ? R.string.scope_remove_button
                        : R.string.scope_add_button)
                : R.string.scope_add_button;
        boolean activeScopeStyle = state.scopeKnown && state.scopeSelected;
        scopeButton.setIcon(null);
        scopeButton.setText(scopeTextRes);
        scopeButton.setBackgroundTintList(
                activeScopeStyle
                        ? ColorStateList.valueOf(activeBgColor)
                        : style.defaultBgTint
        );
        scopeButton.setTextColor(
                activeScopeStyle ? activeFgColor : style.defaultTextColor
        );
        scopeButton.setStrokeWidth(
                activeScopeStyle ? 0 : style.defaultStrokeWidth
        );
        scopeButton.setContentDescription(activity.getString(scopeTextRes));
        scopeButton.setEnabled(state.scopeKnown);
        scopeButton.setAlpha(state.scopeKnown ? 1f : 0.6f);
    }

    private void refreshDpisToggleButton(
            MaterialButton dpisToggleButton,
            AppConfigDialogBinder.AppConfigDialogState state,
            ActionButtonStyle style
    ) {
        if (dpisToggleButton == null || state == null || style == null) {
            return;
        }
        int activeBgColor = MaterialColors.getColor(
                dpisToggleButton,
                com.google.android.material.R.attr.colorSecondaryContainer
        );
        int activeFgColor = MaterialColors.getColor(
                dpisToggleButton,
                com.google.android.material.R.attr.colorOnSecondaryContainer
        );
        String buttonText = activity.getString(
                state.dpisEnabled
                        ? R.string.dialog_dpis_disable_button
                        : R.string.dialog_dpis_enable_button
        );
        boolean enabledActive = state.dpisEnabled;
        dpisToggleButton.setIcon(null);
        dpisToggleButton.setText(buttonText);
        dpisToggleButton.setBackgroundTintList(
                enabledActive
                        ? ColorStateList.valueOf(activeBgColor)
                        : style.defaultBgTint
        );
        dpisToggleButton.setTextColor(
                enabledActive ? activeFgColor : style.defaultTextColor
        );
        dpisToggleButton.setStrokeWidth(
                enabledActive ? 0 : style.defaultStrokeWidth
        );
        dpisToggleButton.setContentDescription(buttonText);
        dpisToggleButton.setEnabled(!state.previewFromGlobalPrefill);
        dpisToggleButton.setAlpha(state.previewFromGlobalPrefill ? 0.6f : 1f);
    }

    private void clearLandDetailInputFocus(View root) {
        if (root == null) {
            return;
        }
        FormInputFocusBinder.clearFocusAndHideIme(
                root,
                root.findViewById(R.id.land_detail_viewport_input),
                root.findViewById(R.id.land_detail_font_scale_input),
                root.findViewById(R.id.dialog_wechat_target_field_input)
        );
    }

    private void bindAdaptiveAdvancedActions(View root) {
        if (root == null) {
            return;
        }
        root.addOnLayoutChangeListener(
                (
                        view,
                        left,
                        top,
                        right,
                        bottom,
                        oldLeft,
                        oldTop,
                        oldRight,
                        oldBottom) -> updateAdvancedActionsLayout(view)
        );
        root.post(() -> updateAdvancedActionsLayout(root));
    }

    private void updateAdvancedActionsLayout(View root) {
        if (root == null || root.getWidth() <= 0) {
            return;
        }
        LinearLayout primaryRow = root.findViewById(
                R.id.land_detail_advanced_primary_row
        );
        LinearLayout resetRow = root.findViewById(
                R.id.land_detail_advanced_reset_row
        );
        MaterialButton resetButton = root.findViewById(
                R.id.land_detail_reset_row
        );
        if (primaryRow == null || resetRow == null || resetButton == null) {
            return;
        }
        int buttonMinWidth = activity
                .getResources()
                .getDimensionPixelSize(
                        R.dimen.land_app_detail_advanced_button_wrap_min_width
                );
        int spacing = activity
                .getResources()
                .getDimensionPixelSize(
                        R.dimen.land_app_detail_action_button_spacing
                );
        int contentHorizontalPadding
                = activity
                        .getResources()
                        .getDimensionPixelSize(
                                R.dimen.land_app_detail_padding_horizontal
                        )
                * 2
                + activity
                        .getResources()
                        .getDimensionPixelSize(R.dimen.land_app_detail_card_padding)
                * 2;
        int threeButtonRequiredWidth
                = buttonMinWidth * 3 + spacing * 2 + contentHorizontalPadding;
        boolean wrapReset = root.getWidth() < threeButtonRequiredWidth;
        if (wrapReset && resetButton.getParent() != resetRow) {
            primaryRow.removeView(resetButton);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            resetRow.addView(resetButton, params);
            resetRow.setVisibility(View.VISIBLE);
        } else if (!wrapReset && resetButton.getParent() != primaryRow) {
            resetRow.removeView(resetButton);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );
            params.setMarginStart(spacing);
            primaryRow.addView(resetButton, params);
            resetRow.setVisibility(View.GONE);
        } else {
            resetRow.setVisibility(wrapReset ? View.VISIBLE : View.GONE);
        }
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

    private void bindProcessButton(
            View root,
            int buttonId,
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action
    ) {
        MaterialButton button = root.findViewById(buttonId);
        button.setOnClickListener(clicked
                -> actions.executeProcessAction(item, action)
        );
        TouchFeedbackBinder.bindPressHaptic(button);
    }

    private AppListItem currentFontConfigItem(View root, AppListItem item) {
        if (root == null || item == null) {
            return item;
        }
        TextInputEditText fontInput = root.findViewById(
                R.id.land_detail_font_scale_input
        );
        AppConfigDialogBinder.ModeToggle fontToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_font_mode_toggle_button),
                        root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_font_mode_system_label),
                        root.findViewById(R.id.land_detail_font_mode_compat_label)
                );
        Integer fontScalePercent = AppConfigInputValidation.parseFontScalePercentOrNull(
                inputTextOf(fontInput)
        );
        String fontMode = fontScalePercent == null
                ? FontApplyMode.OFF
                : AppConfigDialogBinder.resolveFontMode(fontToggle);
        return item.withFontConfig(fontScalePercent, fontMode);
    }

    private void bindSaveButton(
            View root,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            MaterialButton saveButton
    ) {
        if (saveButton == null) {
            return;
        }
        saveButton.setOnClickListener(clicked -> {
            if (!updateSaveButtonState(root, saveButton)) {
                return;
            }
            TextInputEditText viewportInput = root.findViewById(
                    R.id.land_detail_viewport_input
            );
            TextInputEditText fontInput = root.findViewById(
                    R.id.land_detail_font_scale_input
            );
            AppConfigDialogBinder.ModeToggle viewportToggle
                    = new AppConfigDialogBinder.ModeToggle(
                            root.findViewById(
                                    R.id.land_detail_viewport_mode_toggle_button
                            ),
                            root.findViewById(
                                    R.id.land_detail_viewport_mode_toggle_thumb
                            ),
                            root.findViewById(
                                    R.id.land_detail_viewport_mode_scale_label
                            ),
                            root.findViewById(
                                    R.id.land_detail_viewport_mode_width_label
                            )
                    );
            AppConfigDialogBinder.ModeToggle fontToggle
                    = new AppConfigDialogBinder.ModeToggle(
                            root.findViewById(R.id.land_detail_font_mode_toggle_button),
                            root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                            root.findViewById(R.id.land_detail_font_mode_system_label),
                            root.findViewById(R.id.land_detail_font_mode_compat_label)
                    );
            Integer viewportValue = parsePercentOrNull(inputTextOf(viewportInput));
            Integer fontPercent = parsePercentOrNull(inputTextOf(fontInput));
            actions.saveDraft(
                    item,
                    state,
                    viewportValue,
                    AppConfigDialogBinder.resolveViewportMode(viewportToggle),
                    fontPercent,
                    fontPercent == null
                            ? FontApplyMode.OFF
                            : AppConfigDialogBinder.resolveFontMode(fontToggle),
                    state.selectedTypefaceId,
                    state.draftFontHookDomainsRaw,
                    state.viewportApplyMode,
                    state.viewportApplyModeResetRequested,
                    state.fontHookDomainsResetRequested,
                    state.viewportScaleInput,
                    state.viewportAbsoluteInput,
                    state.dpisEnabled,
                    root,
                    saveButton
            );
        });
        TouchFeedbackBinder.bindPressHaptic(saveButton);
    }

    private void bindAdaptiveActionDock(View root, MaterialButton saveButton) {
        if (root == null || saveButton == null) {
            return;
        }
        root.addOnLayoutChangeListener(
                (
                        view,
                        left,
                        top,
                        right,
                        bottom,
                        oldLeft,
                        oldTop,
                        oldRight,
                        oldBottom) -> updateSaveButtonPresentation(view, saveButton)
        );
        root.post(() -> updateSaveButtonPresentation(root, saveButton));
    }

    private void updateSaveButtonPresentation(
            View root,
            MaterialButton saveButton
    ) {
        int rootWidth = root != null ? root.getWidth() : 0;
        if (rootWidth <= 0 || saveButton == null) {
            return;
        }
        int dockHorizontalMargins
                = activity
                        .getResources()
                        .getDimensionPixelSize(
                                R.dimen.land_app_detail_dock_margin_horizontal
                        ) * 2;
        int dockPadding
                = activity
                        .getResources()
                        .getDimensionPixelSize(R.dimen.land_app_detail_dock_padding)
                * 2;
        int saveExpandedWidth = activity
                .getResources()
                .getDimensionPixelSize(
                        R.dimen.land_app_detail_dock_save_expanded_min_width
                );
        int saveCompactWidth = activity
                .getResources()
                .getDimensionPixelSize(R.dimen.land_app_detail_dock_button_height);
        int spacing = activity
                .getResources()
                .getDimensionPixelSize(R.dimen.land_app_detail_dock_button_spacing);
        int processButtonWidth
                = activity
                        .getResources()
                        .getDimensionPixelSize(
                                R.dimen.land_app_detail_dock_process_button_min_width
                        ) * 3;
        int processDividerWidth
                = activity
                        .getResources()
                        .getDimensionPixelSize(
                                R.dimen.land_app_detail_dock_process_divider_width
                        ) * 2;
        int expandedRequiredWidth
                = dockHorizontalMargins
                + dockPadding
                + saveExpandedWidth
                + spacing
                + processButtonWidth
                + processDividerWidth;
        boolean compact = rootWidth < expandedRequiredWidth;
        int targetWidth = compact ? saveCompactWidth : saveExpandedWidth;
        ViewGroup.LayoutParams params = saveButton.getLayoutParams();
        if (params != null && params.width != targetWidth) {
            params.width = targetWidth;
            saveButton.setLayoutParams(params);
        }
        saveButton.setMinWidth(targetWidth);
        saveButton.setMinimumWidth(targetWidth);
        saveButton.setIconPadding(0);
        saveButton.setContentDescription(
                activity.getString(R.string.status_save_button)
        );
        if (compact) {
            saveButton.setText(null);
            saveButton.setIconResource(R.drawable.ic_save_24dp);
            saveButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        } else {
            saveButton.setIcon(null);
            saveButton.setText(R.string.status_save_button);
        }
    }

    private static boolean updateSaveButtonState(
            View root,
            MaterialButton saveButton
    ) {
        if (root == null) {
            return true;
        }
        MaterialButton resolvedSaveButton
                = saveButton != null
                        ? saveButton
                        : root.findViewById(R.id.land_detail_save_button);
        TextInputLayout viewportInputLayout = root.findViewById(
                R.id.land_detail_viewport_input_layout
        );
        TextInputEditText viewportInput = root.findViewById(
                R.id.land_detail_viewport_input
        );
        TextInputLayout fontInputLayout = root.findViewById(
                R.id.land_detail_font_scale_input_layout
        );
        TextInputEditText fontInput = root.findViewById(
                R.id.land_detail_font_scale_input
        );
        if (viewportInputLayout == null
                || viewportInput == null
                || fontInputLayout == null
                || fontInput == null
                || resolvedSaveButton == null) {
            return true;
        }
        AppConfigDialogBinder.ModeToggle viewportToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                        root.findViewById(R.id.land_detail_viewport_mode_width_label)
                );
        AppConfigDialogBinder.ModeToggle fontToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_font_mode_toggle_button),
                        root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_font_mode_system_label),
                        root.findViewById(R.id.land_detail_font_mode_compat_label)
                );
        boolean valid = AppConfigDialogBinder.updateSaveButtonState(
                viewportInputLayout,
                viewportInput,
                viewportToggle,
                fontInputLayout,
                fontInput,
                resolvedSaveButton
        );
        valid = valid && WechatTargetFieldSheetBinder.isInputValid(root);
        boolean hasChanged = hasUnsavedChanges(root);
        resolvedSaveButton.setEnabled(hasChanged && valid);
        updateUnsavedBadge(root);
        return valid;
    }

    static void markDraftSaved(View root, MaterialButton saveButton) {
        if (root == null) {
            return;
        }
        setCleanStateSignature(root, buildStateSignature(root));
        if (saveButton != null) {
            saveButton.setEnabled(false);
        }
        updateUnsavedBadge(root);
    }

    static AppConfigDialogBinder.AppConfigDialogState stateFor(View root) {
        Object tag = root != null
                ? root.getTag(R.id.land_detail_hook_chain_row)
                : null;
        return tag instanceof AppConfigDialogBinder.AppConfigDialogState
                ? (AppConfigDialogBinder.AppConfigDialogState) tag
                : null;
    }

    static void applyRetainedDraft(
            Activity activity,
            View root,
            AppListItem item,
            String selectedTypefaceId,
            String draftFontHookDomainsRaw,
            String viewportApplyMode,
            boolean fontHookDomainsResetRequested,
            boolean viewportApplyModeResetRequested
    ) {
        AppConfigDialogBinder.AppConfigDialogState state = stateFor(root);
        if (activity == null || root == null || state == null) {
            return;
        }
        state.selectedTypefaceId = selectedTypefaceId;
        state.draftFontHookDomainsRaw = draftFontHookDomainsRaw;
        state.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        state.fontHookDomainsResetRequested = fontHookDomainsResetRequested;
        state.viewportApplyModeResetRequested = viewportApplyModeResetRequested;

        MaterialTextView typefaceValue = root.findViewById(
                R.id.land_detail_typeface_value
        );
        if (typefaceValue != null) {
            typefaceValue.setText(formatTypefaceValue(activity, selectedTypefaceId));
        }
        MaterialTextView hookValue = root.findViewById(
                R.id.land_detail_hook_chain_value
        );
        if (hookValue != null) {
            hookValue.setText(formatHookChainValue(activity, item, state));
        }
        updateSaveButtonState(
                root,
                root.findViewById(R.id.land_detail_save_button)
        );
    }

    private static String inputTextOf(TextInputEditText input) {
        return input != null && input.getText() != null
                ? input.getText().toString()
                : "";
    }

    private static String labelTextOf(MaterialTextView textView) {
        return textView != null && textView.getText() != null
                ? textView.getText().toString()
                : "";
    }

    private static boolean hasUnsavedChanges(View root) {
        String cleanStateSignature = cleanStateSignature(root);
        return !cleanStateSignature.equals(buildStateSignature(root));
    }

    private static void updateUnsavedBadge(View root) {
        MaterialTextView badge = root.findViewById(
                R.id.land_detail_unsaved_badge
        );
        if (badge != null) {
            badge.setVisibility(
                    hasUnsavedChanges(root) ? View.VISIBLE : View.GONE
            );
        }
    }

    private static void setCleanStateSignature(View root, String signature) {
        if (root != null) {
            root.setTag(
                    R.id.land_detail_save_button,
                    signature != null ? signature : ""
            );
        }
    }

    private static String cleanStateSignature(View root) {
        Object tag = root != null
                ? root.getTag(R.id.land_detail_save_button)
                : null;
        return tag instanceof String ? (String) tag : "";
    }

    private static String buildStateSignature(View root) {
        String viewportText = inputTextOf(
                root.findViewById(R.id.land_detail_viewport_input)
        );
        String fontText = inputTextOf(
                root.findViewById(R.id.land_detail_font_scale_input)
        );
        String typefaceText = labelTextOf(
                root.findViewById(R.id.land_detail_typeface_value)
        );
        String hookChainText = labelTextOf(
                root.findViewById(R.id.land_detail_hook_chain_value)
        );
        AppConfigDialogBinder.ModeToggle viewportToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                        root.findViewById(R.id.land_detail_viewport_mode_width_label)
                );
        AppConfigDialogBinder.ModeToggle fontToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_font_mode_toggle_button),
                        root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_font_mode_system_label),
                        root.findViewById(R.id.land_detail_font_mode_compat_label)
                );
        return String.join(
                "|",
                viewportText,
                AppConfigDialogBinder.resolveViewportMode(viewportToggle),
                fontText,
                AppConfigDialogBinder.resolveFontMode(fontToggle),
                typefaceText,
                hookChainText
        );
    }

    private static String emptyStateSignature() {
        return String.join(
                "|",
                "",
                ViewportTargetType.RELATIVE_SCALE,
                "",
                FontApplyMode.SYSTEM_EMULATION,
                "",
                ""
        );
    }

    private void resetDraft(
            View root,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            MaterialTextView typefaceValue,
            MaterialTextView hookValue,
            MaterialButton saveButton
    ) {
        TextInputEditText viewportInput = root.findViewById(
                R.id.land_detail_viewport_input
        );
        TextInputEditText fontInput = root.findViewById(
                R.id.land_detail_font_scale_input
        );
        TextInputLayout viewportInputLayout = root.findViewById(
                R.id.land_detail_viewport_input_layout
        );
        AppConfigDialogBinder.ModeToggle viewportToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
                        root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                        root.findViewById(R.id.land_detail_viewport_mode_width_label)
                );
        AppConfigDialogBinder.ModeToggle fontToggle
                = new AppConfigDialogBinder.ModeToggle(
                        root.findViewById(R.id.land_detail_font_mode_toggle_button),
                        root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                        root.findViewById(R.id.land_detail_font_mode_system_label),
                        root.findViewById(R.id.land_detail_font_mode_compat_label)
                );
        viewportInput.setText("");
        fontInput.setText("");
        WechatTargetFieldSheetBinder.clearDraft(root);
        state.selectedTypefaceId = null;
        state.clearViewportInputs();
        state.clearHookChainStateForReset();
        typefaceValue.setText(formatTypefaceValue(state.selectedTypefaceId));
        hookValue.setText(formatHookChainValue(item, state));
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportToggle,
                ViewportTargetType.RELATIVE_SCALE,
                true
        );
        bindViewportInputHint(
                viewportInputLayout,
                ViewportTargetType.RELATIVE_SCALE
        );
        AppConfigDialogBinder.bindFontModeToggle(
                fontToggle,
                FontApplyMode.SYSTEM_EMULATION,
                true
        );
        updateSaveButtonState(root, saveButton);
    }

    private CharSequence formatStatus(
            AppListItem item,
            boolean systemHooksEnabled
    ) {
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
                item.hasAppSpecificConfig()
        );
        int warnColor
                = com.google.android.material.color.MaterialColors.getColor(
                        activity.findViewById(android.R.id.content),
                        androidx.appcompat.R.attr.colorError
                );
        return AppStatusFormatter.applyConfigSegmentsWarnStyle(
                status,
                warnColor,
                AppStatusFormatter.shouldWarnViewportEmulation(
                        item.viewportTargetSpec,
                        item.viewportMode,
                        systemHooksEnabled,
                        item.dpisEnabled
                ),
                AppStatusFormatter.shouldWarnFontEmulation(
                        item.fontScalePercent,
                        item.fontMode,
                        systemHooksEnabled,
                        item.dpisEnabled
                )
        );
    }

    private String formatTypefaceValue(String selectedTypefaceId) {
        return formatTypefaceValue(activity, selectedTypefaceId);
    }

    private static String formatTypefaceValue(
            Activity activity,
            String selectedTypefaceId
    ) {
        String typefaceId
                = selectedTypefaceId != null && !selectedTypefaceId.isBlank()
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
        FontLibraryEntry imported
                = ConfigStoreFactory.createActiveFontLibraryStore(
                        activity,
                        DpisApplication.getXposedService()
                ).findById(typefaceId);
        if (imported != null) {
            return imported.displayName;
        }
        return activity.getString(
                R.string.dialog_typeface_missing_named,
                typefaceId
        );
    }

    private String formatHookChainValue(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        return formatHookChainValue(activity, item, state);
    }

    private static String formatHookChainValue(
            Activity activity,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        ArrayList<String> parts = new ArrayList<>();
        String viewportMode
                = state != null
                        ? ViewportApplyMode.normalize(state.viewportApplyMode)
                        : ViewportApplyMode.OFF;
        if (ViewportApplyMode.SYSTEM.equals(viewportMode)) {
            parts.add(
                    activity.getString(
                            R.string.land_detail_hook_chain_viewport_system
                    )
            );
        } else if (ViewportApplyMode.COMPAT.equals(viewportMode)) {
            parts.add(
                    activity.getString(
                            R.string.land_detail_hook_chain_viewport_compat
                    )
            );
        }

        HookDomainOverride override = resolveHookDomainOverride(activity, item, state);
        if (override.customPathEnabled) {
            int selectedCount
                    = FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                            override.enabledKnownDomains
                    ).size();
            int totalCount
                    = FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size();
            parts.add(
                    activity.getString(
                            R.string.land_detail_hook_chain_font_count,
                            selectedCount,
                            totalCount
                    )
            );
        }
        if (parts.isEmpty()) {
            return activity.getString(R.string.land_detail_hook_chain_default);
        }
        return String.join(
                activity.getString(R.string.land_detail_hook_chain_separator),
                parts
        );
    }

    private static HookDomainOverride resolveHookDomainOverride(
            Activity activity,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic();
        }
        if (state != null
                && (state.previewFromGlobalPrefill
                        || state.draftFontHookDomainsRaw != null)) {
            return HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw);
        }
        if (item == null || item.packageName == null || item.packageName.isBlank()) {
            return HookDomainOverride.automatic();
        }
        DpiConfigStore store = DpisApplication.getActiveHookConfigStore(activity);
        return new HookDomainOverrideStore(store).read(item.packageName);
    }

    private static final class ActionButtonStyle {

        final ColorStateList defaultBgTint;
        final int defaultStrokeWidth;
        final int defaultTextColor;

        private ActionButtonStyle(
                ColorStateList defaultBgTint,
                int defaultStrokeWidth,
                int defaultTextColor
        ) {
            this.defaultBgTint = defaultBgTint;
            this.defaultStrokeWidth = defaultStrokeWidth;
            this.defaultTextColor = defaultTextColor;
        }

        static ActionButtonStyle capture(MaterialButton button) {
            if (button == null) {
                return null;
            }
            return new ActionButtonStyle(
                    button.getBackgroundTintList(),
                    button.getStrokeWidth(),
                    MaterialColors.getColor(
                            button,
                            androidx.appcompat.R.attr.colorPrimary
                    )
            );
        }
    }
}
