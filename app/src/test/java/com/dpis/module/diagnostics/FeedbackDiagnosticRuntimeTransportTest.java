package com.dpis.module.diagnostics;

import com.dpis.module.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dpis.module.root.RootAppProcessLauncher;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class FeedbackDiagnosticRuntimeTransportTest {
    @Before
    public void setUp() {
        FeedbackDiagnosticRuntimeTransport.cancel(command ->
                new RootAppProcessLauncher.ShellResult(0, ""));
    }

    @After
    public void tearDown() {
        FeedbackDiagnosticRuntimeTransport.cancel(command ->
                new RootAppProcessLauncher.ShellResult(0, ""));
    }

    @Test
    public void defaultClosedTransportReportsUnavailable() {
        FeedbackDiagnosticRuntimeTransport.record(
                "runtime",
                "dpis_log",
                "com.example.app",
                "target app matched: package=com.example.app"
        );

        FeedbackDiagnosticRuntimeTransport.Status status =
                FeedbackDiagnosticRuntimeTransport.statusForTest();

        assertFalse(status.available);
        assertTrue(status.message.contains("not started"));
    }

    @Test
    public void startFailureReturnsUnavailableStatus() {
        FeedbackDiagnosticRuntimeTransport.Status status =
                FeedbackDiagnosticRuntimeTransport.start(
                        "com.example.app",
                        command -> new RootAppProcessLauncher.ShellResult(1, "permission denied")
                );

        assertFalse(status.available);
        assertTrue(status.message.contains("permission denied"));
    }

    @Test
    public void enabledTransportCommandsCreateMarkerAndReadEvents() {
        FakeShell shell = new FakeShell("{\"timestampMillis\":1700000000100,"
                + "\"displayTime\":\"11-14 22:13:20.100\","
                + "\"source\":\"runtime-transport\","
                + "\"category\":\"runtime\","
                + "\"route\":\"font\","
                + "\"stage\":\"dpis_log\","
                + "\"package\":\"com.example.app\","
                + "\"message\":\"target app matched: package=com.example.app\"}\n");

        FeedbackDiagnosticRuntimeTransport.Status status =
                FeedbackDiagnosticRuntimeTransport.start("com.example.app", shell::run);
        FeedbackDiagnosticRuntimeTransport.Snapshot snapshot =
                FeedbackDiagnosticRuntimeTransport.stopSnapshot(shell::run);

        assertTrue(status.available);
        assertTrue(shell.commands.get(0).contains("chmod 666"));
        assertTrue(shell.commands.get(0).contains("active-session"));
        assertTrue(snapshot.available);
        assertTrue(snapshot.events.get(0).contains("source=runtime-transport"));
        assertTrue(snapshot.events.get(0).contains("route=font"));
        assertTrue(snapshot.events.get(0).contains("stage=dpis_log"));
    }

    @Test
    public void activeSessionDiscoveryReportsLocalSession() {
        FakeShell shell = new FakeShell("");
        FeedbackDiagnosticRuntimeTransport.start("com.example.app", shell::run);

        assertTrue(FeedbackDiagnosticRuntimeTransport.activeSessionDiscoveryDetail()
                .contains("source=local-session"));
    }

    private static final class FakeShell {
        final List<String> commands = new ArrayList<>();
        final String readOutput;

        FakeShell(String readOutput) {
            this.readOutput = readOutput;
        }

        RootAppProcessLauncher.ShellResult run(String command) {
            commands.add(command);
            if (command.startsWith("cat ")) {
                return new RootAppProcessLauncher.ShellResult(0, readOutput);
            }
            return new RootAppProcessLauncher.ShellResult(0, "");
        }
    }
}
