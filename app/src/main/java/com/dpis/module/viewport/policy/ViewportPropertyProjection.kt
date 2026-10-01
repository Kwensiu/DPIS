package com.dpis.module.viewport

object ViewportPropertyProjection {
    class Encoded(
        @JvmField val systemEmulationValue: Int,
        @JvmField val targetType: String,
        @JvmField val scaleMilliPercent: Int,
        @JvmField val compatConfigValue: Int,
        @JvmField val compatMode: String,
    )

    class Decoded(targetSpec: ViewportTargetSpec?, mode: String?) {
        @JvmField
        val targetSpec: ViewportTargetSpec = targetSpec ?: ViewportTargetSpec.off()
        @JvmField
        val mode: String = if (this.targetSpec.isEnabled()) {
            ViewportApplyMode.normalize(mode)
        } else {
            ViewportApplyMode.OFF
        }
    }

    @JvmStatic
    fun encode(targetSpec: ViewportTargetSpec?, mode: String?): Encoded {
        val normalizedMode = ViewportApplyMode.normalize(mode)
        val normalizedTarget = targetSpec ?: ViewportTargetSpec.off()
        val enabled = normalizedTarget.isEnabled() && ViewportApplyMode.isEnabled(normalizedMode)
        val systemEmulationValue = if (enabled && normalizedTarget.isAbsoluteDp() &&
            (ViewportApplyMode.SYSTEM == normalizedMode || ViewportApplyMode.AUTO == normalizedMode)
        ) normalizedTarget.absoluteWidthDp() else 0
        val compatConfigValue = if (enabled && normalizedTarget.isAbsoluteDp()) {
            normalizedTarget.absoluteWidthDp()
        } else 0
        val scaleMilliPercent = if (enabled && normalizedTarget.isRelativeScale()) {
            normalizedTarget.scaleMilliPercent()
        } else 0
        return Encoded(
            systemEmulationValue,
            if (enabled) normalizedTarget.type() else ViewportTargetType.OFF,
            scaleMilliPercent,
            compatConfigValue,
            if (enabled) normalizedMode else ViewportApplyMode.OFF,
        )
    }

    @JvmStatic
    fun decode(
        systemEmulationValue: Int?,
        targetType: String?,
        scaleMilliPercent: Int?,
        compatConfigValue: Int?,
        compatMode: String?,
    ): Decoded {
        val targetTypeMissing = targetType.isNullOrBlank()
        val type = ViewportTargetType.normalize(targetType)
        val mode = ViewportApplyMode.normalize(compatMode)
        val widthDp = nonZeroOrNull(compatConfigValue) ?: nonZeroOrNull(systemEmulationValue)
        if (ViewportTargetType.RELATIVE_SCALE == type) {
            return Decoded(
                scaleMilliPercent?.let { ViewportTargetSpec.relativeScale(it) }
                    ?: ViewportTargetSpec.off(),
                mode,
            )
        }
        if (ViewportTargetType.ABSOLUTE_DP == type) {
            return Decoded(widthDp?.let { ViewportTargetSpec.absoluteDp(it) }
                ?: ViewportTargetSpec.off(), mode)
        }
        // Legacy fallback: no type property set, but width exists.
        if (targetTypeMissing && widthDp != null) return Decoded(
            ViewportTargetSpec.absoluteDp(
                widthDp
            ), mode
        )
        return Decoded(ViewportTargetSpec.off(), mode)
    }

    private fun nonZeroOrNull(value: Int?): Int? = value?.takeIf { it > 0 }
}
