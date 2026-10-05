package com.dpis.module.viewport

import android.app.Application
import android.content.Context
import android.os.Bundle
import com.dpis.module.fonts.FontDebugStatsStore
import com.dpis.module.fonts.FontDebugStatsTransport
import java.lang.reflect.Method

object ViewportDebugReporter {
    @Volatile
    private var lastSummary: String? = null

    @JvmStatic
    fun report(
        store: com.dpis.module.config.DpisConfigStore?,
        packageName: String?,
        viewportMode: String?,
        sourceWidthDp: Int,
        sourceHeightDp: Int,
        sourceDensityDpi: Int,
        result: ViewportOverride.Result?,
        sharedResult: VirtualDisplayOverride.Result?,
        configurationApplied: Boolean,
    ) {
        if (result == null || packageName.isNullOrEmpty()) return
        val changed =
            result.widthDp != sourceWidthDp || result.heightDp != sourceHeightDp || result.densityDpi != sourceDensityDpi
        var modeText = if (ViewportApplyMode.FIELD_REWRITE == viewportMode) "兼容" else "系统"
        if (!changed) modeText += "(未变化)"
        val summary =
            "视口 $packageName | $modeText | dp ${sourceWidthDp}x$sourceHeightDp -> ${result.widthDp}x${result.heightDp}" +
                    " | dpi $sourceDensityDpi -> ${result.densityDpi} | px ${sharedResult?.widthPx ?: -1}x${sharedResult?.heightPx ?: -1}" +
                    " | cfg=${if (configurationApplied) "on" else "off"}"
        if (summary == lastSummary) return
        val context = resolveContext() ?: return
        val extras =
            Bundle().apply { putString(FontDebugStatsStore.EXTRA_VIEWPORT_DEBUG_SUMMARY, summary) }
        try {
            FontDebugStatsTransport.sendUpdate(context, extras)
            lastSummary = summary
        } catch (_: Throwable) {
        }
    }

    private fun resolveContext(): Context? = try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val currentApplication: Method = activityThread.getDeclaredMethod("currentApplication")
        currentApplication.invoke(null) as? Application
    } catch (_: Throwable) {
        null
    }
}
