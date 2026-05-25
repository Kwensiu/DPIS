package com.dpis.module;

import java.util.Objects;

final class ViewportTargetSpec {
    static final int MIN_SCALE_PERMILLE = 500;
    static final int MAX_SCALE_PERMILLE = 2000;
    static final int DEFAULT_SCALE_PERMILLE = 1000;

    private final String type;
    private final int scalePermille;
    private final int absoluteWidthDp;

    private ViewportTargetSpec(String type, int scalePermille, int absoluteWidthDp) {
        this.type = ViewportTargetType.normalize(type);
        this.scalePermille = scalePermille;
        this.absoluteWidthDp = absoluteWidthDp;
    }

    static ViewportTargetSpec off() {
        return new ViewportTargetSpec(ViewportTargetType.OFF, 0, 0);
    }

    static ViewportTargetSpec relativeScale(int scalePermille) {
        if (scalePermille < MIN_SCALE_PERMILLE || scalePermille > MAX_SCALE_PERMILLE) {
            return off();
        }
        return new ViewportTargetSpec(ViewportTargetType.RELATIVE_SCALE, scalePermille, 0);
    }

    static ViewportTargetSpec absoluteDp(int widthDp) {
        if (widthDp < 1) {
            return off();
        }
        return new ViewportTargetSpec(ViewportTargetType.ABSOLUTE_DP, 0, widthDp);
    }

    String type() {
        return type;
    }

    int scalePermille() {
        return scalePermille;
    }

    int absoluteWidthDp() {
        return absoluteWidthDp;
    }

    boolean isRelativeScale() {
        return ViewportTargetType.RELATIVE_SCALE.equals(type);
    }

    boolean isAbsoluteDp() {
        return ViewportTargetType.ABSOLUTE_DP.equals(type);
    }

    boolean isEnabled() {
        return isRelativeScale() || isAbsoluteDp();
    }

    int activeValue() {
        if (isRelativeScale()) {
            return scalePermille;
        }
        if (isAbsoluteDp()) {
            return absoluteWidthDp;
        }
        return 0;
    }

    String fingerprint() {
        if (isRelativeScale()) {
            return "r" + Integer.toString(scalePermille, 36);
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
        return scalePermille == other.scalePermille
                && absoluteWidthDp == other.absoluteWidthDp
                && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, scalePermille, absoluteWidthDp);
    }

    @Override
    public String toString() {
        if (isRelativeScale()) {
            return "relative_scale:" + scalePermille;
        }
        if (isAbsoluteDp()) {
            return "absolute_dp:" + absoluteWidthDp;
        }
        return "off";
    }
}
