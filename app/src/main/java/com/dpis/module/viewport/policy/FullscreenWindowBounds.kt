package com.dpis.module.viewport

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Rect

/**
 * App-process read of window mode and max bounds. An unreadable mode stays
 * unknown. This path does not use the system_server field fallback.
 */
internal object FullscreenWindowBounds {
    fun windowingMode(config: Configuration?): Int? {
        val windowConfiguration = readWindowConfiguration(config) ?: return null
        return try {
            val method = windowConfiguration.javaClass.getMethod("getWindowingMode")
            method.invoke(windowConfiguration) as? Int
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    fun maxBounds(config: Configuration?): Rect? {
        val windowConfiguration = readWindowConfiguration(config) ?: return null
        return try {
            val bounds = windowConfiguration.javaClass.getMethod("getMaxBounds")
                .invoke(windowConfiguration)
            bounds as? Rect
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    @SuppressLint("BlockedPrivateApi")
    private fun readWindowConfiguration(config: Configuration?): Any? {
        if (config == null) return null
        return try {
            val field = Configuration::class.java.getDeclaredField("windowConfiguration")
            field.isAccessible = true
            field.get(config) ?: Configuration::class.java.getMethod("getWindowConfiguration")
                .invoke(config)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }
}
