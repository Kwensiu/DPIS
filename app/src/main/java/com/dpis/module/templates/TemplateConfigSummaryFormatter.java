package com.dpis.module.templates;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TemplateConfigSummaryFormatter {
    public interface Text {
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

    public interface TypefaceResolver {
        TypefaceStatus resolve(String typefaceId);
    }

    private final Text text;
    private final TypefaceResolver typefaceResolver;

    public TemplateConfigSummaryFormatter(Text text, TypefaceResolver typefaceResolver) {
        this.text = Objects.requireNonNull(text, "text");
        this.typefaceResolver = typefaceResolver;
    }

    public Result format(TemplateConfigValue value) {
        TemplateConfigValue normalized = TemplateCustomSemantics.customValue(value);
        ArrayList<String> parts = new ArrayList<>();
        ArrayList<String> viewportParts = new ArrayList<>();
        if (normalized.isRelativeScaleViewport()) {
            viewportParts.add(TemplateConfigValue.formatScaleMilliPercent(
                    normalized.viewportScaleMilliPercent));
        } else if (normalized.isAbsoluteDpViewport()) {
            viewportParts.add(normalized.viewportWidthDp + "dp");
        }
        boolean viewportModeConfigured =
                TemplateCustomSemantics.isCustomViewportApplyMode(normalized.viewportApplyMode);
        boolean viewportDraftConfigured =
                !TemplateConfigValue.VIEWPORT_TARGET_OFF.equals(normalized.viewportTargetType)
                        || normalized.viewportScaleMilliPercentDraft != null
                        || normalized.viewportWidthDpDraft != null;
        if (viewportParts.isEmpty() && (viewportModeConfigured || viewportDraftConfigured)) {
            viewportParts.add(text.noValue());
        }
        if (!viewportParts.isEmpty() && !normalized.hasViewportTargetValue()) {
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
        boolean fontModeConfigured = TemplateConfigValue.isFontApplyModeEnabled(normalized.fontApplyMode);
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
        String normalized = TemplateConfigValue.normalizeViewportTargetType(targetType);
        if (TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP.equals(normalized)) {
            return text.viewportTargetTypeWidth();
        }
        if (TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE.equals(normalized)) {
            return text.viewportTargetTypeScale();
        }
        return "";
    }

    private String modeLabel(String mode) {
        String normalizedViewportMode = TemplateConfigValue.normalizeViewportApplyMode(mode);
        if (TemplateConfigValue.VIEWPORT_MODE_AUTO.equals(normalizedViewportMode)) {
            return text.modeAuto();
        }
        if (TemplateConfigValue.VIEWPORT_MODE_SYSTEM.equals(normalizedViewportMode)
                || TemplateConfigValue.FONT_MODE_SYSTEM_EMULATION.equals(mode)) {
            return text.modeSystem();
        }
        if (TemplateConfigValue.VIEWPORT_MODE_COMPAT.equals(normalizedViewportMode)
                || TemplateConfigValue.FONT_MODE_FIELD_REWRITE.equals(mode)) {
            return text.modeCompat();
        }
        return "";
    }

    public static final class Result {
        public final List<String> summaryParts;
        public final TypefaceStatus typefaceStatus;
        private final String emptySummary;

        Result(List<String> summaryParts, TypefaceStatus typefaceStatus, String emptySummary) {
            this.summaryParts = List.copyOf(summaryParts);
            this.typefaceStatus = typefaceStatus != null ? typefaceStatus : TypefaceStatus.none();
            this.emptySummary = emptySummary != null ? emptySummary : "";
        }

        public String summary() {
            if (summaryParts.isEmpty()) {
                return emptySummary;
            }
            return String.join(" · ", summaryParts);
        }
    }

    public static final class TypefaceStatus {
        public final String typefaceId;
        public final String displayName;
        public final boolean missing;

        private TypefaceStatus(String typefaceId, String displayName, boolean missing) {
            this.typefaceId = typefaceId;
            this.displayName = displayName;
            this.missing = missing;
        }

        public static TypefaceStatus none() {
            return new TypefaceStatus(null, null, false);
        }

        public static TypefaceStatus resolved(String typefaceId, String displayName) {
            if (typefaceId == null || typefaceId.isBlank()
                    || displayName == null || displayName.isBlank()) {
                return none();
            }
            return new TypefaceStatus(typefaceId, displayName, false);
        }

        public static TypefaceStatus missing(String typefaceId) {
            return new TypefaceStatus(typefaceId, null, true);
        }

        public boolean resolved() {
            return typefaceId != null && displayName != null && !missing;
        }
    }
}
