package com.dpis.module.diagnostics;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.*;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.After;
import org.junit.Test;

public final class FeedbackDiagnosticRuntimeEventsTest {
    @After
    public void tearDown() {
        FeedbackDiagnosticRuntimeEvents.cancel();
    }

    @Test
    public void defaultClosedCollectorDoesNotRecord() {
        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "target app matched: package=com.example.app"
        );

        assertTrue(FeedbackDiagnosticRuntimeEvents.snapshotForTest().isEmpty());
    }

    @Test
    public void enabledCollectorRecordsTargetPackageEvents() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "target app matched: package=com.example.app, dpi=411"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertTrue(events.get(0).contains("package=com.example.app"));
    }

    @Test
    public void nonTargetPackageEventsAreIgnored() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "target app matched: package=com.other.app"
        );

        assertTrue(FeedbackDiagnosticRuntimeEvents.stopSnapshot().isEmpty());
    }

    @Test
    public void disabledViewportConfigMarksViewportHitUnexpected() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, false, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_VIEWPORT app-process state seeded: package=com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=unexpected_route_hit"));
    }

    @Test
    public void repeatedSameRouteEventEmitsWarning() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_VIEWPORT app-process state seeded: package=com.example.app"
        );
        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_VIEWPORT app-process state seeded: package=com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=repeated_write")));
    }

    @Test
    public void hookInstalledSummaryIsClassifiedAsConfigNotViewportMutation() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, false, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "hooks installed (safe mode): package=com.example.app "
                        + "viewportEnabled=false fontMode=FIELD_REWRITE "
                        + "resolvedViewportMode=off"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=config"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertFalse(events.get(0).contains("unexpected_route_hit"));
    }

    @Test
    public void hookReadyIsNotClassifiedAsMutationApplied() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT Flutter settings hook ready for com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=hook_ready"));
        assertFalse(events.get(0).contains("mutation_applied"));
    }

    @Test
    public void overrideIsClassifiedAsMutationApplied() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=mutation_applied"));
    }

    @Test
    public void featureOffSkipIsClassifiedAsSkipped() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, false, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "Resources write hooks skipped: package=com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=skipped"));
        assertFalse(events.get(0).contains("unexpected_route_hit"));
    }

    @Test
    public void appHookPlanWithSuppressedNoneIsConfigResolvedNotSkipped() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT app hook plan: package=com.example.app "
                        + "fontMode=field_rewrite suppressed=none "
                        + "debugDisableTextViewAbsoluteRewrite=false"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=config"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertFalse(events.get(0).contains("stage=skipped"));
    }

    @Test
    public void packageReadyAndModuleLoadedAreRouteCallbacks() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "module loaded onPackageLoaded enter: package=com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=app_process"));
        assertTrue(events.get(0).contains("stage=route_callback_entered"));
    }

    @Test
    public void skipHookMessagesRemainSkipped() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request(true, true, true, true));

        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT skip abstract WebSettings#setTextZoom hook: package=com.example.app"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=skipped"));
    }

    private static FeedbackDiagnosticCoordinator.Request request(
            boolean inScope,
            boolean dpisEnabled,
            boolean viewportEnabled,
            boolean fontEnabled
    ) {
        return new FeedbackDiagnosticCoordinator.Request(
                "com.example.app",
                "Example",
                "1.2.3",
                true,
                inScope,
                dpisEnabled,
                false,
                viewportEnabled ? ViewportTargetSpec.absoluteDp(411) : ViewportTargetSpec.off(),
                viewportEnabled ? ViewportApplyMode.AUTO : ViewportApplyMode.OFF,
                fontEnabled ? 120 : null,
                fontEnabled ? FontApplyMode.FIELD_REWRITE : FontApplyMode.OFF,
                null,
                null,
                null
        );
    }
}
