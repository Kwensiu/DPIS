package com.dpis.module.diagnostics

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackDiagnosticPageRequestTest {
    @Test
    fun keepsTargetIdentityForRestore() {
        val item = AppListItem(
            "App",
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
        val request = FeedbackDiagnosticPageRequest(item, EditorDraft.fromItem(item), "1.2.3")
        assertEquals("com.example.app", request.item.packageName)
        assertEquals("com.example.app", request.draft.packageName)
        assertEquals("1.2.3", request.versionName)
    }
}
