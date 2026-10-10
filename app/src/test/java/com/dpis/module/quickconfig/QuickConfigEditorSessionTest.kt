package com.dpis.module

import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.quickconfig.QuickConfigEditorSession
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class QuickConfigEditorSessionTest {
    @Test
    fun configurationChangeKeepsDraftBaselineAndChildDestination() {
        val draft = draft("125", "112")
        val savedDraft = draft("100", "100")
        val session = QuickConfigEditorSession(
            null,
            draft,
            savedDraft,
            ConfigEditorDestination.HOOK_CHAIN_FONT
        )

        assertSame(draft, session.draft)
        assertSame(savedDraft, session.savedDraft)
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_FONT, session.destination)
    }

    @Test
    fun missingSavedBaselineAndDestinationUseCurrentDraftAndMainPage() {
        val draft = draft("125", "112")
        val session = QuickConfigEditorSession(null, draft, null, null)

        assertSame(draft, session.savedDraft)
        assertEquals(ConfigEditorDestination.MAIN, session.destination)
    }

    private fun draft(viewport: String, font: String) = EditorDraft(
        "com.example.target",
        viewport,
        viewport,
        "",
        ViewportTargetType.RELATIVE_SCALE,
        font,
        "system",
        null,
        null,
        ViewportApplyMode.AUTO,
        false,
        false,
        "",
        false,
        true,
    )
}
