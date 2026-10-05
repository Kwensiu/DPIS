package com.dpis.module.viewport.window

import android.content.res.Configuration

/** Coarse ownership used by the existing viewport routes. */
enum class WindowScopeKind {
    DISPLAY,
    WINDOW,
    UNKNOWN,
}

/** More precise framework window state. This is not a capability or policy result. */
enum class WindowMode {
    FULLSCREEN,
    FREEFORM,
    SPLIT_SCREEN,
    UNKNOWN,
}

/** Evidence source used to classify a current configuration or task. */
enum class WindowScopeEvidence {
    PLATFORM_WINDOWING_MODE,
    PLATFORM_BOUNDS,
    COLOROS_TASK_UTILS,
    COLOROS_WINDOWING_MODE,
}

data class WindowScopeInput(
    val configuration: Configuration?,
    val taskInfo: Any? = null,
)

data class WindowScopeDecision(
    val kind: WindowScopeKind,
    val evidence: WindowScopeEvidence? = null,
    val mode: WindowMode = when (kind) {
        WindowScopeKind.DISPLAY -> WindowMode.FULLSCREEN
        WindowScopeKind.WINDOW -> WindowMode.UNKNOWN
        WindowScopeKind.UNKNOWN -> WindowMode.UNKNOWN
    },
)

interface WindowScopeDetector {
    fun detect(input: WindowScopeInput): WindowScopeDecision?
}
