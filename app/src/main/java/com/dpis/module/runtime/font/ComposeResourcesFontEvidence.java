package com.dpis.module.runtime.font;

import com.dpis.module.fonts.hookdomain.FontHookArbitration;

public final class ComposeResourcesFontEvidence {
    private static final float MATCH_TOLERANCE = 0.001f;

    public static final class Summary {
        public final boolean resourcesFontDomainEnabled;
        public final boolean fontScaleMatches;
        public final boolean scaledDensityRatioMatches;
        public final boolean composeHeavyCurrentRoot;
        public final boolean resourcesHandled;
        public final float scaledDensityRatio;

        Summary(boolean resourcesFontDomainEnabled,
                boolean fontScaleMatches,
                boolean scaledDensityRatioMatches,
                boolean composeHeavyCurrentRoot,
                boolean resourcesHandled,
                float scaledDensityRatio) {
            this.resourcesFontDomainEnabled = resourcesFontDomainEnabled;
            this.fontScaleMatches = fontScaleMatches;
            this.scaledDensityRatioMatches = scaledDensityRatioMatches;
            this.composeHeavyCurrentRoot = composeHeavyCurrentRoot;
            this.resourcesHandled = resourcesHandled;
            this.scaledDensityRatio = scaledDensityRatio;
        }
    }

    private ComposeResourcesFontEvidence() {
    }

    public static boolean isResourcesHandledCompose(FontHookArbitration.FontDomainPlan domainPlan,
                                             float observedFontScale,
                                             float observedDensity,
                                             float observedScaledDensity,
                                             float targetFactor,
                                             boolean composeHeavyCurrentRoot) {
        return summarize(
                domainPlan,
                observedFontScale,
                observedDensity,
                observedScaledDensity,
                targetFactor,
                composeHeavyCurrentRoot).resourcesHandled;
    }

    public static Summary summarize(FontHookArbitration.FontDomainPlan domainPlan,
                             float observedFontScale,
                             float observedDensity,
                             float observedScaledDensity,
                             float targetFactor,
                             boolean composeHeavyCurrentRoot) {
        boolean resourcesFontDomainEnabled = domainPlan != null
                && domainPlan.resourcesFontEnabled;
        boolean fontScaleMatches = factorsMatch(observedFontScale, targetFactor);
        float ratio = scaledDensityRatio(observedDensity, observedScaledDensity);
        boolean ratioMatches = factorsMatch(ratio, targetFactor);
        boolean resourcesHandled = resourcesFontDomainEnabled
                && fontScaleMatches
                && ratioMatches
                && composeHeavyCurrentRoot;
        return new Summary(
                resourcesFontDomainEnabled,
                fontScaleMatches,
                ratioMatches,
                composeHeavyCurrentRoot,
                resourcesHandled,
                ratio);
    }

    private static float scaledDensityRatio(float density, float scaledDensity) {
        if (density <= 0f || scaledDensity <= 0f) {
            return 0f;
        }
        return scaledDensity / density;
    }

    private static boolean factorsMatch(float observed, float targetFactor) {
        return targetFactor > 0f
                && observed > 0f
                && Math.abs(observed - targetFactor) <= MATCH_TOLERANCE;
    }
}
