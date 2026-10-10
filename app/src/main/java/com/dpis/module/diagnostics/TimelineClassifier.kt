package com.dpis.module.diagnostics

import java.util.Locale

internal object TimelineClassifier {
    fun classify(level: String?, message: String?, context: Context?): Event? {
        val normalized = message.orEmpty().trim()
        if (normalized.isEmpty()) return null

        val lower = normalized.lowercase(Locale.ROOT)
        val route = routeFor(lower)
        val stage = stageFor(lower)
        if (stage == null || route == null || !isHotPathRelevant(lower, stage, route)) return null

        val resolvedStage = if (shouldMarkUnexpected(stage, route, context)) {
            Stage.UNEXPECTED_ROUTE_HIT
        } else {
            stage
        }
        return Event(
            category = "runtime",
            route = route.value,
            stage = resolvedStage.value,
            level = level.orEmpty().trim().ifEmpty { "I" },
            message = normalized,
        )
    }

    private fun stageFor(lower: String): Stage? = when {
        isSystemServerInstallSummary(lower) || isSystemServerHookReady(lower) -> Stage.HOOK_READY
        isSystemServerPackageReady(lower) -> Stage.ROUTE_CALLBACK_ENTERED
        isSystemServerApply(lower) || isSystemServerFontApply(lower) -> Stage.MUTATION_APPLIED
        isSystemServerSkip(lower) || lower.contains("auto hot reload failed") -> Stage.SKIPPED
        lower.contains("repeated_write") -> Stage.REPEATED_WRITE
        isSkip(lower) -> Stage.SKIPPED
        isHookReady(lower) -> Stage.HOOK_READY
        isMutationApplied(lower) -> Stage.MUTATION_APPLIED
        isMutationCandidate(lower) -> Stage.MUTATION_CANDIDATE
        isRouteCallback(lower) -> Stage.ROUTE_CALLBACK_ENTERED
        isConfigResolved(lower) -> Stage.CONFIG_RESOLVED
        else -> null
    }

    private fun routeFor(lower: String): Route? = when {
        lower.contains("hot reload") -> Route.HOT_RELOAD
        lower.contains("system_server") -> Route.SYSTEM_SERVER
        isConfigRoute(lower) -> Route.CONFIG
        lower.contains("wechat dpi") -> Route.WECHAT_DPI
        lower.contains("dpis_viewport") || lower.contains("dpis_viewport_marker") -> Route.VIEWPORT
        lower.contains("dpis_font") -> Route.FONT
        lower.contains("font_style") || lower.contains("typeface") -> Route.TYPEFACE
        containsAny(lower, VIEWPORT_MARKERS) -> Route.VIEWPORT
        containsAny(lower, FONT_MARKERS) -> Route.FONT
        isRouteCallback(lower) || isHookReady(lower) || isSkip(lower) -> Route.APP_PROCESS
        else -> null
    }

    private fun isHotPathRelevant(lower: String, stage: Stage, route: Route): Boolean {
        if (stage in HOT_PATH_STAGES) return route in RELEVANT_ROUTES
        return containsAny(lower, HOT_PATH_MARKERS)
    }

    private fun shouldMarkUnexpected(stage: Stage, route: Route, context: Context?): Boolean {
        if (stage in EXPECTED_STAGES) return false
        if (context == null || !context.appEnabled) {
            return stage == Stage.ROUTE_CALLBACK_ENTERED || stage == Stage.MUTATION_APPLIED
        }
        return when (route) {
            Route.VIEWPORT -> !context.viewportExpected && stage == Stage.MUTATION_APPLIED
            Route.FONT -> !context.fontExpected && stage == Stage.MUTATION_APPLIED
            Route.TYPEFACE -> !context.typefaceExpected && stage == Stage.MUTATION_APPLIED
            Route.WECHAT_DPI -> !context.wechatDpiExpected &&
                (stage == Stage.ROUTE_CALLBACK_ENTERED || stage == Stage.MUTATION_APPLIED)
            else -> false
        }
    }

    private fun isSkip(lower: String): Boolean {
        if (isConfigResolved(lower)) return false
        return containsAny(lower, SKIP_MARKERS)
    }

    private fun isSystemServerInstallSummary(lower: String) =
        lower.contains("system_server") && lower.contains("install summary")

    private fun isSystemServerHookReady(lower: String) =
        lower.contains("system_server") &&
            containsAny(lower, SYSTEM_SERVER_HOOK_READY_MARKERS)

    private fun isSystemServerPackageReady(lower: String) =
        lower.contains("system_server") && lower.contains("package ready")

    private fun isSystemServerApply(lower: String) =
        lower.contains("system_server") &&
            containsAny(lower, SYSTEM_SERVER_APPLY_MARKERS)

    private fun isSystemServerFontApply(lower: String) =
        lower.contains("system_server") && lower.contains("fontscale")

    private fun isSystemServerSkip(lower: String) =
        lower.contains("system_server") && containsAny(lower, SYSTEM_SERVER_SKIP_MARKERS)

    private fun isHookReady(lower: String) =
        containsAny(lower, HOOK_READY_MARKERS)

    private fun isConfigResolved(lower: String) =
        containsAny(lower, CONFIG_RESOLVED_MARKERS)

    private fun isConfigRoute(lower: String) =
        containsAny(lower, CONFIG_ROUTE_MARKERS)

    private fun isRouteCallback(lower: String) =
        containsAny(lower, ROUTE_CALLBACK_MARKERS)

    private fun isMutationApplied(lower: String) =
        containsAny(lower, MUTATION_APPLIED_MARKERS)

    private fun isMutationCandidate(lower: String) =
        containsAny(lower, MUTATION_CANDIDATE_MARKERS)

    private fun containsAny(value: String, markers: Array<String>): Boolean {
        for (marker in markers) {
            if (value.contains(marker)) return true
        }
        return false
    }

    data class Context(
        val appEnabled: Boolean,
        val viewportExpected: Boolean,
        val fontExpected: Boolean,
        val typefaceExpected: Boolean,
        val wechatDpiExpected: Boolean,
    )

    data class Event(
        val category: String,
        val route: String,
        val stage: String,
        val level: String,
        val message: String,
    ) {
        fun category() = category
        fun route() = route
        fun stage() = stage
        fun level() = level
        fun message() = message
    }

    private enum class Route(val value: String) {
        CONFIG("config"),
        VIEWPORT("viewport"),
        FONT("font"),
        TYPEFACE("typeface"),
        WECHAT_DPI("wechat_dpi"),
        SYSTEM_SERVER("system_server"),
        HOT_RELOAD("hot_reload"),
        APP_PROCESS("app_process"),
    }

    private enum class Stage(val value: String) {
        HOOK_READY("hook_ready"),
        CONFIG_RESOLVED("config_resolved"),
        ROUTE_CALLBACK_ENTERED("route_callback_entered"),
        MUTATION_CANDIDATE("mutation_candidate"),
        MUTATION_APPLIED("mutation_applied"),
        SKIPPED("skipped"),
        UNEXPECTED_ROUTE_HIT("unexpected_route_hit"),
        REPEATED_WRITE("repeated_write"),
    }

    private val HOT_PATH_STAGES = setOf(
        Stage.SKIPPED,
        Stage.HOOK_READY,
        Stage.CONFIG_RESOLVED,
        Stage.ROUTE_CALLBACK_ENTERED,
        Stage.MUTATION_APPLIED,
        Stage.MUTATION_CANDIDATE,
    )
    private val EXPECTED_STAGES = setOf(
        Stage.SKIPPED,
        Stage.HOOK_READY,
        Stage.CONFIG_RESOLVED,
        Stage.MUTATION_CANDIDATE,
    )
    private val RELEVANT_ROUTES = setOf(
        Route.FONT,
        Route.VIEWPORT,
        Route.TYPEFACE,
        Route.WECHAT_DPI,
        Route.SYSTEM_SERVER,
        Route.HOT_RELOAD,
        Route.APP_PROCESS,
        Route.CONFIG,
    )
    private val VIEWPORT_MARKERS = arrayOf(
        "viewport", "display", "windowmetrics", "resources", "density", "configuration",
    )
    private val FONT_MARKERS = arrayOf(
        "font", "text", "paint", "flutter", "webview", "scaleddensity", "textscalefactor",
    )
    private val HOT_PATH_MARKERS = arrayOf(
        "dpis_font", "dpis_viewport", "hook", "route", "override", "rewrite", "applied",
    )
    private val SKIP_MARKERS = arrayOf(
        "skipped", "skip ", "suppressed", "disabled", "missing", "not configured", "inactive target",
    )
    private val SYSTEM_SERVER_HOOK_READY_MARKERS = arrayOf("hook ready", "hooks ready", "install enter")
    private val SYSTEM_SERVER_APPLY_MARKERS = arrayOf(" apply:", " apply ", "mutation_applied")
    private val SYSTEM_SERVER_SKIP_MARKERS = arrayOf(" skip:", " skipped")
    private val HOOK_READY_MARKERS = arrayOf(
        "hook ready", "hooks ready", "probe ready", "scan ready", "bridge ready",
        "retry hook ready", "fallback hooks ready",
    )
    private val CONFIG_RESOLVED_MARKERS = arrayOf(
        "target app matched", "hook plan", "route plan", "hooks installed", "app hook plan",
        "resolved", "configured", "install active", "install requested", "fontmode=",
        "viewportenabled=", "resolvedviewportmode=", "suppressed=none", "debugdisable",
    )
    private val CONFIG_ROUTE_MARKERS = arrayOf(
        "target app matched", "hook plan", "app hook plan", "hooks installed", "viewportenabled=",
        "fontmode=", "resolvedviewportmode=",
    )
    private val ROUTE_CALLBACK_MARKERS = arrayOf(
        "callback hit", "route enter", "route hit", "class hit", "install entry", "package ready",
        "onpackageloaded enter", "onpackageready enter", "module loaded", "loadedapk",
        "application-attach", "activity resume", "view attach", "content provider",
        "platform message hook", "hook install attempted",
    )
    private val MUTATION_APPLIED_MARKERS = arrayOf(
        "applied", " env apply", " override", " rewrite", "state seeded", "fontscale:",
        "textscalefactor override", "displaymetrics override", "settings message override",
        "resend user settings",
    )
    private val MUTATION_CANDIDATE_MARKERS = arrayOf(
        "observed", "probe configured", "probe scheduled", "late maps probe", "caller(",
        "method observed", "class observed",
    )
}
