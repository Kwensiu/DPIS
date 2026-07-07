package com.dpis.module.diagnostics;

import com.dpis.module.*;

import java.util.List;

final class FeedbackDiagnosticRuntimeSelfTest {
    private static volatile Status lastStatus = Status.notStarted();

    private FeedbackDiagnosticRuntimeSelfTest() {
    }

    static Status runUiTransportSelfTest(
            String packageName,
            FeedbackDiagnosticRuntimeTransport.ShellRunner shellRunner
    ) {
        FeedbackDiagnosticRuntimeTransport.Status status =
                FeedbackDiagnosticRuntimeTransport.statusForTest();
        if (!status.available) {
            lastStatus = Status.unavailable(status.message);
            return lastStatus;
        }
        String token = "ui-self-test-" + System.currentTimeMillis();
        boolean writeOk = FeedbackDiagnosticRuntimeTransport.writeSelfTestEvent(
                packageName,
                token,
                shellRunner
        );
        if (!writeOk) {
            lastStatus = Status.failed("ui transport write failed", 0);
            return lastStatus;
        }
        FeedbackDiagnosticRuntimeTransport.Snapshot snapshot =
                FeedbackDiagnosticRuntimeTransport.peekSnapshot(shellRunner);
        boolean ok = snapshot.available
                && snapshot.events.stream().anyMatch(event -> event.contains(token));
        lastStatus = ok
                ? Status.ok(snapshot.events.size())
                : Status.failed(snapshot.note, snapshot.events.size());
        return lastStatus;
    }

    static Status lastStatus() {
        return lastStatus;
    }

    static void resetForTest() {
        lastStatus = Status.notStarted();
    }

    static boolean hasHotPathProbe(List<String> timelineEvents) {
        if (timelineEvents == null) {
            return false;
        }
        return timelineEvents.stream().anyMatch(event ->
                event.contains("source=runtime-hotpath"));
    }

    static final class Status {
        final boolean prepared;
        final boolean uiWriteReadOk;
        final int transportEventCount;
        final String message;

        private Status(
                boolean prepared,
                boolean uiWriteReadOk,
                int transportEventCount,
                String message
        ) {
            this.prepared = prepared;
            this.uiWriteReadOk = uiWriteReadOk;
            this.transportEventCount = Math.max(0, transportEventCount);
            this.message = message != null ? message : "";
        }

        static Status notStarted() {
            return new Status(false, false, 0, "runtime self-test not started");
        }

        static Status unavailable(String message) {
            return new Status(false, false, 0, message);
        }

        static Status ok(int count) {
            return new Status(true, true, count, "ui transport write/read ok");
        }

        static Status failed(String message, int count) {
            String reason = message == null || message.isBlank()
                    ? "ui transport write/read failed"
                    : message;
            return new Status(true, false, count, reason);
        }
    }
}
