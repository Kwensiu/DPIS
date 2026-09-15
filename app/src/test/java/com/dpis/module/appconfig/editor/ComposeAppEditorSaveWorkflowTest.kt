package com.dpis.module

import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeAppEditorSaveWorkflowTest {
    @Test
    fun saveReturnsFalseWhenItemOrDraftIsMissing() {
        val recording = Recording()
        val workflow = recording.workflow()

        assertFalse(workflow.save(null, null))
        assertFalse(workflow.save(null, draft()))
        assertFalse(workflow.save(ITEM, null))
        assertEquals(0, recording.persistCalls)
        assertEquals(0, recording.afterPersistCalls)
        assertEquals(0, recording.shownMessages.size)
        assertFalse(recording.successfulSave)
    }

    @Test
    fun saveStopsAfterFailedPersistAndDoesNotRunEffects() {
        val recording = Recording(
            persistResult = AppConfigSaveHandler.Result.failure(R.string.status_save_invalid),
        )
        val workflow = recording.workflow()

        assertFalse(workflow.save(ITEM, draft()))
        assertEquals(1, recording.persistCalls)
        assertEquals(0, recording.afterPersistCalls)
        assertEquals(listOf(R.string.status_save_invalid), recording.shownMessages)
        assertFalse(recording.successfulSave)
    }

    @Test
    fun saveStopsAfterFailedPersistWithoutAMessage() {
        val recording = Recording(
            persistResult = AppConfigSaveHandler.Result.failure(0),
        )
        val workflow = recording.workflow()

        assertFalse(workflow.save(ITEM, draft()))
        assertTrue(recording.shownMessages.isEmpty())
        assertEquals(0, recording.afterPersistCalls)
        assertFalse(recording.successfulSave)
    }

    @Test
    fun saveStopsWhenAfterPersistFails() {
        val recording = Recording(
            persistResult = AppConfigSaveHandler.Result.success(0),
            afterPersistResult = AppConfigSaveHandler.Result.failure(R.string.status_save_invalid),
        )
        val workflow = recording.workflow()

        assertFalse(workflow.save(ITEM, draft()))
        assertEquals(1, recording.afterPersistCalls)
        assertEquals(listOf(R.string.status_save_invalid), recording.shownMessages)
        assertFalse(recording.successfulSave)
    }

    @Test
    fun saveRunsPostPersistEffectsAndSurfacesSuccess() {
        val recording = Recording(
            persistResult = AppConfigSaveHandler.Result.success(0),
        )
        val workflow = recording.workflow()
        val savedDraft = draft()

        assertTrue(workflow.save(ITEM, savedDraft))
        assertEquals(1, recording.persistCalls)
        assertSame(ITEM, recording.savedItem)
        assertEquals(ViewportTargetType.RELATIVE_SCALE, recording.savedViewportTargetType)
        assertEquals(ViewportApplyMode.AUTO, recording.savedViewportApplyMode)
        assertEquals(100, recording.savedFontPercent)
        assertEquals(FontApplyMode.SYSTEM_EMULATION, recording.savedFontMode)
        assertEquals("serif", recording.savedTypefaceId)
        assertEquals("resources_font", recording.savedHookDomains)
        assertTrue(recording.savedViewportApplyModeReset)
        assertTrue(recording.savedFontHookDomainsReset)
        assertEquals("100", recording.savedViewportScaleInput)
        assertEquals("600", recording.savedViewportAbsoluteInput)
        assertEquals("480", recording.syncedWechatDpi)
        assertEquals(ITEM.packageName, recording.syncedPackageName)
        assertTrue(recording.syncedDpisEnabled)
        assertTrue(recording.shownMessages.isEmpty())
        assertTrue(recording.successfulSave)
        assertSame(ITEM, recording.successfulItem)
        assertSame(savedDraft, recording.successfulDraft)
    }

    @Test
    fun saveSurfacesPersistHintThenRunsSuccessfulEffects() {
        val persistMessage = R.string.status_save_invalid
        val recording = Recording(
            persistResult = AppConfigSaveHandler.Result.success(persistMessage),
        )
        val workflow = recording.workflow()

        assertTrue(workflow.save(ITEM, draft()))
        assertEquals(listOf(persistMessage), recording.shownMessages)
        assertTrue(recording.successfulSave)
    }

    private class Recording(
        private val persistResult: AppConfigSaveHandler.Result =
            AppConfigSaveHandler.Result.success(0),
        private val afterPersistResult: AppConfigSaveHandler.Result? = null,
    ) {
        var persistCalls = 0
        var afterPersistCalls = 0
        var savedItem: AppListItem? = null
        var savedViewportTargetType: String? = null
        var savedViewportApplyMode: String? = null
        var savedFontPercent: Int? = null
        var savedFontMode: String? = null
        var savedTypefaceId: String? = null
        var savedHookDomains: String? = null
        var savedViewportApplyModeReset = false
        var savedFontHookDomainsReset = false
        var savedViewportScaleInput: String? = null
        var savedViewportAbsoluteInput: String? = null
        var syncedWechatDpi: String? = null
        var syncedPackageName: String? = null
        var syncedDpisEnabled = false
        val shownMessages = mutableListOf<Int>()
        var successfulSave = false
        var successfulItem: AppListItem? = null
        var successfulDraft: EditorDraft? = null

        fun workflow() = ComposeAppEditorSaveWorkflow(
            persister = ComposeAppEditorSaveWorkflow.Persister { item, draft, _, fontPercent ->
                persistCalls++
                savedItem = item
                savedViewportTargetType = draft.viewportMode
                savedViewportApplyMode = draft.viewportApplyMode
                savedFontPercent = fontPercent
                savedFontMode = draft.fontMode
                savedTypefaceId = draft.selectedTypefaceId
                savedHookDomains = draft.draftFontHookDomainsRaw
                savedViewportApplyModeReset = draft.viewportApplyModeResetRequested
                savedFontHookDomainsReset = draft.fontHookDomainsResetRequested
                savedViewportScaleInput = draft.viewportScaleInput
                savedViewportAbsoluteInput = draft.viewportAbsoluteInput
                persistResult
            },
            effects = object : ComposeAppEditorSaveWorkflow.PostSaveEffects {
                override fun afterPersist(
                    result: AppConfigSaveHandler.Result,
                    item: AppListItem,
                    draft: EditorDraft,
                ): AppConfigSaveHandler.Result {
                    afterPersistCalls++
                    syncedWechatDpi = draft.wechatDpiInput
                    syncedPackageName = item.packageName
                    syncedDpisEnabled = draft.dpisEnabled
                    return afterPersistResult ?: result
                }

                override fun showMessage(messageResId: Int) {
                    shownMessages += messageResId
                }

                override fun afterSuccessfulSave(item: AppListItem, draft: EditorDraft) {
                    successfulSave = true
                    successfulItem = item
                    successfulDraft = draft
                }
            },
        )
    }

    private companion object {
        val ITEM: AppListItem = AppListItem(
            "Example",
            "com.example.app",
            true,
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

        fun draft() = EditorDraft(
            ITEM.packageName,
            "100",
            "100",
            "600",
            ViewportTargetType.RELATIVE_SCALE,
            "100",
            FontApplyMode.SYSTEM_EMULATION,
            "serif",
            "resources_font",
            ViewportApplyMode.AUTO,
            true,
            true,
            "480",
            true,
            true,
        )
    }
}
