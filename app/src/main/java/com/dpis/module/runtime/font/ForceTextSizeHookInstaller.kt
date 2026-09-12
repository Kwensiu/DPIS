package com.dpis.module.runtime.font

import com.dpis.module.config.DpisConfigStore
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
        return PaintFallbackDecision(
            PaintFallbackAction.valueOf(result.action.name), result.adjustedPx
        )
    }
}
