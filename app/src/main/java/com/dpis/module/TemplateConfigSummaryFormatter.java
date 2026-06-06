package com.dpis.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TemplateConfigSummaryFormatter {
    interface Text {
        String emptySummary();

        String viewportScale(int wholePercent, int decimalPercent);

        String viewportWidth(int widthDp);

        String viewportMode(String modeLabel);

        String fontScale(int percent);

        String fontMode(String modeLabel);

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
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        ArrayList<String> parts = new ArrayList<>();
        ViewportTargetSpec viewportTargetSpec = normalized.viewportTargetSpec;
        if (viewportTargetSpec.isRelativeScale()) {
            int whole = viewportTargetSpec.scalePermille() / 10;
            int decimal = viewportTargetSpec.scalePermille() % 10;
            parts.add(text.viewportScale(whole, decimal));
        } else if (viewportTargetSpec.isAbsoluteDp()) {
            parts.add(text.viewportWidth(viewportTargetSpec.absoluteWidthDp()));
        }
        if (ViewportApplyMode.SYSTEM.equals(normalized.viewportApplyMode)
                || ViewportApplyMode.COMPAT.equals(normalized.viewportApplyMode)) {
            parts.add(text.viewportMode(modeLabel(normalized.viewportApplyMode)));
        }
        if (normalized.fontScalePercent != null) {
            parts.add(text.fontScale(normalized.fontScalePercent));
        }
        if (FontApplyMode.isEnabled(normalized.fontApplyMode)) {
            parts.add(text.fontMode(modeLabel(normalized.fontApplyMode)));
        }
        TypefaceStatus typefaceStatus = resolveTypeface(normalized.typefaceId);
        if (typefaceStatus.resolved()) {
            parts.add(text.typeface(typefaceStatus.displayName));
        }
        if (normalized.fontHookDomainsRaw != null) {
            parts.add(text.hookDomains());
        }
        return new Result(parts, typefaceStatus, text.emptySummary());
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
