package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAppProcessLauncher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FeedbackDiagnosticPerfettoTraceTest {
    @Test
    public void unavailableStartDoesNotBlockDiagnostic() {
        FeedbackDiagnosticPerfettoTrace.StartResult result =
                FeedbackDiagnosticPerfettoTrace.start(command ->
                        new RootAppProcessLauncher.ShellResult(1, "perfetto unavailable"));

        assertFalse(result.available);
        assertTrue(result.note.contains("perfetto unavailable"));
    }

    @Test
    public void stopReturnsDeviceSideMetadataWithoutReadingTrace() {
        FeedbackDiagnosticPerfettoTrace.StartResult start =
                FeedbackDiagnosticPerfettoTrace.start(command ->
                        new RootAppProcessLauncher.ShellResult(0, ""));
        assertTrue(start.available);

        FeedbackDiagnosticPerfettoTrace.StopResult stop =
                FeedbackDiagnosticPerfettoTrace.StopResult.available(
                        4096L, false, "trace retained on device");
        assertTrue(stop.available);
        assertEquals(4096L, stop.sizeBytes);
        assertFalse(stop.truncated);
        assertTrue(stop.note.contains("retained"));
    }
}
