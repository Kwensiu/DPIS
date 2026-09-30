package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessPerformanceTest {
    @Test
    fun aggregatesRouteCountsAndLatencyPercentiles() {
        val performance = ProcessPerformance()

        performance.call("paint_fallback")
        performance.applied("paint_fallback")
        performance.skipped("paint_fallback", "known_applied")
        performance.kept("paint_fallback")
        performance.duration("paint_fallback", 1_000L)
        performance.duration("paint_fallback", 20_000L)
        performance.duration("paint_fallback", 10_000L)

        val route = performance.snapshot()["paint_fallback"]!!

        assertEquals(1L, route.calls)
        assertEquals(1L, route.applied)
        assertEquals(1L, route.skipped)
        assertEquals(1L, route.kept)
        assertEquals(3L, route.measuredCalls)
        assertEquals(10L, route.p50Us)
        assertEquals(20L, route.p95Us)
        assertEquals(20L, route.p99Us)
        assertEquals(20L, route.maxUs)
        assertEquals(1L, route.skipReasons["known_applied"])
    }

    @Test
    fun publishCadenceIsBoundedAndResettable() {
        val performance = ProcessPerformance()

        assertTrue(performance.shouldPublish(1_000L))
        assertTrue(!performance.shouldPublish(1_100L))
        assertTrue(performance.shouldPublish(1_500L))

        performance.call("font")
        performance.reset()
        assertTrue(performance.snapshot().isEmpty())
        assertTrue(performance.shouldPublish(2_000L))
    }

    @Test
    fun bodySamplesCoverTheSessionInsteadOfTheFirstEntries() {
        val performance = ProcessPerformance()

        for (call in 1..5) {
            performance.recordBodySample(
                "resources_read_display_metrics_override",
                call * 1_000L,
                2,
            )
        }

        val route = performance.snapshot()["resources_read_display_metrics_override"]!!
        assertEquals(5L, route.calls)
        assertEquals(2L, route.measuredCalls)
        assertEquals(2, route.sampleStride)
        assertEquals(2L, route.p50Us)
        assertEquals(5L, route.maxUs)
    }
}
