package com.dpis.module.runtime.font

import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker

internal object TextViewAppearanceHookInstaller {
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT = "textview_set_text_appearance_context"
    private const val HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES = "textview_set_text_appearance_res"

    fun installTextAppearanceHooks(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
try {
            val setTextAppearanceCtx = textViewClass.getDeclaredMethod(
                "setTextAppearance", Context::class.java, Int::class.javaPrimitiveType
            )
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(setTextAppearanceCtx)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker result
                    }
                    val thisObject = chain.getThisObject()
                    if (thisObject !is TextView) {
                        return@Hooker result
                    }
                    val detail = ("view=" + thisObject.javaClass.getName()
                            + ", factor=" + factor
                            + ", percent=" + targetPercent)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "text_appearance",
                        detail
                    )
                    try {
                        if (ForceTextSizeHookRuntime.applyTextViewSizeOverride(thisObject, factor, domainPlan)) {
                            RuntimeHotPathEvents.applied(
                                packageName,
                                "text_appearance",
                                detail
                            )
                            ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                                xposed,
                                packageName,
                                HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT,
                                "TextAppearance(Context,int) fallback applied"
                            )
                        }
                    } finally {
                        RuntimeHotPathEvents.end(
                            packageName,
                            "text_appearance",
                            detail
                        )
                    }
                    ForceTextSizeHookRuntime.logIfChanged(
                        ForceTextSizeHookRuntime.buildFontLogKey(packageName, "text-appearance-ctx"),
                        ("DPIS_FONT TextAppearance override: view="
                                + thisObject.javaClass.getName()
                                + ", factor=" + factor
                                + ", percent=" + targetPercent)
                    )
                    result
                })
        } catch (ignored: Throwable) {
        }
        try {
            val setTextAppearanceRes =
                textViewClass.getDeclaredMethod("setTextAppearance", Int::class.javaPrimitiveType)
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(setTextAppearanceRes)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker result
                    }
                    val thisObject = chain.getThisObject()
                    if (thisObject !is TextView) {
                        return@Hooker result
                    }
                    val detail = ("view=" + thisObject.javaClass.getName()
                            + ", factor=" + factor
                            + ", percent=" + targetPercent)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "text_appearance_int",
                        detail
                    )
                    try {
                        if (ForceTextSizeHookRuntime.applyTextViewSizeOverride(thisObject, factor, domainPlan)) {
                            RuntimeHotPathEvents.applied(
                                packageName,
                                "text_appearance_int",
                                detail
                            )
                            ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                                xposed,
                                packageName,
                                HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES,
                                "TextAppearance(int) fallback applied"
                            )
                        }
                    } finally {
                        RuntimeHotPathEvents.end(
                            packageName,
                            "text_appearance_int",
                            detail
                        )
                    }
                    ForceTextSizeHookRuntime.logIfChanged(
                        ForceTextSizeHookRuntime.buildFontLogKey(packageName, "text-appearance-res"),
                        ("DPIS_FONT TextAppearance(int) override: view="
                                + thisObject.javaClass.getName()
                                + ", factor=" + factor
                                + ", percent=" + targetPercent)
                    )
                    result
                })
        } catch (ignored: Throwable) {
        }
    }

    private fun installTextViewDrawHook(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?
    ) {
        try {
            val onDrawMethod = textViewClass.getDeclaredMethod("onDraw", Canvas::class.java)
            xposed.hook(onDrawMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        val thisObject = chain!!.getThisObject()
                        if (thisObject is TextView
                            && true != ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()
                        ) {
                            ForceTextSizeHookRuntime.applyTextViewSizeOverride(thisObject, factor, domainPlan)
                        }
                    }
                    chain!!.proceed()
                })
            ForceTextSizeHookRuntime.logIfChanged(
                ForceTextSizeHookRuntime.buildFontLogKey(packageName, "textview-ondraw-hook"),
                "DPIS_FONT TextView onDraw guard enabled"
            )
        } catch (ignored: Throwable) {
        }
    }
}
