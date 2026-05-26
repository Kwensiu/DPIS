package com.dpis.module;

final class AppConfigInputValidation {
    private AppConfigInputValidation() {
    }

    static Integer parsePositiveIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Integer parseFontScalePercentOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return (value >= 50 && value <= 300) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static ViewportTargetSpec parseViewportTargetSpec(String raw, String viewportTargetType) {
        Integer value = parsePositiveIntOrNull(raw);
        if (value == null) {
            return ViewportTargetSpec.off();
        }
        if (ViewportTargetType.RELATIVE_SCALE.equals(
                ViewportTargetType.normalize(viewportTargetType))) {
            if (value < ViewportTargetSpec.MIN_SCALE_PERCENT
                    || value > ViewportTargetSpec.MAX_SCALE_PERCENT) {
                return ViewportTargetSpec.off();
            }
            return ViewportTargetSpec.relativeScale(value * 10);
        }
        return ViewportTargetSpec.absoluteDp(value);
    }

    static String formatViewportInput(ViewportTargetSpec spec) {
        if (spec == null || !spec.isEnabled()) {
            return "";
        }
        if (spec.isRelativeScale()) {
            return String.valueOf(spec.scalePermille() / 10);
        }
        return String.valueOf(spec.absoluteWidthDp());
    }

    static String initialViewportTargetType(ViewportTargetSpec spec) {
        return spec != null && spec.isAbsoluteDp()
                ? ViewportTargetType.ABSOLUTE_DP
                : ViewportTargetType.RELATIVE_SCALE;
    }

    static String initialFontMode(String fontMode) {
        String normalized = FontApplyMode.normalize(fontMode);
        return FontApplyMode.isEnabled(normalized)
                ? normalized
                : FontApplyMode.SYSTEM_EMULATION;
    }

    static boolean isViewportInputValid(String raw, String viewportTargetType) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        Integer value = parsePositiveIntOrNull(raw);
        if (value == null) {
            return false;
        }
        if (ViewportTargetType.RELATIVE_SCALE.equals(
                ViewportTargetType.normalize(viewportTargetType))) {
            return value >= ViewportTargetSpec.MIN_SCALE_PERCENT
                    && value <= ViewportTargetSpec.MAX_SCALE_PERCENT;
        }
        return true;
    }

    static boolean isFontScaleInputValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return parseFontScalePercentOrNull(raw) != null;
    }
}
