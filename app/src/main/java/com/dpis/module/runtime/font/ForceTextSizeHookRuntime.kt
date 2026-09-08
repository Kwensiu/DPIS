package com.dpis.module.runtime.font

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.TextView.BufferType
import com.dpis.module.BuildConfig
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.diagnostics.RuntimeTransport.isCaptureActive
import com.dpis.module.fonts.FontDebugStatsReporter
import com.dpis.module.fonts.FontFieldRewriteMath
import com.dpis.module.fonts.FontMutationScheduler
import com.dpis.module.fonts.PaintDiagnosticCallerSampler
import com.dpis.module.fonts.PaintProvenanceTracker
import com.dpis.module.fonts.TextViewFontProvenanceTracker
import com.dpis.module.fonts.TextViewFontProvenanceTracker.UnitKind
import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.font.FontScaleOverride.resolve
import com.dpis.module.runtime.font.FontScaleOverride.toPx
import com.dpis.module.runtime.font.PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.max

object ForceTextSizeHookRuntime {
    private const val BRIDGE_LOG_PREFIX = "DPIS "
    private const val XIAOHEIHE_EXPRESSION_TEXT_VIEW =
        "com.max.xiaoheihe.module.expression.widget.ExpressionTextView"
    private const val FONT_LOG_KEY_PREFIX = "font"
    private const val FONT_HOT_LOG_KEY_PREFIX = "font-hot"
    private const val BRIDGE_LOG_KEY_PREFIX = "font-bridge"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT = "textview_set_text_size_with_unit"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP =
        "textview_set_text_size_default_sp"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT = "textview_set_text"
    private const val HOOK_ID_TEXTVIEW_ATTACH = "textview_on_attached_to_window"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT =
        "textview_set_text_appearance_context"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES = "textview_set_text_appearance_res"
    private const val HOOK_ID_PAINT_SET_TEXT_SIZE = "paint_set_text_size"
    private const val HOOK_ID_TEXTPAINT_SET_TEXT_SIZE = "textpaint_set_text_size"

    private var installedPid: Int
        get() = TextSizeRuntimeState.installedPid
        set(value) { TextSizeRuntimeState.installedPid = value }
    private val LAST_MESSAGES get() = TextSizeRuntimeState.lastMessages
    private val HOT_LOG_COUNTS get() = TextSizeRuntimeState.hotLogCounts
    private val CALLER_SAMPLE_COUNTS get() = TextSizeRuntimeState.callerSampleCounts
    private val CALLER_SOURCE_COUNTS get() = TextSizeRuntimeState.callerSourceCounts
    internal val INTERNAL_UPDATE get() = TextSizeRuntimeState.internalUpdate
    internal val INTERNAL_TEXT_UPDATE get() = TextSizeRuntimeState.internalTextUpdate
    private val TEXT_VIEW_SET_TEXT_SIZE_DEPTH get() = TextSizeRuntimeState.textViewSetTextSizeDepth
    private val EXPRESSION_BASE_TEXT_SIZES get() = TextSizeRuntimeState.expressionBaseTextSizes
    private val TEXT_VIEW_BASE_TEXT_SIZES get() = TextSizeRuntimeState.textViewBaseTextSizes
    private val COMMENT_TEXT_BASE_TEXT_SIZES get() = TextSizeRuntimeState.commentTextBaseTextSizes
    private val LAST_TARGET_TEXT_SIZES: MutableMap<TextView?, TargetTextSize?> =
        Collections.synchronizedMap<TextView?, TargetTextSize?>(
            WeakHashMap<TextView?, TargetTextSize?>()
        )
    private const val SIZE_EPSILON_PX = 0.5f
    private const val MAX_SAMPLES_PER_CALLER = 1
    private const val MAX_SAMPLES_PER_SOURCE = 1
    private const val HOT_LOG_INTERVAL = 32
    private const val MAX_STACK_FRAMES = 6
    private const val SLOW_TEXT_MUTATION_THRESHOLD_NS = 1000000L

    @Volatile
    internal var verboseFontLogsEnabled = false

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
        clearThreadLocalState()
    }

    /** Clears hook-local state before a hot reload can reuse this process thread.  */
    private fun clearThreadLocalState() {
        INTERNAL_UPDATE.remove()
        INTERNAL_TEXT_UPDATE.remove()
        TEXT_VIEW_SET_TEXT_SIZE_DEPTH.remove()
    }

    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String,
        store: DpisConfigStore?,
        domainPlan: FontDomainPlan?
    ) {
        install(
            xposed,
            packageName,
            store,
            domainPlan,
            ModernApiCapabilitiesResolver.fromXposed(xposed)
        )
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String,
        store: DpisConfigStore?,
        domainPlan: FontDomainPlan? = FontHookArbitration.resolveDomainPlan(true, true),
        apiCapabilities: ModernApiCapabilities = ModernApiCapabilitiesResolver.fromXposed(xposed)
    ) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(ForceTextSizeHookRuntime::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            val fontScale = resolve(store, packageName, 1.0f)
            val targetPercent = fontScale.targetPercent
            val factor = resolveFieldRewriteFactor(
                store, packageName
            )
            verboseFontLogsEnabled = isVerboseFontLogsEnabled(store)
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val textViewClass = Class.forName("android.widget.TextView", false, bootClassLoader)
            TextViewTextSizeHookInstaller.install(
                xposed, textViewClass, factor, targetPercent, packageName, domainPlan, apiCapabilities
            )
            if (TextSizePolicy.shouldInstallCurrentPxTextViewFallbacks(domainPlan)) {
                installTextAppearanceHooks(
                    xposed,
                    textViewClass,
                    factor,
                    targetPercent,
                    packageName,
                    domainPlan,
                    apiCapabilities
                )
                installTextViewAttachHook(
                    xposed,
                    textViewClass,
                    factor,
                    targetPercent,
                    packageName,
                    domainPlan,
                    apiCapabilities
                )
            } else {
                logIfChanged(
                    buildFontLogKey(packageName, "textview-current-px-fallback-suppressed"),
                    "DPIS_FONT TextView current-px fallbacks suppressed: reason="
                            + domainPlan!!.reason
                )
            }
            if (domainPlan == null || domainPlan.paintFallbackEnabled) {
                installPaintTextSizeHooks(
                    xposed,
                    factor,
                    targetPercent,
                    packageName,
                    apiCapabilities
                )
            } else {
                // Paint/TextPaint cannot reliably tell whether incoming sizes were already
                // handled by Resources, WebView, or TextView domains, so keep it as a last fallback.
                logIfChanged(
                    buildFontLogKey(packageName, "paint-fallback-suppressed"),
                    "DPIS_FONT Paint/TextPaint fallback suppressed: reason="
                            + domainPlan.reason
                )
            }
            if (TextSizePolicy.shouldInstallCurrentPxTextViewFallbacks(domainPlan)) {
                installExpressionTextSetTextHook(
                    xposed,
                    textViewClass,
                    factor,
                    targetPercent,
                    packageName,
                    domainPlan,
                    apiCapabilities
                )
            }
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i(
                ("ForceTextSize hook ready"
                        + ", paintFallback=" + (domainPlan == null
                        || domainPlan.paintFallbackEnabled))
            )
            bridgeLog(
                xposed, ("DPIS_FONT ForceTextSize hook ready: package=" + packageName
                        + ", hookIds="
                        + HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT + ","
                        + HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP + ","
                        + HOOK_ID_TEXTVIEW_SET_TEXT + ","
                        + HOOK_ID_TEXTVIEW_ATTACH + ","
                        + HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT + ","
                        + HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES + ","
                        + HOOK_ID_PAINT_SET_TEXT_SIZE + ","
                        + HOOK_ID_TEXTPAINT_SET_TEXT_SIZE)
            )
        }
    }

    @Throws(ReflectiveOperationException::class)
    private fun installPaintTextSizeHooks(
        xposed: XposedInterface,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        apiCapabilities: ModernApiCapabilities
    ) {
        PaintTextSizeHookInstaller.installPaintTextSizeHooks(xposed, factor, targetPercent, packageName, apiCapabilities)
    }

    @JvmStatic
    internal fun resolvePaintFallbackDecisionForTest(
        paint: Any?,
        incomingPx: Float,
        currentPx: Float,
        factor: Float,
        strongerDomainOwns: Boolean
    ): PaintFallbackDecision = PaintFallbackResolver.resolve(
        paint, incomingPx, currentPx, factor, strongerDomainOwns
    )

    private fun installTextViewAttachHook(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
        TextViewAttachHookInstaller.install(xposed, textViewClass, factor, targetPercent, packageName, domainPlan, apiCapabilities)
    }

    private fun installExpressionTextSetTextHook(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
        TextViewSetTextHookInstaller.install(xposed, textViewClass, factor, targetPercent, packageName, domainPlan, apiCapabilities)
    }

    internal fun reinforceTextViewTarget(
        textView: TextView?,
        factor: Float,
        domainPlan: FontDomainPlan?
    ): Boolean {
        if (textView == null) {
            return false
        }
        val target = LAST_TARGET_TEXT_SIZES.get(textView)
        var desiredPx = if (target != null && target.matchesFactor(factor))
            target.px
        else
            null
        if (desiredPx == null || desiredPx <= 0f) {
            if (!isCommentLikeNode(textView) || !TextSizePolicy.isScaleFactorActive(factor)) {
                return false
            }
            if (TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                    textView, factor
                )
            ) {
                return false
            }
            val currentPx = textView.getTextSize()
            desiredPx = FontFieldRewriteMath.resolveScaledTextSize<TextView?>(
                currentPx, factor, COMMENT_TEXT_BASE_TEXT_SIZES, textView
            )
            if (desiredPx <= 0f) {
                return false
            }
        }
        if (!shouldApplyTargetSize(textView, desiredPx)) {
            return false
        }
        INTERNAL_UPDATE.set(true)
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, desiredPx)
            markAppliedTargetSize(textView, desiredPx, factor)
            return true
        } finally {
            INTERNAL_UPDATE.remove()
        }
    }

    internal fun recordCurrentPxReinforceHotPath(
        packageName: String?,
        textView: TextView,
        factor: Float,
        targetPercent: Int?
    ) {
        val detail = ("reason=set_text_reinforce"
                + ", view=" + textView.javaClass.getName()
                + ", factor=" + factor
                + ", percent=" + targetPercent)
        RuntimeHotPathEvents.begin(
            packageName,
            "textview_current_px_fallback",
            detail
        )
        RuntimeHotPathEvents.applied(
            packageName,
            "textview_current_px_fallback",
            detail
        )
        RuntimeHotPathEvents.end(
            packageName,
            "textview_current_px_fallback",
            detail
        )
    }

    internal fun recordSlowTextMutationEvidence(
        packageName: String?,
        routeName: String?,
        detail: String?,
        frameworkDurationNs: Long,
        bookkeepingDurationNs: Long,
        diagnosticCaptureActive: Boolean
    ) {
        val totalDurationNs = frameworkDurationNs + bookkeepingDurationNs
        if (totalDurationNs < SLOW_TEXT_MUTATION_THRESHOLD_NS
            || !diagnosticCaptureActive
        ) {
            return
        }
        RuntimeHotPathEvents.event(
            packageName,
            "font",
            routeName,
            "slow_mutation_breakdown",
            (detail
                    + ",frameworkUs=" + (frameworkDurationNs / 1000L)
                    + ",bookkeepingUs=" + (bookkeepingDurationNs / 1000L)
                    + ",totalUs=" + (totalDurationNs / 1000L))
        )
    }

    internal fun recordCurrentPxKeptHotPath(
        packageName: String?,
        textView: TextView,
        factor: Float,
        targetPercent: Int?
    ) {
        val detail = ("reason=current_target"
                + ", view=" + textView.javaClass.getName()
                + ", factor=" + factor
                + ", percent=" + targetPercent)
        // Kept is an aggregate-only outcome; do not create begin/end rows for
        // a callback that performed no mutation.
        RuntimeHotPathEvents.kept(
            packageName,
            "textview_current_px_fallback",
            detail
        )
    }

    private fun isCommentLikeNode(textView: TextView?): Boolean {
        if (textView == null) {
            return false
        }
        if (containsCommentHint(textView.javaClass.getName())) {
            return true
        }
        try {
            val viewId = textView.getId()
            if (viewId != View.NO_ID) {
                val entryName = textView.getResources().getResourceEntryName(viewId)
                if (containsCommentHint(entryName)) {
                    return true
                }
            }
        } catch (ignored: Throwable) {
        }
        var parent = textView.getParent()
        var depth = 0
        while (parent != null && depth < 4) {
            if (containsCommentHint(parent.javaClass.getName())) {
                return true
            }
            parent = parent.getParent()
            depth++
        }
        return false
    }

    private fun containsCommentHint(text: String?): Boolean {
        return FontFieldRewriteMath.containsCommentHint(text)
    }

    private fun installTextAppearanceHooks(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
        TextViewAppearanceHookInstaller.installTextAppearanceHooks(xposed, textViewClass, factor, targetPercent, packageName, domainPlan, apiCapabilities)
    }

    internal fun applyExpressionTextSizeOverride(textView: TextView, factor: Float) {
        val currentPx = textView.getTextSize()
        val desiredPx = FontFieldRewriteMath.resolveScaledTextSize<TextView?>(
            currentPx, factor, EXPRESSION_BASE_TEXT_SIZES, textView
        )
        if (!shouldApplyTargetSize(textView, desiredPx)) {
            return
        }
        INTERNAL_UPDATE.set(true)
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, desiredPx)
            markAppliedTargetSize(textView, desiredPx, factor)
        } finally {
            INTERNAL_UPDATE.remove()
        }
    }

    internal fun applyTextViewSizeOverride(
        textView: TextView,
        factor: Float,
        domainPlan: FontDomainPlan?
    ): Boolean {
        val currentPx = textView.getTextSize()
        if (isKnownAppliedTextSize(textView, currentPx, factor)) {
            return false
        }
        if (TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, factor
            )
        ) {
            return false
        }
        val expectedPx = FontFieldRewriteMath.resolveScaledTextSize<TextView?>(
            currentPx, factor, TEXT_VIEW_BASE_TEXT_SIZES, textView
        )
        if (!shouldApplyTargetSize(textView, expectedPx)) {
            return false
        }
        INTERNAL_UPDATE.set(true)
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, expectedPx)
            markAppliedTargetSize(textView, expectedPx, factor)
            TextViewFontProvenanceTracker.recordApplied(
                textView,
                currentPx,
                expectedPx,
                factor,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_CURRENT_PX_FALLBACK,
                UnitKind.UNKNOWN
            )
            return true
        } finally {
            INTERNAL_UPDATE.remove()
        }
    }

    /**
     * Distinguishes a no-op owned by this fallback route from an observation
     * that was intentionally left to a stronger font domain.
     */
    internal fun shouldKeepCurrentTextViewFallback(
        textView: TextView,
        factor: Float
    ): Boolean {
        val currentPx = textView.getTextSize()
        if (isKnownAppliedTextSize(textView, currentPx, factor)) {
            return true
        }
        if (TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, factor
            )
        ) {
            return false
        }
        val expectedPx = FontFieldRewriteMath.resolveScaledTextSize<TextView?>(
            currentPx, factor, TEXT_VIEW_BASE_TEXT_SIZES, textView
        )
        return expectedPx > 0f && !shouldApplyTargetSize(textView, expectedPx)
    }

    internal fun shouldApplyTargetSize(textView: TextView?, targetPx: Float): Boolean {
        if (textView == null || targetPx <= 0f) {
            return false
        }
        val currentPx = textView.getTextSize()
        return abs(currentPx - targetPx) >= SIZE_EPSILON_PX
    }

    internal fun shouldKeepCurrentTextViewTarget(
        textView: TextView?,
        incomingPx: Float,
        factor: Float
    ): Boolean {
        if (textView == null || incomingPx <= 0f) {
            return false
        }
        val currentPx = textView.getTextSize()
        val target = LAST_TARGET_TEXT_SIZES.get(textView)
        var targetPx = if (target != null && target.matchesFactor(factor))
            target.px
        else
            null
        if (targetPx == null) {
            targetPx = TextViewFontProvenanceTracker.appliedTargetForFactor(textView, factor)
        }
        if (targetPx == null) {
            return false
        }
        val basePx = TEXT_VIEW_BASE_TEXT_SIZES.get(textView)
        if (basePx == null
            || (!FontFieldRewriteMath.approximatelyEqual(incomingPx, basePx)
                    && !FontFieldRewriteMath.approximatelyEqual(incomingPx, targetPx))
        ) {
            return false
        }
        return FontMutationScheduler.decide(
            incomingPx,
            currentPx,
            targetPx,
            factor,
            false
        ).action() == FontMutationScheduler.Action.KEEP_CURRENT
    }

    internal fun markAppliedTargetSize(textView: TextView?, targetPx: Float, factor: Float) {
        if (textView == null || targetPx <= 0f) {
            return
        }
        val previous = LAST_TARGET_TEXT_SIZES.get(textView)
        if (previous != null && previous.px.compareTo(targetPx) == 0 && previous.matchesFactor(
                factor
            )
        ) {
            return
        }
        LAST_TARGET_TEXT_SIZES.put(textView, TargetTextSize(targetPx, factor))
    }

    internal fun isKnownAppliedTextSize(
        textView: TextView?,
        currentPx: kotlin.Float,
        factor: kotlin.Float
    ): Boolean {
        if (textView == null || currentPx <= 0f) {
            return false
        }
        val target = LAST_TARGET_TEXT_SIZES.get(textView)
        return target != null && target.matchesFactor(factor)
                && FontFieldRewriteMath.isKnownScaledTextSize(
            currentPx,
            factor,
            target.px
        )
    }

    internal fun recordTextViewBase(
        textView: TextView?,
        basePx: kotlin.Float,
        factor: kotlin.Float
    ) {
        if (textView == null || basePx <= 0f) {
            return
        }
        if (FontFieldRewriteMath.shouldRecordTextBase(
                basePx,
                factor,
                TEXT_VIEW_BASE_TEXT_SIZES.get(textView),
                targetPx(textView, factor)
            )
        ) {
            TEXT_VIEW_BASE_TEXT_SIZES.put(textView, basePx)
        }
    }

    private fun targetPx(textView: TextView?, factor: kotlin.Float): kotlin.Float? {
        val target = LAST_TARGET_TEXT_SIZES.get(textView)
        return if (target != null && target.matchesFactor(factor)) target.px else null
    }

    internal fun recordResourcesHandledTextSize(
        textView: TextView?,
        currentPx: kotlin.Float,
        factor: kotlin.Float
    ) {
        if (textView == null || currentPx <= 0f || !TextSizePolicy.isScaleFactorActive(factor)) {
            return
        }
        val inferredBasePx = currentPx / factor
        if (inferredBasePx > 0f) {
            TEXT_VIEW_BASE_TEXT_SIZES.put(textView, inferredBasePx)
        }
        markAppliedTargetSize(textView, currentPx, factor)
        TextViewFontProvenanceTracker.recordResourcesHandled(textView, currentPx, factor)
    }

    internal fun recordTextViewRewrite(
        textView: TextView?,
        originalPx: kotlin.Float,
        forcedPx: kotlin.Float,
        factor: kotlin.Float,
        unit: Int
    ) {
        val source: TextViewFontProvenanceTracker.Source?
        val unitKind: UnitKind?
        if (unit == TypedValue.COMPLEX_UNIT_SP) {
            source = TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE
            unitKind = UnitKind.SP
        } else {
            source = TextViewFontProvenanceTracker.Source.TEXTVIEW_ABSOLUTE_REWRITE
            unitKind = UnitKind.ABSOLUTE
        }
        TextViewFontProvenanceTracker.recordApplied(
            textView,
            originalPx,
            forcedPx,
            factor,
            source,
            unitKind
        )
    }

    private fun scaleSpans(source: Spanned, factor: kotlin.Float): CharSequence =
        TextSpanScaler.scale(source, factor)

    internal fun logIfChanged(key: String?, message: String) {
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
        }
    }

    internal fun bridgeMutationAppliedIfChanged(
        xposed: XposedInterface?,
        packageName: String?,
        hookId: String?,
        eventName: String?
    ) {
        bridgeLogIfChanged(
            xposed,
            buildBridgeLogKey(packageName, hookId),
            "DPIS_FONT " + eventName + ": package=" + packageName + ", hookId=" + hookId
        )
    }

    private fun bridgeLogIfChanged(xposed: XposedInterface?, key: String?, message: String) {
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            bridgeLog(xposed, message)
        }
    }

    internal fun logSampled(key: String?, message: String?, interval: Int) {
        if (!verboseFontLogsEnabled) {
            return
        }
        if (interval <= 1) {
            DpisLog.i(message)
            return
        }
        val count = HOT_LOG_COUNTS.getOrDefault(key, 0)!! + 1
        HOT_LOG_COUNTS.put(key, count)
        if (count == 1 || (count % interval) == 0) {
            DpisLog.i(message + ", sample=" + count)
        }
    }

    internal fun buildFontLogKey(packageName: String?, suffix: String?): String {
        val pkg = if (packageName == null) "unknown" else packageName
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix
    }

    internal fun buildHotFontLogKey(packageName: String?, suffix: String?): String {
        val pkg = if (packageName == null) "unknown" else packageName
        return pkg + ":" + FONT_HOT_LOG_KEY_PREFIX + ":" + suffix
    }

    private fun buildBridgeLogKey(packageName: String?, hookId: String?): String {
        val pkg = if (packageName == null) "unknown" else packageName
        val id = if (hookId == null) "unknown" else hookId
        return pkg + ":" + BRIDGE_LOG_KEY_PREFIX + ":" + id
    }

    internal fun logCallerSample(packageName: String?, sourceTag: String?) {
        if (!verboseFontLogsEnabled || !DpisLog.isLoggingEnabled()) {
            return
        }
        val sourceCount = CALLER_SOURCE_COUNTS.getOrDefault(sourceTag, 0)!!
        if (sourceCount >= MAX_SAMPLES_PER_SOURCE) {
            return
        }
        val stackSummary = summarizeStack(Thread.currentThread().getStackTrace())
        if (stackSummary == null || stackSummary.isEmpty()) {
            return
        }
        val callerKey = sourceTag + "|" + stackSummary
        val count = CALLER_SAMPLE_COUNTS.getOrDefault(callerKey, 0)!!
        if (count >= MAX_SAMPLES_PER_CALLER) {
            return
        }
        CALLER_SAMPLE_COUNTS.put(callerKey, count + 1)
        CALLER_SOURCE_COUNTS.put(sourceTag, sourceCount + 1)
        DpisLog.i("DPIS_FONT caller(" + packageName + "," + sourceTag + "): " + stackSummary)
    }

    private fun summarizeStack(trace: Array<StackTraceElement?>?): String? {
        if (trace == null) {
            return null
        }
        val builder = StringBuilder()
        var added = 0
        for (element in trace) {
            if (element == null) {
                continue
            }
            val className = element.getClassName()
            if (className == null) {
                continue
            }
            if (className.startsWith("java.lang.Thread")
                || className.startsWith("de.robv.android.xposed")
                || className.startsWith("io.github.libxposed")
                || className.startsWith("com.dpis.module.ForceTextSizeHookInstaller")
            ) {
                continue
            }
            if (builder.length > 0) {
                builder.append(" <- ")
            }
            builder.append(className)
                .append("#")
                .append(element.getMethodName())
                .append(":")
                .append(element.getLineNumber())
            added++
            if (added >= MAX_STACK_FRAMES) {
                break
            }
        }
        return builder.toString()
    }

    internal val isInsideTextViewSetTextSize: Boolean
        get() = TEXT_VIEW_SET_TEXT_SIZE_DEPTH.get()!! > 0

    /** Preserves nested hook depth while allowing the original call to proceed.  */
    @Throws(Throwable::class)
    internal fun proceedInsideTextViewSetTextSize(chain: XposedInterface.Chain): Any? {
        val depth = TEXT_VIEW_SET_TEXT_SIZE_DEPTH.get()!!
        TEXT_VIEW_SET_TEXT_SIZE_DEPTH.set(depth + 1)
        try {
            return chain.proceed()
        } finally {
            if (depth == 0) {
                TEXT_VIEW_SET_TEXT_SIZE_DEPTH.remove()
            } else {
                TEXT_VIEW_SET_TEXT_SIZE_DEPTH.set(depth)
            }
        }
    }

    internal fun isPaintSizeOwnedByTextLayout(trace: Array<StackTraceElement?>?): Boolean {
        if (trace == null) {
            return false
        }
        var fromSpan = false
        var fromTextLayout = false
        for (element in trace) {
            if (element == null) {
                continue
            }
            val className = element.getClassName()
            if (className == null) {
                continue
            }
            if (className.startsWith("android.text.style.")) {
                fromSpan = true
            }
            if (className.startsWith("android.text.MeasuredParagraph")
                || className.startsWith("android.text.StaticLayout")
                || className.startsWith("android.text.TextLine")
            ) {
                fromTextLayout = true
            }
        }
        return fromSpan && fromTextLayout
    }

    internal fun summarizePaintFallbackStack(trace: Array<StackTraceElement?>?): String {
        if (trace == null) {
            return ""
        }
        val builder = StringBuilder()
        var added = 0
        for (element in trace) {
            if (element == null) {
                continue
            }
            val className = element.getClassName()
            if (className == null || className.startsWith("java.lang.Thread")
                || className.startsWith("de.robv.android.xposed")
                || className.startsWith("io.github.libxposed")
                || className.startsWith("com.dpis.module.ForceTextSizeHookInstaller")
                || "android.graphics.Paint" == className
                || "android.text.TextPaint" == className
            ) {
                continue
            }
            if (builder.length > 0) {
                builder.append(" <- ")
            }
            builder.append(className)
                .append("#")
                .append(element.getMethodName())
                .append(":")
                .append(element.getLineNumber())
            added++
            if (added >= MAX_STACK_FRAMES) {
                break
            }
        }
        return builder.toString()
    }

    @JvmStatic
    fun shouldForceTextUnitForTest(
        unit: Int,
        domainPlan: FontDomainPlan?
    ): Boolean {
        return TextSizePolicy.shouldForceTextUnit(unit, domainPlan)
    }

    internal fun routeNameForTextViewSetTextSize(unit: Int): String {
        return if (unit == TypedValue.COMPLEX_UNIT_SP)
            "textview_sp_rewrite"
        else
            "textview_absolute_rewrite"
    }

    internal fun isSpTextHandledByResources(
        textView: TextView?,
        factor: kotlin.Float,
        domainPlan: FontDomainPlan?
    ): Boolean {
        if (textView == null || domainPlan == null || !domainPlan.resourcesFontEnabled) {
            return false
        }
        val metrics = if (textView.getResources() != null)
            textView.getResources().getDisplayMetrics()
        else
            null
        return metrics != null && FontFieldRewriteMath.isResourcesScaledDensityApplied(
            metrics.density,
            metrics.scaledDensity,
            factor
        )
    }

    private fun isVerboseFontLogsEnabled(store: DpisConfigStore?): Boolean {
        return store != null && store.isFontDebugOverlayEnabled
    }

    private fun bridgeLog(xposed: XposedInterface?, message: String?) {
        if (xposed == null || (!BuildConfig.DEBUG && !DpisLog.isLoggingEnabled())) {
            return
        }
        try {
            xposed.log(Log.INFO, DpisLog.TAG, BRIDGE_LOG_PREFIX + message)
        } catch (ignored: Throwable) {
            // Bridge evidence must not affect target app behavior.
        }
    }

    private class TargetTextSize(val px: kotlin.Float, val factor: kotlin.Float) {
        fun matchesFactor(candidate: kotlin.Float): Boolean {
            return TextSizePolicy.isScaleFactorActive(factor)
                    && TextSizePolicy.isScaleFactorActive(candidate)
                    && abs(factor - candidate) <= 0.001f
        }
    }
}
