package com.dpis.module.runtime.font

import android.util.TypedValue
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan

/** Pure routing decisions shared by the TextView and Paint hook installers. */
internal object TextSizePolicy {
    fun shouldForceTextUnit(unit: Int, domainPlan: FontDomainPlan?): Boolean {
        if (unit == TypedValue.COMPLEX_UNIT_SP) {
            return domainPlan == null || domainPlan.textViewSpRewriteEnabled
        }
        return shouldRewriteAbsoluteTextSize(domainPlan) && unit in setOf(
            TypedValue.COMPLEX_UNIT_PX,
            TypedValue.COMPLEX_UNIT_DIP,
            TypedValue.COMPLEX_UNIT_PT,
            TypedValue.COMPLEX_UNIT_IN,
            TypedValue.COMPLEX_UNIT_MM
        )
    }

    fun shouldRewriteAbsoluteTextSize(domainPlan: FontDomainPlan?): Boolean =
        domainPlan == null || domainPlan.textViewAbsoluteRewriteEnabled

    fun shouldInstallCurrentPxTextViewFallbacks(domainPlan: FontDomainPlan?): Boolean =
        domainPlan == null || domainPlan.textViewCurrentPxFallbackEnabled

    fun isTargetPercentActive(targetPercent: Int?): Boolean =
        targetPercent != null && targetPercent > 0 && targetPercent != 100

    fun isScaleFactorActive(factor: Float): Boolean = factor > 0f && factor != 1.0f
}
