package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAppProcessLauncher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Root-backed Perfetto lifecycle for one feedback diagnostic session.
 *
 * <p>The trace is deliberately owned by the diagnostic coordinator rather than
 * by the target app process. Runtime evidence remains target-process-owned;
 * Perfetto provides the system-wide timeline used for later correlation.</p>
 */
final class FeedbackDiagnosticPerfettoTrace {
    private static final String DIRECTORY = "/data/local/tmp/dpis-feedback-diagnostic";
    private static final long TRACE_DURATION_MS = 60_000L;
    private static final long MAX_TRACE_BYTES = 64L * 1024L * 1024L;

    interface ShellRunner {
        RootAppProcessLauncher.ShellResult run(String command);
    }

    private final String tracePath;
    private final String pidPath;
    private final String errorPath;
    private final String configPath;
    private final ShellRunner shellRunner;
    private boolean started;

    private FeedbackDiagnosticPerfettoTrace(ShellRunner shellRunner) {
        String id = UUID.randomUUID().toString();
        tracePath = DIRECTORY + "/" + id + ".pftrace";
        pidPath = tracePath + ".pid";
        errorPath = tracePath + ".error";
        configPath = tracePath + ".config";
        this.shellRunner = shellRunner;
    }

    static StartResult start(ShellRunner shellRunner) {
        FeedbackDiagnosticPerfettoTrace trace =
                new FeedbackDiagnosticPerfettoTrace(
                        shellRunner != null
                                ? shellRunner
                                : FeedbackDiagnosticPerfettoTrace::runSuCommand);
        RootAppProcessLauncher.ShellResult result = trace.shellRunner.run(
                "mkdir -p " + quote(DIRECTORY)
                        + " && rm -f " + quote(trace.tracePath)
                        + " " + quote(trace.pidPath)
                        + " " + quote(trace.errorPath)
                        + " " + quote(trace.configPath)
                        + " && printf %s " + quote(config())
                        + " > " + quote(trace.configPath)
                        + " && /system/bin/perfetto --txt -c " + quote(trace.configPath)
                        + " -o " + quote(trace.tracePath)
                        + " --background > " + quote(trace.pidPath)
                        + " 2>" + quote(trace.errorPath)
        );
        if (result.code() != 0) {
            return StartResult.unavailable(compact(result.output()));
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
                "if [ -s " + quote(pidPath) + " ]; then kill -TERM $(cat "
                        + quote(pidPath) + ") 2>/dev/null || true; fi"
                        + "; i=0; while [ $i -lt 20 ] && [ ! -f " + quote(tracePath)
                        + " ]; do i=$((i+1)); sleep 0.1; done"
                        + "; if [ -f " + quote(tracePath) + " ]; then"
                        + " size=$(wc -c < " + quote(tracePath) + ")"
                        + "; if [ \"$size\" -le " + MAX_TRACE_BYTES + " ]; then"
                        + " printf 'available:size=%s' \"$size\";"
                        + " else printf 'available:size=%s,truncated=true' " + MAX_TRACE_BYTES + "; fi"
                        + "; else printf 'unavailable:error='; cat " + quote(errorPath)
                        + " 2>/dev/null; exit 2; fi"
                        + "; code=$?; rm -f " + quote(tracePath) + " " + quote(pidPath)
                        + " " + quote(errorPath) + " " + quote(configPath) + "; exit $code"
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
                            : "trace captured on device; not exported");
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

    private static String config() {
        return "buffers { size_kb: 65536 fill_policy: RING_BUFFER }\n"
                + "duration_ms: " + TRACE_DURATION_MS + "\n"
                + "data_sources { config { name: \"linux.ftrace\" "
                + "ftrace_config { "
                + "ftrace_events: \"sched/sched_switch\" "
                + "ftrace_events: \"sched/sched_wakeup\" "
                + "ftrace_events: \"sched/sched_waking\" "
                + "atrace_categories: \"gfx\" "
                + "atrace_categories: \"view\" "
                + "atrace_categories: \"input\" "
                + "atrace_categories: \"binder_driver\" "
                + "} } }\n";
    }

    private static RootAppProcessLauncher.ShellResult runSuCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-c", command});
            String output = new String(process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
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
        final FeedbackDiagnosticPerfettoTrace trace;

        private StartResult(
                boolean available,
                String note,
                FeedbackDiagnosticPerfettoTrace trace
        ) {
            this.available = available;
            this.note = note;
            this.trace = trace;
        }

        static StartResult available(FeedbackDiagnosticPerfettoTrace trace) {
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
        final String note;

        private StopResult(boolean available, long sizeBytes, boolean truncated, String note) {
            this.available = available;
            this.sizeBytes = Math.max(0L, sizeBytes);
            this.truncated = truncated;
            this.note = note != null ? note : "";
        }

        static StopResult available(long sizeBytes, boolean truncated, String note) {
            return new StopResult(true, sizeBytes, truncated, note);
        }

        static StopResult unavailable(String note) {
            return new StopResult(false, 0L, false, note);
        }
    }
}
