package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.FakePrefs
import com.dpis.module.diagnostics.Coordinator
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec

internal object ResourcesReadHookTestSupport {
    const val PACKAGE_NAME = "com.example.target"

    fun stableConfig(): Configuration {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        return config
    }

    fun stableMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        return metrics
    }

    fun putCompatViewport(prefs: FakePrefs, widthDp: Int) {
        prefs.edit()
            .putInt("viewport.$PACKAGE_NAME.width_dp", widthDp)
            .putString("viewport.$PACKAGE_NAME.mode", ViewportApplyMode.COMPAT)
            .commit()
    }

    fun request(): Coordinator.Request {
        return Coordinator.Request(
            PACKAGE_NAME,
            "Target",
            "1",
            true,
            true,
            true,
            false,
            ViewportTargetSpec.relativeScale(90000),
            ViewportApplyMode.COMPAT,
            null,
            FontApplyMode.OFF,
            null,
            null,
            null,
        )
    }
}
