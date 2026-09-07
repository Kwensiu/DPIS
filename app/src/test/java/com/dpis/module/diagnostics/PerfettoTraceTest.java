package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAppProcessLauncher;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PerfettoTraceTest {
    @Test
    public void startsDetachedCliTraceWithoutServiceSideFileWriter() {
        List<String> commands = new ArrayList<>();
        PerfettoTrace.StartResult result = PerfettoTrace.start(
                command -> {
                    commands.add(command);
                    return new RootAppProcessLauncher.ShellResult(0, "");
                }
        );

        assertTrue(result.available);
        assertNotNull(result.trace);
        assertTrue(commands.size() >= 2);

        String launch = commands.get(0);
        assertTrue(launch.contains("/system/bin/perfetto --background-wait --txt"));
        assertTrue(launch.contains(" > '"));
        assertFalse(launch.contains("nohup"));
        assertFalse(launch.contains("--size"));
        assertFalse(launch.contains("write_into_file: true"));
        assertFalse(launch.contains("file_write_period_ms"));
        assertFalse(launch.contains("max_file_size_bytes"));
        assertTrue(launch.contains("linux.process_stats"));
        assertTrue(launch.contains("scan_all_processes_on_start: true"));
        assertTrue(launch.contains("android.surfaceflinger.frametimeline"));
        assertTrue(launch.contains("task/task_newtask"));
        assertTrue(launch.contains("task/task_rename"));
    }

    @Test
    public void exportsCompletedTraceAndCleansUpDeviceFiles() {
        List<String> commands = new ArrayList<>();
        PerfettoTrace.StartResult started = PerfettoTrace.start(
                command -> {
                    commands.add(command);
                    if (command.contains("available:size")) {
                        return new RootAppProcessLauncher.ShellResult(0, "available:size=3");
                    }
                    if (command.startsWith("base64 ")) {
                        return new RootAppProcessLauncher.ShellResult(0, "YWJj");
                    }
                    return new RootAppProcessLauncher.ShellResult(0, "");
                }
        );

        PerfettoTrace.StopResult stopped = started.trace.stop();
        PerfettoTrace.StopResult exported =
                started.trace.consumeStoppedTrace(stopped);

        assertTrue(stopped.available);
        assertTrue(exported.available);
        assertEquals("abc", new String(exported.traceBytes, java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(commands.get(2).contains("rm -f"));
        assertTrue(commands.stream().anyMatch(command -> command.startsWith("base64 ")));
    }

    @Test
    public void reportsUnavailableWhenPerfettoLaunchOrReadinessFails() {
        PerfettoTrace.StartResult launchFailure = PerfettoTrace.start(
                command -> new RootAppProcessLauncher.ShellResult(1, "permission denied"));
        PerfettoTrace.StartResult readinessFailure = PerfettoTrace.start(
                new PerfettoTrace.ShellRunner() {
                    private int calls;

                    @Override
                    public RootAppProcessLauncher.ShellResult run(String command) {
                        calls++;
                        return new RootAppProcessLauncher.ShellResult(
                                calls == 2 ? 2 : 0, calls == 2 ? "process exited" : "");
                    }
                });

        assertFalse(launchFailure.available);
        assertTrue(launchFailure.note.contains("permission denied"));
        assertFalse(readinessFailure.available);
        assertTrue(readinessFailure.note.contains("did not stay running"));
    }

    @Test
    public void stopRejectsMissingOrInvalidTraceSize() {
        PerfettoTrace.StartResult started = startedWith(command -> {
            if (command.contains("available:size")) {
                return new RootAppProcessLauncher.ShellResult(0, "available:size=not-a-number");
            }
            return new RootAppProcessLauncher.ShellResult(0, "");
        });

        PerfettoTrace.StopResult stopped = started.trace.stop();

        assertFalse(stopped.available);
        assertTrue(stopped.note.contains("size was invalid"));
        assertFalse(started.trace.stop().available);
    }

    @Test
    public void truncatedTraceIsDiscardedWithoutExportingBytes() {
        List<String> commands = new ArrayList<>();
        PerfettoTrace.StartResult started = startedWith(command -> {
            commands.add(command);
            if (command.contains("available:size")) {
                return new RootAppProcessLauncher.ShellResult(0, "available:size=16777216,truncated=true");
            }
            return new RootAppProcessLauncher.ShellResult(0, "");
        });

        PerfettoTrace.StopResult exported = started.trace.consumeStoppedTrace(started.trace.stop());

        assertTrue(exported.available);
        assertTrue(exported.truncated);
        assertEquals(0, exported.traceBytes.length);
        assertTrue(exported.note.contains("not exported"));
        assertFalse(commands.stream().anyMatch(command -> command.startsWith("base64 ")));
    }

    @Test
    public void exportRejectsInvalidOrMismatchedBase64Payloads() {
        PerfettoTrace.StartResult invalid = startedWith(command -> {
            if (command.contains("available:size")) return new RootAppProcessLauncher.ShellResult(0, "available:size=3");
            if (command.startsWith("base64 ")) return new RootAppProcessLauncher.ShellResult(0, "not base64");
            return new RootAppProcessLauncher.ShellResult(0, "");
        });
        PerfettoTrace.StartResult mismatch = startedWith(command -> {
            if (command.contains("available:size")) return new RootAppProcessLauncher.ShellResult(0, "available:size=4");
            if (command.startsWith("base64 ")) return new RootAppProcessLauncher.ShellResult(0, "YWJj");
            return new RootAppProcessLauncher.ShellResult(0, "");
        });

        assertTrue(invalid.trace.consumeStoppedTrace(invalid.trace.stop()).note.contains("invalid"));
        assertTrue(mismatch.trace.consumeStoppedTrace(mismatch.trace.stop()).note.contains("size mismatch"));
    }

    private static PerfettoTrace.StartResult startedWith(PerfettoTrace.ShellRunner runner) {
        PerfettoTrace.StartResult started = PerfettoTrace.start(runner);
        assertTrue(started.available);
        return started;
    }
}
