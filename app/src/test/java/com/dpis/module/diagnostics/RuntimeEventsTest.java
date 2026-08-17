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

public final class RuntimeEventsTest {
    @After
    public void tearDown() {
        RuntimeEvents.cancel();
    }

    @Test
    public void defaultClosedCollectorDoesNotRecord() {
        RuntimeEvents.recordDpisLog(
                "I",
                "target app matched: package=com.example.app"
        );

        assertTrue(RuntimeEvents.snapshotForTest().isEmpty());
    }

    @Test
    public void enabledCollectorRecordsTargetPackageEvents() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "target app matched: package=com.example.app, dpi=411"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertTrue(events.get(0).contains("package=com.example.app"));
    }

    @Test
    public void nonTargetPackageEventsAreIgnored() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "target app matched: package=com.other.app"
        );

        assertTrue(RuntimeEvents.stopSnapshot().isEmpty());
    }

    @Test
    public void typefaceEventsUseStableRouteAndOnlyRecordTheTargetPackage() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordTypeface(
                "com.example.app", "source_provider_loaded", "typefaceId=font_demo_ttc_1");
        RuntimeEvents.recordTypeface(
                "com.other.app", "replacement_hit", "source=Paint.setTypeface");

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=typeface"));
        assertTrue(events.get(0).contains("stage=source_provider_loaded"));
        assertTrue(events.get(0).contains("typefaceId=font_demo_ttc_1"));
    }

    @Test
    public void disabledViewportConfigMarksViewportHitUnexpected() {
        RuntimeEvents.start("com.example.app", request(true, true, false, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_VIEWPORT app-process state seeded: package=com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=unexpected_route_hit"));
    }

    @Test
    public void repeatedSameRouteEventEmitsWarning() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_VIEWPORT app-process state seeded: package=com.example.app"
        );
        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_VIEWPORT app-process state seeded: package=com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=repeated_write")));
    }

    @Test
    public void hookInstalledSummaryIsClassifiedAsConfigNotViewportMutation() {
        RuntimeEvents.start("com.example.app", request(true, true, false, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "hooks installed (safe mode): package=com.example.app "
                        + "viewportEnabled=false fontMode=FIELD_REWRITE "
                        + "resolvedViewportMode=off"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=config"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertFalse(events.get(0).contains("unexpected_route_hit"));
    }

    @Test
    public void hookReadyIsNotClassifiedAsMutationApplied() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT Flutter settings hook ready for com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=hook_ready"));
        assertFalse(events.get(0).contains("mutation_applied"));
    }

    @Test
    public void hotPathEventsKeepRouteNameInUiMemoryEvidence() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeHotPathEvents.event(
                "com.example.app",
                "wechat_dpi",
                "bottom_tab_icon",
                "hook_ready",
                "attempt=application_attach"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=wechat_dpi"));
        assertTrue(events.get(0).contains("routeName=bottom_tab_icon"));
    }

    @Test
    public void overrideIsClassifiedAsMutationApplied() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=mutation_applied"));
    }

    @Test
    public void featureOffSkipIsClassifiedAsSkipped() {
        RuntimeEvents.start("com.example.app", request(true, true, false, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "Resources write hooks skipped: package=com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=skipped"));
        assertFalse(events.get(0).contains("unexpected_route_hit"));
    }

    @Test
    public void appHookPlanWithSuppressedNoneIsConfigResolvedNotSkipped() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT app hook plan: package=com.example.app "
                        + "fontMode=field_rewrite suppressed=none "
                        + "debugDisableTextViewAbsoluteRewrite=false"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=config"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertFalse(events.get(0).contains("stage=skipped"));
    }

    @Test
    public void packageReadyAndModuleLoadedAreRouteCallbacks() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "module loaded onPackageLoaded enter: package=com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=app_process"));
        assertTrue(events.get(0).contains("stage=route_callback_entered"));
    }

    @Test
    public void skipHookMessagesRemainSkipped() {
        RuntimeEvents.start("com.example.app", request(true, true, true, true));

        RuntimeEvents.recordDpisLog(
                "I",
                "DPIS_FONT skip abstract WebSettings#setTextZoom hook: package=com.example.app"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=skipped"));
    }

    private static Coordinator.Request request(
            boolean inScope,
            boolean dpisEnabled,
            boolean viewportEnabled,
            boolean fontEnabled
    ) {
        return new Coordinator.Request(
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
