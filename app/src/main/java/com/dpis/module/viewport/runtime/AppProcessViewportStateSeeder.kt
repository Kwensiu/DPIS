package com.dpis.module.viewport

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import com.dpis.module.diagnostics.DpisLog

object AppProcessViewportStateSeeder {
    @JvmStatic
    fun seedDisplayBaseline(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        requestedMode: String?,
        systemHooksEnabled: Boolean,
    ) {
        try {
            val resources = Resources.getSystem()
            seedDisplayBaseline(
                packageName,
                targetSpec,
                requestedMode,
                systemHooksEnabled,
                resources.configuration,
                resources.displayMetrics,
            )
        } catch (throwable: Throwable) {
            DpisLog.e(
                "DPIS_VIEWPORT app-process state seed failed: package=$packageName",
                throwable
            )
        }
    }

    @JvmStatic
    fun seedDisplayBaseline(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        requestedMode: String?,
        systemHooksEnabled: Boolean,
        config: Configuration?,
        metrics: DisplayMetrics?,
    ): ViewportRuntimeRecord? {
        if (packageName.isNullOrBlank() || config == null || metrics == null || targetSpec == null ||
            !targetSpec.isEnabled()
        ) return null
        val mode = EffectiveModeResolver.resolveViewportMode(requestedMode, systemHooksEnabled)
        if (ViewportApplyMode.OFF == mode) return null
        var sourceWidthDp = config.screenWidthDp
        var sourceHeightDp = config.screenHeightDp
        var sourceSmallestWidthDp = config.smallestScreenWidthDp
        val sourceWidthPx = metrics.widthPixels
        val sourceHeightPx = metrics.heightPixels
        if ((sourceWidthDp <= 0 || sourceHeightDp <= 0 || sourceSmallestWidthDp <= 0) &&
            metrics.densityDpi > 0 && sourceWidthPx > 0 && sourceHeightPx > 0
        ) {
            val density = DensityOverride.densityFromDpi(metrics.densityDpi)
            sourceWidthDp = maxOf(1, kotlin.math.round(sourceWidthPx / density).toInt())
            sourceHeightDp = maxOf(1, kotlin.math.round(sourceHeightPx / density).toInt())
            sourceSmallestWidthDp = minOf(sourceWidthDp, sourceHeightDp)
        }
        val targetSmallestWidthDp = resolveTargetSmallestWidthDp(targetSpec, sourceSmallestWidthDp)
        val virtualDisplay = VirtualDisplayPlan.deriveAbsoluteResultFromPhysicalPixels(
            sourceWidthDp,
            sourceHeightDp,
            sourceSmallestWidthDp,
            sourceWidthPx,
            sourceHeightPx,
            targetSmallestWidthDp,
        ) ?: return null
        val viewportResult = ViewportOverride.Result(
            virtualDisplay.widthDp,
            virtualDisplay.heightDp,
            virtualDisplay.smallestWidthDp,
            virtualDisplay.densityDpi,
        )
        val source = ViewportSourceSnapshot.systemDisplayInfo(
            sourceWidthDp,
            sourceHeightDp,
            sourceSmallestWidthDp,
            metrics.densityDpi,
            sourceWidthPx,
            sourceHeightPx,
        )
        val record = VirtualDisplayState.publish(
            packageName,
            targetSpec,
            source,
            viewportResult,
            virtualDisplay,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
        DpisLog.i(
            "DPIS_VIEWPORT app-process state seeded: package=$packageName, targetSpec=$targetSpec" +
                    ", targetSmallestWidthDp=$targetSmallestWidthDp" +
                    ", source=wDp=$sourceWidthDp,hDp=$sourceHeightDp,swDp=$sourceSmallestWidthDp" +
                    ",dpi=${metrics.densityDpi},wPx=$sourceWidthPx,hPx=$sourceHeightPx" +
                    ", result=wDp=${virtualDisplay.widthDp},hDp=${virtualDisplay.heightDp}" +
                    ",swDp=${virtualDisplay.smallestWidthDp},dpi=${virtualDisplay.densityDpi}" +
                    ",wPx=${virtualDisplay.widthPx},hPx=${virtualDisplay.heightPx}",
        )
        return record
    }

    private fun resolveTargetSmallestWidthDp(
        targetSpec: ViewportTargetSpec?,
        sourceSmallestWidthDp: Int
    ): Int {
        if (targetSpec == null || !targetSpec.isEnabled() || sourceSmallestWidthDp <= 0) return 0
        if (targetSpec.isAbsoluteDp()) return targetSpec.absoluteWidthDp()
        if (targetSpec.isRelativeScale()) {
            return maxOf(
                1,
                kotlin.math.round(sourceSmallestWidthDp * targetSpec.scaleMilliPercent() / 100000f)
                    .toInt()
            )
        }
        return 0
    }
}
