package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAppProcessLauncher;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Root-backed Perfetto lifecycle for one feedback diagnostic session.
 *
 * <p>The trace is deliberately owned by the diagnostic coordinator rather than
 * by the target app process. Runtime evidence remains target-process-owned;
 * Perfetto provides the system-wide timeline used for later correlation.</p>
 */
final class PerfettoTrace {
    private static final String DIRECTORY = "/data/local/tmp/dpis-feedback-diagnostic";
    private static final long TRACE_DURATION_MS = 60_000L;
    private static final long TRACE_BUFFER_KB = 8L * 1024L;
    private static final long TRACE_MAX_FILE_BYTES = 16L * 1024L * 1024L;

    interface ShellRunner {
        RootAppProcessLauncher.ShellResult run(String command);
    }

    private final String tracePath;
    private final String pidPath;
    private final String errorPath;
    private final String configPath;
    private final ShellRunner shellRunner;
    private boolean started;

    private PerfettoTrace(ShellRunner shellRunner) {
        String id = UUID.randomUUID().toString();
        tracePath = DIRECTORY + "/" + id + ".pftrace";
        pidPath = tracePath + ".pid";
        errorPath = tracePath + ".error";
        configPath = tracePath + ".config";
        this.shellRunner = shellRunner;
    }

    static StartResult start(ShellRunner shellRunner) {
        PerfettoTrace trace =
                new PerfettoTrace(
                        shellRunner != null
                                ? shellRunner
                                : PerfettoTrace::runSuCommand);
        RootAppProcessLauncher.ShellResult result = trace.shellRunner.run(
                "mkdir -p " + quote(DIRECTORY)
                        + " && rm -f " + quote(trace.tracePath)
                        + " " + quote(trace.pidPath)
                        + " " + quote(trace.errorPath)
                        + " " + quote(trace.configPath)
                        + " && printf %s " + quote(config())
                        + " > " + quote(trace.configPath)
                        // Perfetto owns the detached process here. Do not mix
                        // TraceConfig.write_into_file with CLI -o: Android 16
                        // Perfetto rejects that two-writer configuration.
                        + " && /system/bin/perfetto --background-wait --txt -c "
                        + quote(trace.configPath)
                        + " -o " + quote(trace.tracePath)
                        + " > " + quote(trace.pidPath)
                        + " 2>" + quote(trace.errorPath)
        );
        if (result.code() != 0) {
            return StartResult.unavailable(compact(result.output()));
        }
        RootAppProcessLauncher.ShellResult readiness = trace.shellRunner.run(
                "if [ -s " + quote(trace.pidPath) + " ]"
                        + " && kill -0 $(cat " + quote(trace.pidPath) + ") 2>/dev/null; then"
                        + " exit 0; else cat " + quote(trace.errorPath)
                        + " 2>/dev/null; exit 2; fi"
        );
        if (readiness.code() != 0) {
            trace.discard();
            return StartResult.unavailable(
                    "Perfetto trace did not stay running: " + compact(readiness.output()));
        }
        trace.started = true;
        return StartResult.available(trace);
    }

    StopResult stop() {
        if (!started) {
            return StopResult.unavailable("Perfetto trace was not started");
        }
        started = false;
        RootAppProcessLauncher.ShellResult result = shellRunner.run(
                "if [ -s " + quote(pidPath) + " ]; then kill -INT $(cat "
                        + quote(pidPath) + ") 2>/dev/null || true; fi"
                        + "; i=0; while [ $i -lt 30 ] && [ -s " + quote(pidPath)
                        + " ] && kill -0 $(cat " + quote(pidPath)
                        + ") 2>/dev/null; do i=$((i+1)); sleep 0.1; done"
                        + "; if [ -s " + quote(tracePath) + " ]; then"
                        + " size=$(wc -c < " + quote(tracePath) + ")"
                        + "; if [ \"$size\" -le " + TRACE_MAX_FILE_BYTES + " ]; then"
                        + " printf 'available:size=%s' \"$size\";"
                        + " else printf 'available:size=%s,truncated=true' " + TRACE_MAX_FILE_BYTES + "; fi"
                        + "; else printf 'unavailable:error='; cat " + quote(errorPath)
                        + " 2>/dev/null; exit 2; fi"
                        + "; exit $?"
        );
        if (result.code() != 0 || result.output().isBlank()) {
            return StopResult.unavailable(
                    "Perfetto trace stop failed: " + compact(result.output()));
        }
        String output = result.output().trim();
        if (!output.startsWith("available:size=")) {
            return StopResult.unavailable("Perfetto trace unavailable: " + compact(output));
        }
        String sizeText = output.substring("available:size=".length()).split(",", 2)[0];
        try {
            long size = Long.parseLong(sizeText.trim());
            boolean truncated = output.contains("truncated=true");
            return StopResult.available(size, truncated,
                    truncated ? "trace exceeded device-side size limit"
                            : "trace ready for diagnostic export");
        } catch (NumberFormatException exception) {
            return StopResult.unavailable("Perfetto trace size was invalid");
        }
    }

    void discard() {
        if (!started) {
            return;
        }
        started = false;
        shellRunner.run(
                "if [ -s " + quote(pidPath) + " ]; then kill -TERM $(cat "
                        + quote(pidPath) + ") 2>/dev/null || true; fi"
                        + "; rm -f " + quote(tracePath) + " " + quote(pidPath)
                        + " " + quote(errorPath) + " " + quote(configPath)
        );
    }

    /**
     * Transfers the completed trace into the app process, then removes root-owned temporary
     * files. The trace only exists on-device until its diagnostic ZIP is assembled.
     */
    StopResult consumeStoppedTrace(StopResult stoppedTrace) {
        if (stoppedTrace == null || !stoppedTrace.available) {
            return stoppedTrace != null
                    ? stoppedTrace
                    : StopResult.unavailable("Perfetto trace was not stopped");
        }
        if (stoppedTrace.truncated) {
            discardCompletedTrace();
            return StopResult.available(stoppedTrace.sizeBytes, true, new byte[0],
                    "trace exceeded device-side size limit and was not exported");
        }
        RootAppProcessLauncher.ShellResult result = shellRunner.run(
                "base64 " + quote(tracePath)
                        + "; code=$?; rm -f " + quote(tracePath) + " " + quote(pidPath)
                        + " " + quote(errorPath) + " " + quote(configPath) + "; exit $code"
        );
        if (result.code() != 0 || result.output().isBlank()) {
            return StopResult.unavailable(
                    "Perfetto trace export failed: " + compact(result.output()));
        }
        try {
            byte[] bytes = Base64.getMimeDecoder().decode(result.output());
            if (bytes.length != stoppedTrace.sizeBytes) {
                return StopResult.unavailable("Perfetto trace export size mismatch");
            }
            return StopResult.available(bytes.length, false, bytes,
                    "trace exported with diagnostic package");
        } catch (IllegalArgumentException exception) {
            return StopResult.unavailable("Perfetto trace export was invalid");
        }
    }

    private void discardCompletedTrace() {
        shellRunner.run(
                "rm -f " + quote(tracePath) + " " + quote(pidPath)
                        + " " + quote(errorPath) + " " + quote(configPath)
        );
    }

    private static String config() {
        return "buffers { size_kb: " + TRACE_BUFFER_KB + " fill_policy: RING_BUFFER }\n"
                + "duration_ms: " + TRACE_DURATION_MS + "\n"
                + "data_sources { config { name: \"linux.ftrace\" "
                + "ftrace_config { "
                + "ftrace_events: \"sched/sched_switch\" "
                + "ftrace_events: \"sched/sched_wakeup\" "
                + "ftrace_events: \"sched/sched_waking\" "
                + "ftrace_events: \"sched/sched_process_exit\" "
                + "ftrace_events: \"sched/sched_process_free\" "
                + "ftrace_events: \"task/task_newtask\" "
                + "ftrace_events: \"task/task_rename\" "
                + "atrace_categories: \"gfx\" "
                + "atrace_categories: \"view\" "
                + "atrace_categories: \"input\" "
                + "atrace_categories: \"binder_driver\" "
                + "} } }\n"
                + "data_sources { config { name: \"linux.process_stats\" "
                + "process_stats_config { scan_all_processes_on_start: true } } }\n"
                + "data_sources { config { name: \"android.surfaceflinger.frametimeline\" } }\n";
    }

    private static RootAppProcessLauncher.ShellResult runSuCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", command});
            InputStream input = process.getInputStream();
            ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                outputBytes.write(buffer, 0, read);
            }
            String output = outputBytes.toString(StandardCharsets.UTF_8.name());
            return new RootAppProcessLauncher.ShellResult(process.waitFor(), output);
        } catch (Exception exception) {
            return new RootAppProcessLauncher.ShellResult(-1, exception.getMessage());
        }
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String compact(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    static final class StartResult {
        final boolean available;
        final String note;
        final PerfettoTrace trace;

        private StartResult(
                boolean available,
                String note,
                PerfettoTrace trace
        ) {
            this.available = available;
            this.note = note;
            this.trace = trace;
        }

        static StartResult available(PerfettoTrace trace) {
            return new StartResult(true, "", trace);
        }

        static StartResult unavailable(String note) {
            return new StartResult(false, note, null);
        }
    }

    static final class StopResult {
        final boolean available;
        final long sizeBytes;
        final boolean truncated;
        final byte[] traceBytes;
        final String note;

        private StopResult(
                boolean available,
                long sizeBytes,
                boolean truncated,
                byte[] traceBytes,
                String note
        ) {
            this.available = available;
            this.sizeBytes = Math.max(0L, sizeBytes);
            this.truncated = truncated;
            this.traceBytes = traceBytes != null ? traceBytes.clone() : new byte[0];
            this.note = note != null ? note : "";
        }

        static StopResult available(long sizeBytes, boolean truncated, String note) {
            return available(sizeBytes, truncated, new byte[0], note);
        }

        static StopResult available(
                long sizeBytes,
                boolean truncated,
                byte[] traceBytes,
                String note
        ) {
            return new StopResult(true, sizeBytes, truncated, traceBytes, note);
        }

        static StopResult unavailable(String note) {
            return new StopResult(false, 0L, false, new byte[0], note);
        }
    }
}
