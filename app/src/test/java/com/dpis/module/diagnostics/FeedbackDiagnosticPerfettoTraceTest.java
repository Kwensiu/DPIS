package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAppProcessLauncher;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeedbackDiagnosticPerfettoTraceTest {
    @Test
    public void startsDetachedCliTraceWithoutServiceSideFileWriter() {
        List<String> commands = new ArrayList<>();
        FeedbackDiagnosticPerfettoTrace.StartResult result = FeedbackDiagnosticPerfettoTrace.start(
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
        FeedbackDiagnosticPerfettoTrace.StartResult started = FeedbackDiagnosticPerfettoTrace.start(
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

        FeedbackDiagnosticPerfettoTrace.StopResult stopped = started.trace.stop();
        FeedbackDiagnosticPerfettoTrace.StopResult exported =
                started.trace.consumeStoppedTrace(stopped);

        assertTrue(stopped.available);
        assertTrue(exported.available);
        assertEquals("abc", new String(exported.traceBytes, java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(commands.get(2).contains("rm -f"));
        assertTrue(commands.stream().anyMatch(command -> command.startsWith("base64 ")));
    }
}
