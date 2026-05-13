package com.dpis.displaytool;

final class SceneAnomaly {
    static final SceneAnomaly NONE = new SceneAnomaly(false, "");

    final boolean suspicious;
    final String reason;

    private SceneAnomaly(boolean suspicious, String reason) {
        this.suspicious = suspicious;
        this.reason = reason;
    }

    static SceneAnomaly classify(
            float actualTextPx,
            float expectedTextPx,
            float fontScale
    ) {
        if (expectedTextPx <= 0f || actualTextPx <= 0f) {
            return NONE;
        }

        float renderedScale = actualTextPx / expectedTextPx;
        if (renderedScale >= 0.75f && renderedScale <= 1.35f) {
            return NONE;
        }

        if (renderedScale < 0.75f) {
            return new SceneAnomaly(true, "no_scale");
        }

        if (fontScale > 1.05f && near(renderedScale, fontScale)) {
            return new SceneAnomaly(true, "double_scale");
        }

        return new SceneAnomaly(true, "inconsistent_readings");
    }

    private static boolean near(float actual, float expected) {
        float tolerance = Math.max(0.15f, expected * 0.2f);
        return Math.abs(actual - expected) <= tolerance;
    }
}
