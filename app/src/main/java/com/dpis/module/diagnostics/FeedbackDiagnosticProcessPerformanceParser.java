package com.dpis.module.diagnostics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FeedbackDiagnosticProcessPerformanceParser {
    private FeedbackDiagnosticProcessPerformanceParser() {
    }

    static List<ProcessSummary> parse(List<String> events) {
        Map<String, ProcessSummary> summaries = new LinkedHashMap<>();
        if (events == null) {
            return List.of();
        }
        for (String event : events) {
            if (event == null || !event.contains("category=performance")
                    || !event.contains("stage=aggregate")) {
                continue;
            }
            String message = field(event, "message=");
            String process = field(message, "process=");
            String pid = field(message, "pid=");
            ProcessSummary summary = summaries.computeIfAbsent(
                    process + "|" + pid,
                    ignored -> new ProcessSummary(process, pid)
            );
            for (String routePart : message.split(";route=")) {
                if (routePart.startsWith("process=") || routePart.isBlank()) {
                    continue;
                }
                RouteSummary route = parseRoute(routePart);
                if (route != null) {
                    summary.routes.put(route.route, route);
                }
            }
        }
        List<ProcessSummary> result = new ArrayList<>(summaries.values());
        result.sort(Comparator.comparing(summary -> summary.process));
        return result;
    }

    static List<ProcessSummary> parseMutationAppliedFallback(List<String> events) {
        Map<String, ProcessSummary> summaries = new LinkedHashMap<>();
        if (events == null) {
            return List.of();
        }
        for (String event : events) {
            if (event == null || !event.contains("stage=mutation_applied")) {
                continue;
            }
            String process = tokenField(event, "process=");
            String route = appliedRoute(event);
            if (route.isBlank()) {
                continue;
            }
            ProcessSummary summary = summaries.computeIfAbsent(
                    valueOrDefault(process, "unknown") + "|unknown",
                    ignored -> new ProcessSummary(valueOrDefault(process, "unknown"), "unknown")
            );
            RouteSummary routeSummary = summary.routes.computeIfAbsent(route, RouteSummary::new);
            routeSummary.calls++;
            routeSummary.applied++;
        }
        List<ProcessSummary> result = new ArrayList<>(summaries.values());
        result.sort(Comparator.comparing(summary -> summary.process));
        return result;
    }

    private static RouteSummary parseRoute(String value) {
        String[] fields = value.split(",");
        if (fields.length < 2) {
            return null;
        }
        String route = fields[0].trim();
        if (route.isBlank()) {
            return null;
        }
        RouteSummary summary = new RouteSummary(route);
        for (int i = 1; i < fields.length; i++) {
            String field = fields[i].trim();
            int separator = field.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String name = field.substring(0, separator);
            long numericValue = parseLong(field.substring(separator + 1));
            switch (name) {
                case "calls" -> summary.calls = numericValue;
                case "applied" -> summary.applied = numericValue;
                case "skipped" -> summary.skipped = numericValue;
                case "measuredCalls" -> summary.measuredCalls = numericValue;
                case "p50Us" -> summary.p50Us = numericValue;
                case "p95Us" -> summary.p95Us = numericValue;
                case "p99Us" -> summary.p99Us = numericValue;
                case "maxUs" -> summary.maxUs = numericValue;
                default -> {
                }
            }
        }
        return summary;
    }

    private static String field(String value, String prefix) {
        if (value == null) {
            return "";
        }
        int start = value.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        if ("message=".equals(prefix)) {
            return value.substring(start).trim();
        }
        int end = value.indexOf(',', start);
        if (end < 0) {
            end = value.length();
        }
        return value.substring(start, end).trim();
    }

    private static String tokenField(String value, String prefix) {
        if (value == null) {
            return "";
        }
        int start = value.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        int end = value.indexOf(' ', start);
        if (end < 0) {
            end = value.length();
        }
        return value.substring(start, end).trim();
    }

    private static String appliedRoute(String event) {
        String message = field(event, "message=");
        String hookId = field(message, "hookId=");
        if (!hookId.isBlank()) {
            return hookId;
        }
        String routeName = tokenField(event, "routeName=");
        if (!routeName.isBlank()) {
            return routeName;
        }
        return tokenField(event, "route=");
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    static final class ProcessSummary {
        final String process;
        final String pid;
        final Map<String, RouteSummary> routes = new LinkedHashMap<>();

        ProcessSummary(String process, String pid) {
            this.process = process;
            this.pid = pid;
        }
    }

    static final class RouteSummary {
        final String route;
        long calls;
        long applied;
        long skipped;
        long measuredCalls;
        long p50Us;
        long p95Us;
        long p99Us;
        long maxUs;

        RouteSummary(String route) {
            this.route = route;
        }
    }
}
