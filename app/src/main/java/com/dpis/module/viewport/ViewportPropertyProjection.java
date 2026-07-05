package com.dpis.module.viewport;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

public final class ViewportPropertyProjection {

    public static final class Encoded {
        public final int systemEmulationValue;
        public final String targetType;
        public final int scaleMilliPercent;
        public final int compatConfigValue;
        public final String compatMode;

        public Encoded(int systemEmulationValue, String targetType, int scaleMilliPercent,
                int compatConfigValue, String compatMode) {
            this.systemEmulationValue = systemEmulationValue;
            this.targetType = targetType;
            this.scaleMilliPercent = scaleMilliPercent;
            this.compatConfigValue = compatConfigValue;
            this.compatMode = compatMode;
        }
    }

    public static final class Decoded {
        public final ViewportTargetSpec targetSpec;
        public final String mode;

        public Decoded(ViewportTargetSpec targetSpec, String mode) {
            this.targetSpec = targetSpec != null ? targetSpec : ViewportTargetSpec.off();
            this.mode = this.targetSpec.isEnabled()
                    ? ViewportApplyMode.normalize(mode)
                    : ViewportApplyMode.OFF;
        }
    }

    private ViewportPropertyProjection() {
    }

    public static Encoded encode(ViewportTargetSpec targetSpec, String mode) {
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
        int scaleMilliPercent = enabled && normalizedTarget.isRelativeScale()
                ? normalizedTarget.scaleMilliPercent() : 0;
        String targetType = enabled ? normalizedTarget.type() : ViewportTargetType.OFF;
        String compatMode = enabled ? normalizedMode : ViewportApplyMode.OFF;

        return new Encoded(systemEmulationValue, targetType, scaleMilliPercent,
                compatConfigValue, compatMode);
    }

    public static Decoded decode(Integer systemEmulationValue, String targetType,
                          Integer scaleMilliPercent, Integer compatConfigValue, String compatMode) {
        boolean targetTypeMissing = targetType == null || targetType.trim().isEmpty();
        String type = ViewportTargetType.normalize(targetType);
        String mode = ViewportApplyMode.normalize(compatMode);

        Integer widthDp = nonZeroOrNull(compatConfigValue);
        if (widthDp == null) {
            widthDp = nonZeroOrNull(systemEmulationValue);
        }

        if (ViewportTargetType.RELATIVE_SCALE.equals(type)) {
            ViewportTargetSpec spec = scaleMilliPercent != null
                    ? ViewportTargetSpec.relativeScale(scaleMilliPercent)
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
