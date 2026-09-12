package com.dpis.module

import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.EditorActions
import com.dpis.module.appconfig.EditorDraft
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

        actions.restartProcess()
        actions.startFeedbackDiagnostic()
        actions.save()
        actions.reset()
        actions.close()

        assertEquals(AppConfigDialogBinder.ProcessAction.RESTART, host.processAction)
        assertSame(draft, host.diagnosticDraft)
        assertSame(draft, host.savedDraft)
        assertTrue(host.resetCalled)
        assertTrue(host.closed)
    }

    private class RecordingHost : EditorActions.Host {
        var updatedDraft: EditorDraft? = null
        var scopeSelectedBeforeCallback = false
        var onScopeSelected: Runnable? = null
        var requestedDpisEnabled = false
        var dpisChangeSucceeds = true
        var processAction: AppConfigDialogBinder.ProcessAction? = null
        var diagnosticDraft: EditorDraft? = null
        var savedDraft: EditorDraft? = null
        var closed = false
        var resetCalled = false

        override fun updateDraft(draft: EditorDraft) {
            updatedDraft = draft
        }

        override fun resetDraft() {
            resetCalled = true
            updatedDraft = draft(false).cleared()
        }

        override fun showWechatDpiHelp() {}
        override fun navigate(destination: ConfigEditorDestination) {}

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

        override fun executeProcessAction(action: AppConfigDialogBinder.ProcessAction) {
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
