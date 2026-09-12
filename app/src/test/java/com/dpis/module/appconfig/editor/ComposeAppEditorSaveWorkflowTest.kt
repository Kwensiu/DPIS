package com.dpis.module

import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeAppEditorSaveWorkflowTest {
    @Test
    fun saveReturnsFalseWhenItemOrDraftIsMissing() {
        val host = RecordingHost()
        val workflow = ComposeAppEditorSaveWorkflow(host)

        assertFalse(workflow.save(null, null))
        assertFalse(workflow.save(null, draft()))
        assertFalse(workflow.save(ITEM, null))
        assertEquals(0, host.saveCalls)
        assertEquals(0, host.shownMessages.size)
    }

    @Test
    fun saveStopsAfterFailedPersistAndSurfacesTheFailureMessage() {
        val host = RecordingHost(
            persistResult = AppConfigSaveHandler.Result.failure(R.string.status_save_invalid),
        )
        val workflow = ComposeAppEditorSaveWorkflow(host)

        assertFalse(workflow.save(ITEM, draft()))
        assertEquals(1, host.saveCalls)
        assertEquals(listOf(R.string.status_save_invalid), host.shownMessages)
        assertFalse(host.scopeRequested)
        assertFalse(host.proxySynced)
        assertFalse(host.editorRefreshed)
    }

    @Test
    fun saveStopsAfterFailedPersistWithoutAMessage() {
        val host = RecordingHost(
            persistResult = AppConfigSaveHandler.Result.failure(0),
        )
        val workflow = ComposeAppEditorSaveWorkflow(host)

        assertFalse(workflow.save(ITEM, draft()))
        assertTrue(host.shownMessages.isEmpty())
        assertFalse(host.scopeRequested)
        assertFalse(host.proxySynced)
        assertFalse(host.editorRefreshed)
    }

    @Test
    fun saveRunsPostPersistEffectsAndSurfacesSuccess() {
        val host = RecordingHost(
            persistResult = AppConfigSaveHandler.Result.success(0),
        )
        val workflow = ComposeAppEditorSaveWorkflow(host)
        val savedDraft = draft()

        assertTrue(workflow.save(ITEM, savedDraft))
        assertEquals(1, host.saveCalls)
        assertSame(ITEM, host.savedItem)
        assertEquals(ViewportTargetType.RELATIVE_SCALE, host.savedViewportTargetType)
        assertEquals(ViewportApplyMode.AUTO, host.savedViewportApplyMode)
        assertEquals(100, host.savedFontPercent)
        assertEquals(FontApplyMode.SYSTEM_EMULATION, host.savedFontMode)
        assertEquals("serif", host.savedTypefaceId)
        assertEquals("resources_font", host.savedHookDomains)
        assertTrue(host.savedViewportApplyModeReset)
        assertTrue(host.savedFontHookDomainsReset)
        assertEquals("100", host.savedViewportScaleInput)
        assertEquals("600", host.savedViewportAbsoluteInput)
        assertEquals("480", host.syncedWechatDpi)
        assertEquals(ITEM.packageName, host.syncedPackageName)
        assertTrue(host.syncedDpisEnabled)
        assertEquals(listOf(R.string.status_save_success_inline), host.shownMessages)
        assertTrue(host.scopeRequested)
        assertTrue(host.proxySynced)
        assertTrue(host.editorRefreshed)
    }

    @Test
    fun saveSurfacesBothPersistAndSuccessMessages() {
        val persistMessage = R.string.status_save_invalid
        val host = RecordingHost(
            persistResult = AppConfigSaveHandler.Result.success(persistMessage),
        )
        val workflow = ComposeAppEditorSaveWorkflow(host)

        assertTrue(workflow.save(ITEM, draft()))
        assertEquals(
            listOf(persistMessage, R.string.status_save_success_inline),
            host.shownMessages,
        )
        assertTrue(host.scopeRequested)
        assertTrue(host.proxySynced)
        assertTrue(host.editorRefreshed)
    }

    private class RecordingHost(
        private val persistResult: AppConfigSaveHandler.Result = AppConfigSaveHandler.Result.success(0),
    ) : ComposeAppEditorSaveWorkflow.Host {
        var saveCalls = 0
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
        var scopeRequested = false
        var proxySynced = false
        var editorRefreshed = false

        override fun saveResolvedConfig(
            item: AppListItem,
            viewport: ViewportTargetSpec,
            viewportTargetType: String,
            viewportApplyMode: String,
            fontPercent: Int?,
            fontMode: String,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            viewportApplyModeResetRequested: Boolean,
            fontHookDomainsResetRequested: Boolean,
            viewportScaleInput: String,
            viewportAbsoluteInput: String,
        ): AppConfigSaveHandler.Result {
            saveCalls++
            savedItem = item
            savedViewportTargetType = viewportTargetType
            savedViewportApplyMode = viewportApplyMode
            savedFontPercent = fontPercent
            savedFontMode = fontMode
            savedTypefaceId = selectedTypefaceId
            savedHookDomains = draftFontHookDomainsRaw
            savedViewportApplyModeReset = viewportApplyModeResetRequested
            savedFontHookDomainsReset = fontHookDomainsResetRequested
            savedViewportScaleInput = viewportScaleInput
            savedViewportAbsoluteInput = viewportAbsoluteInput
            return persistResult
        }

        override fun finalizeRuntimeSync(
            result: AppConfigSaveHandler.Result,
            wechatDpiInput: String,
            packageName: String,
            dpisEnabled: Boolean,
        ): AppConfigSaveHandler.Result {
            syncedWechatDpi = wechatDpiInput
            syncedPackageName = packageName
            syncedDpisEnabled = dpisEnabled
            return result
        }

        override fun showMessage(messageResId: Int) {
            shownMessages += messageResId
        }

        override fun requestScopeAfterSave(item: AppListItem) {
            scopeRequested = true
        }

        override fun syncHyperOsNativeProxy(item: AppListItem) {
            proxySynced = true
        }

        override fun refreshEditor() {
            editorRefreshed = true
        }
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
