package com.dpis.module.viewport

class ViewportTargetSpec private constructor(
    normalizedType: String,
    @JvmField val scaleMilliPercent: Int,
    @JvmField val absoluteWidthDp: Int,
) {
    @JvmField
    val type: String = ViewportTargetType.normalize(normalizedType)

    companion object {
        const val MIN_SCALE_PERCENT = 30
        const val MAX_SCALE_PERCENT = 300
        const val MIN_SCALE_MILLI_PERCENT = MIN_SCALE_PERCENT * 1000
        const val MAX_SCALE_MILLI_PERCENT = MAX_SCALE_PERCENT * 1000
        const val DEFAULT_SCALE_MILLI_PERCENT = 100000
        const val MIN_SCALE_PERMILLE = MIN_SCALE_PERCENT * 10
        const val MAX_SCALE_PERMILLE = MAX_SCALE_PERCENT * 10
        const val DEFAULT_SCALE_PERMILLE = 1000

        @JvmStatic
        fun off() = ViewportTargetSpec(ViewportTargetType.OFF, 0, 0)
        @JvmStatic
        fun relativeScale(scaleMilliPercent: Int) =
            if (scaleMilliPercent !in MIN_SCALE_MILLI_PERCENT..MAX_SCALE_MILLI_PERCENT) off()
            else ViewportTargetSpec(ViewportTargetType.RELATIVE_SCALE, scaleMilliPercent, 0)

        @JvmStatic
        fun absoluteDp(widthDp: Int) =
            if (widthDp < 1) off() else ViewportTargetSpec(
                ViewportTargetType.ABSOLUTE_DP,
                0,
                widthDp
            )
    }

    fun type() = type
    fun scaleMilliPercent() = scaleMilliPercent
    fun absoluteWidthDp() = absoluteWidthDp
    @get:JvmName("getRelativeScaleProperty")
    val isRelativeScale get() = ViewportTargetType.RELATIVE_SCALE == type
    @get:JvmName("getAbsoluteDpProperty")
    val isAbsoluteDp get() = ViewportTargetType.ABSOLUTE_DP == type
    @get:JvmName("getEnabledProperty")
    val isEnabled get() = isRelativeScale || isAbsoluteDp
    fun isRelativeScale() = isRelativeScale
    fun isAbsoluteDp() = isAbsoluteDp
    fun isEnabled() = isEnabled
    fun activeValue() = when {
        isRelativeScale -> scaleMilliPercent; isAbsoluteDp -> absoluteWidthDp; else -> 0
    }

    fun fingerprint() = when {
        isRelativeScale -> "r" + scaleMilliPercent.toString(36)
        isAbsoluteDp -> "a" + absoluteWidthDp.toString(36)
        else -> "off"
    }

    override fun equals(other: Any?) = other is ViewportTargetSpec &&
            type == other.type && scaleMilliPercent == other.scaleMilliPercent && absoluteWidthDp == other.absoluteWidthDp

    override fun hashCode() = 31 * (31 * type.hashCode() + scaleMilliPercent) + absoluteWidthDp
    override fun toString() = when {
        isRelativeScale -> "relative_scale:$scaleMilliPercent"
        isAbsoluteDp -> "absolute_dp:$absoluteWidthDp"
        else -> "off"
    }
}
