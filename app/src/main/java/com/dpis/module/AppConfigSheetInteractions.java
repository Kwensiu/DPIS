package com.dpis.module;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

final class AppConfigSheetInteractions {
    private final AppConfigDialogBinder binder;
    private final AppConfigDialogBinder.Host host;

    AppConfigSheetInteractions(AppConfigDialogBinder binder, AppConfigDialogBinder.Host host) {
        this.binder = binder;
        this.host = host;
    }

    void bind(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        bindDialogValidation(dialogView, item, views, state, style, systemHooksEnabled);
        bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled);
    }

    private void bindDialogValidation(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        android.widget.TextView.OnEditorActionListener doneListener = (v, actionId, event) -> {
            boolean isDoneAction = actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnterDown = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (!isDoneAction && !isEnterDown) {
                return false;
            }
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            return true;
        };
        views.viewportInputView.setOnEditorActionListener(doneListener);
        views.fontInputView.setOnEditorActionListener(doneListener);
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.updateViewportInput(
                        AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle), s);
                AppConfigDialogBinder.updateSaveButtonState(
                        views.viewportInputLayout, views.viewportInputView,
                        views.viewportModeToggle,
                        views.fontInputLayout, views.fontInputView, views.saveButton);
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        views.viewportInputView.addTextChangedListener(validationWatcher);
        views.fontInputView.addTextChangedListener(validationWatcher);
    }

    private void bindDialogActions(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        dialogView.setFocusable(true);
        dialogView.setFocusableInTouchMode(true);
        dialogView.setClickable(true);
        dialogView.setOnClickListener(
                v -> host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView));
        views.scopeButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.toggleScope(item, state.scopeSelected,
                    () -> {
                        state.scopeSelected = true;
                        binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
                    },
                    () -> {
                        state.scopeSelected = false;
                        binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
                    });
        });
        views.startButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.START);
        });
        views.restartButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.RESTART);
        });
        views.stopButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.STOP);
        });
        views.dpisToggleButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            boolean nextEnabled = !state.dpisEnabled;
            if (host.setDpisEnabled(item.packageName, nextEnabled)) {
                state.dpisEnabled = nextEnabled;
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            }
        });
        views.fontHookDomainsButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.showFontHookDomains(item,
                    () -> binder.bindFontHookDomainsButton(views.fontHookDomainsButton, item.packageName));
        });
        views.disableButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            views.viewportInputView.setText("");
            views.fontInputView.setText("");
            state.selectedTypefaceId = null;
            state.clearViewportInputs();
            binder.bindTypefaceSelector(views.typefaceSelectorButton, state.selectedTypefaceId);
            AppConfigDialogBinder.bindViewportModeToggle(
                    views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true);
            AppConfigDialogBinder.bindFontModeToggle(
                    views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            AppConfigDialogBinder.updateSaveButtonState(
                    views.viewportInputLayout, views.viewportInputView,
                    views.viewportModeToggle,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.saveButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            int[] result = host.saveAppConfig(
                    item,
                    views.viewportInputView,
                    views.fontInputView,
                    AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
                    AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
                    state.selectedTypefaceId,
                    state.viewportScaleInput,
                    state.viewportAbsoluteInput);
            if (result[0] == 1) {
                AppConfigDialogBinder.showSaveButtonFeedback(views.saveButton);
                binder.syncHyperOsNativeProxyAfterSave(item, views, state);
                binder.requestScopeAfterSuccessfulSave(
                        dialogView, item, views, state, style, systemHooksEnabled);
            }
            if (result[1] != 0) {
                host.showToast(result[1]);
            }
        });
        views.viewportModeToggle.container.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.toggleViewportMode(
                    views.viewportModeToggle, views.viewportInputView, state);
            binder.bindViewportInputHint(
                    views.viewportInputLayout,
                    AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle));
            AppConfigDialogBinder.updateSaveButtonState(
                    views.viewportInputLayout, views.viewportInputView,
                    views.viewportModeToggle,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.switchViewportTargetType(
                    views.viewportModeToggle, views.viewportInputView, state,
                    ViewportTargetType.RELATIVE_SCALE, true);
            binder.bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.RELATIVE_SCALE);
            AppConfigDialogBinder.updateSaveButtonState(
                    views.viewportInputLayout, views.viewportInputView,
                    views.viewportModeToggle,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.switchViewportTargetType(
                    views.viewportModeToggle, views.viewportInputView, state,
                    ViewportTargetType.ABSOLUTE_DP, true);
            binder.bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.ABSOLUTE_DP);
            AppConfigDialogBinder.updateSaveButtonState(
                    views.viewportInputLayout, views.viewportInputView,
                    views.viewportModeToggle,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.fontModeToggle.container.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.toggleFontMode(views.fontModeToggle);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.fontModeToggle.emulationLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.bindFontModeToggle(
                    views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.fontModeToggle.replaceLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.bindFontModeToggle(
                    views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        });
        views.typefaceSelectorButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            binder.showTypefaceSelector(views.typefaceSelectorButton, state,
                    () -> binder.refreshDialogState(
                            views, state, style, systemHooksEnabled, item));
        });
    }
}
