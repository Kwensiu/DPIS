package com.dpis.module

import com.dpis.module.appconfig.AppConfigEditorChip
import com.dpis.module.appconfig.AppConfigEditorSession
import com.dpis.module.appconfig.EditorDialogStateFactory
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.appconfig.EditorPresentationFactory
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.ui.ConfigEditorDestination

class AppConfigEditorPresentationFactoryTest {
    @Test
    fun createsCleanStateWhenDraftMatchesSavedDraft() {
        val draft = draft("com.example.app", "125")
        val actions = actions()

        val state = presentation(draft, draft, actions)

        assertFalse(state.dirty)
        assertEquals(AppConfigEditorChip.NONE, state.chip)
        assertSame(actions, state.actions)
        assertTrue(state.saveEnabled)
    }

    @Test
    fun marksStateDirtyWhenDraftDiffersFromSavedDraft() {
        val draft = draft("com.example.app", "125")
        val saved = draft("com.example.app", "100")

        val state = presentation(draft, saved, actions())

        assertTrue(state.dirty)
        assertEquals(AppConfigEditorChip.UNSAVED, state.chip)
    }

    @Test
    fun mapsPrefillSessionToPrefillChipInsteadOfUnsaved() {
        val item = app("Example", "com.example.app")
        val session = AppConfigEditorSession.open(
            item,
            false,
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font",
            ),
        )

        val state = EditorPresentationFactory.create(
            item,
            "1.2.3",
            session.draft,
            "默认",
            "Hook",
            session.persistedBaseline,
            false,
            true,
            setOf("android.widget.TextView"),
            ConfigEditorDestination.MAIN,
            actions(),
            session,
        )

        assertEquals(AppConfigEditorChip.PREFILL, state.chip)
        assertFalse(state.dirty)
    }

    @Test
    fun dialogStateProjectionUsesEveryEditorOwnedDraftValue() {
        val draft = EditorDraft(
            "com.example.app", "125", "640", "130", "absolute_dp", "110",
            FontApplyMode.SYSTEM_EMULATION, "font-id", "domain-a,domain-b",
            ViewportApplyMode.COMPAT, true, true, "420", true, false,
        )

        val state = EditorDialogStateFactory.create(app("Example", draft.packageName), draft)

        assertEquals("font-id", state.selectedTypefaceId)
        assertEquals("domain-a,domain-b", state.draftFontHookDomainsRaw)
        assertEquals(ViewportApplyMode.COMPAT, state.viewportApplyMode)
        assertTrue(state.fontHookDomainsResetRequested)
        assertTrue(state.viewportApplyModeResetRequested)
        assertEquals("640", state.viewportScaleInput)
        assertEquals("130", state.viewportAbsoluteInput)
        assertTrue(state.scopeSelected)
        assertFalse(state.dpisEnabled)
    }

    private companion object {
        fun presentation(
            draft: EditorDraft,
            savedDraft: EditorDraft,
            actions: EditorPresentation.Actions,
        ) = EditorPresentationFactory.create(
            app("Example", draft.packageName),
            "1.2.3",
            draft,
            "默认",
            "Hook",
            savedDraft,
            false,
            true,
            setOf("android.widget.TextView"),
            ConfigEditorDestination.MAIN,
            actions,
        )

        fun draft(packageName: String, viewportInput: String) = EditorDraft(
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
            true,
        )

        fun app(label: String, packageName: String) = AppListItem(
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
            null,
        )

        fun actions() = object : EditorPresentation.Actions {
            override fun updateViewportInput(value: String) {}
            override fun changeViewportMode(targetType: String) {}
            override fun updateFontInput(value: String) {}
            override fun changeFontMode(mode: String) {}
            override fun updateWechatDpiInput(value: String) {}
            override fun showWechatDpiHelp() {}
            override fun updateTypeface(typefaceId: String) {}
            override fun updateHookChain(
                rawDomains: String,
                resetDomains: Boolean,
                viewportApplyMode: String,
                resetViewportApplyMode: Boolean,
            ) {}
            override fun navigate(destination: ConfigEditorDestination) {}
            override fun reset() {}
            override fun toggleScope() {}
            override fun toggleDpisEnabled() {}
            override fun startProcess() {}
            override fun restartProcess() {}
            override fun stopProcess() {}
            override fun startFeedbackDiagnostic() {}
            override fun save() {}
            override fun close() {}
        }
    }
}
