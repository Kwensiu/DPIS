package com.dpis.module.runtime.font

import android.util.TypedValue
import android.widget.TextView
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.diagnostics.device.RuntimeTransport.isCaptureActive
import com.dpis.module.fonts.FontDebugStatsReporter
import com.dpis.module.fonts.TextViewFontProvenanceTracker
import com.dpis.module.fonts.TextViewFontProvenanceTracker.UnitKind
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan
import com.dpis.module.runtime.font.FontScaleOverride.toPx
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import kotlin.math.max

/** Installs both TextView text-size overloads while preserving their shared depth guard. */
internal object TextViewTextSizeHookInstaller {
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT = "textview_set_text_size_with_unit"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP = "textview_set_text_size_default_sp"
    private const val HOT_LOG_INTERVAL = 32

    fun install(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
val setTextSizeMethod = textViewClass.getDeclaredMethod(
                "setTextSize",
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(setTextSizeMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker chain!!.proceed()
                    }
                    val unit = chain!!.getArg(0) as Int
                    val size = chain.getArg(1) as Float
                    if (size <= 0f) {
                        return@Hooker chain.proceed()
                    }
                    val thisObject = chain.getThisObject()
                    if (thisObject !is TextView) {
                        return@Hooker chain.proceed()
                    }
                    val originalPx = toPx(
                        unit, size, thisObject.getResources().getDisplayMetrics()
                    )
                    if (ForceTextSizeHookRuntime.shouldKeepCurrentTextViewTarget(thisObject, originalPx, factor)) {
                        RuntimeHotPathEvents.kept(
                            packageName,
                            ForceTextSizeHookRuntime.routeNameForTextViewSetTextSize(unit),
                            ("reason=current_target, view=" + thisObject.javaClass.getName()
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        return@Hooker null
                    }
                    val result =
                        ForceTextSizeHookRuntime.proceedInsideTextViewSetTextSize(chain)
                    if (true == ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    if (originalPx <= 0f) {
                        return@Hooker result
                    }
                    if (ForceTextSizeHookRuntime.isKnownAppliedTextSize(thisObject, originalPx, factor)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            ForceTextSizeHookRuntime.routeNameForTextViewSetTextSize(unit),
                            ("reason=known_applied, view=" + thisObject.javaClass.getName()
                                    + ", px=" + originalPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        ForceTextSizeHookRuntime.markAppliedTargetSize(thisObject, originalPx, factor)
                        return@Hooker result
                    }
                    if (unit == TypedValue.COMPLEX_UNIT_SP
                        && ForceTextSizeHookRuntime.isSpTextHandledByResources(thisObject, factor, domainPlan)
                    ) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "textview_sp_rewrite",
                            ("reason=resources_handled, view=" + thisObject.javaClass.getName()
                                    + ", px=" + originalPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        ForceTextSizeHookRuntime.recordResourcesHandledTextSize(thisObject, originalPx, factor)
                        return@Hooker result
                    }
                    val shouldForceUnit = TextSizePolicy.shouldForceTextUnit(unit, domainPlan)
                    if (!shouldForceUnit) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            ForceTextSizeHookRuntime.routeNameForTextViewSetTextSize(unit),
                            ("reason=domain_disabled, unit=" + unit
                                    + ", view=" + thisObject.javaClass.getName()
                                    + ", px=" + originalPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        ForceTextSizeHookRuntime.recordTextViewBase(thisObject, originalPx, factor)
                        return@Hooker result
                    }
                    val forcedPx = originalPx * factor
                    if (!ForceTextSizeHookRuntime.shouldApplyTargetSize(thisObject, forcedPx)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            ForceTextSizeHookRuntime.routeNameForTextViewSetTextSize(unit),
                            ("reason=no_change, unit=" + unit
                                    + ", view=" + thisObject.javaClass.getName()
                                    + ", in=" + originalPx
                                    + ", out=" + forcedPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        return@Hooker result
                    }
                    val routeName = ForceTextSizeHookRuntime.routeNameForTextViewSetTextSize(unit)
                    val detail = ("unit=" + unit
                            + ", view=" + thisObject.javaClass.getName()
                            + ", in=" + originalPx
                            + ", out=" + forcedPx
                            + ", factor=" + factor
                            + ", percent=" + targetPercent)
                    RuntimeHotPathEvents.begin(packageName, routeName, detail)
                    ForceTextSizeHookRuntime.INTERNAL_UPDATE.set(true)
                    try {
                        val diagnosticCaptureActive =
                            isCaptureActive
                        val frameworkStartedAt = if (diagnosticCaptureActive)
                            System.nanoTime()
                        else
                            0L
                        thisObject.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx)
                        val frameworkDurationNs = if (diagnosticCaptureActive) max(
                            0L,
                            System.nanoTime() - frameworkStartedAt
                        ) else
                            0L
                        val bookkeepingStartedAt = if (diagnosticCaptureActive)
                            System.nanoTime()
                        else
                            0L
                        ForceTextSizeHookRuntime.recordTextViewBase(thisObject, originalPx, factor)
                        ForceTextSizeHookRuntime.markAppliedTargetSize(thisObject, forcedPx, factor)
                        ForceTextSizeHookRuntime.recordTextViewRewrite(thisObject, originalPx, forcedPx, factor, unit)
                        val bookkeepingDurationNs = if (diagnosticCaptureActive) max(
                            0L,
                            System.nanoTime() - bookkeepingStartedAt
                        ) else
                            0L
                        RuntimeHotPathEvents.applied(packageName, routeName, detail)
                        ForceTextSizeHookRuntime.recordSlowTextMutationEvidence(
                            packageName,
                            routeName,
                            detail,
                            frameworkDurationNs,
                            bookkeepingDurationNs,
                            diagnosticCaptureActive
                        )
                        ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                            xposed,
                            packageName,
                            HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT,
                            "textview setTextSize(unit) override applied"
                        )
                    } finally {
                        ForceTextSizeHookRuntime.INTERNAL_UPDATE.remove()
                        RuntimeHotPathEvents.end(packageName, routeName, detail)
                    }
                    if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        ForceTextSizeHookRuntime.logSampled(
                            ForceTextSizeHookRuntime.buildHotFontLogKey(packageName, "text-size-unit-" + unit),
                            ("DPIS_FONT ForceTextSize override: unit=" + unit
                                    + ", size=" + size
                                    + ", px=" + originalPx + " -> " + forcedPx
                                    + ", view=" + thisObject.javaClass.getName()
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent),
                            HOT_LOG_INTERVAL
                        )
                        ForceTextSizeHookRuntime.logCallerSample(packageName, "text-size-unit")
                    }
                    FontDebugStatsReporter.recordUnit(
                        unit,
                        thisObject.javaClass.getName(),
                        thisObject.getContext()
                    )
                    result
                })
            val setTextSizeFloatMethod =
                textViewClass.getDeclaredMethod("setTextSize", Float::class.javaPrimitiveType)
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(setTextSizeFloatMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    // Skip when this one-arg setTextSize(float) call is a nested
                    // forwarding call from the two-arg setTextSize(int, float)
                    // overload (observed on OEM ROMs where the unit overload
                    // delegates down to the float one). The depth ThreadLocal is
                    // maintained by the WITH_UNIT hook around its chain.proceed(),
                    // so depth > 0 here is equivalent to seeing >= 2 TextView#setTextSize
                    // frames on the stack, without the per-call getStackTrace() cost.
                    if (ForceTextSizeHookRuntime.isInsideTextViewSetTextSize) {
                        return@Hooker chain!!.proceed()
                    }
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker chain!!.proceed()
                    }
                    val thisObject = chain!!.getThisObject()
                    if (thisObject !is TextView) {
                        return@Hooker chain.proceed()
                    }
                    val sizeSp = chain.getArg(0) as Float
                    if (sizeSp <= 0f) {
                        return@Hooker chain.proceed()
                    }
                    val originalPx = toPx(
                        TypedValue.COMPLEX_UNIT_SP,
                        sizeSp,
                        thisObject.getResources().getDisplayMetrics()
                    )
                    if (ForceTextSizeHookRuntime.shouldKeepCurrentTextViewTarget(thisObject, originalPx, factor)) {
                        RuntimeHotPathEvents.kept(
                            packageName,
                            "textview_sp_rewrite",
                            ("reason=current_target, view=" + thisObject.javaClass.getName()
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        return@Hooker null
                    }
                    val result =
                        ForceTextSizeHookRuntime.proceedInsideTextViewSetTextSize(chain)
                    if (true == ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    if (originalPx <= 0f) {
                        return@Hooker result
                    }
                    if (ForceTextSizeHookRuntime.isKnownAppliedTextSize(thisObject, originalPx, factor)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "textview_sp_rewrite",
                            ("reason=known_applied, view=" + thisObject.javaClass.getName()
                                    + ", px=" + originalPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        ForceTextSizeHookRuntime.markAppliedTargetSize(thisObject, originalPx, factor)
                        return@Hooker result
                    }
                    if (ForceTextSizeHookRuntime.isSpTextHandledByResources(thisObject, factor, domainPlan)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "textview_sp_rewrite",
                            ("reason=resources_handled, view=" + thisObject.javaClass.getName()
                                    + ", px=" + originalPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        ForceTextSizeHookRuntime.recordResourcesHandledTextSize(thisObject, originalPx, factor)
                        return@Hooker result
                    }
                    if (!(domainPlan == null || domainPlan.textViewSpRewriteEnabled)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "textview_sp_rewrite",
                            ("reason=domain_disabled, view=" + thisObject.javaClass.getName()
                                    + ", px=" + originalPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        ForceTextSizeHookRuntime.recordTextViewBase(thisObject, originalPx, factor)
                        return@Hooker result
                    }
                    val forcedPx = originalPx * factor
                    if (!ForceTextSizeHookRuntime.shouldApplyTargetSize(thisObject, forcedPx)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "textview_sp_rewrite",
                            ("reason=no_change, view=" + thisObject.javaClass.getName()
                                    + ", in=" + originalPx
                                    + ", out=" + forcedPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        return@Hooker result
                    }
                    val detail = ("view=" + thisObject.javaClass.getName()
                            + ", in=" + originalPx
                            + ", out=" + forcedPx
                            + ", factor=" + factor
                            + ", percent=" + targetPercent)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "textview_sp_rewrite",
                        detail
                    )
                    ForceTextSizeHookRuntime.INTERNAL_UPDATE.set(true)
                    try {
                        val diagnosticCaptureActive =
                            isCaptureActive
                        val frameworkStartedAt = if (diagnosticCaptureActive)
                            System.nanoTime()
                        else
                            0L
                        thisObject.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx)
                        val frameworkDurationNs = if (diagnosticCaptureActive) max(
                            0L,
                            System.nanoTime() - frameworkStartedAt
                        ) else
                            0L
                        val bookkeepingStartedAt = if (diagnosticCaptureActive)
                            System.nanoTime()
                        else
                            0L
                        ForceTextSizeHookRuntime.recordTextViewBase(thisObject, originalPx, factor)
                        ForceTextSizeHookRuntime.markAppliedTargetSize(thisObject, forcedPx, factor)
                        TextViewFontProvenanceTracker.recordApplied(
                            thisObject,
                            originalPx,
                            forcedPx,
                            factor,
                            TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
                            UnitKind.SP
                        )
                        val bookkeepingDurationNs = if (diagnosticCaptureActive) max(
                            0L,
                            System.nanoTime() - bookkeepingStartedAt
                        ) else
                            0L
                        RuntimeHotPathEvents.applied(
                            packageName,
                            "textview_sp_rewrite",
                            detail
                        )
                        ForceTextSizeHookRuntime.recordSlowTextMutationEvidence(
                            packageName,
                            "textview_sp_rewrite",
                            detail,
                            frameworkDurationNs,
                            bookkeepingDurationNs,
                            diagnosticCaptureActive
                        )
                        ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                            xposed,
                            packageName,
                            HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP,
                            "textview setTextSize(default sp) override applied"
                        )
                    } finally {
                        ForceTextSizeHookRuntime.INTERNAL_UPDATE.remove()
                        RuntimeHotPathEvents.end(
                            packageName,
                            "textview_sp_rewrite",
                            detail
                        )
                    }
                    if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        ForceTextSizeHookRuntime.logSampled(
                            ForceTextSizeHookRuntime.buildHotFontLogKey(packageName, "text-size-float"),
                            ("DPIS_FONT ForceTextSize override: unit=SP(default)"
                                    + ", size=" + sizeSp
                                    + ", px=" + originalPx + " -> " + forcedPx
                                    + ", view=" + thisObject.javaClass.getName()
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent),
                            HOT_LOG_INTERVAL
                        )
                        ForceTextSizeHookRuntime.logCallerSample(packageName, "text-size-float")
                    }
                    FontDebugStatsReporter.record(
                        "text-size-float",
                        thisObject.javaClass.getName(),
                        thisObject.getContext()
                    )
                    result
                })
    }
}
