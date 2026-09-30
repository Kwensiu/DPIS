package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessPerformanceParserTest {
    @Test
    fun groupsTransportSnapshotsByProcessAndPid() {
        val events = listOf(
            "08-12 23:00:00.000 source=runtime-transport " +
                    "category=performance route=runtime stage=aggregate " +
                    "package=com.example.app message=process=com.example.app,pid=123;" +
                    "route=paint_fallback,calls=20,applied=3,skipped=17,kept=4," +
                    "measuredCalls=3,sampleStride=50,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
            "08-12 23:00:01.000 source=runtime-transport " +
                    "category=performance route=runtime stage=aggregate " +
                    "package=com.example.app message=process=com.google.android.webview,pid=456;" +
                    "route=webview_text_zoom,calls=2,applied=2,skipped=0," +
                    "measuredCalls=2,p50Us=5,p95Us=9,p99Us=9,maxUs=9",
        )

        val result = ProcessPerformanceParser.parse(events)

        assertEquals(2, result.size)
        assertEquals("com.example.app", result[0].process)
        assertEquals("123", result[0].pid)
        assertEquals(20L, result[0].routes["paint_fallback"]!!.calls)
        assertEquals(4L, result[0].routes["paint_fallback"]!!.kept)
        assertEquals(50, result[0].routes["paint_fallback"]!!.sampleStride)
        assertEquals("com.google.android.webview", result[1].process)
        assertEquals("456", result[1].pid)
        assertEquals(2L, result[1].routes["webview_text_zoom"]!!.applied)
    }

    @Test
    fun derivesFallbackCountsFromMutationAppliedLogs() {
        val events = listOf(
            "08-13 21:21:51.920 source=lsposed-log category=runtime route=font " +
                    "stage=mutation_applied level=I package=com.example.app " +
                    "process=com.example.app message=DPIS DPIS_FONT Paint.setTextSize " +
                    "fallback applied: package=com.example.app, hookId=paint_set_text_size",
            "08-13 21:21:54.816 source=lsposed-log category=runtime route=font " +
                    "stage=mutation_applied level=I package=com.example.app " +
                    "process=com.example.app message=DPIS DPIS_FONT TextView span rewrite " +
                    "applied: package=com.example.app, hookId=textview_set_text",
        )

        val result = ProcessPerformanceParser.parseMutationAppliedFallback(events)

        assertEquals(1, result.size)
        assertEquals("com.example.app", result[0].process)
        assertEquals("unknown", result[0].pid)
        assertEquals(1L, result[0].routes["paint_set_text_size"]!!.applied)
        assertEquals(1L, result[0].routes["textview_set_text"]!!.calls)
    }
}
