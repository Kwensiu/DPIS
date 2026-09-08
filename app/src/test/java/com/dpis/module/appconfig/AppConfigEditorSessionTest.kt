package com.dpis.module

import com.dpis.module.appconfig.AppConfigEditorChip
import com.dpis.module.appconfig.AppConfigEditorSession
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigEditorSessionTest {
    @Test
    fun unconfiguredOpenWithPrefillShowsPrefillChipAndInitializesDraftFromSnapshot() {
        val session = AppConfigEditorSession.open(app(), hasSavedPackageConfig = false, PREFILL)

        assertEquals(AppConfigEditorChip.PREFILL, session.chip)
        assertNotNull(session.prefillSnapshot)
        assertTrue(session.draft.hasSameSavedConfig(session.prefillSnapshot))
        assertFalse(session.draft.hasSameSavedConfig(session.persistedBaseline))
        assertFalse(session.prefillInvalidated)
        assertEquals("87.5", session.draft.viewportScaleInput)
        assertEquals("125", session.draft.fontInput)
    }

    @Test
    fun mutatingPrefillDraftShowsUnsavedChip() {
        val session = AppConfigEditorSession.open(app(), false, PREFILL)
            .withDraft(
                AppConfigEditorSession.open(app(), false, PREFILL).draft.withFontInput("140"),
            )

        assertEquals(AppConfigEditorChip.UNSAVED, session.chip)
        assertTrue(session.prefillInvalidated)
    }

    @Test
    fun manuallyReturningPrefillDraftToEmptyDefaultHidesChip() {
        val opened = AppConfigEditorSession.open(app(), false, PREFILL)
        val emptied = opened.draft
            .withViewportMode(ViewportTargetType.RELATIVE_SCALE)
            .withViewportInput(ViewportTargetType.RELATIVE_SCALE, "")
            .withFontInput("")
            .withFontMode(FontApplyMode.SYSTEM_EMULATION)
            .withAdvancedConfig(null, null, ViewportApplyMode.OFF, false, false)

        val session = opened.withDraft(emptied)

        assertTrue(session.prefillInvalidated)
        assertFalse(session.draft.fontHookDomainsResetRequested)
        assertFalse(session.draft.viewportApplyModeResetRequested)
        assertTrue(session.persistedBaseline.fontHookDomainsResetRequested)
        assertEquals(AppConfigEditorChip.NONE, session.chip)
    }

    @Test
    fun clearingPrefillNumbersWhileLeavingCustomExtrasStaysUnsaved() {
        val opened = AppConfigEditorSession.open(app(), false, PREFILL)
        val session = opened.withDraft(
            opened.draft
                .withViewportInput(opened.draft.viewportMode, "")
                .withFontInput(""),
        )

        assertEquals(AppConfigEditorChip.UNSAVED, session.chip)
        assertEquals("serif", session.draft.selectedTypefaceId)
        assertEquals(FontApplyMode.FIELD_REWRITE, session.draft.fontMode)
    }

    @Test
    fun resetClearsPrefillChipWithoutWritingAndLaterEditIsUnsaved() {
        val opened = AppConfigEditorSession.open(app(), false, PREFILL)
        val reset = opened.reset()

        assertEquals(AppConfigEditorChip.NONE, reset.chip)
        assertTrue(reset.prefillInvalidated)
        assertTrue(reset.draft.hasSameSavedConfig(reset.persistedBaseline))
        assertEquals("", reset.draft.fontInput)
        assertNotNull(reset.prefillSnapshot)

        val edited = reset.withDraft(reset.draft.withFontInput("110"))
        assertEquals(AppConfigEditorChip.UNSAVED, edited.chip)
        assertTrue(edited.prefillInvalidated)
    }

    @Test
    fun saveFromPrefillSessionClearsPrefillSemantics() {
        val opened = AppConfigEditorSession.open(app(), false, PREFILL)
        val saved = opened.afterSave()

        assertEquals(AppConfigEditorChip.NONE, saved.chip)
        assertNull(saved.prefillSnapshot)
        assertFalse(saved.prefillInvalidated)
        assertTrue(saved.draft.hasSameSavedConfig(saved.persistedBaseline))
        assertEquals("87.5", saved.persistedBaseline.viewportScaleInput)
    }

    @Test
    fun resetThenSaveCreatesPersistedBaselineFromEmptyDefault() {
        val saved = AppConfigEditorSession.open(app(), false, PREFILL).reset().afterSave()

        assertEquals(AppConfigEditorChip.NONE, saved.chip)
        assertNull(saved.prefillSnapshot)
        assertFalse(saved.prefillInvalidated)
        assertEquals("", saved.persistedBaseline.fontInput)
        assertTrue(saved.draft.hasSameSavedConfig(saved.persistedBaseline))
    }

    @Test
    fun openSnapshotStaysStableWhileSessionIsRetained() {
        val first = AppConfigEditorSession.open(app(), false, PREFILL)
        val laterPrefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.relativeScale(150000),
            ViewportApplyMode.SYSTEM,
            160,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
        )

        val retained = AppConfigEditorSession.retainOrOpen(first, app(), false, laterPrefill)

        assertSame(first, retained)
        assertEquals("87.5", retained.prefillSnapshot?.viewportScaleInput)
        assertEquals(AppConfigEditorChip.PREFILL, retained.chip)
    }

    @Test
    fun newSessionAfterCloseResolvesPrefillAgain() {
        val first = AppConfigEditorSession.open(app(), false, PREFILL)
        val laterPrefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.relativeScale(150000),
            ViewportApplyMode.SYSTEM,
            160,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
        )

        val reopened = AppConfigEditorSession.retainOrOpen(null, app(), false, laterPrefill)

        assertEquals("150", reopened.draft.viewportScaleInput)
        assertEquals(AppConfigEditorChip.PREFILL, reopened.chip)
        assertFalse(first.draft.hasSameSavedConfig(reopened.draft))
    }

    @Test
    fun savedPackageConfigIgnoresPrefill() {
        val persistedItem = app().withGlobalPrefillPreview(PREFILL)
        val session = AppConfigEditorSession.open(persistedItem, hasSavedPackageConfig = true, PREFILL)

        assertNull(session.prefillSnapshot)
        assertEquals(AppConfigEditorChip.NONE, session.chip)
        assertTrue(session.draft.hasSameSavedConfig(session.persistedBaseline))
    }

    @Test
    fun emptyPrefillDoesNotCreatePrefillChip() {
        val session = AppConfigEditorSession.open(app(), false, TemplateConfigValue.EMPTY)

        assertNull(session.prefillSnapshot)
        assertEquals(AppConfigEditorChip.NONE, session.chip)
        assertTrue(session.draft.hasSameSavedConfig(session.persistedBaseline))
    }

    @Test
    fun editingBackToPrefillAfterResetStaysUnsaved() {
        val reset = AppConfigEditorSession.open(app(), false, PREFILL).reset()
        val restored = reset.withDraft(reset.prefillSnapshot!!)

        assertEquals(AppConfigEditorChip.UNSAVED, restored.chip)
        assertTrue(restored.prefillInvalidated)
    }

    @Test
    fun clearingScaleOnlyPrefillToEmptyHidesChip() {
        val opened = AppConfigEditorSession.open(app(), false, SCALE_ONLY_PREFILL)
        assertEquals(AppConfigEditorChip.PREFILL, opened.chip)
        assertEquals("30", opened.draft.viewportScaleInput)

        val cleared = opened.withDraft(
            opened.draft.withViewportInput(opened.draft.viewportMode, ""),
        )

        assertEquals(AppConfigEditorChip.NONE, cleared.chip)
        assertTrue(cleared.prefillInvalidated)
    }

    @Test
    fun persistableIdentityIgnoresResetFlagsAndRejectsFieldMismatches() {
        val baseline = AppConfigEditorSession.open(app(), false, SCALE_ONLY_PREFILL).draft
        val resetOnly = baseline.withAdvancedConfig(
            baseline.selectedTypefaceId,
            baseline.draftFontHookDomainsRaw,
            baseline.viewportApplyMode,
            true,
            true,
        )
        assertTrue(baseline.hasSamePersistedConfig(resetOnly))
        assertFalse(baseline.hasSameSavedConfig(resetOnly))
        assertFalse(baseline.hasSamePersistedConfig(null))
        assertFalse(baseline.hasSamePersistedConfig(baseline.withWechatDpiInput("480")))
        assertFalse(baseline.hasSamePersistedConfig(baseline.withFontMode(FontApplyMode.FIELD_REWRITE)))
        assertFalse(
            baseline.hasSamePersistedConfig(
                EditorDraft(
                    "com.other.app",
                    baseline.viewportInput,
                    baseline.viewportScaleInput,
                    baseline.viewportAbsoluteInput,
                    baseline.viewportMode,
                    baseline.fontInput,
                    baseline.fontMode,
                    baseline.selectedTypefaceId,
                    baseline.draftFontHookDomainsRaw,
                    baseline.viewportApplyMode,
                    baseline.fontHookDomainsResetRequested,
                    baseline.viewportApplyModeResetRequested,
                    baseline.wechatDpiInput,
                    baseline.scopeSelected,
                    baseline.dpisEnabled,
                ),
            ),
        )
    }

    @Test
    fun scopeSelectionKeepsPrefillSnapshotAligned() {
        val opened = AppConfigEditorSession.open(app(), false, PREFILL)
        val selected = opened.withScopeSelected(true)

        assertTrue(selected.draft.scopeSelected)
        assertTrue(selected.prefillSnapshot!!.scopeSelected)
        assertTrue(selected.persistedBaseline.scopeSelected)
        assertEquals(AppConfigEditorChip.PREFILL, selected.chip)

        val saved = AppConfigEditorSession.open(app(), true, PREFILL).withScopeSelected(true)
        assertNull(saved.prefillSnapshot)
        assertTrue(saved.draft.scopeSelected)
    }

    @Test
    fun restoringScaleOnlyPrefillValueAfterClearDoesNotShowPrefill() {
        val opened = AppConfigEditorSession.open(app(), false, SCALE_ONLY_PREFILL)
        val restored = opened
            .withDraft(opened.draft.withViewportInput(opened.draft.viewportMode, ""))
            .withDraft(opened.draft.withViewportInput(opened.draft.viewportMode, "30"))

        assertEquals(AppConfigEditorChip.UNSAVED, restored.chip)
        assertTrue(restored.prefillInvalidated)
        assertEquals("30", restored.draft.viewportScaleInput)
    }

    private companion object {
        val SCALE_ONLY_PREFILL: TemplateConfigValue =
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(30_000),
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.OFF,
                null,
                null,
            )

        val PREFILL: TemplateConfigValue = TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.relativeScale(87500),
            ViewportApplyMode.AUTO,
            125,
            FontApplyMode.FIELD_REWRITE,
            "serif",
            "resources_font",
        )

        fun app(): AppListItem = AppListItem(
            "Example",
            "com.example.app",
            false,
            true,
            null,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            true,
            false,
            false,
            null,
        )
    }
}
