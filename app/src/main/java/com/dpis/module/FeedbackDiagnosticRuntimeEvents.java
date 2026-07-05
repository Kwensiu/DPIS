package com.dpis.module;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dpis.module.diagnostics.FeedbackDiagnosticTimelineClassifier;

public final class FeedbackDiagnosticRuntimeEvents {
    private static final long REPEAT_WARNING_WINDOW_MS = 300L;
    private static volatile Session activeSession;

    private FeedbackDiagnosticRuntimeEvents() {
    }

    static void start(String packageName, FeedbackDiagnosticCoordinator.Request request) {
        activeSession = new Session(packageName, request);
    }

    static List<String> stopSnapshot() {
        Session session = activeSession;
        activeSession = null;
        return session != null ? session.snapshot() : List.of();
    }

    static void cancel() {
        activeSession = null;
    }

    static List<String> snapshotForTest() {
        Session session = activeSession;
        return session != null ? session.snapshot() : List.of();
    }

    private static FeedbackDiagnosticTimelineClassifier.Context classifierContext(
            FeedbackDiagnosticCoordinator.Request request
    ) {
        boolean appEnabled = request != null && request.inScope && request.dpisEnabled;
        return new FeedbackDiagnosticTimelineClassifier.Context(
                appEnabled,
                appEnabled && request.viewportTargetSpec.isEnabled(),
                appEnabled && request.fontScalePercent != null,
                appEnabled && request.typefaceId != null,
                appEnabled && request.wechatDpi != null
        );
    }

    public static void recordDpisLog(String level, String message) {
        Session session = activeSession;
        if (session == null) {
            return;
        }
        session.recordDpisLog(level, message);
    }

    static void recordStructured(
            String packageName,
            String route,
            String stage,
            String level,
            String message
    ) {
        Session session = activeSession;
        if (session == null || !session.matchesTarget(packageName)) {
            return;
        }
        session.recordStructured(route, stage, level, message);
    }

    public static void recordHotReload(
            String packageName,
            String route,
            String stage,
            String message
    ) {
        Session session = activeSession;
        if (session == null || !session.matchesTarget(packageName)) {
            return;
        }
        session.recordStructured(route, "hot_reload_" + valueOrDefault(stage, "event"), "I", message);
    }

    private static final class Session {
        private final String targetPackage;
        private final FeedbackDiagnosticTimelineClassifier.Context classifierContext;
        private final List<String> events = new ArrayList<>();
        private final Map<String, Long> lastEventByKey = new HashMap<>();

        Session(String packageName, FeedbackDiagnosticCoordinator.Request request) {
            targetPackage = valueOrEmpty(packageName);
            classifierContext = classifierContext(request);
        }

        synchronized List<String> snapshot() {
            List<String> snapshot = new ArrayList<>(events);
            Collections.sort(snapshot);
            return snapshot;
        }

        synchronized void recordDpisLog(String level, String message) {
            String normalized = valueOrEmpty(message);
            if (normalized.isEmpty() || !isTargetMessage(normalized)) {
                return;
            }
            FeedbackDiagnosticTimelineClassifier.Event event =
                    FeedbackDiagnosticTimelineClassifier.classify(
                            level,
                            normalized,
                            classifierContext
                    );
            if (event == null) {
                return;
            }
            append(event.category(), event.route(), event.stage(), event.level(), event.message());
            warnIfRepeated(event);
        }

        private boolean isTargetMessage(String message) {
            return !targetPackage.isEmpty() && message.contains(targetPackage);
        }

        private boolean matchesTarget(String packageName) {
            return !targetPackage.isEmpty() && targetPackage.equals(valueOrEmpty(packageName));
        }

        private synchronized void recordStructured(
                String route,
                String stage,
                String level,
                String message
        ) {
            String normalized = valueOrEmpty(message);
            String normalizedRoute = valueOrDefault(route, "font");
            String normalizedStage = valueOrDefault(stage, "event");
            append("runtime", normalizedRoute, normalizedStage, level, normalized);
            warnIfRepeated(normalizedRoute, normalizedStage, normalized);
        }

        private void warnIfRepeated(FeedbackDiagnosticTimelineClassifier.Event event) {
            if (!"mutation_applied".equals(event.stage())
                    && !"unexpected_route_hit".equals(event.stage())) {
                return;
            }
            long now = System.currentTimeMillis();
            String key = event.route() + "|" + event.stage() + "|" + event.message();
            Long previous = lastEventByKey.put(key, now);
            if (previous != null && now - previous.longValue() <= REPEAT_WARNING_WINDOW_MS) {
                append(
                        "warning",
                        event.route(),
                        "repeated_write",
                        "W",
                        "same route event repeated within "
                                + REPEAT_WARNING_WINDOW_MS
                                + "ms: "
                                + event.message()
                );
            }
        }

        private void warnIfRepeated(String route, String stage, String message) {
            if (!"applied".equals(stage) && !"mutation_applied".equals(stage)) {
                return;
            }
            long now = System.currentTimeMillis();
            String key = route + "|" + stage + "|" + message;
            Long previous = lastEventByKey.put(key, now);
            if (previous != null && now - previous.longValue() <= REPEAT_WARNING_WINDOW_MS) {
                append(
                        "warning",
                        route,
                        "repeated_write",
                        "W",
                        "same runtime hot path repeated within "
                                + REPEAT_WARNING_WINDOW_MS
                                + "ms: "
                                + message
                );
            }
        }

        private void append(
                String category,
                String route,
                String stage,
                String level,
                String message
        ) {
            events.add(formatTime(System.currentTimeMillis())
                    + " source=runtime-events"
                    + " category=" + category
                    + " route=" + route
                    + " stage=" + stage
                    + " level=" + valueOrDefault(level, "I")
                    + " package=" + targetPackage
                    + " message=" + message);
        }
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date(millis));
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = valueOrEmpty(value);
        return normalized.isEmpty() ? fallback : normalized;
    }
}
