package com.dpis.module.diagnostics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FeedbackDiagnosticLsposedTimelineParser {
    private static final long REPEAT_WARNING_WINDOW_MS = 300L;

    private FeedbackDiagnosticLsposedTimelineParser() {
    }

    public static List<String> parse(
            String raw,
            long startedAtMillis,
            long finishedAtMillis,
            Input input
    ) {
        return parse(
                raw,
                FeedbackDiagnosticSessionWindow.around(startedAtMillis, finishedAtMillis),
                input
        );
    }

    public static List<String> parse(
            String raw,
            FeedbackDiagnosticSessionWindow window,
            Input input
    ) {
        if (raw == null || raw.isBlank() || window == null || window.endMillis() <= 0L) {
            return List.of();
        }
        List<String> events = new ArrayList<>();
        Map<String, Long> lastMutationByKey = new HashMap<>();
        FeedbackDiagnosticTimelineClassifier.Context context = classifierContext(input);
        for (DpisLogEntry entry : DpisLogParser.parseLsposedDpis(raw)) {
            long timestampMillis = resolveTimestampMillis(entry.timestamp, window.startMillis());
            if (!window.contains(timestampMillis)) {
                continue;
            }
            if (!matchesTarget(entry, input)) {
                continue;
            }
            String hotPathEvent = formatHotPathEvent(timestampMillis, entry, input);
            if (hotPathEvent != null) {
                events.add(hotPathEvent);
                continue;
            }
            FeedbackDiagnosticTimelineClassifier.Event event =
                    classify(entry, context);
            if (event != null) {
                events.add(formatEvent(timestampMillis, entry, input, event));
                String repeated = repeatedWriteEvent(
                        timestampMillis,
                        entry,
                        input,
                        event,
                        lastMutationByKey
                );
                if (repeated != null) {
                    events.add(repeated);
                }
            }
        }
        sortTimelineEvents(events);
        return events;
    }

    public static void sortTimelineEvents(List<String> events) {
        Collections.sort(events, TIMELINE_EVENT_COMPARATOR);
    }

    private static final Comparator<String> TIMELINE_EVENT_COMPARATOR =
            Comparator.comparing(FeedbackDiagnosticLsposedTimelineParser::timePrefix)
                    .thenComparingInt(FeedbackDiagnosticLsposedTimelineParser::stageRank)
                    .thenComparing(String::compareTo);

    private static String timePrefix(String event) {
        String normalized = event != null ? event : "";
        return normalized.length() >= 18 ? normalized.substring(0, 18) : normalized;
    }

    private static int stageRank(String event) {
        String stage = fieldValue(event != null ? event : "", "stage", "");
        return switch (stage) {
            case "probe" -> 0;
            case "hook_ready", "config_resolved", "route_callback_entered" -> 1;
            case "begin" -> 2;
            case "mutation_candidate" -> 3;
            case "applied", "mutation_applied" -> 4;
            case "skipped" -> 5;
            case "end" -> 6;
            case "repeated_write" -> 7;
            case "unexpected_route_hit" -> 8;
            default -> 9;
        };
    }

    private static String formatHotPathEvent(
            long timestampMillis,
            DpisLogEntry entry,
            Input input
    ) {
        String message = entry != null ? entry.message : "";
        String hotPathMessage = hotPathMessage(message);
        if (hotPathMessage == null) {
            return null;
        }
        String route = fieldValue(hotPathMessage, "route", "font");
        String stage = fieldValue(hotPathMessage, "stage", "event");
        String routeName = fieldValue(hotPathMessage, "routeName", "unknown");
        String packageName = fieldValue(
                hotPathMessage,
                "package",
                input != null ? input.packageName : "unknown"
        );
        String detail = detailValue(hotPathMessage);
        return formatTime(timestampMillis)
                + " source=runtime-hotpath"
                + " category=runtime"
                + " route=" + route
                + " stage=" + stage
                + " routeName=" + routeName
                + " level=" + valueOrDefault(entry.level, "I")
                + " package=" + valueOrDefault(packageName, "unknown")
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=" + sanitize(detail);
    }

    public static WindowedRawLog windowRawLog(
            LogReadResult result,
            FeedbackDiagnosticSessionWindow window
    ) {
        if (result == null || result.output.isBlank()) {
            return new WindowedRawLog("", 0, 0, 0, 0);
        }
        if (window == null) {
            return new WindowedRawLog(result.output, 0, 0, 0, 0);
        }
        List<String> retained = new ArrayList<>();
        int total = 0;
        int droppedOutsideWindow = 0;
        int droppedUnparsed = 0;
        List<DpisLogEntry> entries = DpisLogParser.parseLsposedDpis(result.output);
        // Non-DPIS lines (other modules/apps, framework noise) are expected to
        // be filtered out and are counted separately from genuine parse misses.
        int droppedNonDpis = Math.max(0, nonBlankLineCount(result.output) - entries.size());
        for (DpisLogEntry entry : entries) {
            total++;
            long timestampMillis = resolveTimestampMillis(entry.timestamp, window.startMillis());
            if (timestampMillis <= 0L) {
                droppedUnparsed++;
                continue;
            }
            if (!window.contains(timestampMillis)) {
                droppedOutsideWindow++;
                continue;
            }
            retained.add(formatRawEntry(entry));
        }
        return new WindowedRawLog(
                String.join("\n", retained),
                total,
                droppedOutsideWindow,
                droppedUnparsed,
                droppedNonDpis
        );
    }

    private static boolean matchesTarget(
            DpisLogEntry entry,
            Input input
    ) {
        String packageName = input != null ? input.packageName : "";
        if (packageName == null || packageName.isBlank()) {
            return true;
        }
        String message = entry != null ? entry.message : "";
        String process = entry != null ? entry.process : "";
        return message.contains(packageName) || process.equals(packageName);
    }

    private static String formatEvent(
            long timestampMillis,
            DpisLogEntry entry,
            Input input,
            FeedbackDiagnosticTimelineClassifier.Event event
    ) {
        return formatTime(timestampMillis)
                + " source=lsposed-log"
                + " category=" + event.category()
                + " route=" + event.route()
                + " stage=" + event.stage()
                + " level=" + event.level()
                + " package=" + valueOrDefault(input != null ? input.packageName : "", "unknown")
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=" + sanitize(event.message());
    }

    private static FeedbackDiagnosticTimelineClassifier.Event classify(
            DpisLogEntry entry,
            FeedbackDiagnosticTimelineClassifier.Context context
    ) {
        return FeedbackDiagnosticTimelineClassifier.classify(
                entry != null ? entry.level : "",
                entry != null ? entry.message : "",
                context
        );
    }

    private static String repeatedWriteEvent(
            long timestampMillis,
            DpisLogEntry entry,
            Input input,
            FeedbackDiagnosticTimelineClassifier.Event event,
            Map<String, Long> lastMutationByKey
    ) {
        if (!"mutation_applied".equals(event.stage())
                && !"unexpected_route_hit".equals(event.stage())) {
            return null;
        }
        String key = event.route() + "|" + event.stage() + "|" + event.message();
        Long previous = lastMutationByKey.put(key, timestampMillis);
        if (previous == null || timestampMillis - previous.longValue() > REPEAT_WARNING_WINDOW_MS) {
            return null;
        }
        return formatTime(timestampMillis)
                + " source=lsposed-log"
                + " category=warning"
                + " route=" + event.route()
                + " stage=repeated_write"
                + " level=W"
                + " package=" + valueOrDefault(input != null ? input.packageName : "", "unknown")
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=same route event repeated within "
                + REPEAT_WARNING_WINDOW_MS
                + "ms: "
                + sanitize(event.message());
    }

    private static FeedbackDiagnosticTimelineClassifier.Context classifierContext(
            Input input
    ) {
        return new FeedbackDiagnosticTimelineClassifier.Context(
                input != null && input.appEnabled,
                input != null && input.viewportEnabled,
                input != null && input.fontScaleEnabled,
                input != null && input.typefaceEnabled,
                input != null && input.wechatDpiEnabled
        );
    }

    private static String formatRawEntry(DpisLogEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append('[')
                .append(entry.timestamp)
                .append("] ")
                .append(valueOrDefault(entry.level, "I"))
                .append('/')
                .append(valueOrDefault(entry.tag, "DPIS"));
        if (!entry.process.isBlank()) {
            builder.append(" (").append(entry.process).append(')');
        }
        if (!entry.modulePackage.isBlank()) {
            builder.append(" [").append(entry.modulePackage).append(']');
        }
        if (!entry.message.isBlank()) {
            builder.append(' ').append(entry.message);
        }
        return builder.toString();
    }

    public static long resolveTimestampMillis(String timestamp, long anchorMillis) {
        String normalized = timestamp != null ? timestamp.trim() : "";
        if (normalized.isEmpty()) {
            return -1L;
        }
        Calendar anchor = Calendar.getInstance(Locale.US);
        anchor.setTimeInMillis(anchorMillis);
        int year = anchor.get(Calendar.YEAR);
        Long millis = parse(year + "-" + normalized, "yyyy-MM-dd HH:mm:ss.SSS");
        if (millis != null) {
            return millis;
        }
        millis = parse(year + "-" + normalized, "yyyy-MM-dd HH:mm:ss");
        return millis != null ? millis : -1L;
    }

    private static Long parse(String value, String pattern) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            Date date = format.parse(value);
            return date != null ? date.getTime() : null;
        } catch (ParseException exception) {
            return null;
        }
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date(millis));
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static int nonBlankLineCount(String raw) {
        int count = 0;
        for (String line : raw.split("\\R")) {
            if (!line.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static String fieldValue(String message, String fieldName, String fallback) {
        String prefix = fieldName + "=";
        int start = message.indexOf(prefix);
        if (start < 0) {
            return fallback;
        }
        start += prefix.length();
        int end = message.indexOf(' ', start);
        String value = end >= 0 ? message.substring(start, end) : message.substring(start);
        return value.isBlank() ? fallback : value;
    }

    private static String detailValue(String message) {
        String marker = " detail=";
        int start = message.indexOf(marker);
        if (start < 0) {
            return message;
        }
        return message.substring(start + marker.length());
    }

    private static String hotPathMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim();
        if (normalized.startsWith("DPIS ")) {
            normalized = normalized.substring("DPIS ".length()).trim();
        }
        return normalized.startsWith("DPIS_DIAG_HOTPATH ") ? normalized : null;
    }

    public static final class Input {
        private final String packageName;
        private final boolean appEnabled;
        private final boolean viewportEnabled;
        private final boolean fontScaleEnabled;
        private final boolean typefaceEnabled;
        private final boolean wechatDpiEnabled;

        public Input(
                String packageName,
                boolean appEnabled,
                boolean viewportEnabled,
                boolean fontScaleEnabled,
                boolean typefaceEnabled,
                boolean wechatDpiEnabled
        ) {
            this.packageName = packageName != null ? packageName : "";
            this.appEnabled = appEnabled;
            this.viewportEnabled = viewportEnabled;
            this.fontScaleEnabled = fontScaleEnabled;
            this.typefaceEnabled = typefaceEnabled;
            this.wechatDpiEnabled = wechatDpiEnabled;
        }
    }

    public static final class WindowedRawLog {
        private final String output;
        private final int totalParsed;
        private final int droppedOutsideWindow;
        private final int droppedUnparsed;
        private final int droppedNonDpis;

        WindowedRawLog(
                String output,
                int totalParsed,
                int droppedOutsideWindow,
                int droppedUnparsed,
                int droppedNonDpis
        ) {
            this.output = output != null ? output : "";
            this.totalParsed = totalParsed;
            this.droppedOutsideWindow = droppedOutsideWindow;
            this.droppedUnparsed = droppedUnparsed;
            this.droppedNonDpis = droppedNonDpis;
        }

        public String output() {
            return output;
        }

        public int totalParsed() {
            return totalParsed;
        }

        public int droppedOutsideWindow() {
            return droppedOutsideWindow;
        }

        public int droppedUnparsed() {
            return droppedUnparsed;
        }

        public int droppedNonDpis() {
            return droppedNonDpis;
        }
    }
}
