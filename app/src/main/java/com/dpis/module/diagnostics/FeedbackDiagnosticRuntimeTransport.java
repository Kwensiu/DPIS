package com.dpis.module.diagnostics;

import com.dpis.module.*;

import com.dpis.module.root.RootAppProcessLauncher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class FeedbackDiagnosticRuntimeTransport {
    private static final String DIRECTORY = "/data/local/tmp/dpis-feedback-diagnostic";
    private static final String MARKER_FILE = DIRECTORY + "/active-session";
    private static final String SESSION_PROPERTY = "debug.dpis.diag.session";
    private static final String EVENT_FILE_NAME = "runtime-events.jsonl";
    private static final long MAX_EXPORT_BYTES = 256L * 1024L;
    private static volatile Session activeSession;
    private static volatile long lastMarkerCheckMillis;
    private static volatile RemoteSession remoteSession;

    public interface ShellRunner {
        RootAppProcessLauncher.ShellResult run(String command);
    }

    private FeedbackDiagnosticRuntimeTransport() {
    }

    public static Status start(String packageName, ShellRunner shellRunner) {
        String sessionId = UUID.randomUUID().toString();
        String eventPath = DIRECTORY + "/" + sessionId + "-" + EVENT_FILE_NAME;
        ShellRunner runner = shellRunner != null
                ? shellRunner
                : FeedbackDiagnosticRuntimeTransport::runSuCommand;
        RootAppProcessLauncher.ShellResult result = runner.run("mkdir -p " + shellQuote(DIRECTORY)
                + " && chmod 755 " + shellQuote(DIRECTORY)
                + " && : > " + shellQuote(eventPath)
                + " && chmod 666 " + shellQuote(eventPath)
                + " && printf %s " + shellQuote(eventPath)
                + " > " + shellQuote(MARKER_FILE)
                + " && chmod 644 " + shellQuote(MARKER_FILE)
                + " && setprop " + shellQuote(SESSION_PROPERTY) + " " + shellQuote(sessionId));
        if (result.code() != 0) {
            activeSession = new Session("", false,
                    "runtime transport unavailable: " + compact(result.output()));
            return Status.unavailable(activeSession.reason);
        }
        activeSession = new Session(eventPath, true, "");
        remoteSession = null;
        lastMarkerCheckMillis = 0L;
        return Status.available(eventPath);
    }

    public static Snapshot stopSnapshot(ShellRunner shellRunner) {
        Session session = activeSession;
        activeSession = null;
        remoteSession = null;
        lastMarkerCheckMillis = 0L;
        if (session == null) {
            return Snapshot.unavailable("runtime transport unavailable: not started");
        }
        if (!session.available) {
            return Snapshot.unavailable(session.reason);
        }
        ShellRunner runner = shellRunner != null
                ? shellRunner
                : FeedbackDiagnosticRuntimeTransport::runSuCommand;
        RootAppProcessLauncher.ShellResult readResult = runner.run("cat "
                + shellQuote(session.eventPath)
                + " 2>/dev/null | head -c "
                + MAX_EXPORT_BYTES
                + "; rm -f "
                + shellQuote(session.eventPath)
                + " "
                + shellQuote(MARKER_FILE)
                + "; setprop " + shellQuote(SESSION_PROPERTY) + " ''");
        if (readResult.code() != 0) {
            return Snapshot.unavailable(
                    "runtime transport unavailable: " + compact(readResult.output()));
        }
        return Snapshot.available(parseEvents(readResult.output()));
    }

    public static Snapshot peekSnapshot(ShellRunner shellRunner) {
        Session session = activeSession;
        if (session == null) {
            return Snapshot.unavailable("runtime transport unavailable: not started");
        }
        if (!session.available) {
            return Snapshot.unavailable(session.reason);
        }
        ShellRunner runner = shellRunner != null
                ? shellRunner
                : FeedbackDiagnosticRuntimeTransport::runSuCommand;
        RootAppProcessLauncher.ShellResult readResult = runner.run("cat "
                + shellQuote(session.eventPath)
                + " 2>/dev/null | head -c "
                + MAX_EXPORT_BYTES);
        if (readResult.code() != 0) {
            return Snapshot.unavailable(
                    "runtime transport unavailable: " + compact(readResult.output()));
        }
        return Snapshot.available(parseEvents(readResult.output()));
    }

    public static void cancel(ShellRunner shellRunner) {
        Session session = activeSession;
        activeSession = null;
        remoteSession = null;
        lastMarkerCheckMillis = 0L;
        if (session == null || !session.available) {
            return;
        }
        ShellRunner runner = shellRunner != null
                ? shellRunner
                : FeedbackDiagnosticRuntimeTransport::runSuCommand;
        runner.run("rm -f " + shellQuote(session.eventPath) + " "
                + shellQuote(MARKER_FILE)
                + "; setprop " + shellQuote(SESSION_PROPERTY) + " ''");
    }

    public static void record(String category, String stage, String packageName, String message) {
        record(category, "", stage, packageName, message);
    }

    public static void record(
            String category,
            String route,
            String stage,
            String packageName,
            String message
    ) {
        Session local = activeSession;
        if (local != null && local.available) {
            appendLine(local.eventPath, toLine(category, route, stage, packageName, message));
            return;
        }
        RemoteSession remote = resolveRemoteSession();
        if (remote != null) {
            appendLine(remote.eventPath, toLine(category, route, stage, packageName, message));
        }
    }

    /**
     * Publishes a compact process-local performance aggregate. Unlike
     * {@link #record(String, String, String, String)}, this is emitted at a
     * bounded cadence by the target process and must not be called per hook
     * callback.
     */
    public static void recordPerformanceSnapshot(
            String packageName,
            String processName,
            int pid,
            Map<String, FeedbackDiagnosticProcessPerformance.RouteSnapshot> routes
    ) {
        if (routes == null || routes.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append("process=").append(valueOrDefault(processName, "unknown"))
                .append(",pid=").append(pid);
        for (Map.Entry<String, FeedbackDiagnosticProcessPerformance.RouteSnapshot> entry
                : routes.entrySet()) {
            FeedbackDiagnosticProcessPerformance.RouteSnapshot snapshot = entry.getValue();
            message.append(";route=").append(entry.getKey())
                    .append(",calls=").append(snapshot.calls)
                    .append(",applied=").append(snapshot.applied)
                    .append(",skipped=").append(snapshot.skipped)
                    .append(",measuredCalls=").append(snapshot.measuredCalls)
                    .append(",p50Us=").append(snapshot.p50Us)
                    .append(",p95Us=").append(snapshot.p95Us)
                    .append(",p99Us=").append(snapshot.p99Us)
                    .append(",maxUs=").append(snapshot.maxUs);
            if (!snapshot.skipReasons.isEmpty()) {
                message.append(",skipReasons=");
                boolean first = true;
                for (Map.Entry<String, Long> reason : snapshot.skipReasons.entrySet()) {
                    if (!first) {
                        message.append('|');
                    }
                    message.append(reason.getKey()).append(':').append(reason.getValue());
                    first = false;
                }
            }
        }
        record("performance", "runtime", "aggregate", packageName, message.toString());
        if (isCaptureActive()) {
            DpisLog.i("DPIS_DIAG_PERF " + message);
        }
    }

    public static Status statusForTest() {
        Session session = activeSession;
        if (session == null) {
            return Status.unavailable("runtime transport unavailable: not started");
        }
        return session.available ? Status.available(session.eventPath) : Status.unavailable(session.reason);
    }

    public static String activeEventPath() {
        Session session = activeSession;
        if (session != null && session.available) {
            return session.eventPath;
        }
        RemoteSession remote = resolveRemoteSession();
        return remote != null ? remote.eventPath : "";
    }

    public static boolean isCaptureActive() {
        Session session = activeSession;
        if (session != null && session.available) {
            return true;
        }
        return resolveRemoteSession() != null;
    }

    public static boolean writeSelfTestEvent(
            String packageName,
            String message,
            ShellRunner shellRunner
    ) {
        String eventPath = activeEventPath();
        if (eventPath.isBlank()) {
            return false;
        }
        ShellRunner runner = shellRunner != null
                ? shellRunner
                : FeedbackDiagnosticRuntimeTransport::runSuCommand;
        String line = toLine("runtime", "self_test", "self_test", packageName, message);
        RootAppProcessLauncher.ShellResult result = runner.run(
                "printf %s\\\\n " + shellQuote(line) + " >> " + shellQuote(eventPath)
        );
        return result.code() == 0;
    }

    private static RemoteSession resolveRemoteSession() {
        long now = System.currentTimeMillis();
        RemoteSession cached = remoteSession;
        if (cached != null && now - lastMarkerCheckMillis < 1_000L) {
            return cached.available ? cached : null;
        }
        lastMarkerCheckMillis = now;
        File marker = new File(MARKER_FILE);
        if (marker.isFile()) {
            try {
                String eventPath = new String(
                        Files.readAllBytes(marker.toPath()),
                        StandardCharsets.UTF_8
                ).trim();
                if (eventPath.startsWith(DIRECTORY + "/")) {
                    remoteSession = RemoteSession.available(eventPath);
                    return remoteSession;
                }
            } catch (IOException ignored) {
                // The property fallback remains available when the marker is hidden.
            }
        }
        String sessionId = readSystemProperty(SESSION_PROPERTY);
        if (!sessionId.isBlank() && sessionId.matches("[0-9a-fA-F-]{36}")) {
            remoteSession = RemoteSession.available(
                    DIRECTORY + "/" + sessionId + "-" + EVENT_FILE_NAME
            );
            return remoteSession;
        }
        remoteSession = RemoteSession.unavailable();
        return null;
    }

    private static String readSystemProperty(String name) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Object value = systemProperties.getMethod("get", String.class, String.class)
                    .invoke(null, name, "");
            return value != null ? value.toString().trim() : "";
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private static void appendLine(String eventPath, String line) {
        try {
            Files.write(
                    new File(eventPath).toPath(),
                    (line + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException | RuntimeException ignored) {
            // Runtime diagnostics must never affect hooked app behavior.
        }
    }

    private static String toLine(
            String category,
            String route,
            String stage,
            String packageName,
            String message
    ) {
        long now = System.currentTimeMillis();
        return "{\"timestampMillis\":" + now
                + ",\"displayTime\":\"" + jsonEscape(formatTime(now)) + "\""
                + ",\"source\":\"runtime-transport\""
                + ",\"category\":\"" + jsonEscape(valueOrDefault(category, "runtime")) + "\""
                + ",\"route\":\"" + jsonEscape(valueOrDefault(route, "")) + "\""
                + ",\"stage\":\"" + jsonEscape(valueOrDefault(stage, "event")) + "\""
                + ",\"package\":\"" + jsonEscape(valueOrDefault(packageName, "unknown")) + "\""
                + ",\"message\":\"" + jsonEscape(sanitize(message)) + "\""
                + "}";
    }

    private static List<String> parseEvents(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> events = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            long timestampMillis = readLongField(line, "timestampMillis", 0L);
            String displayTime = readStringField(line, "displayTime");
            String category = readStringField(line, "category");
            String route = readStringField(line, "route");
            String stage = readStringField(line, "stage");
            String packageName = readStringField(line, "package");
            String message = readStringField(line, "message");
            if (timestampMillis <= 0L || message.isBlank()) {
                continue;
            }
            events.add(valueOrDefault(displayTime, formatTime(timestampMillis))
                    + " source=runtime-transport"
                    + " category=" + valueOrDefault(category, "runtime")
                    + routePart(route)
                    + " stage=" + valueOrDefault(stage, "event")
                    + " package=" + valueOrDefault(packageName, "unknown")
                    + " message=" + message);
        }
        Collections.sort(events);
        return events;
    }

    private static String routePart(String route) {
        String normalized = valueOrDefault(route, "");
        return normalized.isEmpty() ? "" : " route=" + normalized;
    }

    private static RootAppProcessLauncher.ShellResult runSuCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                readAll(reader, output);
                readAll(errReader, output);
            }
            return new RootAppProcessLauncher.ShellResult(process.waitFor(), output.toString());
        } catch (IOException exception) {
            return new RootAppProcessLauncher.ShellResult(-1, exceptionMessage(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new RootAppProcessLauncher.ShellResult(-1, exceptionMessage(exception));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void readAll(BufferedReader reader, StringBuilder output)
            throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (output.length() > 0) {
                output.append('\n');
            }
            output.append(line);
        }
    }

    private static String readStringField(String line, String fieldName) {
        String prefix = "\"" + fieldName + "\":\"";
        int start = line.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaped) {
                value.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                break;
            } else {
                value.append(ch);
            }
        }
        return value.toString();
    }

    private static long readLongField(String line, String fieldName, long fallback) {
        String prefix = "\"" + fieldName + "\":";
        int start = line.indexOf(prefix);
        if (start < 0) {
            return fallback;
        }
        start += prefix.length();
        int end = start;
        while (end < line.length() && Character.isDigit(line.charAt(end))) {
            end++;
        }
        try {
            return Long.parseLong(line.substring(start, end));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(millis));
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String compact(String value) {
        String normalized = sanitize(value);
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private static String exceptionMessage(Exception exception) {
        return exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();
    }

    public static final class Status {
        public final boolean available;
        public final String path;
        public final String message;

        private Status(boolean available, String path, String message) {
            this.available = available;
            this.path = path != null ? path : "";
            this.message = message != null ? message : "";
        }

        static Status available(String path) {
            return new Status(true, path, "runtime transport available");
        }

        static Status unavailable(String message) {
            return new Status(false, "", message);
        }
    }

    public static final class Snapshot {
        public final boolean available;
        public final List<String> events;
        public final String note;

        private Snapshot(boolean available, List<String> events, String note) {
            this.available = available;
            this.events = events != null ? new ArrayList<>(events) : List.of();
            this.note = note != null ? note : "";
        }

        static Snapshot available(List<String> events) {
            return new Snapshot(true, events, "");
        }

        static Snapshot unavailable(String note) {
            return new Snapshot(false, List.of(), note);
        }
    }

    private static final class Session {
        final String eventPath;
        final boolean available;
        final String reason;

        Session(String eventPath, boolean available, String reason) {
            this.eventPath = eventPath != null ? eventPath : "";
            this.available = available;
            this.reason = reason != null ? reason : "";
        }
    }

    private static final class RemoteSession {
        final boolean available;
        final String eventPath;

        private RemoteSession(boolean available, String eventPath) {
            this.available = available;
            this.eventPath = eventPath != null ? eventPath : "";
        }

        static RemoteSession available(String eventPath) {
            return new RemoteSession(true, eventPath);
        }

        static RemoteSession unavailable() {
            return new RemoteSession(false, "");
        }
    }
}
