package com.dpis.module.appconfig;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.ConfigEditorDestination;
import com.dpis.module.applist.AppListItem;

/**
 * Builds the per-app editor actions without owning Activity state or side effects.
 *
 * The host is deliberately capability-shaped: Activities keep ownership of persistence, dialogs,
 * runtime work, and refresh timing while this controller owns the immutable draft transitions.
 */
public final class EditorActions {
    public interface Host {
        void updateDraft(EditorDraft draft);
        void showWechatDpiHelp();
        void navigate(ConfigEditorDestination destination);
        void toggleScope(boolean currentlySelected, Runnable onSelected, Runnable onDeselected);
        boolean setDpisEnabled(boolean enabled);
        void executeProcessAction(AppConfigDialogBinder.ProcessAction action);
        void startFeedbackDiagnostic(EditorDraft draft);
        void save(EditorDraft draft);
        void close();
    }

    private EditorActions() {
    }

    public static EditorPresentation.Actions create(
            Host host,
            AppListItem item,
            EditorDraft draft
    ) {
        return new EditorPresentation.Actions() {
            @Override public void updateViewportInput(String value) {
                host.updateDraft(draft.withViewportInput(draft.viewportMode, value));
            }

            @Override public void changeViewportMode(String targetType) {
                host.updateDraft(draft.withViewportMode(targetType));
            }

            @Override public void updateFontInput(String value) {
                host.updateDraft(draft.withFontInput(value));
            }

            @Override public void changeFontMode(String mode) {
                host.updateDraft(draft.withFontMode(mode));
            }

            @Override public void updateWechatDpiInput(String value) {
                host.updateDraft(draft.withWechatDpiInput(value));
            }

            @Override public void showWechatDpiHelp() {
                host.showWechatDpiHelp();
            }

            @Override public void updateTypeface(String typefaceId) {
                host.updateDraft(draft.withAdvancedConfig(
                        typefaceId,
                        draft.draftFontHookDomainsRaw,
                        draft.viewportApplyMode,
                        draft.fontHookDomainsResetRequested,
                        draft.viewportApplyModeResetRequested));
            }

            @Override public void updateHookChain(
                    String rawDomains,
                    boolean resetDomains,
                    String viewportApplyMode,
                    boolean resetViewportApplyMode
            ) {
                host.updateDraft(draft.withAdvancedConfig(
                        draft.selectedTypefaceId,
                        rawDomains,
                        viewportApplyMode,
                        resetDomains,
                        resetViewportApplyMode));
            }

            @Override public void navigate(ConfigEditorDestination destination) {
                host.navigate(destination);
            }

            @Override public void reset() {
                host.updateDraft(draft.cleared());
            }

            @Override public void toggleScope() {
                host.toggleScope(
                        draft.scopeSelected,
                        () -> host.updateDraft(draft.withScopeSelected(true)),
                        () -> host.updateDraft(draft.withScopeSelected(false)));
            }

            @Override public void toggleDpisEnabled() {
                boolean enabled = !draft.dpisEnabled;
                if (host.setDpisEnabled(enabled)) {
                    host.updateDraft(draft.withDpisEnabled(enabled));
                }
            }

            @Override public void startProcess() {
                host.executeProcessAction(AppConfigDialogBinder.ProcessAction.START);
            }

            @Override public void restartProcess() {
                host.executeProcessAction(AppConfigDialogBinder.ProcessAction.RESTART);
            }

            @Override public void stopProcess() {
                host.executeProcessAction(AppConfigDialogBinder.ProcessAction.STOP);
            }

            @Override public void startFeedbackDiagnostic() {
                host.startFeedbackDiagnostic(draft);
            }

            @Override public void save() {
                host.save(draft);
            }

            @Override public void close() {
                host.close();
            }
        };
    }
}
