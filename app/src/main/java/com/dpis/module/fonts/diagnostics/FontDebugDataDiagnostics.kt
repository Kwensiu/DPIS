package com.dpis.module.fonts

import android.content.SharedPreferences
import com.dpis.module.config.DpisConfigStore

object FontDebugDataDiagnostics {
    enum class NoDataReason {
        NONE,
        SCOPE_MISSING,
        NOT_INJECTED,
        NO_EVENTS,
    }

    @JvmStatic
    fun resolveNoDataReason(
        store: DpisConfigStore?,
        preferences: SharedPreferences?
    ): NoDataReason {
        if (preferences == null) return NoDataReason.NOT_INJECTED
        if (store == null || store.getConfiguredPackages().isNullOrEmpty()) {
            return NoDataReason.SCOPE_MISSING
        }
        if (FontDebugStatsSchema.hasAnyFontEventSignal(preferences)) return NoDataReason.NONE
        if (FontDebugStatsSchema.hasViewportSignal(preferences)) return NoDataReason.NO_EVENTS
        return NoDataReason.NOT_INJECTED
    }
}
