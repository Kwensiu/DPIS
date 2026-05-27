package com.dpis.module;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

final class AppConfigSheetModeValidationBinder {
    private final AppConfigDialogBinder binder;
    private final AppConfigDialogBinder.Host host;

    AppConfigSheetModeValidationBinder(AppConfigDialogBinder binder, AppConfigDialogBinder.Host host) {
        this.binder = binder;
        this.host = host;
    }

    void bindDialogValidation(View dialogView,
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
        TextWatcher viewportValidationWatcher = new TextWatcher() {
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
        TextWatcher fontValidationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
        views.viewportInputView.addTextChangedListener(viewportValidationWatcher);
        views.fontInputView.addTextChangedListener(fontValidationWatcher);
    }

    void bindModeToggles(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
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
    }
}
