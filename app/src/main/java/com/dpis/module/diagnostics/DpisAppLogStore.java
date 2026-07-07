package com.dpis.module.diagnostics;

import com.dpis.module.DpisLog;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DpisAppLogStore {
    private static final int DEFAULT_MAX_STORED_LINES = 5_000;
    private static final long DEFAULT_MAX_STORED_BYTES = 1024L * 1024L;
    private static final String LOG_DIRECTORY_NAME = "dpis_logs";
    private static final String LOG_FILE_NAME = "app_log.jsonl";
    private static final String DPIS_LOG_SOURCE = "DPIS";
    private static final String DPIS_MODULE_PACKAGE = "io.github.kwensiu.dpis";
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile(
            "\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    );
    private static final Pattern JSON_LONG_FIELD_PATTERN = Pattern.compile(
            "\"%s\"\\s*:\\s*(-?\\d+)"
    );
    private final Context appContext;
    private final File logFile;
    private final int maxStoredLines;
    private final long maxStoredBytes;
    private final Object lock = new Object();

    public DpisAppLogStore(Context context) {
        appContext = context.getApplicationContext();
        logFile = new File(new File(appContext.getFilesDir(), LOG_DIRECTORY_NAME), LOG_FILE_NAME);
        maxStoredLines = DEFAULT_MAX_STORED_LINES;
        maxStoredBytes = DEFAULT_MAX_STORED_BYTES;
    }

    DpisAppLogStore(File logFile, int maxStoredLines, long maxStoredBytes) {
        appContext = null;
        this.logFile = logFile;
        this.maxStoredLines = Math.max(1, maxStoredLines);
        this.maxStoredBytes = Math.max(128L, maxStoredBytes);
    }

    public void record(String level, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        long timestampMillis = System.currentTimeMillis();
        synchronized (lock) {
            try {
                ensureParentDirectory();
                Files.write(
                        logFile.toPath(),
                        (toJsonLine(timestampMillis, level, message) + "\n")
                                .getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
                trimToCapacity();
            } catch (IOException ignored) {
                // Logging must never affect runtime behavior.
            }
        }
    }

    public List<DpisLogEntry> readRecentEntries() {
        synchronized (lock) {
            return readEntriesLocked();
        }
    }

    public List<DpisLogEntry> readRecentEntries(int maxEntries) {
        synchronized (lock) {
            List<DpisLogEntry> entries = readEntriesLocked();
            if (maxEntries <= 0 || entries.size() <= maxEntries) {
                return entries;
            }
            return new ArrayList<>(entries.subList(entries.size() - maxEntries, entries.size()));
        }
    }

    private List<DpisLogEntry> readEntriesLocked() {
        List<DpisLogEntry> entries = new ArrayList<>();
        if (!logFile.isFile()) {
            return entries;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return entries;
        }
        for (String line : lines) {
            DpisLogEntry entry = parseJsonLine(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private DpisLogEntry parseJsonLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        long timestampMillis = readLongField(line, "timestampMillis", 0L);
        String message = readStringField(line, "message");
        if (message.isBlank()) {
            return null;
        }
        return new DpisLogEntry(
                timestampMillis,
                formatTime(timestampMillis),
                readStringField(line, "level"),
                readStringField(line, "source"),
                readStringField(line, "process"),
                readStringField(line, "package"),
                readStringField(line, "tag"),
                message,
                false
        );
    }

    private String toJsonLine(long timestampMillis, String level, String message) {
        return "{"
                + "\"timestampMillis\":" + timestampMillis
                + ",\"displayTime\":\"" + jsonEscape(formatTime(timestampMillis)) + "\""
                + ",\"level\":\"" + jsonEscape(sanitize(level)) + "\""
                + ",\"source\":\"" + DPIS_LOG_SOURCE + "\""
                + ",\"package\":\"" + DPIS_MODULE_PACKAGE + "\""
                + ",\"process\":\"" + jsonEscape(currentProcessName()) + "\""
                + ",\"tag\":\"" + DpisLog.TAG + "\""
                + ",\"message\":\"" + jsonEscape(sanitize(message)) + "\""
                + "}";
    }

    private void ensureParentDirectory() throws IOException {
        File parent = logFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
    }

    private void trimToCapacity() throws IOException {
        if (!logFile.isFile()) {
            return;
        }
        List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        List<String> retained = retainNewestWithinCapacity(lines);
        if (retained.size() != lines.size() || logFile.length() > maxStoredBytes) {
            Files.write(logFile.toPath(), retained, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private List<String> retainNewestWithinCapacity(List<String> lines) {
        List<String> retained = new ArrayList<>();
        long retainedBytes = 0L;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            long lineBytes = line.getBytes(StandardCharsets.UTF_8).length + 1L;
            if (!retained.isEmpty()
                    && (retained.size() >= maxStoredLines
                    || retainedBytes + lineBytes > maxStoredBytes)) {
                break;
            }
            retained.add(0, line);
            retainedBytes += lineBytes;
        }
        return retained;
    }

    private static String sanitize(String value) {
        return value == null
                ? ""
                : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String currentProcessName() {
        if (appContext == null) {
            return DPIS_MODULE_PACKAGE;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            if (processName != null && !processName.isBlank()) {
                return processName;
            }
        }
        return appContext.getPackageName();
    }

    private static String formatTime(long timestampMillis) {
        if (timestampMillis <= 0L) {
            return "";
        }
        return new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date(timestampMillis));
    }

    private static String readStringField(String line, String fieldName) {
        Matcher matcher = Pattern.compile(String.format(
                Locale.US,
                JSON_STRING_FIELD_PATTERN.pattern(),
                Pattern.quote(fieldName)
        )).matcher(line);
        return matcher.find() ? jsonUnescape(matcher.group(1)) : "";
    }

    private static long readLongField(String line, String fieldName, long fallback) {
        Matcher matcher = Pattern.compile(String.format(
                Locale.US,
                JSON_LONG_FIELD_PATTERN.pattern(),
                Pattern.quote(fieldName)
        )).matcher(line);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder();
        String safeValue = value != null ? value : "";
        for (int i = 0; i < safeValue.length(); i++) {
            char c = safeValue.charAt(i);
            if (c == '"' || c == '\\') {
                builder.append('\\').append(c);
            } else if (c == '\t') {
                builder.append("\\t");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    // This only reverses escapes emitted by jsonEscape; sanitize() removes line breaks before write.
    private static String jsonUnescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                builder.append(c == 't' ? '\t' : c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                builder.append(c);
            }
        }
        if (escaped) {
            builder.append('\\');
        }
        return builder.toString();
    }
}
