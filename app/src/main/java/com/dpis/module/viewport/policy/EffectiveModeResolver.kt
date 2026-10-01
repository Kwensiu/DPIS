package com.dpis.module.viewport

import com.dpis.module.fonts.FontApplyMode

object EffectiveModeResolver {
    @JvmStatic
    fun resolveViewportMode(requestedMode: String?, systemHooksEnabled: Boolean): String {
        val normalized = ViewportApplyMode.normalize(requestedMode)
        if (normalized == ViewportApplyMode.AUTO) {
            // Auto is system-first, then falls back to app-process compatibility.
            return if (systemHooksEnabled) ViewportApplyMode.SYSTEM else ViewportApplyMode.COMPAT
        }
        return if (normalized == ViewportApplyMode.SYSTEM && !systemHooksEnabled) {
            ViewportApplyMode.OFF
        } else {
            normalized
        }
    }

    @JvmStatic
    fun resolveFontMode(requestedMode: String?, systemHooksEnabled: Boolean): String {
        val normalized = FontApplyMode.normalize(requestedMode)
        return if (normalized == FontApplyMode.SYSTEM_EMULATION && !systemHooksEnabled) {
            FontApplyMode.OFF
        } else {
            normalized
        }
    }
}
