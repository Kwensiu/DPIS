package com.dpis.module.runtime.font;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ResourcesFontScheduler {
    private static final float MATCH_TOLERANCE = 0.001f;
    private static final float MIN_BASE_FONT_SCALE = 0.5f;
    private static final float MAX_BASE_FONT_SCALE = 2.0f;

    private static final Map<Object, State> RESOURCE_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());
    // Keyed by packageName|round(targetFactor*1000). Suppression is cleared by
    // event transitions (base<->target observations), not by a time-based TTL:
    // inside an app process the package is fixed and the target factor rarely
    // changes, so this map stays small and a stale entry self-corrects on the
    // next conflicting observation rather than expiring on a timer.
    private static final Map<String, State> PACKAGE_STATES =
            Collections.synchronizedMap(new HashMap<>());

    private ResourcesFontScheduler() {
    }

    public static void observe(String packageName,
                        ComposeResourcesFontEvidence.Summary evidence,
                        float observedFontScale,
                        float targetFactor,
                        long nowMs) {
        // Package-only observations cannot safely suppress Resources reads.
        // Suppression requires a concrete Resources owner observed from a root.
    }

    public static void observe(String packageName,
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
            synchronized (RESOURCE_STATES) {
                State current = RESOURCE_STATES.get(resourceScope);
                if (!isTargetSuppression(current, packageName, targetFactor)) {
                    RESOURCE_STATES.remove(resourceScope);
                }
                if (targetFactor > 0f) {
                    String packageKey = packageStateKey(packageName, targetFactor);
                    State packageState = PACKAGE_STATES.get(packageKey);
                    if (!isTargetSuppression(packageState, packageName, targetFactor)) {
                        PACKAGE_STATES.remove(packageKey);
                    }
                }
            }
            return;
        }
        if (!evidence.resourcesHandled || targetFactor <= 0f || observedFontScale <= 0f) {
            return;
        }
        float baseFontScale = observedFontScale / targetFactor;
        if (baseFontScale < MIN_BASE_FONT_SCALE || baseFontScale > MAX_BASE_FONT_SCALE) {
            return;
        }
        State state = State.suppressedToBase(
                packageName, baseFontScale, targetFactor, observedFontScale);
        // Keep the check-then-put atomic under one monitor so a concurrent
        // read-conflict target suppression (observeResourcesFontScale) cannot be
        // clobbered by this Compose base suppression between the check and the put.
        synchronized (RESOURCE_STATES) {
            State current = RESOURCE_STATES.get(resourceScope);
            if (isTargetSuppression(current, packageName, targetFactor)) {
                return;
            }
            State packageState = PACKAGE_STATES.get(packageStateKey(packageName, targetFactor));
            if (isTargetSuppression(packageState, packageName, targetFactor)) {
                RESOURCE_STATES.put(resourceScope, packageState);
                return;
            }
            RESOURCE_STATES.put(resourceScope, state);
            PACKAGE_STATES.put(packageStateKey(packageName, targetFactor), state);
        }
    }

    public static void observeResourcesFontScale(Object resourceScope,
                                          String packageName,
                                          float observedFontScale,
                                          float targetFactor) {
        if (packageName == null || packageName.isBlank()
                || resourceScope == null
                || observedFontScale <= 0f
                || targetFactor <= 0f) {
            return;
        }
        synchronized (RESOURCE_STATES) {
            State state = RESOURCE_STATES.get(resourceScope);
            if (state == null
                    || !state.packageName.equals(packageName)
                    || !factorsMatch(state.targetFactor, targetFactor)) {
                RESOURCE_STATES.put(resourceScope, State.observed(
                        packageName, targetFactor, observedFontScale));
                return;
            }
            Float conflictBase = resolveConflictBaseFontScale(
                    state.lastObservedFontScale, observedFontScale, targetFactor);
            if (conflictBase != null && isValidBaseFontScale(conflictBase)) {
                State suppressed = State.suppressedToTarget(
                        packageName, conflictBase, targetFactor, observedFontScale);
                RESOURCE_STATES.put(resourceScope, suppressed);
                PACKAGE_STATES.put(packageStateKey(packageName, targetFactor), suppressed);
                return;
            }
            RESOURCE_STATES.put(resourceScope, state.withLastObserved(observedFontScale));
        }
    }

    public static FontScaleOverride.Result maybeSuppressResourcesFont(String packageName,
                                                               FontScaleOverride.Result result) {
        return result;
    }

    public static FontScaleOverride.Result maybeSuppressResourcesFont(Object resourceScope,
                                                               String packageName,
                                                               FontScaleOverride.Result result) {
        return maybeSuppressResourcesFont(
                resourceScope, packageName, result, System.currentTimeMillis());
    }

    public static FontScaleOverride.Result maybeSuppressResourcesFont(String packageName,
                                                               FontScaleOverride.Result result,
                                                               long nowMs) {
        return result;
    }

    public static FontScaleOverride.Result maybeSuppressResourcesFont(Object resourceScope,
                                                               String packageName,
                                                               FontScaleOverride.Result result,
                                                               long nowMs) {
        if (packageName == null || packageName.isBlank() || result == null) {
            return result;
        }
        float targetFactor = targetFactor(result);
        State state = activeState(resourceScope, packageName, targetFactor);
        if (state == null) {
            state = activePackageState(packageName, targetFactor);
        }
        if (state == null) {
            return result;
        }
        return new FontScaleOverride.Result(
                result.original,
                state.effectiveFontScale,
                result.targetPercent,
                Math.abs(state.effectiveFontScale - result.original) > FontScaleOverride.EPSILON);
    }

    public static float maybeSuppressMetricsFontScale(String packageName, float currentFontScale) {
        return currentFontScale > 0f ? currentFontScale : 1.0f;
    }

    public static float maybeSuppressMetricsFontScale(Object resourceScope,
                                               String packageName,
                                               float currentFontScale) {
        return maybeSuppressMetricsFontScale(
                resourceScope, packageName, currentFontScale, System.currentTimeMillis());
    }

    public static float maybeSuppressMetricsFontScale(String packageName,
                                               float currentFontScale,
                                               long nowMs) {
        return currentFontScale > 0f ? currentFontScale : 1.0f;
    }

    public static float maybeSuppressMetricsFontScale(Object resourceScope,
                                               String packageName,
                                               float currentFontScale,
                                               long nowMs) {
        // Legacy compatibility overload: callers that still pass a timestamp.
        // The scheduler no longer expires by time, so nowMs is intentionally
        // ignored. Delegate to the float-targetFactor overload with factor 0f,
        // which degrades to a resource-scope lookup that ignores the factor.
        // New callers should use the (..., float targetFactor) overload instead.
        return maybeSuppressMetricsFontScale(resourceScope, packageName, currentFontScale, 0f);
    }

    public static float maybeSuppressMetricsFontScale(Object resourceScope,
                                               String packageName,
                                               float currentFontScale,
                                               float targetFactor) {
        float original = currentFontScale > 0f ? currentFontScale : 1.0f;
        if (packageName == null || packageName.isBlank()) {
            return original;
        }
        State state = activeState(resourceScope, packageName, targetFactor);
        if (state == null) {
            state = activePackageState(packageName, targetFactor);
        }
        if (state == null) {
            return original;
        }
        return state.effectiveFontScale;
    }

    public static void clearForTest() {
        RESOURCE_STATES.clear();
        PACKAGE_STATES.clear();
    }

    static boolean isPackageSuppressed(String packageName, float targetFactor) {
        return activePackageState(packageName, targetFactor) != null;
    }

    static boolean isPackageTargetSuppressed(String packageName, float targetFactor) {
        State state = activePackageState(packageName, targetFactor);
        return isTargetSuppression(state, packageName, targetFactor);
    }

    private static State activeState(Object resourceScope, String packageName, float targetFactor) {
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
        if (!state.suppressed) {
            return null;
        }
        if (targetFactor > 0f && !factorsMatch(state.targetFactor, targetFactor)) {
            return null;
        }
        return state;
    }

    private static State activePackageState(String packageName, float targetFactor) {
        if (packageName == null || packageName.isBlank() || targetFactor <= 0f) {
            return null;
        }
        State state = PACKAGE_STATES.get(packageStateKey(packageName, targetFactor));
        if (state == null || !state.suppressed) {
            return null;
        }
        if (!state.packageName.equals(packageName)) {
            return null;
        }
        if (!factorsMatch(state.targetFactor, targetFactor)) {
            return null;
        }
        return state;
    }

    private static Float resolveConflictBaseFontScale(float previous,
                                                     float current,
                                                     float targetFactor) {
        if (previous <= 0f || current <= 0f || targetFactor <= 0f
                || factorsMatch(previous, current)) {
            return null;
        }
        if (factorsMatch(previous * targetFactor, current)) {
            return previous;
        }
        if (factorsMatch(current * targetFactor, previous)) {
            return current;
        }
        return null;
    }

    private static boolean isValidBaseFontScale(float baseFontScale) {
        return baseFontScale >= MIN_BASE_FONT_SCALE && baseFontScale <= MAX_BASE_FONT_SCALE;
    }

    private static float targetFactor(FontScaleOverride.Result result) {
        if (result == null || result.targetPercent == null || result.targetPercent <= 0) {
            return 0f;
        }
        return result.targetPercent / 100.0f;
    }

    private static boolean factorsMatch(float first, float second) {
        return first > 0f
                && second > 0f
                && Math.abs(first - second) <= MATCH_TOLERANCE;
    }

    private static boolean isTargetSuppression(State state,
                                               String packageName,
                                               float targetFactor) {
        return state != null
                && state.suppressed
                && state.packageName.equals(packageName)
                && factorsMatch(state.targetFactor, targetFactor)
                && !factorsMatch(state.effectiveFontScale, state.baseFontScale);
    }

    private static String packageStateKey(String packageName, float targetFactor) {
        return packageName + "|" + Math.round(targetFactor * 1000f);
    }

    private static final class State {
        final String packageName;
        final float baseFontScale;
        final float effectiveFontScale;
        final float targetFactor;
        final float lastObservedFontScale;
        final boolean suppressed;

        private State(String packageName,
                      float baseFontScale,
                      float effectiveFontScale,
                      float targetFactor,
                      float lastObservedFontScale,
                      boolean suppressed) {
            this.packageName = packageName;
            this.baseFontScale = baseFontScale;
            this.effectiveFontScale = effectiveFontScale;
            this.targetFactor = targetFactor;
            this.lastObservedFontScale = lastObservedFontScale;
            this.suppressed = suppressed;
        }

        static State observed(String packageName, float targetFactor, float observedFontScale) {
            return new State(packageName, observedFontScale, observedFontScale,
                    targetFactor, observedFontScale, false);
        }

        static State suppressedToBase(String packageName,
                                      float baseFontScale,
                                      float targetFactor,
                                      float observedFontScale) {
            return new State(packageName, baseFontScale, baseFontScale,
                    targetFactor, observedFontScale, true);
        }

        static State suppressedToTarget(String packageName,
                                        float baseFontScale,
                                        float targetFactor,
                                        float observedFontScale) {
            return new State(packageName, baseFontScale, baseFontScale * targetFactor,
                    targetFactor, observedFontScale, true);
        }

        State withLastObserved(float observedFontScale) {
            return new State(packageName, baseFontScale, effectiveFontScale,
                    targetFactor, observedFontScale, suppressed);
        }
    }
}
