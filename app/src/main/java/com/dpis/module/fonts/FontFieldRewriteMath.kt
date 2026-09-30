package com.dpis.module.fonts

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object FontFieldRewriteMath {
    private const val RELATIVE_EPSILON = 0.01f
    private const val ABSOLUTE_EPSILON_FLOOR_PX = 0.25f
    private const val FACTOR_MATCH_TOLERANCE = 0.001f

    @JvmStatic
    fun scaleAbsoluteSize(originalSize: Int, factor: Float): Int {
        if (originalSize <= 0) {
            return originalSize
        }
        if (!isScaleFactorActive(factor)) {
            return originalSize
        }
        return maxOf(1, (originalSize * factor).roundToInt())
    }

    @JvmStatic
    fun scaleRelativeSize(originalSize: Float, factor: Float): Float {
        if (!isScaleFactorActive(factor)) {
            return originalSize
        }
        return originalSize * factor
    }

    @JvmStatic
    fun <T> resolveScaledTextSize(
        currentPx: Float,
        factor: Float,
        baseMap: MutableMap<T, Float?>,
        key: T,
    ): Float {
        if (currentPx <= 0f || !isScaleFactorActive(factor)) {
            return currentPx
        }
        var basePx = baseMap[key]
        if (basePx == null || basePx <= 0f) {
            basePx = currentPx
            baseMap[key] = basePx
        }
        var expectedPx = basePx * factor
        if (abs(currentPx - expectedPx) > 1.5f) {
            basePx = currentPx
            baseMap[key] = basePx
            expectedPx = basePx * factor
        }
        return expectedPx
    }

    @JvmStatic
    fun isKnownScaledTextSize(
        currentPx: Float,
        factor: Float,
        lastAppliedPx: Float?,
    ): Boolean {
        if (currentPx <= 0f || !isScaleFactorActive(factor)) {
            return false
        }
        return lastAppliedPx != null
                && lastAppliedPx > 0f
                && approximatelyEqual(currentPx, lastAppliedPx)
    }

    @JvmStatic
    fun isKnownAppliedPaintSize(
        incomingPx: Float,
        factor: Float,
        lastAppliedPx: Float?,
        factorAtApply: Float?,
    ): Boolean {
        if (incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return false
        }
        if (factorAtApply == null || abs(factorAtApply - factor) > FACTOR_MATCH_TOLERANCE) {
            return false
        }
        return lastAppliedPx != null
                && lastAppliedPx > 0f
                && approximatelyEqual(incomingPx, lastAppliedPx)
    }

    @JvmStatic
    fun shouldRecordTextBase(
        incomingPx: Float,
        factor: Float,
        lastAppliedPx: Float?,
    ): Boolean {
        if (incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return false
        }
        return !isKnownScaledTextSize(incomingPx, factor, lastAppliedPx)
    }

    @JvmStatic
    fun shouldRecordTextBase(
        incomingPx: Float,
        factor: Float,
        basePx: Float?,
        lastAppliedPx: Float?,
    ): Boolean {
        if (!shouldRecordTextBase(incomingPx, factor, lastAppliedPx)) {
            return false
        }
        return basePx == null || basePx <= 0f || !approximatelyEqual(incomingPx, basePx)
    }

    @JvmStatic
    fun isSpAlreadyCoveredByScaledDensity(
        density: Float,
        scaledDensity: Float,
        factor: Float,
    ): Boolean {
        if (density <= 0f || scaledDensity <= 0f || !isScaleFactorActive(factor)) {
            return false
        }
        return abs(scaledDensity / density - factor) <= FACTOR_MATCH_TOLERANCE
    }

    @JvmStatic
    fun approximatelyEqual(firstPx: Float, secondPx: Float): Boolean {
        val tolerance = maxOf(
            ABSOLUTE_EPSILON_FLOOR_PX,
            maxOf(abs(firstPx), abs(secondPx)) * RELATIVE_EPSILON,
        )
        return abs(firstPx - secondPx) <= tolerance
    }

    @JvmStatic
    fun containsCommentHint(text: String?): Boolean {
        if (text.isNullOrEmpty()) {
            return false
        }
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("comment")
                || lower.contains("reply")
                || lower.contains("hblineheight")
                || lower.contains("bbs")
    }

    private fun isScaleFactorActive(factor: Float): Boolean = factor > 0f && factor != 1.0f
}
