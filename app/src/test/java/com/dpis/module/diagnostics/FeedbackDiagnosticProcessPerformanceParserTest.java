package com.dpis.module.diagnostics;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public final class FeedbackDiagnosticProcessPerformanceParserTest {
    @Test
    public void groupsTransportSnapshotsByProcessAndPid() {
        List<String> events = List.of(
                "08-12 23:00:00.000 source=runtime-transport "
                        + "category=performance route=runtime stage=aggregate "
                        + "package=com.example.app message=process=com.example.app,pid=123;"
                        + "route=paint_fallback,calls=20,applied=3,skipped=17,"
                        + "measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
                "08-12 23:00:01.000 source=runtime-transport "
                        + "category=performance route=runtime stage=aggregate "
                        + "package=com.example.app message=process=com.google.android.webview,pid=456;"
                        + "route=webview_text_zoom,calls=2,applied=2,skipped=0,"
                        + "measuredCalls=2,p50Us=5,p95Us=9,p99Us=9,maxUs=9"
        );

        List<FeedbackDiagnosticProcessPerformanceParser.ProcessSummary> result =
                FeedbackDiagnosticProcessPerformanceParser.parse(events);

        assertEquals(2, result.size());
        assertEquals("com.example.app", result.get(0).process);
        assertEquals(20L, result.get(0).routes.get("paint_fallback").calls);
        assertEquals("com.google.android.webview", result.get(1).process);
        assertEquals(2L, result.get(1).routes.get("webview_text_zoom").applied);
    }
}
