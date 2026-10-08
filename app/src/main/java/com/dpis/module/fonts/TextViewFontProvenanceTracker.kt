package com.dpis.module.fonts

import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs

object TextViewFontProvenanceTracker {
    enum class Source { RESOURCE_FONT_SCALE, TEXTVIEW_SP_REWRITE, TEXTVIEW_ABSOLUTE_REWRITE, TEXTVIEW_CURRENT_PX_FALLBACK }
    enum class UnitKind { SP, ABSOLUTE, UNKNOWN }

    private val entries = Collections.synchronizedMap(WeakHashMap<Any, Entry>())

    @JvmStatic
    fun recordResourcesHandled(textView: Any?, currentPx: Float, factor: Float) {
        if (textView != null && currentPx > 0f && isScaleFactorActive(factor)) record(
            textView,
            currentPx / factor,
            currentPx,
            factor,
            Source.RESOURCE_FONT_SCALE,
            UnitKind.SP
        )
    }

    @JvmStatic
    fun recordApplied(
        textView: Any?,
        basePx: Float,
        appliedPx: Float,
        factor: Float,
        source: Source?,
        unitKind: UnitKind?
    ) {
        if (textView != null && basePx > 0f && appliedPx > 0f && source != null && unitKind != null && isScaleFactorActive(
                factor
            )
        ) record(textView, basePx, appliedPx, factor, source, unitKind)
    }

    @JvmStatic
    fun hasStrongerProvenanceForCurrentPxFallback(textView: Any?, factor: Float): Boolean =
        getEntry(textView)?.let {
            synchronized(it) {
                isSameFactor(
                    it.factorAtApply,
                    factor
                ) && it.source in setOf(
                    Source.RESOURCE_FONT_SCALE,
                    Source.TEXTVIEW_SP_REWRITE,
                    Source.TEXTVIEW_ABSOLUTE_REWRITE
                )
            }
        } ?: false

    @JvmStatic
    fun appliedTargetForFactor(textView: Any?, factor: Float): Float? =
        getEntry(textView)?.let { entry ->
            synchronized(entry) {
                entry.appliedPx.takeIf {
                    isSameFactor(
                        entry.factorAtApply,
                        factor
                    )
                }
            }
        }

    @JvmStatic
    fun snapshotForTest(textView: Any?): Entry? =
        getEntry(textView)?.let { synchronized(it) { Entry(it) } }

    private fun record(
        textView: Any,
        basePx: Float,
        appliedPx: Float,
        factor: Float,
        source: Source,
        unitKind: UnitKind
    ) {
        val entry = getOrCreateEntry(textView); synchronized(entry) {
            if (entry.basePx == basePx && entry.appliedPx == appliedPx && entry.factorAtApply == factor && entry.source == source && entry.unitKind == unitKind) return; entry.basePx =
            basePx; entry.appliedPx = appliedPx; entry.factorAtApply = factor; entry.source =
            source; entry.unitKind = unitKind
        }
    }

    private fun getEntry(textView: Any?): Entry? =
        textView?.let { synchronized(entries) { entries[it] } }

    private fun getOrCreateEntry(textView: Any) =
        synchronized(entries) { entries[textView] ?: Entry().also { entries[textView] = it } }

    private fun isSameFactor(recorded: Float, factor: Float) =
        isScaleFactorActive(recorded) && isScaleFactorActive(factor) && abs(recorded - factor) <= 0.001f

    private fun isScaleFactorActive(factor: Float) = factor > 0f && factor != 1f

    class Entry {
        @JvmField
        var basePx = 0f
        @JvmField
        var appliedPx = 0f
        @JvmField
        var factorAtApply = 0f
        @JvmField
        var source: Source? = null
        @JvmField
        var unitKind: UnitKind? = null

        internal constructor()
        internal constructor(sourceEntry: Entry) {
            basePx = sourceEntry.basePx; appliedPx = sourceEntry.appliedPx; factorAtApply =
                sourceEntry.factorAtApply; source = sourceEntry.source; unitKind =
                sourceEntry.unitKind
        }
    }
}
