package com.dpis.module

import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.editor.AppConfigEditorPersister
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigEditorPersisterTest {
    @Test
    fun persistMapsParsedDraftOntoTheSharedSaveHandler() {
        val store = DpisConfigStore(FakePrefs())
        val item = AppListItem(
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
        val persister = AppConfigEditorPersister(
            AppConfigSaveHandler(),
            { true },
            { store },
        )
        val draft = EditorDraft(
            item.packageName,
            "100",
            "100",
            "600",
            ViewportTargetType.RELATIVE_SCALE,
            "140",
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            ViewportApplyMode.AUTO,
            false,
            false,
            "",
            true,
            true,
        )

        val result = persister.persist(
            item,
            draft,
            ViewportTargetSpec.relativeScale(100000),
            140,
        )

        assertTrue(result.success)
        assertEquals(140, store.getTargetFontScalePercent(item.packageName))
        assertEquals(FontApplyMode.SYSTEM_EMULATION, store.getTargetFontApplyMode(item.packageName))
        assertEquals(ViewportApplyMode.AUTO, store.getTargetViewportApplyMode(item.packageName))
    }
}
