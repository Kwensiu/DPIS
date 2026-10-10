package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class WechatDpiEvidenceTest {
    @Test
    fun mutationWinsOverEarlierDeferredDisplayMetricsAttempt() {
        val summary = WechatDpiEvidence.summarize(
            listOf(
                event("displaymetrics", "deferred", "attempt=package_ready"),
                event("displaymetrics", "mutation_applied", "attempt=application_attach"),
                event("bottom_tab_icon", "hook_ready", "attempt=application_attach"),
            ),
        )

        assertEquals("mutation applied", summary.displayMetrics)
        assertEquals("hook ready; no callback observed during session", summary.bottomTabIcon)
    }

    @Test
    fun legacyTransportMessageStillIdentifiesTheRouteName() {
        val summary = WechatDpiEvidence.summarize(
            listOf(
                "11-15 06:13:20.100 source=runtime-transport category=runtime " +
                        "route=wechat_dpi stage=skipped package=com.tencent.mm " +
                        "message=hot path route=bottom_tab_icon, reason=class_not_found",
            ),
        )

        assertEquals(
            "skipped (hot path route=bottom_tab_icon, reason=class_not_found)",
            summary.bottomTabIcon,
        )
    }

    private fun event(routeName: String, stage: String, detail: String): String =
        "11-15 06:13:20.100 source=runtime-transport category=runtime " +
                "route=wechat_dpi routeName=$routeName stage=$stage package=com.tencent.mm message=$detail"
}
