package com.dpis.module;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.appconfig.EditorDraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.EditorDialogStateFactory;
import com.dpis.module.appconfig.EditorPresentationFactory;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import java.util.Set;
import org.junit.Test;

public final class AppConfigEditorPresentationFactoryTest {
    @Test
    public void createsCleanStateWhenDraftMatchesSavedDraft() {
        EditorDraft draft = draft("com.example.app", "125");
        EditorPresentation.Actions actions = actions();

        EditorPresentation.State state = create(
                draft, draft, actions);

        assertFalse(state.dirty);
        assertSame(actions, state.actions);
        assertTrue(state.saveEnabled);
    }

    @Test
    public void marksStateDirtyWhenDraftDiffersFromSavedDraft() {
        EditorDraft draft = draft("com.example.app", "125");
        EditorDraft saved = draft("com.example.app", "100");

        EditorPresentation.State state = create(
                draft, saved, actions());

        assertTrue(state.dirty);
    }

    @Test
    public void dialogStateProjectionUsesEveryEditorOwnedDraftValue() {
        EditorDraft draft = new EditorDraft(
                "com.example.app", "125", "640", "130", "absolute_dp", "110",
                FontApplyMode.SYSTEM_EMULATION, "font-id", "domain-a,domain-b",
                ViewportApplyMode.COMPAT, true, true, "420", true, false);

        AppConfigDialogBinder.AppConfigDialogState state =
                EditorDialogStateFactory.create(app("Example", draft.packageName), draft);

        assertEquals("font-id", state.selectedTypefaceId);
        assertEquals("domain-a,domain-b", state.draftFontHookDomainsRaw);
        assertEquals(ViewportApplyMode.COMPAT, state.viewportApplyMode);
        assertTrue(state.fontHookDomainsResetRequested);
        assertTrue(state.viewportApplyModeResetRequested);
        assertEquals("640", state.viewportScaleInput);
        assertEquals("130", state.viewportAbsoluteInput);
        assertTrue(state.scopeSelected);
        assertFalse(state.dpisEnabled);
    }

    private static EditorPresentation.State create(
            EditorDraft draft,
            EditorDraft savedDraft,
            EditorPresentation.Actions actions
    ) {
        return EditorPresentationFactory.create(
                app("Example", draft.packageName),
                "1.2.3",
                draft,
                "默认",
                "Hook",
                savedDraft,
                false,
                true,
                Set.of("android.widget.TextView"),
                ConfigEditorDestination.MAIN,
                actions
        );
    }

    private static EditorDraft draft(String packageName, String viewportInput) {
        return new EditorDraft(
                packageName,
                viewportInput,
                viewportInput,
                "",
                "relative_scale",
                "100",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                ViewportApplyMode.OFF,
                false,
                false,
                "",
                true,
                true
        );
    }

    private static AppListItem app(String label, String packageName) {
        return new AppListItem(
                label,
                packageName,
                true,
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                null,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.OFF,
                null,
                false,
                null,
                true,
                false,
                true,
                false,
                false,
                null
        );
    }

    private static EditorPresentation.Actions actions() {
        return new EditorPresentation.Actions() {
            @Override public void updateViewportInput(String value) {}
            @Override public void changeViewportMode(String targetType) {}
            @Override public void updateFontInput(String value) {}
            @Override public void changeFontMode(String mode) {}
            @Override public void updateWechatDpiInput(String value) {}
            @Override public void showWechatDpiHelp() {}
            @Override public void updateTypeface(String typefaceId) {}
            @Override public void updateHookChain(String rawDomains, boolean resetDomains,
                    String viewportApplyMode, boolean resetViewportApplyMode) {}
            @Override public void navigate(ConfigEditorDestination destination) {}
            @Override public void reset() {}
            @Override public void toggleScope() {}
            @Override public void toggleDpisEnabled() {}
            @Override public void startProcess() {}
            @Override public void restartProcess() {}
            @Override public void stopProcess() {}
            @Override public void startFeedbackDiagnostic() {}
            @Override public void save() {}
            @Override public void close() {}
        };
    }
}
