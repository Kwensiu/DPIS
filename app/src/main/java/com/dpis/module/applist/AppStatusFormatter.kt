package com.dpis.module.applist

import android.content.res.Resources
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigInput
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.EffectiveModeResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.Locale

/** Formats the compact and detailed status shown for an application. */
object AppStatusFormatter {
    class StatusInput constructor(
        @JvmField val inScope: Boolean,
        @JvmField val scopeKnown: Boolean,
        @JvmField val installed: Boolean,
        viewportTargetSpec: ViewportTargetSpec?,
        @JvmField val viewportMode: String?,
        @JvmField val fontScalePercent: Int?,
        @JvmField val fontMode: String?,
        @JvmField val typefaceId: String?,
        @JvmField val dpisEnabled: Boolean,
        @JvmField val appSpecificConfigActive: Boolean,
        @JvmField val wechatDpi: Int?
    ) {
        @JvmField
        val viewportTargetSpec: ViewportTargetSpec = viewportTargetSpec ?: ViewportTargetSpec.off()

        constructor(
            inScope: Boolean,
            scopeKnown: Boolean,
            viewportTargetSpec: ViewportTargetSpec?,
            viewportMode: String?,
            fontScalePercent: Int?,
            fontMode: String?,
            typefaceId: String?,
            dpisEnabled: Boolean
        ) : this(inScope, scopeKnown, true, viewportTargetSpec, viewportMode, fontScalePercent,
            fontMode, typefaceId, dpisEnabled, false, null)

        constructor(
            inScope: Boolean,
            scopeKnown: Boolean,
            viewportWidthDp: Int?,
            viewportMode: String?,
            fontScalePercent: Int?,
            fontMode: String?,
            typefaceId: String?,
            dpisEnabled: Boolean
        ) : this(inScope, scopeKnown, true,
            viewportWidthDp?.let(ViewportTargetSpec::absoluteDp) ?: ViewportTargetSpec.off(),
            viewportMode, fontScalePercent, fontMode, typefaceId, dpisEnabled, false, null)

        constructor(
            inScope: Boolean,
            viewportWidthDp: Int?,
            viewportMode: String?,
            fontScalePercent: Int?,
            fontMode: String?,
            typefaceId: String?,
            dpisEnabled: Boolean
        ) : this(inScope, true, viewportWidthDp, viewportMode, fontScalePercent, fontMode,
            typefaceId, dpisEnabled)
    }

    class Labels(
        @JvmField val injected: String,
        @JvmField val notInjected: String,
        @JvmField val enabled: String,
        @JvmField val disabled: String,
        @JvmField val notEnabled: String,
        @JvmField val notInstalled: String,
        @JvmField val noValue: String,
        @JvmField val emulation: String,
        @JvmField val replace: String,
        @JvmField val viewportScale: String,
        @JvmField val viewportWidth: String,
        @JvmField val font: String,
        @JvmField val wechatDpi: String,
        @JvmField val locale: Locale
    )

    @JvmStatic
    fun format(resources: Resources, input: StatusInput?): String = format(labelsFrom(resources), input)

    @JvmStatic
    fun format(labels: Labels, input: StatusInput?): String = formatInternal(labels, normalizeInput(input), false)

    @JvmStatic
    fun formatCompact(resources: Resources, input: StatusInput?): String =
        formatCompact(labelsFrom(resources), input)

    @JvmStatic
    fun formatCompact(labels: Labels, input: StatusInput?): String =
        formatInternal(labels, normalizeInput(input), true)

    @JvmStatic
    fun shouldWarnViewportEmulation(
        viewportWidthDp: Int?,
        viewportMode: String?,
        systemHooksEnabled: Boolean,
        dpisEnabled: Boolean
    ): Boolean {
        if (!dpisEnabled || viewportWidthDp == null) return false
        val requested = ViewportApplyMode.normalize(viewportMode)
        val effective = EffectiveModeResolver.resolveViewportMode(requested, systemHooksEnabled)
        return requested == ViewportApplyMode.SYSTEM_EMULATION &&
            effective != ViewportApplyMode.SYSTEM_EMULATION
    }

    @JvmStatic
    fun shouldWarnViewportEmulation(
        viewportTargetSpec: ViewportTargetSpec?,
        viewportMode: String?,
        systemHooksEnabled: Boolean,
        dpisEnabled: Boolean
    ): Boolean = shouldWarnViewportEmulation(
        viewportTargetSpec?.takeIf { it.isEnabled }?.activeValue(),
        viewportMode,
        systemHooksEnabled,
        dpisEnabled
    )

    @JvmStatic
    fun shouldWarnFontEmulation(
        fontScalePercent: Int?,
        fontMode: String?,
        systemHooksEnabled: Boolean,
        dpisEnabled: Boolean
    ): Boolean {
        if (!dpisEnabled || fontScalePercent == null) return false
        val requested = FontApplyMode.normalize(fontMode)
        val effective = EffectiveModeResolver.resolveFontMode(requested, systemHooksEnabled)
        return requested == FontApplyMode.SYSTEM_EMULATION &&
            effective != FontApplyMode.SYSTEM_EMULATION
    }

    @JvmStatic
    fun applyConfigSegmentsWarnStyle(
        statusText: String?,
        warnColor: Int,
        warnViewport: Boolean,
        warnFont: Boolean
    ): CharSequence? {
        if (statusText.isNullOrEmpty()) return statusText
        val ranges = resolveWarnSegmentRanges(statusText, warnViewport, warnFont)
        if (ranges.isEmpty()) return statusText
        return SpannableString(statusText).also { styled ->
            ranges.forEach { range ->
                styled.setSpan(
                    ForegroundColorSpan(warnColor), range[0], range[1],
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    @JvmStatic
    fun resolveWarnSegmentRanges(
        statusText: String?,
        warnViewport: Boolean,
        warnFont: Boolean
    ): Array<IntArray> {
        if (statusText.isNullOrEmpty() || (!warnViewport && !warnFont)) return emptyArray()
        val ranges = ArrayList<IntArray>(2)
        val segmentCount = statusText.count { it == '|' } + 1
        val viewportIndex = if (segmentCount >= 3) 1 else 0
        val fontMinIndex = if (segmentCount >= 3) 2 else 1
        if (warnViewport) resolveSegmentRange(statusText, viewportIndex)?.let(ranges::add)
        if (warnFont) resolveFirstSegmentRangeContaining(statusText, "%", fontMinIndex)
            ?.let(ranges::add)
        return ranges.toTypedArray()
    }

    private fun labelsFrom(resources: Resources): Labels {
        val locales = resources.configuration.locales
        val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        return Labels(
            resources.getString(R.string.app_status_scoped),
            resources.getString(R.string.app_status_unscoped),
            resources.getString(R.string.app_status_enabled),
            resources.getString(R.string.app_status_disabled),
            resources.getString(R.string.app_status_not_enabled),
            resources.getString(R.string.app_status_not_installed),
            resources.getString(R.string.app_status_no_value),
            resources.getString(R.string.app_status_mode_system),
            resources.getString(R.string.app_status_mode_compat),
            resources.getString(R.string.app_status_viewport_scale),
            resources.getString(R.string.app_status_viewport_width),
            resources.getString(R.string.app_status_font_prefix),
            resources.getString(R.string.app_status_wechat_dpi),
            locale
        )
    }

    private fun normalizeInput(input: StatusInput?): StatusInput = input ?: StatusInput(
        false, true, true, ViewportTargetSpec.off(), ViewportApplyMode.OFF,
        null, FontApplyMode.OFF, null, true, false, null
    )

    private fun formatInternal(labels: Labels, input: StatusInput, compact: Boolean): String {
        val viewportWidthDp = input.viewportTargetSpec.takeIf { it.isAbsoluteDp }?.absoluteWidthDp()
        val scopeText = if (!input.scopeKnown) null else if (input.installed) {
            if (input.inScope) labels.injected else labels.notInjected
        } else labels.notInstalled
        if (!input.dpisEnabled) return joinSegments(scopeText, labels.disabled)

        val viewportMode = ViewportApplyMode.normalize(input.viewportMode)
        val widthText = if (input.viewportTargetSpec.isRelativeScale) {
            formatViewportScale(labels, input.viewportTargetSpec.scaleMilliPercent(), viewportMode, compact)
        } else if (viewportWidthDp != null) {
            formatViewport(labels, viewportWidthDp, viewportMode, compact)
        } else if (compact) null else if (input.appSpecificConfigActive) labels.enabled else labels.notEnabled
        val hasCustomTypeface = !input.typefaceId.isNullOrBlank()
        val fontMode = FontApplyMode.normalize(input.fontMode)
        val appSpecificText = input.wechatDpi?.let { formatWechatDpi(labels, it) }
        if (compact && viewportWidthDp == null && !input.viewportTargetSpec.isRelativeScale &&
            input.fontScalePercent == null && !hasCustomTypeface && appSpecificText == null
        ) {
            return joinSegments(scopeText, labels.noValue)
        }
        if (!FontApplyMode.isEnabled(fontMode) || input.fontScalePercent == null) {
            return if (compact) joinSegments(scopeText, widthText, appSpecificText)
            else joinSegments(scopeText, widthText,
                formatFont(labels, null, fontMode, compact, hasCustomTypeface), appSpecificText)
        }
        return joinSegments(scopeText, widthText,
            formatFont(labels, input.fontScalePercent, fontMode, compact, hasCustomTypeface),
            appSpecificText)
    }

    private fun formatViewport(labels: Labels, widthDp: Int, mode: String, compact: Boolean): String {
        val value = labels.viewportWidth + " " + String.format(labels.locale, "%ddp", widthDp)
        return if (compact) value else "$value(${modeText(labels, mode)})"
    }

    private fun formatViewportScale(labels: Labels, scale: Int, mode: String, compact: Boolean): String {
        val value = labels.viewportScale + " " + AppConfigInput.formatScaleMilliPercent(scale)
        return if (compact) value else "$value(${modeText(labels, mode)})"
    }

    private fun formatFont(
        labels: Labels,
        fontScalePercent: Int?,
        mode: String,
        compact: Boolean,
        hasCustomTypeface: Boolean
    ): String? {
        if (fontScalePercent == null && !hasCustomTypeface) return null
        val prefix = if (hasCustomTypeface) "${labels.font}[C]" else labels.font
        if (fontScalePercent == null) return prefix
        val value = prefix + " " + String.format(labels.locale, "%d%%", fontScalePercent)
        return if (compact) value else "$value(${modeText(labels, mode)})"
    }

    private fun formatWechatDpi(labels: Labels, dpi: Int): String =
        labels.wechatDpi + " " + String.format(labels.locale, "%d", dpi)

    private fun modeText(labels: Labels, mode: String): String =
        if (mode == ViewportApplyMode.FIELD_REWRITE || mode == FontApplyMode.FIELD_REWRITE) {
            labels.replace
        } else labels.emulation

    private fun joinSegments(vararg segments: String?): String =
        segments.filterNot { it.isNullOrBlank() }.joinToString(" | ")

    private fun resolveFirstSegmentRangeContaining(text: String, needle: String, minIndex: Int): IntArray? {
        var start = 0
        var index = 0
        while (start <= text.length) {
            val separator = text.indexOf('|', start)
            val end = if (separator >= 0) separator else text.length
            if (index >= minIndex && text.substring(start, end).contains(needle)) {
                return trimRange(text, start, end)
            }
            if (separator < 0) return null
            start = separator + 1
            index++
        }
        return null
    }

    private fun resolveSegmentRange(text: String, targetIndex: Int): IntArray? {
        var start = 0
        var index = 0
        while (start <= text.length) {
            val separator = text.indexOf('|', start)
            val end = if (separator >= 0) separator else text.length
            if (index == targetIndex) return trimRange(text, start, end)
            if (separator < 0) return null
            start = separator + 1
            index++
        }
        return null
    }

    private fun trimRange(text: String, start: Int, end: Int): IntArray? {
        var trimmedStart = start
        var trimmedEnd = end
        while (trimmedStart < trimmedEnd && text[trimmedStart].isWhitespace()) trimmedStart++
        while (trimmedEnd > trimmedStart && text[trimmedEnd - 1].isWhitespace()) trimmedEnd--
        return if (trimmedStart < trimmedEnd) intArrayOf(trimmedStart, trimmedEnd) else null
    }
}
