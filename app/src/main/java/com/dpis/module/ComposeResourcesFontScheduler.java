package com.dpis.module;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class ComposeResourcesFontScheduler {
    static final long SUPPRESSION_TTL_MS = 30_000L;
    private static final float MIN_BASE_FONT_SCALE = 0.5f;
    private static final float MAX_BASE_FONT_SCALE = 2.0f;

    private static final Map<Object, State> RESOURCE_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ComposeResourcesFontScheduler() {
    }

    static void observe(String packageName,
                        ComposeResourcesFontEvidence.Summary evidence,
                        float observedFontScale,
                        float targetFactor,
                        long nowMs) {
        // Package-only observations cannot safely suppress Resources reads.
        // Suppression requires a concrete Resources owner observed from a root.
    }

    static void observe(String packageName,
                        String scopeKey,
                        Object resourceScope,
                        ComposeResourcesFontEvidence.Summary evidence,
                        float observedFontScale,
                        float targetFactor,
                        long nowMs) {
        if (packageName == null || packageName.isBlank()
                || resourceScope == null
                || evidence == null) {
            return;
        }
        if (!evidence.composeHeavyCurrentRoot) {
            RESOURCE_STATES.remove(resourceScope);
            return;
        }
        if (!evidence.resourcesHandled || targetFactor <= 0f || observedFontScale <= 0f) {
            return;
        }
        float baseFontScale = observedFontScale / targetFactor;
        if (baseFontScale < MIN_BASE_FONT_SCALE || baseFontScale > MAX_BASE_FONT_SCALE) {
            return;
        }
        RESOURCE_STATES.put(resourceScope, new State(packageName, baseFontScale, nowMs));
    }

    static FontScaleOverride.Result maybeSuppressResourcesFont(String packageName,
                                                               FontScaleOverride.Result result) {
        return result;
    }

    static FontScaleOverride.Result maybeSuppressResourcesFont(Object resourceScope,
                                                               String packageName,
                                                               FontScaleOverride.Result result) {
        return maybeSuppressResourcesFont(
                resourceScope, packageName, result, System.currentTimeMillis());
    }

    static FontScaleOverride.Result maybeSuppressResourcesFont(String packageName,
                                                               FontScaleOverride.Result result,
                                                               long nowMs) {
        return result;
    }

    static FontScaleOverride.Result maybeSuppressResourcesFont(Object resourceScope,
                                                               String packageName,
                                                               FontScaleOverride.Result result,
                                                               long nowMs) {
        if (packageName == null || packageName.isBlank() || result == null) {
            return result;
        }
        State state = activeState(resourceScope, packageName, nowMs);
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
        return currentFontScale > 0f ? currentFontScale : 1.0f;
    }

    static float maybeSuppressMetricsFontScale(Object resourceScope,
                                               String packageName,
                                               float currentFontScale) {
        return maybeSuppressMetricsFontScale(
                resourceScope, packageName, currentFontScale, System.currentTimeMillis());
    }

    static float maybeSuppressMetricsFontScale(String packageName,
                                               float currentFontScale,
                                               long nowMs) {
        return currentFontScale > 0f ? currentFontScale : 1.0f;
    }

    static float maybeSuppressMetricsFontScale(Object resourceScope,
                                               String packageName,
                                               float currentFontScale,
                                               long nowMs) {
        float original = currentFontScale > 0f ? currentFontScale : 1.0f;
        if (packageName == null || packageName.isBlank()) {
            return original;
        }
        State state = activeState(resourceScope, packageName, nowMs);
        if (state == null) {
            return original;
        }
        return state.baseFontScale;
    }

    static void clearForTest() {
        RESOURCE_STATES.clear();
    }

    private static State activeState(Object resourceScope, String packageName, long nowMs) {
        if (resourceScope == null) {
            return null;
        }
        State state = RESOURCE_STATES.get(resourceScope);
        if (state == null) {
            return null;
        }
        if (!state.packageName.equals(packageName)) {
            return null;
        }
        if (nowMs - state.updatedAtMs > SUPPRESSION_TTL_MS) {
            RESOURCE_STATES.remove(resourceScope);
            return null;
        }
        return state;
    }

    private static final class State {
        final String packageName;
        final float baseFontScale;
        final long updatedAtMs;

        State(String packageName, float baseFontScale, long updatedAtMs) {
            this.packageName = packageName;
            this.baseFontScale = baseFontScale;
            this.updatedAtMs = updatedAtMs;
        }
    }
}
