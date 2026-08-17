package com.dpis.module.diagnostics;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public final class WechatDpiEvidenceTest {
    @Test
    public void mutationWinsOverEarlierDeferredDisplayMetricsAttempt() {
        WechatDpiEvidence.Summary summary = WechatDpiEvidence.summarize(List.of(
                event("displaymetrics", "deferred", "attempt=package_ready"),
                event("displaymetrics", "mutation_applied", "attempt=application_attach"),
                event("bottom_tab_icon", "hook_ready", "attempt=application_attach")
        ));

        assertEquals("mutation applied", summary.getDisplayMetrics());
        assertEquals("hook ready; no callback observed during session", summary.getBottomTabIcon());
    }

    @Test
    public void legacyTransportMessageStillIdentifiesTheRouteName() {
        WechatDpiEvidence.Summary summary = WechatDpiEvidence.summarize(List.of(
                "11-15 06:13:20.100 source=runtime-transport category=runtime "
                        + "route=wechat_dpi stage=skipped package=com.tencent.mm "
                        + "message=hot path route=bottom_tab_icon, reason=class_not_found"
        ));

        assertEquals("skipped (hot path route=bottom_tab_icon, reason=class_not_found)",
                summary.getBottomTabIcon());
    }

    private static String event(String routeName, String stage, String detail) {
        return "11-15 06:13:20.100 source=runtime-transport category=runtime "
                + "route=wechat_dpi routeName=" + routeName
                + " stage=" + stage + " package=com.tencent.mm message=" + detail;
    }
}
