package com.dpis.module.templates

import java.util.List
import java.util.Objects

class TemplateConfigSummaryFormatter(text: Text?, private val typefaceResolver: TypefaceResolver?) {
    interface Text {
        fun emptySummary(): String?

        fun viewportSummary(detail: String?): String?

        fun viewportTargetTypeScale(): String

        fun viewportTargetTypeWidth(): String

        fun fontSummary(detail: String?): String?

        fun noValue(): String?

        fun typeface(displayName: String?): String?

        fun hookDomains(): String?

        fun modeAuto(): String?

        fun modeSystem(): String?

        fun modeCompat(): String?
    }

    fun interface TypefaceResolver {
        fun resolve(typefaceId: String?): TypefaceStatus?
    }

    private val text: Text

    init {
        this.text = Objects.requireNonNull<Text>(text, "text")
    }

    fun format(value: TemplateConfigValue?): Result {
        val normalized = TemplateCustomSemantics.customValue(value)
        val parts = ArrayList<String?>()
        val viewportParts = ArrayList<String?>()
        if (normalized.isRelativeScaleViewport) {
            viewportParts.add(
                TemplateConfigValue.formatScaleMilliPercent(
                    normalized.viewportScaleMilliPercent
                )
            )
        } else if (normalized.isAbsoluteDpViewport) {
            viewportParts.add(normalized.viewportWidthDp.toString() + "dp")
        }
        val viewportModeConfigured =
            TemplateCustomSemantics.isCustomViewportApplyMode(normalized.viewportApplyMode)
        val viewportDraftConfigured =
            (TemplateConfigValue.VIEWPORT_TARGET_OFF != normalized.viewportTargetType) || normalized.viewportScaleMilliPercentDraft != null || normalized.viewportWidthDpDraft != null
        if (viewportParts.isEmpty() && (viewportModeConfigured || viewportDraftConfigured)) {
            viewportParts.add(text.noValue())
        }
        if (!viewportParts.isEmpty() && !normalized.hasViewportTargetValue()) {
            val targetTypeLabel = viewportTargetTypeLabel(normalized.viewportTargetType)
            if (!targetTypeLabel.isEmpty()) {
                viewportParts.add(targetTypeLabel)
            }
        }
        if (!viewportParts.isEmpty() && viewportModeConfigured) {
            viewportParts.add(modeLabel(normalized.viewportApplyMode))
        }
        if (!viewportParts.isEmpty()) {
            parts.add(text.viewportSummary(joinDetails(viewportParts)))
        }

        val fontParts = ArrayList<String?>()
        if (normalized.fontScalePercent != null) {
            fontParts.add(normalized.fontScalePercent.toString() + "%")
        }
        val fontModeConfigured =
            TemplateConfigValue.isFontApplyModeEnabled(normalized.fontApplyMode)
        if (fontParts.isEmpty() && fontModeConfigured) {
            fontParts.add(text.noValue())
        }
        if (!fontParts.isEmpty() && fontModeConfigured) {
            fontParts.add(modeLabel(normalized.fontApplyMode))
        }
        val typefaceStatus = resolveTypeface(normalized.typefaceId)
        if (typefaceStatus.resolved()) {
            fontParts.add(typefaceStatus.displayName)
        }
        if (!fontParts.isEmpty()) {
            parts.add(text.fontSummary(joinDetails(fontParts)))
        }
        if (normalized.fontHookDomainsRaw != null) {
            parts.add(text.hookDomains())
        }
        return Result(parts, typefaceStatus, text.emptySummary())
    }

    private fun joinDetails(details: MutableList<String?>): String {
        return details.filterNotNull().joinToString(" · ")
    }

    private fun resolveTypeface(typefaceId: String?): TypefaceStatus {
        if (typefaceId == null || typefaceId.isBlank()) {
            return TypefaceStatus.none()
        }
        if (typefaceResolver == null) {
            return TypefaceStatus.absent(typefaceId)
        }
        val status = typefaceResolver.resolve(typefaceId)
        return if (status != null) status else TypefaceStatus.absent(typefaceId)
    }

    private fun viewportTargetTypeLabel(targetType: String?): String {
        val normalized = TemplateConfigValue.normalizeViewportTargetType(targetType)
        if (TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP == normalized) {
            return text.viewportTargetTypeWidth()
        }
        if (TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE == normalized) {
            return text.viewportTargetTypeScale()
        }
        return ""
    }

    private fun modeLabel(mode: String?): String? {
        val normalizedViewportMode = TemplateConfigValue.normalizeViewportApplyMode(mode)
        if (TemplateConfigValue.VIEWPORT_MODE_AUTO == normalizedViewportMode) {
            return text.modeAuto()
        }
        if (TemplateConfigValue.VIEWPORT_MODE_SYSTEM == normalizedViewportMode
            || TemplateConfigValue.FONT_MODE_SYSTEM_EMULATION == mode
        ) {
            return text.modeSystem()
        }
        if (TemplateConfigValue.VIEWPORT_MODE_COMPAT == normalizedViewportMode
            || TemplateConfigValue.FONT_MODE_FIELD_REWRITE == mode
        ) {
            return text.modeCompat()
        }
        return ""
    }

    class Result internal constructor(
        summaryParts: MutableList<String?>,
        typefaceStatus: TypefaceStatus?,
        emptySummary: String?
    ) {
        @JvmField
        val summaryParts: MutableList<String?>
        @JvmField
        val typefaceStatus: TypefaceStatus
        @JvmField
        val emptySummary: String

        init {
            this.summaryParts = List.copyOf<String?>(summaryParts)
            this.typefaceStatus =
                if (typefaceStatus != null) typefaceStatus else TypefaceStatus.none()
            this.emptySummary = if (emptySummary != null) emptySummary else ""
        }

        fun summary(): String {
            if (summaryParts.isEmpty()) {
                return emptySummary
            }
            return summaryParts.filterNotNull().joinToString(" · ")
        }
    }

    class TypefaceStatus private constructor(
        @JvmField val typefaceId: String?,
        @JvmField val displayName: String?,
        @JvmField val missing: Boolean
    ) {
        fun resolved(): Boolean {
            return typefaceId != null && displayName != null && !missing
        }

        companion object {
            @JvmStatic
            fun none(): TypefaceStatus {
                return TypefaceStatus(null, null, false)
            }

            @JvmStatic
            fun resolved(typefaceId: String?, displayName: String?): TypefaceStatus {
                if (typefaceId == null || typefaceId.isBlank()
                    || displayName == null || displayName.isBlank()
                ) {
                    return none()
                }
                return TypefaceStatus(typefaceId, displayName, false)
            }

            @JvmStatic
            fun absent(typefaceId: String?): TypefaceStatus {
                return TypefaceStatus(typefaceId, null, true)
            }

            @JvmStatic
            fun missing(typefaceId: String?): TypefaceStatus {
                return absent(typefaceId)
            }
        }
    }
}
