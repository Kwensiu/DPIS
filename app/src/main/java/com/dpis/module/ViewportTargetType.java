package com.dpis.module;

public final class ViewportTargetType {
    public static final String OFF = "off";
    public static final String RELATIVE_SCALE = "relative_scale";
    public static final String ABSOLUTE_DP = "absolute_dp";

    private ViewportTargetType() {
    }

    public static String normalize(String type) {
        if (RELATIVE_SCALE.equals(type)) {
            return RELATIVE_SCALE;
        }
        if (ABSOLUTE_DP.equals(type)) {
            return ABSOLUTE_DP;
        }
        return OFF;
    }
}
