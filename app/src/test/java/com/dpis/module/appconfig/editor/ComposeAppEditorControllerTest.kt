package com.dpis.module

import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.appconfig.AppConfigEditorChip
import com.dpis.module.appconfig.EditorDialogStateFactory
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeAppEditorControllerTest {
    @Test
    fun createStateRequiresMatchingPackageItemAndSession() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)

        assertNull(controller.createState())

        viewModel.editingPackageName = ITEM.packageName
        assertNull(controller.createState())

        host.item = null
        viewModel.editorSession = session(ITEM, saved = false)
        assertNull(controller.createState())

        host.item = OTHER
        viewModel.editorSession = session(ITEM, saved = false)
        assertNull(controller.createState())
    }

    @Test
    fun createStateProjectsPrefillSessionAndWiresActions() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)

        controller.open(ITEM)

        val state = controller.createState()
        assertNotNull(state)
        assertEquals(AppConfigEditorChip.PREFILL, state!!.chip)
        assertFalse(state.dirty)
        assertEquals("1.2.3", state.versionName)
        assertEquals(ConfigEditorDestination.MAIN, state.destination)

        state.actions.reset()
        assertEquals(AppConfigEditorChip.NONE, viewModel.editorSession!!.chip)
        assertTrue(viewModel.editorSession!!.prefillInvalidated)
    }

    @Test
    fun openIgnoresNullAndReusesSessionForTheSamePackage() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)

        controller.open(null)
        assertNull(viewModel.editorSession)
        assertEquals(0, host.refreshCount)

        controller.open(ITEM)
        val opened = viewModel.editorSession
        assertEquals(AppConfigEditorChip.PREFILL, opened!!.chip)
        assertEquals(1, host.refreshCount)

        controller.open(ITEM)
        assertSame(opened, viewModel.editorSession)
        assertEquals(2, host.refreshCount)
    }

    @Test
    fun openReplacesSessionWhenPackageChangesAndFallsBackWhenHostHasNoItem() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)

        controller.open(ITEM)
        host.item = null
        controller.open(OTHER)

        assertEquals(OTHER.packageName, viewModel.editingPackageName)
        assertEquals(OTHER.packageName, viewModel.editorSession!!.draft.packageName)
        assertEquals(AppConfigEditorChip.PREFILL, viewModel.editorSession!!.chip)
    }

    @Test
    fun openSavedPackageIgnoresPrefillAndCanRestoreClosedDraft() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost(hasSaved = true)
        val restored = editorDraft(ITEM.packageName, "140")
        host.restoredDraft = restored
        val controller = ComposeAppEditorController(viewModel, host)

        controller.open(ITEM)

        assertNull(viewModel.editorSession!!.prefillSnapshot)
        assertEquals("140", viewModel.editorSession!!.draft.fontInput)
        assertEquals(AppConfigEditorChip.NONE, viewModel.editorSession!!.chip)
    }

    @Test
    fun openSavedPackageKeepsOpenedSessionWhenClosedDraftCannotBeRestored() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost(hasSaved = true)
        host.restoredDraft = null
        val controller = ComposeAppEditorController(viewModel, host)

        controller.open(ITEM)

        assertNull(viewModel.editorSession!!.prefillSnapshot)
        assertEquals(AppConfigEditorChip.NONE, viewModel.editorSession!!.chip)
        assertEquals("", viewModel.editorSession!!.draft.fontInput)
    }

    @Test
    fun updateAndResetDraftNoOpWithoutSessionThenMutateWhenOpen() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)
        val next = editorDraft(ITEM.packageName, "140")

        controller.updateDraft(null)
        controller.updateDraft(next)
        controller.resetDraft()
        assertNull(viewModel.editorSession)

        controller.open(ITEM)
        controller.updateDraft(next)
        assertEquals(AppConfigEditorChip.UNSAVED, viewModel.editorSession!!.chip)
        assertEquals("140", viewModel.editorSession!!.draft.fontInput)

        controller.resetDraft()
        assertEquals(AppConfigEditorChip.NONE, viewModel.editorSession!!.chip)
        assertEquals("", viewModel.editorSession!!.draft.fontInput)
    }

    @Test
    fun updateAdvancedDraftCopiesDialogOwnedFieldsIntoTheSession() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)
        controller.open(ITEM)
        val dialogState = AppConfigDialogBinder.AppConfigDialogState.fromItem(ITEM)
        dialogState.selectedTypefaceId = "serif"
        dialogState.draftFontHookDomainsRaw = "resources_font"
        dialogState.viewportApplyMode = ViewportApplyMode.SYSTEM
        dialogState.fontHookDomainsResetRequested = true
        dialogState.viewportApplyModeResetRequested = true

        controller.updateAdvancedDraft(viewModel.editorSession!!.draft, dialogState)

        val draft = viewModel.editorSession!!.draft
        assertEquals("serif", draft.selectedTypefaceId)
        assertEquals("resources_font", draft.draftFontHookDomainsRaw)
        assertEquals(ViewportApplyMode.SYSTEM, draft.viewportApplyMode)
        assertTrue(draft.fontHookDomainsResetRequested)
        assertTrue(draft.viewportApplyModeResetRequested)
    }

    @Test
    fun refreshCloseAndSaveFeedbackFollowHostAndSessionBoundaries() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)
        controller.open(ITEM)

        controller.refresh()
        assertEquals(1, host.appsLoadCount)

        controller.markSaved(null)
        assertFalse(viewModel.isEditingSaveFeedback)

        val draft = viewModel.editorSession!!.draft
        controller.markSaved(draft)
        assertTrue(viewModel.isEditingSaveFeedback)
        assertNull(viewModel.editorSession!!.prefillSnapshot)
        assertEquals(AppConfigEditorChip.NONE, viewModel.editorSession!!.chip)
        assertEquals(1, host.delayed.size)
        host.delayed.single().run()
        assertFalse(viewModel.isEditingSaveFeedback)

        controller.markSaved(draft)
        viewModel.isEditingSaveFeedback = false
        host.delayed.last().run()
        assertFalse(viewModel.isEditingSaveFeedback)

        controller.close()
        assertNull(viewModel.editorSession)
        assertNull(viewModel.editingPackageName)
    }

    @Test
    fun presentationActionsDelegateSaveCloseNavigateAndSideEffects() {
        val viewModel = MainViewModel(emptyState())
        val host = RecordingHost()
        val controller = ComposeAppEditorController(viewModel, host)
        controller.open(ITEM)
        val presented = controller.createState()!!
        val actions = presented.actions

        actions.navigate(ConfigEditorDestination.TYPEFACE)
        assertEquals(ConfigEditorDestination.TYPEFACE, viewModel.editingDestination)

        actions.showWechatDpiHelp()
        assertTrue(host.wechatHelpShown)

        actions.toggleScope()
        assertTrue(host.scopeToggled)

        actions.toggleDpisEnabled()
        assertFalse(host.requestedDpisEnabled!!)

        actions.startProcess()
        assertEquals(AppConfigDialogBinder.ProcessAction.START, host.processAction)

        actions.startFeedbackDiagnostic()
        assertSame(presented.draft, host.diagnosticDraft)

        host.saveSucceeds = false
        actions.save()
        assertFalse(viewModel.isEditingSaveFeedback)

        host.saveSucceeds = true
        actions.save()
        assertTrue(viewModel.isEditingSaveFeedback)

        actions.close()
        assertNull(viewModel.editorSession)
    }

    @Test
    fun markSavedNoOpsWhenThereIsNoSession() {
        val viewModel = MainViewModel(emptyState())
        val controller = ComposeAppEditorController(viewModel, RecordingHost())

        controller.markSaved(editorDraft(ITEM.packageName, "125"))

        assertNull(viewModel.editorSession)
        assertFalse(viewModel.isEditingSaveFeedback)
    }

    private class RecordingHost(
        var hasSaved: Boolean = false,
    ) : ComposeAppEditorController.Host {
        var item: AppListItem? = ITEM
        var restoredDraft: EditorDraft? = null
        var refreshCount = 0
        var appsLoadCount = 0
        var wechatHelpShown = false
        var scopeToggled = false
        var requestedDpisEnabled: Boolean? = null
        var processAction: AppConfigDialogBinder.ProcessAction? = null
        var diagnosticDraft: EditorDraft? = null
        var saveSucceeds = true
        val delayed = mutableListOf<Runnable>()

        override fun resolveEditorItem(packageName: String) = item?.takeIf { it.packageName == packageName }
        override fun hasSavedPackageConfig(packageName: String) = hasSaved
        override fun resolveGlobalPrefill(): TemplateConfigValue? = PREFILL
        override fun resolvePackageVersionName(packageName: String) = "1.2.3"
        override fun createDialogState(item: AppListItem, draft: EditorDraft) =
            EditorDialogStateFactory.create(item, draft)
        override fun typefaceSelectorText(typefaceId: String?) = typefaceId ?: "default"
        override fun hookChainText(
            item: AppListItem,
            state: AppConfigDialogBinder.AppConfigDialogState,
        ) = "hook"
        override fun systemHooksEnabled() = true
        override fun automaticFontHookDomains() = setOf("android.widget.TextView")
        override fun restoreClosedDraft(item: AppListItem, draft: EditorDraft?) = restoredDraft
        override fun refreshEditor() { refreshCount++ }
        override fun requestAppsLoad() { appsLoadCount++ }
        override fun showWechatDpiHelp() { wechatHelpShown = true }
        override fun toggleScope(
            item: AppListItem,
            currentlySelected: Boolean,
            onSelected: Runnable,
            onDeselected: Runnable,
        ) {
            scopeToggled = true
        }
        override fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean {
            requestedDpisEnabled = enabled
            return true
        }
        override fun executeProcessAction(
            item: AppListItem,
            action: AppConfigDialogBinder.ProcessAction,
        ) {
            processAction = action
        }
        override fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft) {
            diagnosticDraft = draft
        }
        override fun save(item: AppListItem, draft: EditorDraft) = saveSucceeds
        override fun postDelayed(delayMillis: Long, action: Runnable) {
            delayed += action
        }
    }

    private companion object {
        val PREFILL: TemplateConfigValue = TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.relativeScale(87500),
            ViewportApplyMode.AUTO,
            125,
            FontApplyMode.FIELD_REWRITE,
            "serif",
            "resources_font",
        )
        val ITEM: AppListItem = app("Example", "com.example.app")
        val OTHER: AppListItem = app("Other", "com.example.other")

        fun emptyState() = MainUiState.initial(
            "",
            AppListFilterState.defaultState(),
            emptyList(),
            emptySet(),
        )

        fun editorDraft(packageName: String, fontInput: String) = EditorDraft(
            packageName, "", "", "", "relative_scale", fontInput,
            FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF,
            false, false, "", true, true,
        )

        fun session(item: AppListItem, saved: Boolean) =
            com.dpis.module.appconfig.AppConfigEditorSession.open(item, saved, PREFILL)

        fun app(label: String, packageName: String) = AppListItem(
            label,
            packageName,
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
