package com.dpis.module.diagnostics;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public final class ProcessPerformanceParserTest {
    @Test
    public void groupsTransportSnapshotsByProcessAndPid() {
        List<String> events = List.of(
                "08-12 23:00:00.000 source=runtime-transport "
                        + "category=performance route=runtime stage=aggregate "
                        + "package=com.example.app message=process=com.example.app,pid=123;"
                        + "route=paint_fallback,calls=20,applied=3,skipped=17,kept=4,"
                        + "measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
                "08-12 23:00:01.000 source=runtime-transport "
                        + "category=performance route=runtime stage=aggregate "
                        + "package=com.example.app message=process=com.google.android.webview,pid=456;"
                        + "route=webview_text_zoom,calls=2,applied=2,skipped=0,"
                        + "measuredCalls=2,p50Us=5,p95Us=9,p99Us=9,maxUs=9"
        );

        List<ProcessPerformanceParser.ProcessSummary> result =
                ProcessPerformanceParser.parse(events);

        assertEquals(2, result.size());
        assertEquals("com.example.app", result.get(0).process);
        assertEquals("123", result.get(0).pid);
        assertEquals(20L, result.get(0).routes.get("paint_fallback").calls);
        assertEquals(4L, result.get(0).routes.get("paint_fallback").kept);
        assertEquals("com.google.android.webview", result.get(1).process);
        assertEquals("456", result.get(1).pid);
        assertEquals(2L, result.get(1).routes.get("webview_text_zoom").applied);
    }

    @Test
    public void derivesFallbackCountsFromMutationAppliedLogs() {
        List<String> events = List.of(
                "08-13 21:21:51.920 source=lsposed-log category=runtime route=font "
                        + "stage=mutation_applied level=I package=com.example.app "
                        + "process=com.example.app message=DPIS DPIS_FONT Paint.setTextSize "
                        + "fallback applied: package=com.example.app, hookId=paint_set_text_size",
                "08-13 21:21:54.816 source=lsposed-log category=runtime route=font "
                        + "stage=mutation_applied level=I package=com.example.app "
                        + "process=com.example.app message=DPIS DPIS_FONT TextView span rewrite "
                        + "applied: package=com.example.app, hookId=textview_set_text"
        );

        List<ProcessPerformanceParser.ProcessSummary> result =
                ProcessPerformanceParser.parseMutationAppliedFallback(events);

        assertEquals(1, result.size());
        assertEquals("com.example.app", result.get(0).process);
        assertEquals("unknown", result.get(0).pid);
        assertEquals(1L, result.get(0).routes.get("paint_set_text_size").applied);
        assertEquals(1L, result.get(0).routes.get("textview_set_text").calls);
    }
}
