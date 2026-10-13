package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import android.graphics.Rect
import kotlin.math.roundToInt

/**
 * Pixels for a system_server configuration. A window frame wins. Otherwise max
 * bounds are the physical display. This path does not consult windowing mode.
 */
object DisplayPixels {
    @JvmStatic
    fun widthPx(configuration: Configuration?, frame: Rect?): Int {
        if (frame != null && frame.width() > 0) {
            return frame.width()
        }
        val bounds = maxBounds(configuration)
        if (bounds != null && bounds.width() > 0) {
            return bounds.width()
        }
        if (configuration == null || configuration.screenWidthDp <= 0 || configuration.densityDpi <= 0) {
            return 0
        }
        return (configuration.screenWidthDp * (configuration.densityDpi / 160.0f)).roundToInt()
    }

    @JvmStatic
    fun heightPx(configuration: Configuration?, frame: Rect?): Int {
        if (frame != null && frame.height() > 0) {
            return frame.height()
        }
        val bounds = maxBounds(configuration)
        if (bounds != null && bounds.height() > 0) {
            return bounds.height()
        }
        if (configuration == null || configuration.screenHeightDp <= 0 || configuration.densityDpi <= 0) {
            return 0
        }
        return (configuration.screenHeightDp * (configuration.densityDpi / 160.0f)).roundToInt()
    }

    /** Launch configurations expose app bounds in screenHeightDp; max bounds retain the physical display. */
    private fun maxBounds(configuration: Configuration?): Rect? {
        if (configuration == null) {
            return null
        }
        return try {
            var windowConfiguration = Reflection.readField(
                configuration, "windowConfiguration",
            )
            if (windowConfiguration == null) {
                windowConfiguration = Configuration::class.java
                    .getMethod("getWindowConfiguration")
                    .invoke(configuration)
            }
            if (windowConfiguration == null) {
                return null
            }
            var bounds = Reflection.readField(
                windowConfiguration, "mMaxBounds",
            )
            if (bounds == null) {
                bounds = windowConfiguration.javaClass
                    .getMethod("getMaxBounds")
                    .invoke(windowConfiguration)
            }
            val rect = bounds as? Rect ?: return null
            Rect(rect)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }
}
