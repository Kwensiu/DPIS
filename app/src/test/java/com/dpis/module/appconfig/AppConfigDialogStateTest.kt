package com.dpis.module.appconfig

import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppConfigDialogStateTest {
    @Test
    fun fromItemKeepsIndependentScaleAndAbsoluteViewportInputs() {
        val item = AppListItem(
            "Example",
            "com.example.app",
            true,
            true,
            600,
            150_000,
            ViewportApplyMode.AUTO,
            ViewportTargetType.RELATIVE_SCALE,
            ViewportTargetSpec.relativeScale(150_000),
            120,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            true,
            true,
            false,
            false,
            null,
        )

        val state = AppConfigDialogState.fromItem(item)

        assertEquals("150", state.viewportScaleInput)
        assertEquals("600", state.viewportAbsoluteInput)
        assertEquals("150", state.viewportInputFor(ViewportTargetType.RELATIVE_SCALE))
        assertEquals("600", state.viewportInputFor(ViewportTargetType.ABSOLUTE_DP))
        assertFalse(state.fontHookDomainsResetRequested)
    }

    @Test
    fun fromOverlaysEditorDraftOntoItemBaseline() {
        val item = AppListItem(
            "Example",
            "com.example.app",
            true,
            true,
            600,
            150_000,
            ViewportApplyMode.AUTO,
            ViewportTargetType.RELATIVE_SCALE,
            ViewportTargetSpec.relativeScale(150_000),
            120,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            true,
            true,
            false,
            false,
            null,
        )
        val draft = EditorDraft.fromItem(item).withFontInput("110").withDpisEnabled(false)

        val state = AppConfigDialogState.from(item, draft)

        assertEquals(false, state.dpisEnabled)
        assertEquals(item.packageName, state.packageName)
    }
}
