package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AppConfigEditorDraftTest {
    @Test
    public void immediateDpisStateSurvivesSubsequentFieldEdits() {
        AppConfigEditorDraft draft = draft().withDpisEnabled(true)
                .withFontInput("125");

        assertTrue(draft.dpisEnabled);
        assertEquals("125", draft.fontInput);
    }

    @Test
    public void immediateScopeStateSurvivesAdvancedConfigEdits() {
        AppConfigEditorDraft draft = draft().withScopeSelected(true)
                .withAdvancedConfig("serif", "resources_font", ViewportApplyMode.COMPAT,
                        false, false);

        assertTrue(draft.scopeSelected);
        assertEquals("serif", draft.selectedTypefaceId);
        assertEquals(ViewportApplyMode.COMPAT, draft.viewportApplyMode);
    }

    @Test
    public void resetKeepsImmediateStateWhileClearingPersistedConfigDraft() {
        AppConfigEditorDraft cleared = draft().withDpisEnabled(true).withScopeSelected(true)
                .cleared();

        assertTrue(cleared.dpisEnabled);
        assertTrue(cleared.scopeSelected);
        assertEquals("", cleared.viewportInputFor(ViewportTargetType.RELATIVE_SCALE));
        assertEquals("", cleared.fontInput);
        assertEquals(FontApplyMode.SYSTEM_EMULATION, cleared.fontMode);
        assertEquals(ViewportApplyMode.OFF, cleared.viewportApplyMode);
        assertTrue(cleared.fontHookDomainsResetRequested);
        assertTrue(cleared.viewportApplyModeResetRequested);
    }

    @Test
    public void fontHookDomainsAreEditableOnlyForFieldRewriteMode() {
        assertTrue(draft().withFontMode(FontApplyMode.FIELD_REWRITE).fontHookDomainsEditable());
        assertFalse(draft().withFontMode(FontApplyMode.SYSTEM_EMULATION).fontHookDomainsEditable());
    }

    @Test
    public void viewportModeSwitchRetainsIndependentScaleAndAbsoluteInputs() {
        AppConfigEditorDraft draft = draft()
                .withViewportInput(ViewportTargetType.RELATIVE_SCALE, "125")
                .withViewportMode(ViewportTargetType.ABSOLUTE_DP)
                .withViewportInput(ViewportTargetType.ABSOLUTE_DP, "720")
                .withViewportMode(ViewportTargetType.RELATIVE_SCALE);

        assertEquals("125", draft.viewportInputFor(ViewportTargetType.RELATIVE_SCALE));
        assertEquals("720", draft.viewportInputFor(ViewportTargetType.ABSOLUTE_DP));
    }

    @Test
    public void fontModeSwitchDoesNotDiscardFontInput() {
        AppConfigEditorDraft draft = draft().withFontInput("135")
                .withFontMode(FontApplyMode.FIELD_REWRITE)
                .withFontMode(FontApplyMode.SYSTEM_EMULATION);

        assertEquals("135", draft.fontInput);
        assertEquals(FontApplyMode.SYSTEM_EMULATION, draft.fontMode);
    }

    @Test
    public void automaticHookDomainsRemainExplicitAfterAdvancedConfigUpdate() {
        AppConfigEditorDraft automatic = draft().withAdvancedConfig(
                null, "", ViewportApplyMode.AUTO, true, false);
        AppConfigEditorDraft updated = automatic.withAdvancedConfig(
                automatic.selectedTypefaceId,
                automatic.draftFontHookDomainsRaw,
                ViewportApplyMode.COMPAT,
                automatic.fontHookDomainsResetRequested,
                false);

        assertEquals("", updated.draftFontHookDomainsRaw);
        assertTrue(updated.fontHookDomainsResetRequested);
        assertEquals(ViewportApplyMode.COMPAT, updated.viewportApplyMode);
    }

    private static AppConfigEditorDraft draft() {
        return new AppConfigEditorDraft(
                "com.example.target",
                "100",
                "100",
                "600",
                ViewportTargetType.RELATIVE_SCALE,
                "100",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                ViewportApplyMode.AUTO,
                false,
                false,
                "",
                false,
                false
        );
    }
}
