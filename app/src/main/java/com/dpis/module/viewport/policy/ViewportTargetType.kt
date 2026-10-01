package com.dpis.module.viewport

object ViewportTargetType {
    const val OFF = "off"
    const val RELATIVE_SCALE = "relative_scale"
    const val ABSOLUTE_DP = "absolute_dp"

    @JvmStatic
    fun normalize(type: String?): String = when (type) {
        RELATIVE_SCALE -> RELATIVE_SCALE
        ABSOLUTE_DP -> ABSOLUTE_DP
        else -> OFF
    }
}
