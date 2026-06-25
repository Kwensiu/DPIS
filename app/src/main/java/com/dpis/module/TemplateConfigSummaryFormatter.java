package com.dpis.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TemplateConfigSummaryFormatter {
    interface Text {
        String emptySummary();

        String viewportSummary(String detail);

        String viewportTargetTypeScale();

        String viewportTargetTypeWidth();

        String fontSummary(String detail);

        String noValue();

        String typeface(String displayName);

        String hookDomains();

        String modeAuto();

        String modeSystem();

        String modeCompat();
    }

    interface TypefaceResolver {
        TypefaceStatus resolve(String typefaceId);
    }

    private final Text text;
    private final TypefaceResolver typefaceResolver;

    TemplateConfigSummaryFormatter(Text text, TypefaceResolver typefaceResolver) {
        this.text = Objects.requireNonNull(text, "text");
        this.typefaceResolver = typefaceResolver;
    }

    Result format(TemplateConfigValue value) {
        TemplateConfigValue normalized = TemplateCustomSemantics.customValue(value);
        ArrayList<String> parts = new ArrayList<>();
        ViewportTargetSpec viewportTargetSpec = normalized.viewportTargetSpec;
        ArrayList<String> viewportParts = new ArrayList<>();
        if (viewportTargetSpec.isRelativeScale()) {
            viewportParts.add(AppConfigInputValidation.formatScaleMilliPercent(
                    viewportTargetSpec.scaleMilliPercent()));
        } else if (viewportTargetSpec.isAbsoluteDp()) {
            viewportParts.add(viewportTargetSpec.absoluteWidthDp() + "dp");
        }
        boolean viewportModeConfigured =
                TemplateCustomSemantics.isCustomViewportApplyMode(normalized.viewportApplyMode);
        boolean viewportDraftConfigured =
                !ViewportTargetType.OFF.equals(normalized.viewportTargetType)
                        || normalized.viewportScaleMilliPercentDraft != null
                        || normalized.viewportWidthDpDraft != null;
        if (viewportParts.isEmpty() && (viewportModeConfigured || viewportDraftConfigured)) {
            viewportParts.add(text.noValue());
        }
        if (!viewportParts.isEmpty() && !viewportTargetSpec.isEnabled()) {
            String targetTypeLabel = viewportTargetTypeLabel(normalized.viewportTargetType);
            if (!targetTypeLabel.isEmpty()) {
                viewportParts.add(targetTypeLabel);
            }
        }
        if (!viewportParts.isEmpty() && viewportModeConfigured) {
            viewportParts.add(modeLabel(normalized.viewportApplyMode));
        }
        if (!viewportParts.isEmpty()) {
            parts.add(text.viewportSummary(joinDetails(viewportParts)));
        }

        ArrayList<String> fontParts = new ArrayList<>();
        if (normalized.fontScalePercent != null) {
            fontParts.add(normalized.fontScalePercent + "%");
        }
        boolean fontModeConfigured = FontApplyMode.isEnabled(normalized.fontApplyMode);
        if (fontParts.isEmpty() && fontModeConfigured) {
            fontParts.add(text.noValue());
        }
        if (!fontParts.isEmpty() && fontModeConfigured) {
            fontParts.add(modeLabel(normalized.fontApplyMode));
        }
        TypefaceStatus typefaceStatus = resolveTypeface(normalized.typefaceId);
        if (typefaceStatus.resolved()) {
            fontParts.add(typefaceStatus.displayName);
        }
        if (!fontParts.isEmpty()) {
            parts.add(text.fontSummary(joinDetails(fontParts)));
        }
        if (normalized.fontHookDomainsRaw != null) {
            parts.add(text.hookDomains());
        }
        return new Result(parts, typefaceStatus, text.emptySummary());
    }

    private String joinDetails(List<String> details) {
        return String.join(" · ", details);
    }

    private TypefaceStatus resolveTypeface(String typefaceId) {
        if (typefaceId == null || typefaceId.isBlank()) {
            return TypefaceStatus.none();
        }
        if (typefaceResolver == null) {
            return TypefaceStatus.missing(typefaceId);
        }
        TypefaceStatus status = typefaceResolver.resolve(typefaceId);
        return status != null ? status : TypefaceStatus.missing(typefaceId);
    }

    private String viewportTargetTypeLabel(String targetType) {
        String normalized = ViewportTargetType.normalize(targetType);
        if (ViewportTargetType.ABSOLUTE_DP.equals(normalized)) {
            return text.viewportTargetTypeWidth();
        }
        if (ViewportTargetType.RELATIVE_SCALE.equals(normalized)) {
            return text.viewportTargetTypeScale();
        }
        return "";
    }

    private String modeLabel(String mode) {
        String normalizedViewportMode = ViewportApplyMode.normalize(mode);
        if (ViewportApplyMode.AUTO.equals(normalizedViewportMode)) {
            return text.modeAuto();
        }
        if (ViewportApplyMode.SYSTEM.equals(normalizedViewportMode)
                || FontApplyMode.SYSTEM_EMULATION.equals(mode)) {
            return text.modeSystem();
        }
        if (ViewportApplyMode.COMPAT.equals(normalizedViewportMode)
                || FontApplyMode.FIELD_REWRITE.equals(mode)) {
            return text.modeCompat();
        }
        return "";
    }

    static final class Result {
        final List<String> summaryParts;
        final TypefaceStatus typefaceStatus;
        private final String emptySummary;

        Result(List<String> summaryParts, TypefaceStatus typefaceStatus, String emptySummary) {
            this.summaryParts = List.copyOf(summaryParts);
            this.typefaceStatus = typefaceStatus != null ? typefaceStatus : TypefaceStatus.none();
            this.emptySummary = emptySummary != null ? emptySummary : "";
        }

        String summary() {
            if (summaryParts.isEmpty()) {
                return emptySummary;
            }
            return String.join(" · ", summaryParts);
        }
    }

    static final class TypefaceStatus {
        final String typefaceId;
        final String displayName;
        final boolean missing;

        private TypefaceStatus(String typefaceId, String displayName, boolean missing) {
            this.typefaceId = typefaceId;
            this.displayName = displayName;
            this.missing = missing;
        }

        static TypefaceStatus none() {
            return new TypefaceStatus(null, null, false);
        }

        static TypefaceStatus resolved(String typefaceId, String displayName) {
            if (typefaceId == null || typefaceId.isBlank()
                    || displayName == null || displayName.isBlank()) {
                return none();
            }
            return new TypefaceStatus(typefaceId, displayName, false);
        }

        static TypefaceStatus missing(String typefaceId) {
            return new TypefaceStatus(typefaceId, null, true);
        }

        boolean resolved() {
            return typefaceId != null && displayName != null && !missing;
        }
    }
}
