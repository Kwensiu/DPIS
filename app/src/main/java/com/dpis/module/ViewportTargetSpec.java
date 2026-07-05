package com.dpis.module;

import java.util.Objects;

public final class ViewportTargetSpec {
    static final int MIN_SCALE_PERCENT = 30;
    static final int MAX_SCALE_PERCENT = 300;
    static final int MIN_SCALE_MILLI_PERCENT = MIN_SCALE_PERCENT * 1000;
    static final int MAX_SCALE_MILLI_PERCENT = MAX_SCALE_PERCENT * 1000;
    static final int DEFAULT_SCALE_MILLI_PERCENT = 100000;
    // Legacy constants for backward compatibility
    static final int MIN_SCALE_PERMILLE = MIN_SCALE_PERCENT * 10;
    static final int MAX_SCALE_PERMILLE = MAX_SCALE_PERCENT * 10;
    static final int DEFAULT_SCALE_PERMILLE = 1000;

    private final String type;
    private final int scaleMilliPercent;
    private final int absoluteWidthDp;

    private ViewportTargetSpec(String type, int scaleMilliPercent, int absoluteWidthDp) {
        this.type = ViewportTargetType.normalize(type);
        this.scaleMilliPercent = scaleMilliPercent;
        this.absoluteWidthDp = absoluteWidthDp;
    }

    public static ViewportTargetSpec off() {
        return new ViewportTargetSpec(ViewportTargetType.OFF, 0, 0);
    }

    public static ViewportTargetSpec relativeScale(int scaleMilliPercent) {
        if (scaleMilliPercent < MIN_SCALE_MILLI_PERCENT || scaleMilliPercent > MAX_SCALE_MILLI_PERCENT) {
            return off();
        }
        return new ViewportTargetSpec(ViewportTargetType.RELATIVE_SCALE, scaleMilliPercent, 0);
    }

    public static ViewportTargetSpec absoluteDp(int widthDp) {
        if (widthDp < 1) {
            return off();
        }
        return new ViewportTargetSpec(ViewportTargetType.ABSOLUTE_DP, 0, widthDp);
    }

    public String type() {
        return type;
    }

    public int scaleMilliPercent() {
        return scaleMilliPercent;
    }

    public int absoluteWidthDp() {
        return absoluteWidthDp;
    }

    public boolean isRelativeScale() {
        return ViewportTargetType.RELATIVE_SCALE.equals(type);
    }

    public boolean isAbsoluteDp() {
        return ViewportTargetType.ABSOLUTE_DP.equals(type);
    }

    public boolean isEnabled() {
        return isRelativeScale() || isAbsoluteDp();
    }

    public int activeValue() {
        if (isRelativeScale()) {
            return scaleMilliPercent;
        }
        if (isAbsoluteDp()) {
            return absoluteWidthDp;
        }
        return 0;
    }

    String fingerprint() {
        if (isRelativeScale()) {
            return "r" + Integer.toString(scaleMilliPercent, 36);
        }
        if (isAbsoluteDp()) {
            return "a" + Integer.toString(absoluteWidthDp, 36);
        }
        return "off";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ViewportTargetSpec other)) {
            return false;
        }
        return scaleMilliPercent == other.scaleMilliPercent
                && absoluteWidthDp == other.absoluteWidthDp
                && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, scaleMilliPercent, absoluteWidthDp);
    }

    @Override
    public String toString() {
        if (isRelativeScale()) {
            return "relative_scale:" + scaleMilliPercent;
        }
        if (isAbsoluteDp()) {
            return "absolute_dp:" + absoluteWidthDp;
        }
        return "off";
    }
}
