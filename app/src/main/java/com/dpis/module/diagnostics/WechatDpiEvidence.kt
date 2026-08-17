package com.dpis.module.diagnostics

/**
 * Collapses duplicate WeChat route events from UI memory, runtime transport,
 * and the LSPosed bridge into support-facing lifecycle states.
 *
 * A mutation is stronger evidence than an earlier setup failure because the
 * same cold start may intentionally defer DexKit from package-ready to attach.
 */
object WechatDpiEvidence {
    @JvmStatic
    fun summarize(events: List<String>?): Summary {
        val parsed = events.orEmpty().mapNotNull(::parse).filter { it.route == WECHAT_ROUTE }
        return Summary(
            routeEntry = if (parsed.any { it.stage == CALLBACK_ENTERED && it.routeName in entryRoutes }) {
                "observed"
            } else {
                "missing"
            },
            displayMetrics = summarizeRoute(parsed, DISPLAY_METRICS),
            bottomTabIcon = summarizeRoute(parsed, BOTTOM_TAB_ICON),
            resourceRecovery = summarizeRecovery(parsed),
        )
    }

    private fun summarizeRecovery(events: List<Event>): String {
        val event = events.lastOrNull { it.routeName == RESOURCE_RECOVERY } ?: return "no evidence"
        return when (event.stage) {
            "reapplied" -> "reapplied (${event.detail})"
            "reapply_failed" -> "reapply failed (${event.detail})"
            "confirmed" -> "confirmed (${event.detail})"
            "config_missing" -> "configuration missing (${event.detail})"
            else -> "${event.stage} (${event.detail})"
        }
    }

    private fun summarizeRoute(events: List<Event>, routeName: String): String {
        val routeEvents = events.filter { it.routeName == routeName }
        return when {
            routeEvents.any { it.stage == MUTATION_APPLIED } -> "mutation applied"
            routeEvents.any { it.stage == CALLBACK_ENTERED } -> "callback observed; no mutation evidence"
            routeEvents.any { it.stage == HOOK_READY } -> "hook ready; no callback observed during session"
            routeEvents.lastOrNull { it.stage == SKIPPED } != null -> {
                "skipped (${routeEvents.last { it.stage == SKIPPED }.detail})"
            }
            routeEvents.lastOrNull { it.stage == DEFERRED } != null -> {
                "deferred; no application-attach outcome (${routeEvents.last { it.stage == DEFERRED }.detail})"
            }
            else -> "no evidence"
        }
    }

    private fun parse(raw: String): Event? {
        val route = token(raw, "route") ?: return null
        val stage = token(raw, "stage") ?: return null
        val detail = raw.substringAfter(" message=", "").ifBlank { "observed" }
        val routeName = token(raw, "routeName")
            ?: token(detail.substringAfter("hot path ", ""), "route")
            ?: return null
        return Event(route, routeName, stage, detail)
    }

    private fun token(value: String, name: String): String? {
        val prefix = "$name="
        val start = value.indexOf(prefix)
        if (start < 0) return null
        val valueStart = start + prefix.length
        val end = value.indexOfAny(charArrayOf(' ', ','), valueStart).let {
            if (it < 0) value.length else it
        }
        return value.substring(valueStart, end).takeIf { it.isNotBlank() }
    }

    data class Summary(
        val routeEntry: String,
        val displayMetrics: String,
        val bottomTabIcon: String,
        val resourceRecovery: String,
    )

    private data class Event(
        val route: String,
        val routeName: String,
        val stage: String,
        val detail: String,
    )

    private const val WECHAT_ROUTE = "wechat_dpi"
    private const val DISPLAY_METRICS = "displaymetrics"
    private const val BOTTOM_TAB_ICON = "bottom_tab_icon"
    private const val RESOURCE_RECOVERY = "resource_recovery"
    private const val CALLBACK_ENTERED = "route_callback_entered"
    private const val HOOK_READY = "hook_ready"
    private const val MUTATION_APPLIED = "mutation_applied"
    private const val SKIPPED = "skipped"
    private const val DEFERRED = "deferred"
    private val entryRoutes = setOf(
        "package_ready",
        "application_attach",
        "hot_reload_package_ready",
    )
}
