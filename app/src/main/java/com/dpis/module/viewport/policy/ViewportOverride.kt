package com.dpis.module.viewport

import android.annotation.SuppressLint
import android.content.res.Configuration
import java.lang.reflect.Field

object ViewportOverride {
    class Result(
        @JvmField val widthDp: Int,
        @JvmField val heightDp: Int,
        @JvmField val smallestWidthDp: Int,
        @JvmField val densityDpi: Int,
    )

    @JvmStatic
    fun derive(config: Configuration?, targetWidthDp: Int): Result? =
        derive(config, targetWidthDp, false, null)

    @JvmStatic
    fun derive(
        config: Configuration?,
        targetWidthDp: Int,
        windowScoped: Boolean,
        stableTarget: VirtualDisplayOverride.Result?,
    ): Result? {
        if (config == null || targetWidthDp <= 0) return null
        if (windowScoped) return deriveWindowScoped(config, stableTarget)
        val maxBounds = readMaxBounds(config)
        return derive(
            config,
            targetWidthDp,
            false,
            null,
            maxBounds?.width() ?: 0,
            maxBounds?.height() ?: 0,
            !expandsToMaxBounds(readWindowingMode(config)),
        )
    }

    /** Fullscreen screenHeightDp is the app frame. Max bounds are the physical display. */
    @JvmStatic
    fun derive(
        config: Configuration?,
        targetWidthDp: Int,
        windowScoped: Boolean,
        stableTarget: VirtualDisplayOverride.Result?,
        maxWidthPx: Int,
        maxHeightPx: Int,
        smallerWindow: Boolean,
    ): Result? {
        if (config == null || targetWidthDp <= 0) return null
        if (windowScoped) return deriveWindowScoped(config, stableTarget)
        val fullDisplay = fullscreenSourceDp(
            config.screenWidthDp,
            config.screenHeightDp,
            config.densityDpi,
            maxWidthPx,
            maxHeightPx,
            smallerWindow,
        )
        val sourceWidth = fullDisplay?.widthDp
            ?: config.screenWidthDp.takeIf { it > 0 }
            ?: targetWidthDp
        val sourceHeight = fullDisplay?.heightDp
            ?: config.screenHeightDp.takeIf { it > 0 }
            ?: targetWidthDp
        val sourceSmallest = config.smallestScreenWidthDp.takeIf { it > 0 }
            ?: minOf(sourceWidth, sourceHeight)
        val viewportScale = targetWidthDp.toFloat() / sourceSmallest.toFloat()
        val targetWidth = maxOf(1, kotlin.math.round(sourceWidth * viewportScale).toInt())
        val targetHeight = maxOf(1, kotlin.math.round(sourceHeight * viewportScale).toInt())
        val targetDensityDpi = if (config.densityDpi > 0) {
            maxOf(
                1,
                kotlin.math.round(config.densityDpi * sourceSmallest.toFloat() / targetWidthDp)
                    .toInt()
            )
        } else {
            0
        }
        return Result(targetWidth, targetHeight, targetWidthDp, targetDensityDpi)
    }

    class FullDisplayDp(
        @JvmField val widthDp: Int,
        @JvmField val heightDp: Int,
    )

    @JvmStatic
    fun fullscreenSourceDp(
        screenWidthDp: Int,
        screenHeightDp: Int,
        densityDpi: Int,
        maxWidthPx: Int,
        maxHeightPx: Int,
        smallerWindow: Boolean,
    ): FullDisplayDp? {
        if (smallerWindow || densityDpi <= 0 || maxWidthPx <= 0 || maxHeightPx <= 0) {
            return null
        }
        val widthDp = maxOf(1, kotlin.math.round(maxWidthPx * 160f / densityDpi).toInt())
        val heightDp = maxOf(1, kotlin.math.round(maxHeightPx * 160f / densityDpi).toInt())
        if (widthDp <= screenWidthDp + 1 && heightDp <= screenHeightDp + 1) {
            return null
        }
        return FullDisplayDp(widthDp, heightDp)
    }

    /**
     * Max bounds are the whole display. Expand only when the window mode is
     * known to be fullscreen. An unreadable mode must not give a split window
     * that full height.
     */
    @JvmStatic
    fun expandsToMaxBounds(windowingMode: Int?): Boolean =
        windowingMode != null && windowingMode <= 1

    private fun readWindowingMode(config: Configuration?): Int? {
        val windowConfiguration = readWindowConfiguration(config) ?: return null
        return try {
            val method = windowConfiguration.javaClass.getMethod("getWindowingMode")
            method.invoke(windowConfiguration) as? Int
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun readMaxBounds(config: Configuration?): android.graphics.Rect? {
        val windowConfiguration = readWindowConfiguration(config) ?: return null
        return try {
            val bounds = windowConfiguration.javaClass.getMethod("getMaxBounds")
                .invoke(windowConfiguration)
            bounds as? android.graphics.Rect
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

    @JvmStatic
    fun deriveWindowScoped(
        config: Configuration?,
        targetWidthDp: Int,
        stableTarget: VirtualDisplayOverride.Result?,
        physicalWidthPx: Int,
        physicalHeightPx: Int,
    ): Result? {
        if (config == null || physicalWidthPx <= 0 || physicalHeightPx <= 0) {
            return derive(config, targetWidthDp, true, stableTarget)
        }
        var densityDpi = if (stableTarget != null && stableTarget.densityDpi > 0 &&
            (targetWidthDp <= 0 || stableTarget.smallestWidthDp == targetWidthDp)
        ) {
            stableTarget.densityDpi
        } else {
            config.densityDpi
        }
        if (targetWidthDp > 0 && config.smallestScreenWidthDp > 0 && config.densityDpi > 0 &&
            (stableTarget == null || stableTarget.smallestWidthDp != targetWidthDp)
        ) {
            densityDpi = maxOf(
                1,
                kotlin.math.round(
                    config.densityDpi * config.smallestScreenWidthDp.toFloat() / targetWidthDp,
                ).toInt(),
            )
        }
        if (densityDpi <= 0) {
            return derive(config, targetWidthDp, true, stableTarget)
        }
        val density = DensityOverride.densityFromDpi(densityDpi)
        val widthDp = maxOf(1, kotlin.math.round(physicalWidthPx / density).toInt())
        val heightDp = maxOf(1, kotlin.math.round(physicalHeightPx / density).toInt())
        return Result(widthDp, heightDp, minOf(widthDp, heightDp), densityDpi)
    }

    private fun deriveWindowScoped(
        config: Configuration,
        stableTarget: VirtualDisplayOverride.Result?,
    ): Result {
        val sourceWidth = config.screenWidthDp
        val sourceHeight = config.screenHeightDp
        val sourceSmallest = config.smallestScreenWidthDp
        val sourceDensityDpi = config.densityDpi
        if (stableTarget == null || stableTarget.densityDpi <= 0 ||
            sourceWidth <= 0 || sourceHeight <= 0 || sourceDensityDpi <= 0
        ) {
            return Result(sourceWidth, sourceHeight, sourceSmallest, sourceDensityDpi)
        }
        val sourceDensity = DensityOverride.densityFromDpi(sourceDensityDpi)
        val targetDensity = DensityOverride.densityFromDpi(stableTarget.densityDpi)
        val sourceWidthPx = maxOf(1, kotlin.math.round(sourceWidth * sourceDensity).toInt())
        val sourceHeightPx = maxOf(1, kotlin.math.round(sourceHeight * sourceDensity).toInt())
        val targetWidth = maxOf(1, kotlin.math.round(sourceWidthPx / targetDensity).toInt())
        val targetHeight = maxOf(1, kotlin.math.round(sourceHeightPx / targetDensity).toInt())
        return Result(
            targetWidth,
            targetHeight,
            minOf(targetWidth, targetHeight),
            stableTarget.densityDpi
        )
    }

    @JvmStatic
    fun apply(config: Configuration?, result: Result?) {
        if (config == null || result == null) return
        config.screenWidthDp = result.widthDp
        config.screenHeightDp = result.heightDp
        config.smallestScreenWidthDp = result.smallestWidthDp
        if (result.densityDpi > 0) config.densityDpi = result.densityDpi
        setIntFieldIfPresent(config, "compatScreenWidthDp", result.widthDp)
        setIntFieldIfPresent(config, "compatScreenHeightDp", result.heightDp)
        setIntFieldIfPresent(config, "compatSmallestScreenWidthDp", result.smallestWidthDp)
    }

    private fun setIntFieldIfPresent(config: Configuration, fieldName: String, value: Int) {
        try {
            val field: Field = Configuration::class.java.getField(fieldName)
            field.setInt(config, value)
        } catch (_: ReflectiveOperationException) {
            // Some SDK stubs do not expose compat fields.
        }
    }
}
