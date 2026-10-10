package com.dpis.module.appconfig

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

/** Parses and formats the user-editable values used by the app configuration editor. */
object AppConfigInput {
    @JvmStatic
    fun parsePositiveIntOrNull(raw: String?): Int? = raw
        ?.takeUnless(String::isBlank)
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

    @JvmStatic
    fun parseFontScalePercentOrNull(raw: String?): Int? = raw
        ?.takeUnless(String::isBlank)
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it in FONT_SCALE_MIN_PERCENT..FONT_SCALE_MAX_PERCENT }

    @JvmStatic
    fun parseViewportScaleMilliPercentOrNull(raw: String?): Int? {
        val trimmed = raw?.takeUnless(String::isBlank)?.trim() ?: return null

        // Keep the editor strict: a leading dot, plus sign, percent sign, and more than
        // three fractional digits are ambiguous to users and are not persisted.
        if (trimmed.startsWith('.') || trimmed.startsWith('+') || trimmed.endsWith('%')) {
            return null
        }
        val dotIndex = trimmed.indexOf('.')
        val integerPart: Int
        var fractionalPart = 0
        var fractionalDigits = 0
        if (dotIndex < 0) {
            integerPart = trimmed.toIntOrNull() ?: return null
        } else {
            val intPart = trimmed.substring(0, dotIndex)
            val fracPart = trimmed.substring(dotIndex + 1)
            if (intPart.isEmpty() || fracPart.length > 3) {
                return null
            }
            integerPart = intPart.toIntOrNull() ?: return null
            if (fracPart.isNotEmpty()) {
                fractionalPart = fracPart.toIntOrNull() ?: return null
                fractionalDigits = fracPart.length
            }
        }

        // Reject leading zeroes (for example, "083") while allowing "0" and "0.83".
        if (trimmed.length > 1 && trimmed[0] == '0' && trimmed[1] != '.') {
            return null
        }
        repeat(3 - fractionalDigits) {
            fractionalPart *= 10
        }
        val scaleMilliPercent = integerPart * 1000 + fractionalPart
        return scaleMilliPercent.takeIf {
            it in ViewportTargetSpec.MIN_SCALE_MILLI_PERCENT..ViewportTargetSpec.MAX_SCALE_MILLI_PERCENT
        }
    }

    @JvmStatic
    fun formatScaleMilliPercent(scaleMilliPercent: Int): String =
        "${formatScaleMilliPercentInput(scaleMilliPercent)}%"

    @JvmStatic
    fun formatScaleMilliPercentInput(scaleMilliPercent: Int): String {
        val wholePercent = scaleMilliPercent / 1000
        val fractional = scaleMilliPercent % 1000
        if (fractional == 0) {
            return wholePercent.toString()
        }
        val fraction = fractional.toString().padStart(3, '0').trimEnd('0')
        return "$wholePercent.$fraction"
    }

    @JvmStatic
    fun toLegacyScalePermille(scaleMilliPercent: Int): Int =
        Math.round(scaleMilliPercent / 100.0f)

    @JvmStatic
    fun fromLegacyScalePermille(scalePermille: Int): Int = scalePermille * 100

    @JvmStatic
    fun parseViewportTargetSpec(raw: String?, viewportTargetType: String?): ViewportTargetSpec {
        if (ViewportTargetType.RELATIVE_SCALE == ViewportTargetType.normalize(viewportTargetType)) {
            return parseViewportScaleMilliPercentOrNull(raw)
                ?.let(ViewportTargetSpec::relativeScale)
                ?: ViewportTargetSpec.off()
        }
        return parsePositiveIntOrNull(raw)
            ?.let(ViewportTargetSpec::absoluteDp)
            ?: ViewportTargetSpec.off()
    }

    @JvmStatic
    fun formatViewportInput(spec: ViewportTargetSpec?): String {
        if (spec == null || !spec.isEnabled) {
            return ""
        }
        return if (spec.isRelativeScale) {
            formatScaleMilliPercentInput(spec.scaleMilliPercent())
        } else {
            spec.absoluteWidthDp().toString()
        }
    }

    @JvmStatic
    fun initialViewportTargetType(spec: ViewportTargetSpec?): String =
        if (spec?.isAbsoluteDp == true) {
            ViewportTargetType.ABSOLUTE_DP
        } else {
            ViewportTargetType.RELATIVE_SCALE
        }

    @JvmStatic
    fun initialFontMode(fontMode: String?): String {
        val normalized = FontApplyMode.normalize(fontMode)
        return if (FontApplyMode.isEnabled(normalized)) {
            normalized
        } else {
            FontApplyMode.SYSTEM_EMULATION
        }
    }

    @JvmStatic
    fun isViewportInputValid(raw: String?, viewportTargetType: String?): Boolean {
        if (raw.isNullOrBlank()) {
            return true
        }
        return if (ViewportTargetType.RELATIVE_SCALE == ViewportTargetType.normalize(viewportTargetType)) {
            parseViewportScaleMilliPercentOrNull(raw) != null
        } else {
            parsePositiveIntOrNull(raw) != null
        }
    }

    @JvmStatic
    fun isFontScaleInputValid(raw: String?): Boolean =
        raw.isNullOrBlank() || parseFontScalePercentOrNull(raw) != null

    private const val FONT_SCALE_MIN_PERCENT = 50
    private const val FONT_SCALE_MAX_PERCENT = 300
}
