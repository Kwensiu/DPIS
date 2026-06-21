package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
