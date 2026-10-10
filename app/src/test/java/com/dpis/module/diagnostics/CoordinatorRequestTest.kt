package com.dpis.module.diagnostics

import com.dpis.module.FakePrefs
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinatorRequestTest {
    @Test
    fun fromUsesEditorDraftScopeAndDpisWhenPresent() {
        val item = app("com.tencent.mm", 600)
        val draft = EditorDraft.fromItem(item)
            .withScopeSelected(false)
            .withDpisEnabled(false)

        val request = Coordinator.Request.from(item, draft, "8.0.74")

        assertFalse(request.inScope)
        assertFalse(request.dpisEnabled)
        assertEquals(item.packageName, request.packageName)
        assertTrue(request.scopeKnown)
    }

    @Test
    fun fromUsesItemWechatDpiValue() {
        val item = app("com.tencent.mm", 600)

        val request = Coordinator.Request.from(item, null, "8.0.74")

        assertEquals(600, request.wechatDpi)
    }

    @Test
    fun fromAllowsClearedPersistedWechatDpi() {
        val item = app("com.tencent.mm", 600).withWechatDpi(null)

        val request = Coordinator.Request.from(item, null, "8.0.74")

        assertNull(request.wechatDpi)
    }

    @Test
    fun fromPersistedPrefersStoreOverStaleItemSnapshot() {
        val store = DpisConfigStore(FakePrefs())
        val packageName = "com.tencent.mm"
        assertTrue(
            store.setTargetViewportSpec(
                packageName,
                ViewportTargetSpec.relativeScale(90000)
            )
        )
        assertTrue(store.setTargetViewportApplyMode(packageName, ViewportApplyMode.SYSTEM))
        assertTrue(store.setTargetFontScalePercent(packageName, 125))
        assertTrue(store.setTargetFontApplyMode(packageName, FontApplyMode.FIELD_REWRITE))
        assertTrue(store.setTargetTypefaceId(packageName, "font_modern"))
        assertTrue(store.setPackageFontHookDomainsRaw(packageName, "resources_font"))
        assertTrue(store.setWechatDpi(packageName, 610))
        val staleItem = app(packageName, null)

        val request = Coordinator.Request.fromPersisted(staleItem, null, "8.0.74", store)

        assertTrue(request.dpisEnabled)
        assertFalse(request.previewFromGlobalPrefill)
        assertEquals(ViewportTargetSpec.relativeScale(90000), request.viewportTargetSpec)
        assertEquals(ViewportApplyMode.SYSTEM, request.viewportApplyMode)
        assertEquals(125, request.fontScalePercent)
        assertEquals(FontApplyMode.FIELD_REWRITE, request.fontApplyMode)
        assertEquals("font_modern", request.typefaceId)
        assertEquals("resources_font", request.fontHookDomainsRaw)
        assertEquals(610, request.wechatDpi)
    }

    @Test
    fun fromPersistedFallsBackWhenStoreUnavailable() {
        val item = app("com.tencent.mm", 600)

        val request = Coordinator.Request.fromPersisted(item, null, "8.0.74", null)

        assertEquals(600, request.wechatDpi)
    }

    private fun app(packageName: String, wechatDpi: Int?): AppListItem = AppListItem(
        "WeChat",
        packageName,
        true,
        true,
        null,
        null,
        ViewportApplyMode.OFF,
        ViewportTargetType.OFF,
        ViewportTargetSpec.off(),
        null,
        FontApplyMode.OFF,
        null,
        wechatDpi != null,
        wechatDpi,
        true,
        true,
        true,
        false,
        false,
        null,
    )
}
