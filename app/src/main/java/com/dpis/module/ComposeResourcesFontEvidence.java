package com.dpis.module;

final class ComposeResourcesFontEvidence {
    private static final float MATCH_TOLERANCE = 0.001f;

    private ComposeResourcesFontEvidence() {
    }

    static boolean isResourcesHandledCompose(FontHookArbitration.FontDomainPlan domainPlan,
                                             float observedFontScale,
                                             float observedDensity,
                                             float observedScaledDensity,
                                             float targetFactor,
                                             boolean composeHeavyCurrentRoot) {
        return domainPlan != null
                && domainPlan.resourcesFontEnabled
                && factorsMatch(observedFontScale, targetFactor)
                && scaledDensityRatioMatches(
                        observedDensity,
                        observedScaledDensity,
                        targetFactor)
                && composeHeavyCurrentRoot;
    }

    private static boolean scaledDensityRatioMatches(float density,
                                                     float scaledDensity,
                                                     float targetFactor) {
        if (density <= 0f || scaledDensity <= 0f) {
            return false;
        }
        return factorsMatch(scaledDensity / density, targetFactor);
    }

    private static boolean factorsMatch(float observed, float targetFactor) {
        return targetFactor > 0f
                && observed > 0f
                && Math.abs(observed - targetFactor) <= MATCH_TOLERANCE;
    }
}
