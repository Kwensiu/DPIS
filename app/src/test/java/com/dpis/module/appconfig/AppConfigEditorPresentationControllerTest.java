package com.dpis.module;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.appconfig.EditorDraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.EditorActions;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.Test;

public final class AppConfigEditorPresentationControllerTest {
    @Test
    public void updatesDraftThroughHostWithoutOwningActivityState() {
        RecordingHost host = new RecordingHost();
        EditorDraft draft = draft(false);

        EditorActions.create(host, app(), draft)
                .updateViewportInput("125");

        assertEquals("125", host.updatedDraft.viewportScaleInput);
    }

    @Test
    public void scopeAndDpisActionsUseHostSideEffectsBeforePublishingDraft() {
        RecordingHost host = new RecordingHost();
        EditorPresentation.Actions actions =
                EditorActions.create(host, app(), draft(false));

        actions.toggleScope();
        assertFalse(host.scopeSelectedBeforeCallback);
        host.onScopeSelected.run();
        assertTrue(host.updatedDraft.scopeSelected);

        actions.toggleDpisEnabled();
        assertTrue(host.requestedDpisEnabled);
        assertTrue(host.updatedDraft.dpisEnabled);
    }

    @Test
    public void failedDpisChangeDoesNotPublishAnOptimisticDraft() {
        RecordingHost host = new RecordingHost();
        host.dpisChangeSucceeds = false;
        EditorDraft draft = draft(false);

        EditorActions.create(host, app(), draft)
                .toggleDpisEnabled();

        assertTrue(host.requestedDpisEnabled);
        assertNull(host.updatedDraft);
    }

    @Test
    public void delegatesProcessDiagnosticSaveAndCloseWithCurrentSnapshot() {
        RecordingHost host = new RecordingHost();
        EditorDraft draft = draft(true);
        AppListItem item = app();
        EditorPresentation.Actions actions =
                EditorActions.create(host, item, draft);

        actions.restartProcess();
        actions.startFeedbackDiagnostic();
        actions.save();
        actions.close();

        assertEquals(AppConfigDialogBinder.ProcessAction.RESTART, host.processAction);
        assertSame(draft, host.diagnosticDraft);
        assertSame(draft, host.savedDraft);
        assertTrue(host.closed);
    }

    private static EditorDraft draft(boolean enabled) {
        return new EditorDraft(
                "com.example.app", "100", "100", "", "relative_scale", "100",
                FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF,
                false, false, "", false, enabled);
    }

    private static AppListItem app() {
        return new AppListItem(
                "Example", "com.example.app", false, true, null, null,
                ViewportApplyMode.OFF, null, ViewportTargetSpec.off(), null,
                FontApplyMode.OFF, null, false, null, true, false, true, false,
                false, null);
    }

    private static final class RecordingHost
            implements EditorActions.Host {
        EditorDraft updatedDraft;
        boolean scopeSelectedBeforeCallback;
        Runnable onScopeSelected;
        boolean requestedDpisEnabled;
        boolean dpisChangeSucceeds = true;
        AppConfigDialogBinder.ProcessAction processAction;
        AppListItem diagnosticItem;
        EditorDraft diagnosticDraft;
        AppListItem savedItem;
        EditorDraft savedDraft;
        boolean closed;

        @Override public void updateDraft(EditorDraft draft) {
            updatedDraft = draft;
        }

        @Override public void showWechatDpiHelp() {}
        @Override public void navigate(ConfigEditorDestination destination) {}

        @Override public void toggleScope(boolean currentlySelected,
                Runnable onSelected, Runnable onDeselected) {
            scopeSelectedBeforeCallback = currentlySelected;
            onScopeSelected = onSelected;
        }

        @Override public boolean setDpisEnabled(boolean enabled) {
            requestedDpisEnabled = enabled;
            return dpisChangeSucceeds;
        }

        @Override public void executeProcessAction(
                AppConfigDialogBinder.ProcessAction action) {
            processAction = action;
        }

        @Override public void startFeedbackDiagnostic(EditorDraft draft) {
            diagnosticItem = app();
            diagnosticDraft = draft;
        }

        @Override public void save(EditorDraft draft) {
            savedItem = app();
            savedDraft = draft;
        }

        @Override public void close() {
            closed = true;
        }
    }
}
