package com.dpis.module;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ComposeResourcesFontScheduler {
    static final long SUPPRESSION_TTL_MS = 30_000L;
    private static final float MIN_BASE_FONT_SCALE = 0.5f;
    private static final float MAX_BASE_FONT_SCALE = 2.0f;

    private static final Map<String, State> STATES = new ConcurrentHashMap<>();

    private ComposeResourcesFontScheduler() {
    }

    static void observe(String packageName,
                        ComposeResourcesFontEvidence.Summary evidence,
                        float observedFontScale,
                        float targetFactor,
                        long nowMs) {
        if (packageName == null || packageName.isBlank() || evidence == null) {
            return;
        }
        if (!evidence.composeHeavyCurrentRoot) {
            STATES.remove(packageName);
            return;
        }
        if (!evidence.resourcesHandled || targetFactor <= 0f || observedFontScale <= 0f) {
            return;
        }
        float baseFontScale = observedFontScale / targetFactor;
        if (baseFontScale < MIN_BASE_FONT_SCALE || baseFontScale > MAX_BASE_FONT_SCALE) {
            return;
        }
        STATES.put(packageName, new State(baseFontScale, nowMs));
    }

    static FontScaleOverride.Result maybeSuppressResourcesFont(String packageName,
                                                               FontScaleOverride.Result result) {
        return maybeSuppressResourcesFont(packageName, result, System.currentTimeMillis());
    }

    static FontScaleOverride.Result maybeSuppressResourcesFont(String packageName,
                                                               FontScaleOverride.Result result,
                                                               long nowMs) {
        if (packageName == null || packageName.isBlank() || result == null) {
            return result;
        }
        State state = activeState(packageName, nowMs);
        if (state == null) {
            return result;
        }
        return new FontScaleOverride.Result(
                result.original,
                state.baseFontScale,
                result.targetPercent,
                Math.abs(state.baseFontScale - result.original) > FontScaleOverride.EPSILON);
    }

    static float maybeSuppressMetricsFontScale(String packageName, float currentFontScale) {
        return maybeSuppressMetricsFontScale(packageName, currentFontScale, System.currentTimeMillis());
    }

    static float maybeSuppressMetricsFontScale(String packageName,
                                               float currentFontScale,
                                               long nowMs) {
        float original = currentFontScale > 0f ? currentFontScale : 1.0f;
        if (packageName == null || packageName.isBlank()) {
            return original;
        }
        State state = activeState(packageName, nowMs);
        if (state == null) {
            return original;
        }
        return state.baseFontScale;
    }

    static void clearForTest() {
        STATES.clear();
    }

    private static State activeState(String packageName, long nowMs) {
        State state = STATES.get(packageName);
        if (state == null) {
            return null;
        }
        if (nowMs - state.updatedAtMs > SUPPRESSION_TTL_MS) {
            STATES.remove(packageName, state);
            return null;
        }
        return state;
    }

    private static final class State {
        final float baseFontScale;
        final long updatedAtMs;

        State(float baseFontScale, long updatedAtMs) {
            this.baseFontScale = baseFontScale;
            this.updatedAtMs = updatedAtMs;
        }
    }
}
