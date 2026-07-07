package com.dpis.module.diagnostics;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.*;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.applist.AppListItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FeedbackDiagnosticCoordinatorRequestTest {

    @Test
    public void fromUsesItemWechatDpiValue() {
        AppListItem item = app("com.tencent.mm", 600);

        FeedbackDiagnosticCoordinator.Request request = FeedbackDiagnosticCoordinator.Request.from(
                item,
                null,
                "8.0.74"
        );

        assertEquals(Integer.valueOf(600), request.wechatDpi);
    }

    @Test
    public void fromAllowsClearedPersistedWechatDpi() {
        AppListItem item = app("com.tencent.mm", 600).withWechatDpi(null);

        FeedbackDiagnosticCoordinator.Request request = FeedbackDiagnosticCoordinator.Request.from(
                item,
                null,
                "8.0.74"
        );

        assertNull(request.wechatDpi);
    }

    @Test
    public void fromPersistedPrefersStoreOverStaleItemSnapshot() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        String packageName = "com.tencent.mm";
        assertTrue(store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(90000)));
        assertTrue(store.setTargetViewportApplyMode(packageName, ViewportApplyMode.SYSTEM));
        assertTrue(store.setTargetFontScalePercent(packageName, 125));
        assertTrue(store.setTargetFontApplyMode(packageName, FontApplyMode.FIELD_REWRITE));
        assertTrue(store.setTargetTypefaceId(packageName, "font_modern"));
        assertTrue(store.setPackageFontHookDomainsRaw(packageName, "resources_font"));
        assertTrue(store.setWechatDpi(packageName, 610));
        AppListItem staleItem = app(packageName, null);

        FeedbackDiagnosticCoordinator.Request request
                = FeedbackDiagnosticCoordinator.Request.fromPersisted(
                        staleItem,
                        null,
                        "8.0.74",
                        store
                );

        assertTrue(request.dpisEnabled);
        assertFalse(request.previewFromGlobalPrefill);
        assertEquals(ViewportTargetSpec.relativeScale(90000), request.viewportTargetSpec);
        assertEquals(ViewportApplyMode.SYSTEM, request.viewportApplyMode);
        assertEquals(Integer.valueOf(125), request.fontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, request.fontApplyMode);
        assertEquals("font_modern", request.typefaceId);
        assertEquals("resources_font", request.fontHookDomainsRaw);
        assertEquals(Integer.valueOf(610), request.wechatDpi);
    }

    @Test
    public void fromPersistedFallsBackWhenStoreUnavailable() {
        AppListItem item = app("com.tencent.mm", 600);

        FeedbackDiagnosticCoordinator.Request request
                = FeedbackDiagnosticCoordinator.Request.fromPersisted(
                        item,
                        null,
                        "8.0.74",
                        null
                );

        assertEquals(Integer.valueOf(600), request.wechatDpi);
    }

    private static AppListItem app(String packageName, Integer wechatDpi) {
        return new AppListItem(
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
                null
        );
    }
}
