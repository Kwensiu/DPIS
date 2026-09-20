package com.dpis.module.runtime.font

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontMutationScheduler
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import io.github.libxposed.api.XposedInterface

/** Public compatibility facade for the font hook installation boundary. */
object ForceTextSizeHookInstaller {
    enum class PaintFallbackAction { WRITE, SKIP, KEEP, OBSERVE }

    class PaintFallbackDecision(
        @JvmField val action: PaintFallbackAction,
        @JvmField val adjustedPx: Float
    )
    @JvmStatic
    fun resetForHotReload() = ForceTextSizeHookRuntime.resetForHotReload()

    @JvmStatic
    fun install(xposed: XposedInterface, packageName: String, store: DpisConfigStore?, domainPlan: FontDomainPlan?) =
        ForceTextSizeHookRuntime.install(xposed, packageName, store, domainPlan)

    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String,
        store: DpisConfigStore?,
        domainPlan: FontDomainPlan? = null,
        apiCapabilities: ModernApiCapabilities = ModernApiCapabilitiesResolver.fromXposed(xposed)
    ) = ForceTextSizeHookRuntime.install(xposed, packageName, store, domainPlan, apiCapabilities)

    @JvmStatic
    fun shouldForceTextUnitForTest(unit: Int, domainPlan: FontDomainPlan?) =
        ForceTextSizeHookRuntime.shouldForceTextUnitForTest(unit, domainPlan)

    @JvmStatic
    fun resolvePaintFallbackDecisionForTest(
        paint: Any?, incomingPx: Float, currentPx: Float, factor: Float, strongerDomainOwns: Boolean
    ): PaintFallbackDecision {
        val result = ForceTextSizeHookRuntime.resolvePaintFallbackDecisionForTest(
            paint, incomingPx, currentPx, factor, strongerDomainOwns
        )
        val action = when (result.action()) {
            FontMutationScheduler.Action.APPLY -> PaintFallbackAction.WRITE
            // Keep the historical facade contract for callers/tests; the live
            // route uses FontMutationScheduler.Action.KEEP_CURRENT directly.
            FontMutationScheduler.Action.KEEP_CURRENT -> PaintFallbackAction.SKIP
            FontMutationScheduler.Action.PASS_THROUGH -> PaintFallbackAction.SKIP
            FontMutationScheduler.Action.OBSERVE -> PaintFallbackAction.OBSERVE
        }
        val adjustedPx = if (result.action() == FontMutationScheduler.Action.OBSERVE
            || result.action() == FontMutationScheduler.Action.KEEP_CURRENT
            || result.action() == FontMutationScheduler.Action.PASS_THROUGH
        ) {
            incomingPx
        } else {
            result.targetPx()
        }
        return PaintFallbackDecision(action, adjustedPx)
    }
}
