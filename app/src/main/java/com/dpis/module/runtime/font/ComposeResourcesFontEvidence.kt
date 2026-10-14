package com.dpis.module.runtime.font

import com.dpis.module.fonts.hookdomain.FontHookArbitration

object ComposeResourcesFontEvidence {
    private const val MATCH_TOLERANCE = 0.001f

    class Summary(
        @JvmField val resourcesFontDomainEnabled: Boolean,
        @JvmField val fontScaleMatches: Boolean,
        @JvmField val scaledDensityRatioMatches: Boolean,
        @JvmField val composeHeavyCurrentRoot: Boolean,
        @JvmField val resourcesHandled: Boolean,
        @JvmField val scaledDensityRatio: Float,
    )

    @JvmStatic
    fun isResourcesHandledCompose(
        domainPlan: FontHookArbitration.FontDomainPlan?,
        observedFontScale: Float,
        observedDensity: Float,
        observedScaledDensity: Float,
        targetFactor: Float,
        composeHeavyCurrentRoot: Boolean,
    ): Boolean = summarize(
        domainPlan,
        observedFontScale,
        observedDensity,
        observedScaledDensity,
        targetFactor,
        composeHeavyCurrentRoot,
    ).resourcesHandled

    @JvmStatic
    fun summarize(
        domainPlan: FontHookArbitration.FontDomainPlan?,
        observedFontScale: Float,
        observedDensity: Float,
        observedScaledDensity: Float,
        targetFactor: Float,
        composeHeavyCurrentRoot: Boolean,
    ): Summary {
        val resourcesFontDomainEnabled = domainPlan?.resourcesFontEnabled == true
        val fontScaleMatches = factorsMatch(observedFontScale, targetFactor)
        val ratio = scaledDensityRatio(observedDensity, observedScaledDensity)
        val ratioMatches = factorsMatch(ratio, targetFactor)
        return Summary(
            resourcesFontDomainEnabled = resourcesFontDomainEnabled,
            fontScaleMatches = fontScaleMatches,
            scaledDensityRatioMatches = ratioMatches,
            composeHeavyCurrentRoot = composeHeavyCurrentRoot,
            resourcesHandled = resourcesFontDomainEnabled &&
                    fontScaleMatches && ratioMatches && composeHeavyCurrentRoot,
            scaledDensityRatio = ratio,
        )
    }

    private fun scaledDensityRatio(density: Float, scaledDensity: Float): Float =
        if (density <= 0f || scaledDensity <= 0f) 0f else scaledDensity / density

    private fun factorsMatch(observed: Float, targetFactor: Float): Boolean =
        targetFactor > 0f && observed > 0f &&
                kotlin.math.abs(observed - targetFactor) <= MATCH_TOLERANCE
}
