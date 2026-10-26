package com.dpis.module

import com.dpis.module.root.RootAccessProbe
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.ArrayDeque

class RootAccessProbeRefreshTest {
    @Test
    fun concurrentRefreshRequestsRerunAndReceiveTheLatestPermissionState() {
        val tasks = ArrayDeque<() -> Unit>()
        val results = ArrayDeque(
            listOf(
                RootAccessProbe.Result.unavailable(),
                RootAccessProbe.Result.available("test-root"),
            ),
        )
        val callbacks = mutableListOf<RootAccessProbe.Result?>()
        var probeCount = 0

        val probe: () -> RootAccessProbe.Result = {
            probeCount++
            results.removeFirst()
        }
        val executor: (() -> Unit) -> Unit = { task -> tasks.addLast(task) }
        RootAccessProbe.refreshAsyncForTest({ callbacks.add(it) }, probe, executor)
        RootAccessProbe.refreshAsyncForTest({ callbacks.add(it) }, probe, executor)

        assertEquals(1, tasks.size)
        tasks.removeFirst().invoke()

        assertEquals(2, probeCount)
        assertEquals(2, callbacks.size)
        assertEquals(RootAccessProbe.Status.AVAILABLE, callbacks[0]?.status)
        assertEquals(RootAccessProbe.Status.AVAILABLE, callbacks[1]?.status)
    }
}
