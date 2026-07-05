package com.dpis.module.diagnostics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DpisLogParser {
    private static final String DPIS_MODULE_PACKAGE = "io.github.kwensiu.dpis";
    private static final String LSPOSED_HOT_RELOAD_PREFIX = "Auto hot reload ";
    private static final Pattern LSPOSED_TIMESTAMP_PATTERN = Pattern.compile(
            "^\\[\\s*\\d{4}-(\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(\\.\\d+)?\\s+.*"
    );
    private static final Pattern GENERIC_LSPOSED_PATTERN = Pattern.compile(
            "^\\[\\s*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?\\s+.*\\s([VDIWEF])/([^\\]\\s]+)\\s*]\\s*(.*)$"
    );

    private DpisLogParser() {
    }

    public static List<DpisLogEntry> parseLsposedDpis(String raw) {
        List<DpisLogEntry> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return entries;
        }
        Set<String> seen = new LinkedHashSet<>();
        String lastTimestamp = "";
        try (BufferedReader reader = new BufferedReader(new StringReader(raw))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ParsedLine parsedLine = parseLine(line.replace("\u0000", ""), lastTimestamp);
                if (parsedLine == null) {
                    continue;
                }
                if (!parsedLine.timestamp.isEmpty()) {
                    lastTimestamp = parsedLine.timestamp;
                }
                DpisLogEntry entry = entryFromParsedLine(parsedLine);
                if (entry == null) {
                    continue;
                }
                String key = entry.timestamp
                        + "|"
                        + entry.modulePackage
                        + "|"
                        + entry.tag
                        + "|"
                        + entry.process
                        + "|"
                        + entry.message;
                if (seen.add(key)) {
                    entries.add(entry);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        entries.sort(Comparator
                .comparingLong((DpisLogEntry entry) -> entry.timestampMillis)
                .thenComparing(entry -> entry.timestamp)
                .thenComparing(entry -> entry.process)
                .thenComparing(entry -> entry.tag)
                .thenComparing(entry -> entry.message));
        return entries;
    }

    private static DpisLogEntry entryFromParsedLine(ParsedLine line) {
        if (!isRelevantToDpis(line)) {
            return null;
        }
        return new DpisLogEntry(
                sortableTimestampMillis(line.timestamp),
                line.timestamp,
                line.level,
                "LSPosed",
                line.process,
                line.modulePackage,
                line.tag,
                line.message.isEmpty() ? line.raw : line.message,
                true
        );
    }

    private static boolean isRelevantToDpis(ParsedLine line) {
        return DPIS_MODULE_PACKAGE.equals(line.modulePackage)
                || isDpisHotReloadFrameworkWarning(line.message);
    }

    private static boolean isDpisHotReloadFrameworkWarning(String message) {
        return message != null
                && message.contains(DPIS_MODULE_PACKAGE)
                && message.contains(LSPOSED_HOT_RELOAD_PREFIX);
    }

    private static ParsedLine parseLine(String line, String inheritedTimestamp) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String timestamp = extractTime(trimmed);
        ParsedLine parsedLine = parseLsposedMetadata(trimmed);
        if (parsedLine != null) {
            parsedLine.timestamp = timestamp != null ? timestamp : inheritedTimestamp;
            return parsedLine;
        }
        parsedLine = parseGenericLsposedLine(trimmed);
        if (parsedLine != null) {
            parsedLine.timestamp = timestamp != null ? timestamp : inheritedTimestamp;
            return parsedLine;
        }
        String body = stripKnownPrefixes(trimmed);
        return new ParsedLine(
                inheritedTimestamp,
                "",
                "",
                extractFallbackTag(body),
                "",
                body,
                trimmed
        );
    }

    private static ParsedLine parseLsposedMetadata(String line) {
        int prefixEnd = line.indexOf("] (");
        if (prefixEnd < 0) {
            return null;
        }
        int levelIndex = line.lastIndexOf("/LSPosedFramework", prefixEnd);
        String level = "";
        if (levelIndex > 0) {
            level = line.substring(levelIndex - 1, levelIndex);
        }
        int processStart = prefixEnd + 3;
        int processEnd = line.indexOf(")[", processStart);
        if (processEnd < 0) {
            return null;
        }
        int metadataStart = processEnd + 1;
        int metadataEnd = line.indexOf(']', metadataStart);
        if (metadataEnd < 0 || metadataEnd + 1 >= line.length()) {
            return null;
        }
        String process = line.substring(processStart, processEnd);
        String[] metadata = line.substring(metadataStart + 1, metadataEnd).split(",", 3);
        String modulePackage = metadata.length > 0 ? metadata[0] : "";
        String tag = metadata.length > 1 ? metadata[1] : "";
        String message = line.substring(metadataEnd + 1).trim();
        return new ParsedLine("", level, process, modulePackage, tag, message, line);
    }

    private static ParsedLine parseGenericLsposedLine(String line) {
        Matcher matcher = GENERIC_LSPOSED_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        return new ParsedLine(
                "",
                matcher.group(1),
                "",
                "",
                matcher.group(2),
                matcher.group(3).trim(),
                line
        );
    }

    private static String stripKnownPrefixes(String line) {
        String value = line;
        value = value.replaceFirst("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+\\s+", "");
        value = value.replaceFirst("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+", "");
        value = value.replaceFirst("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+\\s+", "");
        value = value.replaceFirst("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+", "");
        value = value.replaceFirst("^\\[[^]]+]\\s*", "");
        return value.trim();
    }

    private static String extractTime(String line) {
        Matcher matcher = LSPOSED_TIMESTAMP_PATTERN.matcher(line);
        if (matcher.matches()) {
            String fraction = matcher.group(2) != null ? matcher.group(2) : "";
            return matcher.group(1).replace('T', ' ') + fraction;
        }
        if (line.matches("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}.*")) {
            return line.substring(0, 14);
        }
        if (line.matches("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}.*")) {
            return line.substring(5, 19);
        }
        return null;
    }

    private static long sortableTimestampMillis(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return 0L;
        }
        String digits = timestamp.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        if (digits.length() > 17) {
            digits = digits.substring(0, 17);
        }
        while (digits.length() < 17) {
            digits += "0";
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private static String extractFallbackTag(String body) {
        int colon = body.indexOf(':');
        if (colon > 0) {
            String candidate = body.substring(0, colon).trim();
            if (!candidate.isEmpty() && candidate.length() <= 48) {
                return candidate;
            }
        }
        return body.contains("DPIS") ? "DPIS" : "LSPosed";
    }

    private static final class ParsedLine {
        String timestamp;
        final String level;
        final String process;
        final String modulePackage;
        final String tag;
        final String message;
        final String raw;

        ParsedLine(String timestamp,
                String level,
                String process,
                String modulePackage,
                String tag,
                String message,
                String raw) {
            this.timestamp = timestamp != null ? timestamp : "";
            this.level = level != null ? level : "";
            this.process = process != null ? process : "";
            this.modulePackage = modulePackage != null ? modulePackage : "";
            this.tag = tag != null ? tag : "";
            this.message = message != null ? message : "";
            this.raw = raw != null ? raw : "";
        }
    }
}
