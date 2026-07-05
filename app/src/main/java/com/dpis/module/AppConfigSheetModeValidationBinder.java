package com.dpis.module;

import com.dpis.module.quirks.WechatDpiSheetBinder;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.google.android.material.textfield.TextInputEditText;
import com.dpis.module.ui.FormInputFocusBinder;

final class AppConfigSheetModeValidationBinder {
    private final AppConfigDialogBinder binder;
    private final AppConfigDialogBinder.Host host;

    AppConfigSheetModeValidationBinder(
            AppConfigDialogBinder binder,
            AppConfigDialogBinder.Host host
    ) {
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
            FormInputFocusBinder.clearFocusAndHideIme(
                    dialogView,
                    views.viewportInputView,
                    views.fontInputView,
                    WechatDpiSheetBinder.inputViewForFocus(dialogView)
            );
            return true;
        };
        views.viewportInputView.setOnEditorActionListener(doneListener);
        views.fontInputView.setOnEditorActionListener(doneListener);
        WechatDpiSheetBinder.bindDoneAction(doneListener, dialogView);
        FormInputFocusBinder.bindDismissOnOutsideTouch(
                dialogView.findViewById(R.id.dialog_app_config_scroll),
                dialogView,
                views.viewportInputView,
                views.fontInputView,
                WechatDpiSheetBinder.inputViewForFocus(dialogView)
        );
        TextWatcher viewportValidationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.updateViewportInput(
                        AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle), s);
                AppConfigDialogBinder.updateSaveButtonState(dialogView, views);
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
                host.onDraftStateChanged(state);
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
                AppConfigDialogBinder.updateSaveButtonState(dialogView, views);
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
                host.onDraftStateChanged(state);
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
            FormInputFocusBinder.clearFocusAndHideIme(dialogView,
                    views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.toggleViewportMode(
                    views.viewportModeToggle, views.viewportInputView, state);
            binder.bindViewportInputHint(
                    views.viewportInputLayout,
                    AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle));
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
        views.viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            FormInputFocusBinder.clearFocusAndHideIme(dialogView,
                    views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.switchViewportTargetType(
                    views.viewportModeToggle, views.viewportInputView, state,
                    ViewportTargetType.RELATIVE_SCALE, true);
            binder.bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.RELATIVE_SCALE);
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
        views.viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            FormInputFocusBinder.clearFocusAndHideIme(dialogView,
                    views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.switchViewportTargetType(
                    views.viewportModeToggle, views.viewportInputView, state,
                    ViewportTargetType.ABSOLUTE_DP, true);
            binder.bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.ABSOLUTE_DP);
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
        views.fontModeToggle.container.setOnClickListener(v -> {
            FormInputFocusBinder.clearFocusAndHideIme(dialogView,
                    views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.toggleFontMode(views.fontModeToggle);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
        views.fontModeToggle.emulationLabel.setOnClickListener(v -> {
            FormInputFocusBinder.clearFocusAndHideIme(dialogView,
                    views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.bindFontModeToggle(
                    views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
        views.fontModeToggle.replaceLabel.setOnClickListener(v -> {
            FormInputFocusBinder.clearFocusAndHideIme(dialogView,
                    views.viewportInputView, views.fontInputView);
            AppConfigDialogBinder.bindFontModeToggle(
                    views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
    }
}
