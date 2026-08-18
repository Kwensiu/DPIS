package com.dpis.module.appconfig;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.applist.AppListItem;

/**
 * Projects an immutable Compose editor draft into the state consumed by legacy dialog helpers.
 *
 * The dialog helpers still own their View-specific behavior. This factory only keeps the shared
 * MainActivity and QuickConfig draft projection consistent while they use those helpers.
 */
public final class EditorDialogStateFactory {
    private EditorDialogStateFactory() {
    }

    public static AppConfigDialogBinder.AppConfigDialogState create(
            AppListItem item,
            EditorDraft draft
    ) {
        AppConfigDialogBinder.AppConfigDialogState state
                = AppConfigDialogBinder.AppConfigDialogState.fromItem(item);
        state.selectedTypefaceId = draft.selectedTypefaceId;
        state.draftFontHookDomainsRaw = draft.draftFontHookDomainsRaw;
        state.viewportApplyMode = draft.viewportApplyMode;
        state.fontHookDomainsResetRequested = draft.fontHookDomainsResetRequested;
        state.viewportApplyModeResetRequested = draft.viewportApplyModeResetRequested;
        state.viewportScaleInput = draft.viewportScaleInput;
        state.viewportAbsoluteInput = draft.viewportAbsoluteInput;
        state.scopeSelected = draft.scopeSelected;
        state.dpisEnabled = draft.dpisEnabled;
        return state;
    }
}
