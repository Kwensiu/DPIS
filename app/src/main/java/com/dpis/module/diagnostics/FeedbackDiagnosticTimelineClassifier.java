package com.dpis.module.diagnostics;

import java.util.Locale;

final class FeedbackDiagnosticTimelineClassifier {
    private FeedbackDiagnosticTimelineClassifier() {
    }

    public static Event classify(String level, String message, Context context) {
        String normalized = valueOrEmpty(message);
        if (normalized.isEmpty()) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        Route route = routeFor(lower);
        Stage stage = stageFor(lower);
        if (stage == null || route == null || !isHotPathRelevant(lower, stage, route)) {
            return null;
        }
        String resolvedStage = stage.value;
        if (shouldMarkUnexpected(stage, route, context)) {
            resolvedStage = Stage.UNEXPECTED_ROUTE_HIT.value;
        }
        return new Event(
                "runtime",
                route.value,
                resolvedStage,
                valueOrDefault(level, "I"),
                normalized
        );
    }

    private static Stage stageFor(String lower) {
        if (isSystemServerInstallSummary(lower) || isSystemServerHookReady(lower)) {
            return Stage.HOOK_READY;
        }
        if (isSystemServerPackageReady(lower)) {
            return Stage.ROUTE_CALLBACK_ENTERED;
        }
        if (isSystemServerApply(lower) || isSystemServerFontApply(lower)) {
            return Stage.MUTATION_APPLIED;
        }
        if (isSystemServerSkip(lower)) {
            return Stage.SKIPPED;
        }
        if (lower.contains("auto hot reload failed")) {
            return Stage.SKIPPED;
        }
        if (lower.contains("repeated_write")) {
            return Stage.REPEATED_WRITE;
        }
        if (isSkip(lower)) {
            return Stage.SKIPPED;
        }
        if (isHookReady(lower)) {
            return Stage.HOOK_READY;
        }
        if (isMutationApplied(lower)) {
            return Stage.MUTATION_APPLIED;
        }
        if (isMutationCandidate(lower)) {
            return Stage.MUTATION_CANDIDATE;
        }
        if (isRouteCallback(lower)) {
            return Stage.ROUTE_CALLBACK_ENTERED;
        }
        if (isConfigResolved(lower)) {
            return Stage.CONFIG_RESOLVED;
        }
        return null;
    }

    private static Route routeFor(String lower) {
        if (lower.contains("hot reload")) {
            return Route.HOT_RELOAD;
        }
        // Preserve the system_server ownership layer even when the message
        // also contains generic display or configuration terminology.
        if (lower.contains("system_server")) {
            return Route.SYSTEM_SERVER;
        }
        if (isConfigRoute(lower)) {
            return Route.CONFIG;
        }
        if (lower.contains("wechat dpi")) {
            return Route.WECHAT_DPI;
        }
        if (lower.contains("dpis_viewport")
                || lower.contains("dpis_viewport_marker")) {
            return Route.VIEWPORT;
        }
        if (lower.contains("dpis_font")) {
            return Route.FONT;
        }
        if (lower.contains("font_style") || lower.contains("typeface")) {
            return Route.TYPEFACE;
        }
        if (lower.contains("viewport")
                || lower.contains("display")
                || lower.contains("windowmetrics")
                || lower.contains("resources")
                || lower.contains("density")
                || lower.contains("configuration")) {
            return Route.VIEWPORT;
        }
        if (lower.contains("font")
                || lower.contains("text")
                || lower.contains("paint")
                || lower.contains("flutter")
                || lower.contains("webview")
                || lower.contains("scaleddensity")
                || lower.contains("textscalefactor")) {
            return Route.FONT;
        }
        if (isRouteCallback(lower) || isHookReady(lower) || isSkip(lower)) {
            return Route.APP_PROCESS;
        }
        return null;
    }

    private static boolean isHotPathRelevant(String lower, Stage stage, Route route) {
        if (stage == Stage.SKIPPED
                || stage == Stage.HOOK_READY
                || stage == Stage.CONFIG_RESOLVED
                || stage == Stage.ROUTE_CALLBACK_ENTERED
                || stage == Stage.MUTATION_APPLIED
                || stage == Stage.MUTATION_CANDIDATE) {
            return route == Route.FONT
                    || route == Route.VIEWPORT
                    || route == Route.TYPEFACE
                    || route == Route.WECHAT_DPI
                    || route == Route.SYSTEM_SERVER
                    || route == Route.HOT_RELOAD
                    || route == Route.APP_PROCESS
                    || route == Route.CONFIG;
        }
        return lower.contains("dpis_font")
                || lower.contains("dpis_viewport")
                || lower.contains("hook")
                || lower.contains("route")
                || lower.contains("override")
                || lower.contains("rewrite")
                || lower.contains("applied");
    }

    private static boolean shouldMarkUnexpected(Stage stage, Route route, Context context) {
        if (stage == Stage.SKIPPED
                || stage == Stage.HOOK_READY
                || stage == Stage.CONFIG_RESOLVED
                || stage == Stage.MUTATION_CANDIDATE) {
            return false;
        }
        if (context == null || !context.appEnabled) {
            return stage == Stage.ROUTE_CALLBACK_ENTERED || stage == Stage.MUTATION_APPLIED;
        }
        if (route == Route.VIEWPORT) {
            return !context.viewportExpected && stage == Stage.MUTATION_APPLIED;
        }
        if (route == Route.FONT) {
            return !context.fontExpected && stage == Stage.MUTATION_APPLIED;
        }
        if (route == Route.TYPEFACE) {
            return !context.typefaceExpected && stage == Stage.MUTATION_APPLIED;
        }
        if (route == Route.WECHAT_DPI) {
            return !context.wechatDpiExpected
                    && (stage == Stage.ROUTE_CALLBACK_ENTERED
                    || stage == Stage.MUTATION_APPLIED);
        }
        return false;
    }

    private static boolean isSkip(String lower) {
        if (isConfigResolved(lower)) {
            return false;
        }
        return lower.contains("skipped")
                || lower.contains("skip ")
                || lower.contains("suppressed")
                || lower.contains("disabled")
                || lower.contains("missing")
                || lower.contains("not configured")
                || lower.contains("inactive target");
    }

    private static boolean isSystemServerInstallSummary(String lower) {
        return lower.contains("system_server") && lower.contains("install summary");
    }

    private static boolean isSystemServerHookReady(String lower) {
        return lower.contains("system_server")
                && (lower.contains("hook ready")
                || lower.contains("hooks ready")
                || lower.contains("install enter"));
    }

    private static boolean isSystemServerPackageReady(String lower) {
        return lower.contains("system_server") && lower.contains("package ready");
    }

    private static boolean isSystemServerApply(String lower) {
        return lower.contains("system_server")
                && (lower.contains(" apply:")
                || lower.contains(" apply ")
                || lower.contains("mutation_applied"));
    }

    private static boolean isSystemServerFontApply(String lower) {
        return lower.contains("system_server") && lower.contains("fontscale");
    }

    private static boolean isSystemServerSkip(String lower) {
        return lower.contains("system_server")
                && (lower.contains(" skip:")
                || lower.contains(" skipped"));
    }

    private static boolean isHookReady(String lower) {
        return lower.contains("hook ready")
                || lower.contains("hooks ready")
                || lower.contains("probe ready")
                || lower.contains("scan ready")
                || lower.contains("bridge ready")
                || lower.contains("retry hook ready")
                || lower.contains("fallback hooks ready");
    }

    private static boolean isConfigResolved(String lower) {
        return lower.contains("target app matched")
                || lower.contains("hook plan")
                || lower.contains("route plan")
                || lower.contains("hooks installed")
                || lower.contains("app hook plan")
                || lower.contains("resolved")
                || lower.contains("configured")
                || lower.contains("install active")
                || lower.contains("install requested")
                || lower.contains("fontmode=")
                || lower.contains("viewportenabled=")
                || lower.contains("resolvedviewportmode=")
                || lower.contains("suppressed=none")
                || lower.contains("debugdisable");
    }

    private static boolean isConfigRoute(String lower) {
        return lower.contains("target app matched")
                || lower.contains("hook plan")
                || lower.contains("app hook plan")
                || lower.contains("hooks installed")
                || lower.contains("viewportenabled=")
                || lower.contains("fontmode=")
                || lower.contains("resolvedviewportmode=");
    }

    private static boolean isRouteCallback(String lower) {
        return lower.contains("callback hit")
                || lower.contains("route enter")
                || lower.contains("route hit")
                || lower.contains("class hit")
                || lower.contains("install entry")
                || lower.contains("package ready")
                || lower.contains("onpackageloaded enter")
                || lower.contains("onpackageready enter")
                || lower.contains("module loaded")
                || lower.contains("loadedapk")
                || lower.contains("application-attach")
                || lower.contains("activity resume")
                || lower.contains("view attach")
                || lower.contains("content provider")
                || lower.contains("platform message hook")
                || lower.contains("hook install attempted");
    }

    private static boolean isMutationApplied(String lower) {
        return lower.contains("applied")
                || lower.contains(" env apply")
                || lower.contains(" override")
                || lower.contains(" rewrite")
                || lower.contains("state seeded")
                || lower.contains("fontscale:")
                || lower.contains("textscalefactor override")
                || lower.contains("displaymetrics override")
                || lower.contains("settings message override")
                || lower.contains("resend user settings");
    }

    private static boolean isMutationCandidate(String lower) {
        return lower.contains("observed")
                || lower.contains("probe configured")
                || lower.contains("probe scheduled")
                || lower.contains("late maps probe")
                || lower.contains("caller(")
                || lower.contains("method observed")
                || lower.contains("class observed");
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = valueOrEmpty(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    public static final class Context {
        final boolean appEnabled;
        final boolean viewportExpected;
        final boolean fontExpected;
        final boolean typefaceExpected;
        final boolean wechatDpiExpected;

        public Context(
                boolean appEnabled,
                boolean viewportExpected,
                boolean fontExpected,
                boolean typefaceExpected,
                boolean wechatDpiExpected
        ) {
            this.appEnabled = appEnabled;
            this.viewportExpected = viewportExpected;
            this.fontExpected = fontExpected;
            this.typefaceExpected = typefaceExpected;
            this.wechatDpiExpected = wechatDpiExpected;
        }
    }

    public static final class Event {
        private final String category;
        private final String route;
        private final String stage;
        private final String level;
        private final String message;

        Event(String category, String route, String stage, String level, String message) {
            this.category = valueOrDefault(category, "runtime");
            this.route = valueOrDefault(route, Route.APP_PROCESS.value);
            this.stage = valueOrDefault(stage, Stage.ROUTE_CALLBACK_ENTERED.value);
            this.level = valueOrDefault(level, "I");
            this.message = valueOrEmpty(message);
        }

        public String category() {
            return category;
        }

        public String route() {
            return route;
        }

        public String stage() {
            return stage;
        }

        public String level() {
            return level;
        }

        public String message() {
            return message;
        }
    }

    private enum Route {
        CONFIG("config"),
        VIEWPORT("viewport"),
        FONT("font"),
        TYPEFACE("typeface"),
        WECHAT_DPI("wechat_dpi"),
        SYSTEM_SERVER("system_server"),
        HOT_RELOAD("hot_reload"),
        APP_PROCESS("app_process");

        final String value;

        Route(String value) {
            this.value = value;
        }
    }

    private enum Stage {
        HOOK_READY("hook_ready"),
        CONFIG_RESOLVED("config_resolved"),
        ROUTE_CALLBACK_ENTERED("route_callback_entered"),
        MUTATION_CANDIDATE("mutation_candidate"),
        MUTATION_APPLIED("mutation_applied"),
        SKIPPED("skipped"),
        UNEXPECTED_ROUTE_HIT("unexpected_route_hit"),
        REPEATED_WRITE("repeated_write");

        final String value;

        Stage(String value) {
            this.value = value;
        }
    }
}
