package com.dpis.module.viewport

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourcesMetricsReadReuseTest {
    @After
    fun tearDown() {
        ResourcesMetricsReadReuse.clearForTest()
    }

    @Test
    fun unchangedReadReusesUntilTheWindowExpires() {
        val scope = Any()

        assertFalse(matches(scope, 1_000L))
        remember(scope, 1_000L)

        assertTrue(matches(scope, 1_000L + 500_000_000L))
        assertFalse(matches(scope, 1_000L + ResourcesMetricsReadReuse.REUSE_TTL_NANOS))
    }

    @Test
    fun returnedMetricsAndGenerationDecideTheHit() {
        val scope = Any()
        remember(scope, 1_000L)

        assertTrue(matches(scope, 1_000L))
        assertFalse(
            ResourcesMetricsReadReuse.matchesDisplayMetrics(
                scope,
                320,
                3.0f.toBits(),
                3.0f.toBits(),
                1080,
                2208,
                1_000L,
            ),
        )
        assertFalse(matches(Any(), 1_000L))
        assertFalse(matches(null, 1_000L))
        ResourcesMetricsReadReuse.bump()
        assertFalse(matches(scope, 1_000L))
    }

    private fun remember(scope: Any, nowNanos: Long) {
        ResourcesMetricsReadReuse.remember(
            scope,
            480,
            3.0f.toBits(),
            3.0f.toBits(),
            1080,
            2208,
            nowNanos,
        )
    }

    private fun matches(scope: Any?, nowNanos: Long): Boolean {
        return ResourcesMetricsReadReuse.matchesDisplayMetrics(
            scope,
            480,
            3.0f.toBits(),
            3.0f.toBits(),
            1080,
            2208,
            nowNanos,
        )
    }
}
