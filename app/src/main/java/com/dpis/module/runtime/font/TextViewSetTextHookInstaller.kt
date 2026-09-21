package com.dpis.module.runtime.font

import android.text.Spanned
import android.widget.TextView
import android.widget.TextView.BufferType
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontDebugStatsReporter
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker

internal object TextViewSetTextHookInstaller {
    private const val HOT_LOG_INTERVAL = 32
    private const val HOOK_ID_TEXTVIEW_SET_TEXT = "textview_set_text"
    private const val XIAOHEIHE_EXPRESSION_TEXT_VIEW = "com.max.xiaoheihe.module.expression.widget.ExpressionTextView"

    fun install(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
val setTextMethod = textViewClass.getDeclaredMethod(
            "setText", CharSequence::class.java, BufferType::class.java
        )
        apiCapabilities.applyStableHookId<HookBuilder>(
            xposed.hook(setTextMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
            HOOK_ID_TEXTVIEW_SET_TEXT
        )
            .intercept(Hooker { chain: XposedInterface.Chain? ->
                if (true == ForceTextSizeHookRuntime.INTERNAL_TEXT_UPDATE.get()) {
                    return@Hooker chain!!.proceed()
                }
                if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                    return@Hooker chain!!.proceed()
                }
                val thisObject = chain!!.thisObject
                if (thisObject !is TextView) {
                    return@Hooker chain.proceed()
                }
                if (XIAOHEIHE_EXPRESSION_TEXT_VIEW == thisObject.javaClass.name) {
                    ForceTextSizeHookRuntime.applyExpressionTextSizeOverride(thisObject, factor)
                }
                val sourceText = chain.getArg(0) as CharSequence?
                if (sourceText !is Spanned) {
                    val result = chain.proceed()
                    reinforceOrKeepCurrentPx(
                        thisObject, factor, targetPercent, packageName, domainPlan
                    )
                    return@Hooker result
                }
                val patched = TextSpanScaler.scale(sourceText, factor)
                if (patched === sourceText) {
                    val result = chain.proceed()
                    reinforceOrKeepCurrentPx(
                        thisObject, factor, targetPercent, packageName, domainPlan
                    )
                    return@Hooker result
                }
                val bufferType = chain.getArg(1) as BufferType?
                val detail = ("view=" + thisObject.javaClass.name
                        + ", factor=" + factor
                        + ", percent=" + targetPercent
                        + ", length=" + patched.length)
                RuntimeHotPathEvents.begin(
                    packageName,
                    "textview_span_rewrite",
                    detail
                )
                ForceTextSizeHookRuntime.INTERNAL_TEXT_UPDATE.set(true)
                try {
                    thisObject.setText(patched, bufferType)
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "textview_span_rewrite",
                        detail
                    )
                    ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                        xposed,
                        packageName,
                        HOOK_ID_TEXTVIEW_SET_TEXT,
                        "TextView span rewrite applied"
                    )
                } finally {
                    ForceTextSizeHookRuntime.INTERNAL_TEXT_UPDATE.remove()
                    RuntimeHotPathEvents.end(
                        packageName,
                        "textview_span_rewrite",
                        detail
                    )
                }
                reinforceOrKeepCurrentPx(
                    thisObject, factor, targetPercent, packageName, domainPlan
                )
                if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                    ForceTextSizeHookRuntime.logSampled(
                        ForceTextSizeHookRuntime.buildHotFontLogKey(
                            packageName, "textview-span-" + thisObject.javaClass.name
                        ),
                        ("DPIS_FONT TextView span override: view="
                                + thisObject.javaClass.name
                                + ", factor=" + factor
                                + ", percent=" + targetPercent
                                + ", length=" + patched.length),
                        HOT_LOG_INTERVAL
                    )
                }
                FontDebugStatsReporter.record(
                    "textview-span",
                    thisObject.javaClass.name,
                    thisObject.context
                )
                null
            })
    }

    private fun reinforceOrKeepCurrentPx(
        textView: TextView,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?
    ) {
        val incomingPx = textView.textSize
        if (ForceTextSizeHookRuntime.reinforceTextViewTarget(textView, factor, domainPlan)) {
            ForceTextSizeHookRuntime.recordCurrentPxReinforceHotPath(
                packageName,
                textView,
                factor,
                targetPercent,
                incomingPx,
                textView.textSize
            )
            FontDebugStatsReporter.record(
                "textview-settext-reinforce",
                textView.javaClass.name,
                textView.context
            )
        } else if (ForceTextSizeHookRuntime.shouldKeepCurrentTextViewFallback(textView, factor)) {
            ForceTextSizeHookRuntime.recordCurrentPxKeptHotPath(
                packageName, textView, factor, targetPercent
            )
        }
    }
}
