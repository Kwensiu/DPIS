package com.dpis.module.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public final class FeedbackDiagnosticProcessPerformanceTest {
    @Test
    public void aggregatesRouteCountsAndLatencyPercentiles() {
        FeedbackDiagnosticProcessPerformance performance =
                new FeedbackDiagnosticProcessPerformance();

        performance.call("paint_fallback");
        performance.applied("paint_fallback");
        performance.skipped("paint_fallback", "known_applied");
        performance.kept("paint_fallback");
        performance.duration("paint_fallback", 1_000L);
        performance.duration("paint_fallback", 20_000L);
        performance.duration("paint_fallback", 10_000L);

        Map<String, FeedbackDiagnosticProcessPerformance.RouteSnapshot> snapshot =
                performance.snapshot();
        FeedbackDiagnosticProcessPerformance.RouteSnapshot route =
                snapshot.get("paint_fallback");

        assertEquals(1L, route.calls);
        assertEquals(1L, route.applied);
        assertEquals(1L, route.skipped);
        assertEquals(1L, route.kept);
        assertEquals(3L, route.measuredCalls);
        assertEquals(10L, route.p50Us);
        assertEquals(20L, route.p95Us);
        assertEquals(20L, route.p99Us);
        assertEquals(20L, route.maxUs);
        assertEquals(Long.valueOf(1L), route.skipReasons.get("known_applied"));
    }

    @Test
    public void publishCadenceIsBoundedAndResettable() {
        FeedbackDiagnosticProcessPerformance performance =
                new FeedbackDiagnosticProcessPerformance();

        assertTrue(performance.shouldPublish(1_000L));
        assertTrue(!performance.shouldPublish(1_100L));
        assertTrue(performance.shouldPublish(1_500L));

        performance.call("font");
        performance.reset();
        assertTrue(performance.snapshot().isEmpty());
        assertTrue(performance.shouldPublish(2_000L));
    }
}
