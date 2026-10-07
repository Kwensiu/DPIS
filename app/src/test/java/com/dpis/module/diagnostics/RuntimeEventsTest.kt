package com.dpis.module.diagnostics

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeEventsTest {
    @After
    fun tearDown() {
        RuntimeEvents.cancel()
    }

    @Test
    fun closedCollectorDoesNotRecordRuntimeLogs() {
        RuntimeEvents.recordDpisLog("I", "target app matched: package=com.example.app")

        assertTrue(RuntimeEvents.snapshotForTest().isEmpty())
    }

    @Test
    fun recordsOnlyTargetPackageRuntimeLogs() {
        RuntimeEvents.start("com.example.app", request())

        RuntimeEvents.recordDpisLog(
            "I",
            "target app matched: package=com.example.app, dpi=411",
        )
        RuntimeEvents.recordDpisLog("I", "target app matched: package=com.other.app")

        val events = RuntimeEvents.stopSnapshot()
        assertEquals(1, events.size)
        assertTrue(events.single().contains("stage=config_resolved"))
        assertTrue(events.single().contains("package=com.example.app"))
    }

    @Test
    fun appLogMessagesEnterTheActiveDiagnosticTimeline() {
        RuntimeEvents.start("com.example.app", request())

        DpisLog.i("DPIS_FONT textScaleFactor override: package=com.example.app")

        val event = RuntimeEvents.stopSnapshot().single()
        assertTrue(event.contains("route=font"))
        assertTrue(event.contains("stage=mutation_applied"))
    }

    @Test
    fun typefaceEvidenceUsesStableRouteAndIgnoresOtherPackages() {
        RuntimeEvents.start("com.example.app", request())

        RuntimeEvents.recordTypeface(
            "com.example.app",
            "source_provider_loaded",
            "typefaceId=font_demo_ttc_1",
        )
        RuntimeEvents.recordTypeface("com.other.app", "replacement_hit", "source=Paint.setTypeface")

        val event = RuntimeEvents.stopSnapshot().single()
        assertTrue(event.contains("route=typeface"))
        assertTrue(event.contains("stage=source_provider_loaded"))
        assertTrue(event.contains("typefaceId=font_demo_ttc_1"))
    }

    @Test
    fun disabledViewportConfigMarksMutationUnexpected() {
        val events = eventFor(request(viewportEnabled = false), "DPIS_VIEWPORT app-process state seeded")

        assertEquals(1, events.size)
        assertTrue(events.single().contains("stage=unexpected_route_hit"))
    }

    @Test
    fun repeatedViewportMutationEmitsAWarning() {
        RuntimeEvents.start("com.example.app", request())
        repeat(2) {
            RuntimeEvents.recordDpisLog("I", "DPIS_VIEWPORT app-process state seeded: package=com.example.app")
        }

        assertTrue(RuntimeEvents.stopSnapshot().any { it.contains("stage=repeated_write") })
    }

    @Test
    fun installedHookSummaryIsConfigurationRatherThanMutation() {
        val event = eventFor(
            request(viewportEnabled = false),
            "hooks installed (safe mode): package=com.example.app " +
                "viewportEnabled=false fontMode=FIELD_REWRITE resolvedViewportMode=off",
        ).single()

        assertTrue(event.contains("route=config"))
        assertTrue(event.contains("stage=config_resolved"))
        assertFalse(event.contains("unexpected_route_hit"))
    }

    @Test
    fun hookReadinessDoesNotClaimAMutation() {
        val event = eventFor(
            request(),
            "DPIS_FONT Flutter settings hook ready for com.example.app",
        ).single()

        assertTrue(event.contains("route=font"))
        assertTrue(event.contains("stage=hook_ready"))
        assertFalse(event.contains("mutation_applied"))
    }

    @Test
    fun hotPathEvidenceKeepsItsRouteName() {
        RuntimeEvents.start("com.example.app", request())

        RuntimeHotPathEvents.event(
            "com.example.app",
            "wechat_dpi",
            "bottom_tab_icon",
            "hook_ready",
            "attempt=application_attach",
        )

        val event = RuntimeEvents.stopSnapshot().single()
        assertTrue(event.contains("route=wechat_dpi"))
        assertTrue(event.contains("routeName=bottom_tab_icon"))
    }

    @Test
    fun textScaleOverrideIsReportedAsApplied() {
        val event = eventFor(
            request(),
            "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app",
        ).single()

        assertTrue(event.contains("route=font"))
        assertTrue(event.contains("stage=mutation_applied"))
    }

    @Test
    fun disabledHookIsReportedAsSkippedWithoutUnexpectedRouteWarning() {
        val event = eventFor(
            request(dpisEnabled = false, viewportEnabled = false),
            "Resources write hooks skipped: package=com.example.app",
        ).single()

        assertTrue(event.contains("stage=skipped"))
        assertFalse(event.contains("unexpected_route_hit"))
    }

    @Test
    fun appHookPlanWithNoSuppressedDomainsIsConfiguration() {
        val event = eventFor(
            request(),
            "DPIS_FONT app hook plan: package=com.example.app " +
                "fontMode=field_rewrite suppressed=none debugDisableTextViewAbsoluteRewrite=false",
        ).single()

        assertTrue(event.contains("route=config"))
        assertTrue(event.contains("stage=config_resolved"))
        assertFalse(event.contains("stage=skipped"))
    }

    @Test
    fun packageLoadIsReportedAsRouteCallback() {
        val event = eventFor(
            request(),
            "module loaded onPackageLoaded enter: package=com.example.app",
        ).single()

        assertTrue(event.contains("route=app_process"))
        assertTrue(event.contains("stage=route_callback_entered"))
    }

    @Test
    fun skippedHookMessagesRemainSkipped() {
        val event = eventFor(
            request(),
            "DPIS_FONT skip abstract WebSettings#setTextZoom hook: package=com.example.app",
        ).single()

        assertTrue(event.contains("stage=skipped"))
    }

    private fun eventFor(request: Coordinator.Request, message: String): List<String> {
        RuntimeEvents.start("com.example.app", request)
        RuntimeEvents.recordDpisLog("I", "$message: package=com.example.app")
        return RuntimeEvents.stopSnapshot()
    }

    private fun request(
        inScope: Boolean = true,
        dpisEnabled: Boolean = true,
        viewportEnabled: Boolean = true,
        fontEnabled: Boolean = true,
    ) = Coordinator.Request(
        "com.example.app",
        "Example",
        "1.2.3",
        true,
        inScope,
        dpisEnabled,
        false,
        if (viewportEnabled) ViewportTargetSpec.absoluteDp(411) else ViewportTargetSpec.off(),
        if (viewportEnabled) ViewportApplyMode.AUTO else ViewportApplyMode.OFF,
        if (fontEnabled) 120 else null,
        if (fontEnabled) FontApplyMode.FIELD_REWRITE else FontApplyMode.OFF,
        null,
        null,
        null,
    )
}
