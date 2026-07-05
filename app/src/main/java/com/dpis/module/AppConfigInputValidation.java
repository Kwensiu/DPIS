package com.dpis.module;

public final class AppConfigInputValidation {
    private AppConfigInputValidation() {
    }

    public static Integer parsePositiveIntOrNull(String raw) {
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

    public static Integer parseFontScalePercentOrNull(String raw) {
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

    public static Integer parseViewportScaleMilliPercentOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Reject leading dot, plus sign, percent sign
        if (trimmed.startsWith(".") || trimmed.startsWith("+") || trimmed.endsWith("%")) {
            return null;
        }
        int dotIndex = trimmed.indexOf('.');
        int integerPart;
        int fractionalPart = 0;
        int fractionalDigits = 0;
        if (dotIndex < 0) {
            // No decimal point: pure integer
            try {
                integerPart = Integer.parseInt(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else {
            String intPart = trimmed.substring(0, dotIndex);
            String fracPart = trimmed.substring(dotIndex + 1);
            if (intPart.isEmpty()) {
                // Reject ".83" but allow "83."
                return null;
            }
            if (fracPart.length() > 3) {
                // Reject more than 3 decimal places
                return null;
            }
            try {
                integerPart = Integer.parseInt(intPart);
            } catch (NumberFormatException ignored) {
                return null;
            }
            if (!fracPart.isEmpty()) {
                try {
                    fractionalPart = Integer.parseInt(fracPart);
                    fractionalDigits = fracPart.length();
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        // Reject leading zeroes (e.g. "083") but allow "0" itself
        if (trimmed.length() > 1 && trimmed.charAt(0) == '0' && trimmed.charAt(1) != '.') {
            return null;
        }
        // Convert to scaleMilliPercent
        int scaleMilliPercent = integerPart * 1000;
        if (fractionalDigits > 0) {
            // Pad fractional part to 3 digits: e.g. 3 -> 300, 33 -> 330, 333 -> 333
            for (int i = fractionalDigits; i < 3; i++) {
                fractionalPart *= 10;
            }
            scaleMilliPercent += fractionalPart;
        }
        if (scaleMilliPercent < ViewportTargetSpec.MIN_SCALE_MILLI_PERCENT
                || scaleMilliPercent > ViewportTargetSpec.MAX_SCALE_MILLI_PERCENT) {
            return null;
        }
        return scaleMilliPercent;
    }

    public static String formatScaleMilliPercent(int scaleMilliPercent) {
        return formatScaleMilliPercentInput(scaleMilliPercent) + "%";
    }

    public static String formatScaleMilliPercentInput(int scaleMilliPercent) {
        int wholePercent = scaleMilliPercent / 1000;
        int fractional = scaleMilliPercent % 1000;
        if (fractional == 0) {
            return String.valueOf(wholePercent);
        }
        String fracStr = String.format("%03d", fractional);
        // Trim trailing zeros
        int end = fracStr.length();
        while (end > 0 && fracStr.charAt(end - 1) == '0') {
            end--;
        }
        return wholePercent + "." + fracStr.substring(0, end);
    }

    public static int toLegacyScalePermille(int scaleMilliPercent) {
        return Math.round(scaleMilliPercent / 100.0f);
    }

    public static int fromLegacyScalePermille(int scalePermille) {
        return scalePermille * 100;
    }

    public static ViewportTargetSpec parseViewportTargetSpec(String raw, String viewportTargetType) {
        if (ViewportTargetType.RELATIVE_SCALE.equals(
                ViewportTargetType.normalize(viewportTargetType))) {
            Integer milliPercent = parseViewportScaleMilliPercentOrNull(raw);
            if (milliPercent == null) {
                return ViewportTargetSpec.off();
            }
            return ViewportTargetSpec.relativeScale(milliPercent);
        }
        Integer value = parsePositiveIntOrNull(raw);
        if (value == null) {
            return ViewportTargetSpec.off();
        }
        return ViewportTargetSpec.absoluteDp(value);
    }

    public static String formatViewportInput(ViewportTargetSpec spec) {
        if (spec == null || !spec.isEnabled()) {
            return "";
        }
        if (spec.isRelativeScale()) {
            return formatScaleMilliPercentInput(spec.scaleMilliPercent());
        }
        return String.valueOf(spec.absoluteWidthDp());
    }

    public static String initialViewportTargetType(ViewportTargetSpec spec) {
        return spec != null && spec.isAbsoluteDp()
                ? ViewportTargetType.ABSOLUTE_DP
                : ViewportTargetType.RELATIVE_SCALE;
    }

    public static String initialFontMode(String fontMode) {
        String normalized = FontApplyMode.normalize(fontMode);
        return FontApplyMode.isEnabled(normalized)
                ? normalized
                : FontApplyMode.SYSTEM_EMULATION;
    }

    public static boolean isViewportInputValid(String raw, String viewportTargetType) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        if (ViewportTargetType.RELATIVE_SCALE.equals(
                ViewportTargetType.normalize(viewportTargetType))) {
            return parseViewportScaleMilliPercentOrNull(raw) != null;
        }
        Integer value = parsePositiveIntOrNull(raw);
        return value != null;
    }

    public static boolean isFontScaleInputValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return parseFontScalePercentOrNull(raw) != null;
    }
}
