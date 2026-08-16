package com.dpis.module.diagnostics;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.*;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

public final class RuntimeHotPathEventsTest {
    @After
    public void tearDown() {
        RuntimeEvents.cancel();
        RuntimeHotPathEvents.resetForTest();
        RuntimeBridgeEvents.setBridgeSink(null);
        RuntimeTransport.cancel(command ->
                new com.dpis.module.root.RootAppProcessLauncher.ShellResult(0, ""));
    }

    @Test
    public void doesNotRecordWhenDiagnosticClosed() {
        RuntimeHotPathEvents.begin(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );

        assertTrue(RuntimeEvents.snapshotForTest().isEmpty());
    }

    @Test
    public void recordsBeginAppliedEndWhenDiagnosticOpen() {
        RuntimeEvents.start("com.example.app", request());

        RuntimeHotPathEvents.begin(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );
        RuntimeHotPathEvents.applied(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );
        RuntimeHotPathEvents.end(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertEquals(3, events.size());
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=begin")));
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=applied")));
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=end")));
        assertTrue(events.stream().anyMatch(event -> event.contains("durationMs=")));
    }

    @Test
    public void repeatedAppliedEmitsRepeatedWrite() {
        RuntimeEvents.start("com.example.app", request());

        RuntimeHotPathEvents.applied(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        RuntimeHotPathEvents.applied(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=repeated_write")));
    }

    @Test
    public void recordsViewportRouteWhenCategoryRouteProvided() {
        RuntimeEvents.start("com.example.app", request());

        RuntimeHotPathEvents.applied(
                "com.example.app",
                "viewport",
                "resources_read_configuration_override",
                "source=ResourcesRead(getConfiguration), widthDp=360->324"
        );

        List<String> events = RuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event ->
                event.contains("route=viewport")
                        && event.contains("stage=applied")
                        && event.contains("resources_read_configuration_override")));
    }

    @Test
    public void aggregatesHotPathCountsAndLatencyPercentiles() {
        RuntimeEvents.start("com.example.app", request());

        RuntimeHotPathEvents.begin(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        RuntimeHotPathEvents.applied(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        RuntimeHotPathEvents.end(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        RuntimeHotPathEvents.skipped(
                "com.example.app",
                "paint_fallback",
                "already applied"
        );
        RuntimeHotPathEvents.kept(
                "com.example.app",
                "paint_fallback",
                "reason=current_target"
        );

        PerformanceSnapshot snapshot =
                RuntimeEvents.stopPerformanceSnapshot();
        assertEquals(1, snapshot.entries().size());
        PerformanceSnapshot.Entry entry = snapshot.entries().get(0);
        assertEquals("paint_fallback", entry.route);
        assertEquals(3L, entry.calls);
        assertEquals(1L, entry.applied);
        assertEquals(1L, entry.skipped);
        assertEquals(1L, entry.kept);
        assertEquals(1L, entry.measuredCalls);
        assertTrue(entry.p50Us >= 0L);
        assertTrue(entry.p95Us >= entry.p50Us);
        assertTrue(entry.p99Us >= entry.p95Us);
        assertEquals(Long.valueOf(1L), entry.skipReasons.get("already_applied"));
    }

    @Test
    public void keepsAreAggregatedWithoutPerCallbackTimelineRecords() {
        RuntimeEvents.start("com.example.app", request());

        RuntimeHotPathEvents.kept(
                "com.example.app",
                "paint_fallback",
                "reason=current_target"
        );

        assertTrue(RuntimeEvents.snapshotForTest().isEmpty());
        PerformanceSnapshot snapshot =
                RuntimeEvents.stopPerformanceSnapshot();
        assertEquals(1L, snapshot.entries().get(0).kept);
    }

    @Test
    public void emitsHotPathAndPerformanceToRegisteredBridgeSink() {
        List<String> bridgeLines = new ArrayList<>();
        RuntimeBridgeEvents.setBridgeSink(bridgeLines::add);
        RuntimeTransport.start(
                "com.example.app",
                command -> new com.dpis.module.root.RootAppProcessLauncher.ShellResult(
                        0,
                        ""
                )
        );

        RuntimeHotPathEvents.begin(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        RuntimeHotPathEvents.applied(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        RuntimeBridgeEvents.flushForTest();

        assertTrue(bridgeLines.stream().anyMatch(line ->
                line.contains("DPIS DPIS_DIAG_HOTPATH")
                        && line.contains("routeName=paint_fallback")));
        assertTrue(bridgeLines.stream().anyMatch(line ->
                line.contains("DPIS DPIS_DIAG_PERF")
                        && line.contains("route=paint_fallback")));
    }

    @Test
    public void doesNotEmitDiagnosticFallbackLogWhenCaptureInactive() {
        RuntimeHotPathEvents.begin(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );

        assertFalse(RuntimeTransport.isCaptureActive());
    }

    private static Coordinator.Request request() {
        return new Coordinator.Request(
                "com.example.app",
                "Example",
                "1.2.3",
                true,
                true,
                true,
                false,
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                120,
                FontApplyMode.FIELD_REWRITE,
                null,
                null,
                null
        );
    }
}
