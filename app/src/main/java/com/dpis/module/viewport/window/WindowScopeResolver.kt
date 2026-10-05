package com.dpis.module.viewport.window

import android.content.res.Configuration

/** Ordered window classification. Vendor evidence wins; platform evidence remains the fallback. */
object WindowScopeResolver {
    private val detectors = mutableListOf<WindowScopeDetector>(
        ColorOsWindowScopeDetector,
        GenericWindowScopeDetector,
    )

    /** Adds vendor or framework evidence without changing the common resolver contract. */
    @JvmStatic
    @Synchronized
    fun register(detector: WindowScopeDetector) {
        if (detector !in detectors) detectors.add(0, detector)
    }

    @JvmStatic
    @Synchronized
    fun resetForTest() {
        detectors.clear()
        detectors.add(ColorOsWindowScopeDetector)
        detectors.add(GenericWindowScopeDetector)
    }

    @JvmStatic
    fun resolve(configuration: Configuration?, taskInfo: Any? = null): WindowScopeDecision {
        val input = WindowScopeInput(configuration, taskInfo)
        val snapshot = synchronized(this) { detectors.toList() }
        return snapshot.firstNotNullOfOrNull { it.detect(input) }
            ?: WindowScopeDecision(WindowScopeKind.UNKNOWN)
    }

    @JvmStatic
    fun isWindowScoped(configuration: Configuration?, taskInfo: Any? = null): Boolean {
        return resolve(configuration, taskInfo).kind == WindowScopeKind.WINDOW
    }
}
