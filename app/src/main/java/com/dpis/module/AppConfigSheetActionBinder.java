package com.dpis.module;

import android.view.View;

final class AppConfigSheetActionBinder {
    private final AppConfigDialogBinder binder;
    private final AppConfigDialogBinder.Host host;

    AppConfigSheetActionBinder(AppConfigDialogBinder binder, AppConfigDialogBinder.Host host) {
        this.binder = binder;
        this.host = host;
    }

    void bindDialogActions(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        dialogView.setFocusable(true);
        dialogView.setFocusableInTouchMode(true);
        dialogView.setClickable(true);
        dialogView.setOnClickListener(
                v -> clearInputFocus(dialogView, views));
        views.scopeButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
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
            clearInputFocus(dialogView, views);
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.START);
        });
        views.restartButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.RESTART);
        });
        views.stopButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.STOP);
        });
        views.dpisToggleButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            if (state.previewFromGlobalPrefill) {
                return;
            }
            boolean nextEnabled = !state.dpisEnabled;
            if (host.setDpisEnabled(item.packageName, nextEnabled)) {
                state.dpisEnabled = nextEnabled;
                WechatTargetFieldSheetBinder.publishForDpisState(item.packageName, nextEnabled);
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            }
        });
        views.fontHookDomainsButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            host.showFontHookDomains(item, state,
                    () -> {
                        binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
                        host.onDraftStateChanged(state);
                    });
        });
        views.disableButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            views.viewportInputView.setText("");
            views.fontInputView.setText("");
            WechatTargetFieldSheetBinder.clearDraft(dialogView);
            state.selectedTypefaceId = null;
            state.clearViewportInputs();
            state.clearHookChainStateForReset();
            binder.bindTypefaceSelector(views.typefaceSelectorButton, state.selectedTypefaceId);
            AppConfigDialogBinder.bindViewportModeToggle(
                    views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true);
            AppConfigDialogBinder.bindFontModeToggle(
                    views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views);
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
            host.onDraftStateChanged(state);
        });
        views.saveButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            if (!WechatTargetFieldSheetBinder.isInputValid(dialogView)) {
                host.showToast(R.string.status_save_invalid);
                return;
            }
            int[] result = host.saveAppConfig(
                    item,
                    views.viewportInputView,
                    views.fontInputView,
                    AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
                    state.viewportApplyMode,
                    state.viewportApplyModeResetRequested,
                    AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
                    state.selectedTypefaceId,
                    state.draftFontHookDomainsRaw,
                    state.fontHookDomainsResetRequested,
                    state.viewportScaleInput,
                    state.viewportAbsoluteInput);
            if (result[0] == 1) {
                DpiConfigStore store = host.getConfigStore();
                if (!WechatTargetFieldSheetBinder.save(
                        dialogView, item.packageName, state.dpisEnabled, store)) {
                    result[0] = 0;
                    result[1] = R.string.status_save_invalid;
                } else {
                    host.requestAppsLoad();
                }
            }
            if (result[0] == 1) {
                state.previewFromGlobalPrefill = false;
                state.draftFontHookDomainsRaw = null;
                state.fontHookDomainsResetRequested = false;
                state.viewportApplyModeResetRequested = false;
                state.captureSavedDraft(views, false);
                AppConfigDialogBinder.showSaveButtonFeedback(views.saveButton);
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
                binder.syncHyperOsNativeProxyAfterSave(item, views, state);
                binder.requestScopeAfterSuccessfulSave(
                        dialogView, item, views, state, style, systemHooksEnabled);
            }
            if (result[1] != 0) {
                host.showToast(result[1]);
            }
        });
    }

    void bindTypefaceSelectorAction(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        views.typefaceSelectorButton.setOnClickListener(v -> {
            clearInputFocus(dialogView, views);
            binder.showTypefaceSelector(views.typefaceSelectorButton, state,
                    () -> {
                        binder.refreshDialogState(
                                views, state, style, systemHooksEnabled, item);
                        host.onDraftStateChanged(state);
                    });
        });
    }

    private static void clearInputFocus(
            View dialogView,
            AppConfigDialogBinder.AppConfigDialogViews views
    ) {
        FormInputFocusBinder.clearFocusAndHideIme(
                dialogView,
                views.viewportInputView,
                views.fontInputView,
                dialogView.findViewById(R.id.dialog_wechat_target_field_input)
        );
    }
}
