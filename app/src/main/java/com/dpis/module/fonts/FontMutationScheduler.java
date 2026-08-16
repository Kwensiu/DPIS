package com.dpis.module.fonts;

/**
 * Synchronous arbitration for font mutations shared by TextView and Paint routes.
 * The caller still owns the framework setter; this class only decides whether
 * an already-target-sized object should keep its current value.
 */
public final class FontMutationScheduler {
    private FontMutationScheduler() {
    }

    public static Decision decide(float incomingPx,
                                  float currentPx,
                                  float targetPx,
                                  float factor,
                                  boolean strongerDomainOwns) {
        if (strongerDomainOwns || !isScaleFactorActive(factor)
                || incomingPx <= 0f || currentPx <= 0f || targetPx <= 0f) {
            return Decision.observe();
        }
        if (FontFieldRewriteMath.approximatelyEqual(currentPx, targetPx)) {
            return Decision.keepCurrent();
        }
        if (FontFieldRewriteMath.approximatelyEqual(incomingPx, targetPx)) {
            return Decision.observe();
        }
        return Decision.apply(targetPx);
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }

    public enum Action {
        KEEP_CURRENT,
        APPLY,
        OBSERVE
    }

    public static final class Decision {
        private final Action action;
        private final float targetPx;

        private Decision(Action action, float targetPx) {
            this.action = action;
            this.targetPx = targetPx;
        }

        private static Decision keepCurrent() {
            return new Decision(Action.KEEP_CURRENT, 0f);
        }

        private static Decision apply(float targetPx) {
            return new Decision(Action.APPLY, targetPx);
        }

        private static Decision observe() {
            return new Decision(Action.OBSERVE, 0f);
        }

        public Action action() {
            return action;
        }

        public float targetPx() {
            return targetPx;
        }
    }
}
