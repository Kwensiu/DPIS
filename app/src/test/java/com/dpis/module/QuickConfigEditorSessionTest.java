package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import org.junit.Test;

public final class QuickConfigEditorSessionTest {

    @Test
    public void configurationChangeKeepsDraftBaselineAndChildDestination() {
        AppConfigEditorDraft draft = draft("125", "112");
        AppConfigEditorDraft savedDraft = draft("100", "100");

        QuickConfigEditorSession session = new QuickConfigEditorSession(
                null,
                draft,
                savedDraft,
                ConfigEditorDestination.HOOK_CHAIN_FONT
        );

        assertSame(draft, session.draft);
        assertSame(savedDraft, session.savedDraft);
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_FONT, session.destination);
    }

    @Test
    public void missingSavedBaselineAndDestinationUseCurrentDraftAndMainPage() {
        AppConfigEditorDraft draft = draft("125", "112");

        QuickConfigEditorSession session = new QuickConfigEditorSession(
                null,
                draft,
                null,
                null
        );

        assertSame(draft, session.savedDraft);
        assertEquals(ConfigEditorDestination.MAIN, session.destination);
    }

    private static AppConfigEditorDraft draft(String viewport, String font) {
        return new AppConfigEditorDraft(
                "com.example.target",
                viewport,
                viewport,
                "",
                ViewportTargetType.RELATIVE_SCALE,
                font,
                "system",
                null,
                null,
                ViewportApplyMode.AUTO,
                false,
                false,
                "",
                false,
                true
        );
    }
}
