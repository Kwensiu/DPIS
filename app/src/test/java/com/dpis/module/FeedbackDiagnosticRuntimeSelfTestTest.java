package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

public final class FeedbackDiagnosticRuntimeSelfTestTest {
    @After
    public void tearDown() {
        FeedbackDiagnosticRuntimeTransport.cancel(command ->
                new RootAppProcessLauncher.ShellResult(0, ""));
        FeedbackDiagnosticRuntimeSelfTest.resetForTest();
    }

    @Test
    public void uiSelfTestReportsSuccessWhenTransportEchoesWrittenEvent() {
        EchoShell shell = new EchoShell();
        FeedbackDiagnosticRuntimeTransport.start("com.example.app", shell::run);

        FeedbackDiagnosticRuntimeSelfTest.Status status =
                FeedbackDiagnosticRuntimeSelfTest.runUiTransportSelfTest(
                        "com.example.app",
                        shell::run
                );

        assertTrue(status.prepared);
        assertTrue(status.uiWriteReadOk);
        assertTrue(status.transportEventCount > 0);
        assertTrue(status.message.contains("ok"));
    }

    @Test
    public void uiSelfTestReportsFailureWhenTransportCannotReadBack() {
        EmptyReadShell shell = new EmptyReadShell();
        FeedbackDiagnosticRuntimeTransport.start("com.example.app", shell::run);

        FeedbackDiagnosticRuntimeSelfTest.Status status =
                FeedbackDiagnosticRuntimeSelfTest.runUiTransportSelfTest(
                        "com.example.app",
                        shell::run
                );

        assertTrue(status.prepared);
        assertFalse(status.uiWriteReadOk);
    }

    private static final class EchoShell {
        final List<String> commands = new ArrayList<>();
        String lastAppendedLine = "";

        RootAppProcessLauncher.ShellResult run(String command) {
            commands.add(command);
            if (command.startsWith("printf %s\\\\n ")) {
                lastAppendedLine = command;
                return new RootAppProcessLauncher.ShellResult(0, "");
            }
            if (command.startsWith("cat ")) {
                return new RootAppProcessLauncher.ShellResult(
                        0,
                        extractJson(lastAppendedLine) + "\n"
                );
            }
            return new RootAppProcessLauncher.ShellResult(0, "");
        }

        private String extractJson(String command) {
            int firstQuote = command.indexOf('\'');
            int secondQuote = command.indexOf('\'', firstQuote + 1);
            if (firstQuote < 0 || secondQuote <= firstQuote) {
                return "";
            }
            return command.substring(firstQuote + 1, secondQuote);
        }
    }

    private static final class EmptyReadShell {
        RootAppProcessLauncher.ShellResult run(String command) {
            if (command.startsWith("printf %s\\\\n ")) {
                return new RootAppProcessLauncher.ShellResult(0, "");
            }
            if (command.startsWith("cat ")) {
                return new RootAppProcessLauncher.ShellResult(0, "");
            }
            return new RootAppProcessLauncher.ShellResult(0, "");
        }
    }
}
