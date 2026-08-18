package com.dpis.module.appconfig;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.ConfigEditorDestination;
import com.dpis.module.applist.AppListItem;

import java.util.Set;

/**
 * Pure projection boundary for the per-app editor.
 *
 * Activity-specific work remains in the caller's Actions implementation; this factory only
 * combines the stable editor inputs into the immutable Compose state. Keeping this step pure makes
 * MainActivity and QuickConfig use the same dirty-state contract before action routing is moved.
 */
public final class EditorPresentationFactory {
    private EditorPresentationFactory() {
    }

    public static EditorPresentation.State create(
            AppListItem item,
            String versionName,
            EditorDraft draft,
            String typefaceSelectorText,
            String hookChainText,
            EditorDraft savedDraft,
            boolean saveFeedbackVisible,
            boolean systemHooksEnabled,
            Set<String> automaticFontHookDomains,
            ConfigEditorDestination destination,
            EditorPresentation.Actions actions
    ) {
        boolean dirty = !draft.hasSameSavedConfig(savedDraft);
        return new EditorPresentation.State(
                item,
                versionName,
                draft,
                typefaceSelectorText,
                hookChainText,
                dirty,
                saveFeedbackVisible,
                systemHooksEnabled,
                automaticFontHookDomains,
                destination,
                actions
        );
    }
}
