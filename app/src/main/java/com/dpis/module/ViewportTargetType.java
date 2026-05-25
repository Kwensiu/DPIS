package com.dpis.module;

final class ViewportTargetType {
    static final String OFF = "off";
    static final String RELATIVE_SCALE = "relative_scale";
    static final String ABSOLUTE_DP = "absolute_dp";

    private ViewportTargetType() {
    }

    static String normalize(String type) {
        if (RELATIVE_SCALE.equals(type)) {
            return RELATIVE_SCALE;
        }
        if (ABSOLUTE_DP.equals(type)) {
            return ABSOLUTE_DP;
        }
        return OFF;
    }
}
