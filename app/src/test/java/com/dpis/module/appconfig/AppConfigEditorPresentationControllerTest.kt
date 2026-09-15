package com.dpis.module

import com.dpis.module.appconfig.AppConfigProcessAction
import com.dpis.module.appconfig.editor.EditorActions
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.ui.ConfigEditorDestination

class AppConfigEditorPresentationControllerTest {
    @Test
    fun updatesDraftThroughHostWithoutOwningActivityState() {
        val host = RecordingHost()
        val draft = draft(false)

        EditorActions.create(host, app(), draft)
            .updateViewportInput("125")

        assertEquals("125", host.updatedDraft!!.viewportScaleInput)
    }

    @Test
    fun scopeAndDpisActionsUseHostSideEffectsBeforePublishingDraft() {
        val host = RecordingHost()
        val actions = EditorActions.create(host, app(), draft(false))

        actions.toggleScope()
        assertFalse(host.scopeSelectedBeforeCallback)
        host.onScopeSelected!!.run()
        assertTrue(host.updatedDraft!!.scopeSelected)

        actions.toggleDpisEnabled()
        assertTrue(host.requestedDpisEnabled)
        assertTrue(host.updatedDraft!!.dpisEnabled)
    }

    @Test
    fun failedDpisChangeDoesNotPublishAnOptimisticDraft() {
        val host = RecordingHost()
        host.dpisChangeSucceeds = false
        val draft = draft(false)

        EditorActions.create(host, app(), draft)
            .toggleDpisEnabled()

        assertTrue(host.requestedDpisEnabled)
        assertNull(host.updatedDraft)
    }

    @Test
    fun delegatesProcessDiagnosticSaveAndCloseWithCurrentSnapshot() {
        val host = RecordingHost()
        val draft = draft(true)
        val item = app()
        val actions = EditorActions.create(host, item, draft)

        actions.startProcess()
        actions.restartProcess()
        actions.stopProcess()
        actions.startFeedbackDiagnostic()
        actions.save()
        actions.reset()
        actions.close()

        assertEquals(AppConfigProcessAction.STOP, host.processAction)
        assertSame(draft, host.diagnosticDraft)
        assertSame(draft, host.savedDraft)
        assertTrue(host.resetCalled)
        assertTrue(host.closed)
    }

    @Test
    fun remainingDraftActionsRewriteTheCurrentSnapshotThroughTheHost() {
        val host = RecordingHost()
        val draft = draft(true)
        val actions = EditorActions.create(host, app(), draft)

        actions.changeViewportMode("absolute_dp")
        assertEquals("absolute_dp", host.updatedDraft!!.viewportMode)

        actions.updateFontInput("125")
        assertEquals("125", host.updatedDraft!!.fontInput)

        actions.changeFontMode(FontApplyMode.FIELD_REWRITE)
        assertEquals(FontApplyMode.FIELD_REWRITE, host.updatedDraft!!.fontMode)

        actions.updateWechatDpiInput("360")
        assertEquals("360", host.updatedDraft!!.wechatDpiInput)

        actions.updateTypeface("serif")
        assertEquals("serif", host.updatedDraft!!.selectedTypefaceId)

        actions.updateHookChain("resources_font", true, ViewportApplyMode.AUTO, true)
        assertEquals("resources_font", host.updatedDraft!!.draftFontHookDomainsRaw)
        assertTrue(host.updatedDraft!!.fontHookDomainsResetRequested)
        assertEquals(ViewportApplyMode.AUTO, host.updatedDraft!!.viewportApplyMode)
        assertTrue(host.updatedDraft!!.viewportApplyModeResetRequested)

        actions.showWechatDpiHelp()
        assertTrue(host.wechatDpiHelpShown)
        actions.navigate(ConfigEditorDestination.TYPEFACE)
        assertEquals(ConfigEditorDestination.TYPEFACE, host.navigatedTo)
    }

    private class RecordingHost : EditorActions.Host {
        var updatedDraft: EditorDraft? = null
        var scopeSelectedBeforeCallback = false
        var onScopeSelected: Runnable? = null
        var requestedDpisEnabled = false
        var dpisChangeSucceeds = true
        var processAction: AppConfigProcessAction? = null
        var diagnosticDraft: EditorDraft? = null
        var savedDraft: EditorDraft? = null
        var closed = false
        var resetCalled = false
        var wechatDpiHelpShown = false
        var navigatedTo: ConfigEditorDestination? = null

        override fun updateDraft(draft: EditorDraft) {
            updatedDraft = draft
        }

        override fun resetDraft() {
            resetCalled = true
            updatedDraft = draft(false).cleared()
        }

        override fun showWechatDpiHelp() {
            wechatDpiHelpShown = true
        }

        override fun navigate(destination: ConfigEditorDestination) {
            navigatedTo = destination
        }

        override fun toggleScope(
            currentlySelected: Boolean,
            onSelected: Runnable,
            onDeselected: Runnable,
        ) {
            scopeSelectedBeforeCallback = currentlySelected
            onScopeSelected = onSelected
        }

        override fun setDpisEnabled(enabled: Boolean): Boolean {
            requestedDpisEnabled = enabled
            return dpisChangeSucceeds
        }

        override fun executeProcessAction(action: AppConfigProcessAction) {
            processAction = action
        }

        override fun startFeedbackDiagnostic(draft: EditorDraft) {
            diagnosticDraft = draft
        }

        override fun save(draft: EditorDraft) {
            savedDraft = draft
        }

        override fun close() {
            closed = true
        }
    }

    private companion object {
        fun draft(enabled: Boolean) = EditorDraft(
            "com.example.app", "100", "100", "", "relative_scale", "100",
            FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF,
            false, false, "", false, enabled,
        )

        fun app() = AppListItem(
            "Example", "com.example.app", false, true, null, null,
            ViewportApplyMode.OFF, null, ViewportTargetSpec.off(), null,
            FontApplyMode.OFF, null, false, null, true, false, true, false,
            false, null,
        )
    }
}
