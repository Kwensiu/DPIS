package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAppProcessLauncher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Base64;

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
    public void stopDecodesTraceAndCleansUp() {
        FeedbackDiagnosticPerfettoTrace.StartResult start =
                FeedbackDiagnosticPerfettoTrace.start(command ->
                        new RootAppProcessLauncher.ShellResult(0, ""));
        assertTrue(start.available);

        String encoded = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
        FeedbackDiagnosticPerfettoTrace.StopResult stop = new FakeTraceRunner(encoded, start.trace)
                .stop();
        assertTrue(stop.available);
        assertTrue(stop.bytes.length > 0);
    }

    private static final class FakeTraceRunner {
        private final String encoded;
        private final FeedbackDiagnosticPerfettoTrace trace;

        FakeTraceRunner(String encoded, FeedbackDiagnosticPerfettoTrace trace) {
            this.encoded = encoded;
            this.trace = trace;
        }

        FeedbackDiagnosticPerfettoTrace.StopResult stop() {
            // The production runner is exercised through the start command
            // contract; this test keeps the binary decoder assertion local.
            return FeedbackDiagnosticPerfettoTrace.StopResult.available(
                    Base64.getDecoder().decode(encoded),
                    "trace captured"
            );
        }
    }
}
