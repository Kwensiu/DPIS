package com.dpis.module;

final class ViewportPropertyProjection {

    static final class Encoded {
        final int systemEmulationValue;
        final String targetType;
        final int scalePermille;
        final int compatConfigValue;
        final String compatMode;

        Encoded(int systemEmulationValue, String targetType, int scalePermille,
                int compatConfigValue, String compatMode) {
            this.systemEmulationValue = systemEmulationValue;
            this.targetType = targetType;
            this.scalePermille = scalePermille;
            this.compatConfigValue = compatConfigValue;
            this.compatMode = compatMode;
        }
    }

    static final class Decoded {
        final ViewportTargetSpec targetSpec;
        final String mode;

        Decoded(ViewportTargetSpec targetSpec, String mode) {
            this.targetSpec = targetSpec != null ? targetSpec : ViewportTargetSpec.off();
            this.mode = this.targetSpec.isEnabled()
                    ? ViewportApplyMode.normalize(mode)
                    : ViewportApplyMode.OFF;
        }
    }

    private ViewportPropertyProjection() {
    }

    static Encoded encode(ViewportTargetSpec targetSpec, String mode) {
        String normalizedMode = ViewportApplyMode.normalize(mode);
        ViewportTargetSpec normalizedTarget = targetSpec != null ? targetSpec : ViewportTargetSpec.off();
        boolean enabled = normalizedTarget.isEnabled() && ViewportApplyMode.isEnabled(normalizedMode);

        int systemEmulationValue = enabled
                && normalizedTarget.isAbsoluteDp()
                && (ViewportApplyMode.SYSTEM.equals(normalizedMode)
                || ViewportApplyMode.AUTO.equals(normalizedMode))
                ? normalizedTarget.absoluteWidthDp() : 0;
        int compatConfigValue = enabled && normalizedTarget.isAbsoluteDp()
                ? normalizedTarget.absoluteWidthDp() : 0;
        int scalePermille = enabled && normalizedTarget.isRelativeScale()
                ? normalizedTarget.scalePermille() : 0;
        String targetType = enabled ? normalizedTarget.type() : ViewportTargetType.OFF;
        String compatMode = enabled ? normalizedMode : ViewportApplyMode.OFF;

        return new Encoded(systemEmulationValue, targetType, scalePermille,
                compatConfigValue, compatMode);
    }

    static Decoded decode(Integer systemEmulationValue, String targetType,
                          Integer scalePermille, Integer compatConfigValue, String compatMode) {
        boolean targetTypeMissing = targetType == null || targetType.trim().isEmpty();
        String type = ViewportTargetType.normalize(targetType);
        String mode = ViewportApplyMode.normalize(compatMode);

        Integer widthDp = nonZeroOrNull(compatConfigValue);
        if (widthDp == null) {
            widthDp = nonZeroOrNull(systemEmulationValue);
        }

        if (ViewportTargetType.RELATIVE_SCALE.equals(type)) {
            ViewportTargetSpec spec = scalePermille != null
                    ? ViewportTargetSpec.relativeScale(scalePermille)
                    : ViewportTargetSpec.off();
            return new Decoded(spec, mode);
        }
        if (ViewportTargetType.ABSOLUTE_DP.equals(type)) {
            ViewportTargetSpec spec = widthDp != null
                    ? ViewportTargetSpec.absoluteDp(widthDp)
                    : ViewportTargetSpec.off();
            return new Decoded(spec, mode);
        }
        // Legacy fallback: no type property set, but width exists.
        if (targetTypeMissing && widthDp != null) {
            return new Decoded(ViewportTargetSpec.absoluteDp(widthDp), mode);
        }
        return new Decoded(ViewportTargetSpec.off(), mode);
    }

    private static Integer nonZeroOrNull(Integer value) {
        return value != null && value > 0 ? value : null;
    }
}
