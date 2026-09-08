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
                    val thisObject = chain.getThisObject()
                    applyTextAppearanceOverride(xposed, thisObject, factor, targetPercent,
                        packageName, domainPlan, "text_appearance", HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT,
                        "text-appearance-ctx", "TextAppearance(Context,int) fallback applied")
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
                    val thisObject = chain.getThisObject()
                    applyTextAppearanceOverride(xposed, thisObject, factor, targetPercent,
                        packageName, domainPlan, "text_appearance_int", HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES,
                        "text-appearance-res", "TextAppearance(int) fallback applied")
                    result
                })
        } catch (ignored: Throwable) {
        }
    }

    private fun applyTextAppearanceOverride(
        xposed: XposedInterface,
        thisObject: Any?,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        eventName: String,
        hookId: String,
        logKey: String,
        appliedMessage: String
    ) {
        if (!TextSizePolicy.isTargetPercentActive(targetPercent) || thisObject !is TextView) return
        val detail = "view=${thisObject.javaClass.name}, factor=$factor, percent=$targetPercent"
        RuntimeHotPathEvents.begin(packageName, eventName, detail)
        try {
            if (ForceTextSizeHookRuntime.applyTextViewSizeOverride(thisObject, factor, domainPlan)) {
                RuntimeHotPathEvents.applied(packageName, eventName, detail)
                ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(xposed, packageName, hookId, appliedMessage)
            }
        } finally {
            RuntimeHotPathEvents.end(packageName, eventName, detail)
        }
        ForceTextSizeHookRuntime.logIfChanged(
            ForceTextSizeHookRuntime.buildFontLogKey(packageName, logKey),
            "DPIS_FONT TextAppearance override: view=${thisObject.javaClass.name}, factor=$factor, percent=$targetPercent"
        )
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
