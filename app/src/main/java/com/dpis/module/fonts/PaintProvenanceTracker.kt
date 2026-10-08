package com.dpis.module.fonts

import java.util.Collections
import java.util.WeakHashMap

object PaintProvenanceTracker {
    private const val MAX_SLOTS = 4
    private val entries = Collections.synchronizedMap(WeakHashMap<Any, Entry>())

    @JvmStatic
    fun isKnownApplied(paint: Any?, incomingPx: Float, factor: Float): Boolean {
        val entry = getEntry(paint) ?: return false
        synchronized(entry) {
            repeat(MAX_SLOTS) { slot ->
                if (FontFieldRewriteMath.isKnownAppliedPaintSize(
                        incomingPx,
                        factor,
                        entry.lastAppliedPxBySlot[slot],
                        entry.factorAtApplyBySlot[slot]
                    )
                ) {
                    entry.promoteSlot(slot); return true
                }
            }; return false
        }
    }

    @JvmStatic
    fun resolveScaled(paint: Any?, incomingPx: Float, factor: Float): Float {
        if (paint == null || incomingPx <= 0f || !isScaleFactorActive(factor)) return incomingPx
        val entry = getOrCreateEntry(paint); synchronized(entry) {
            return entry.resolveScaledLocked(
                incomingPx,
                factor
            )
        }
    }

    @JvmStatic
    fun recordApplied(paint: Any?, appliedPx: Float, factor: Float) {
        if (paint == null || appliedPx <= 0f || !isScaleFactorActive(factor)) return
        val entry = getOrCreateEntry(paint); synchronized(entry) {
            if (entry.basePxBySlot[0] <= 0f) entry.basePxBySlot[0] =
                appliedPx / factor; entry.lastAppliedPxBySlot[0] =
            appliedPx; entry.factorAtApplyBySlot[0] = factor; entry.syncPrimaryFields()
        }
    }

    @JvmStatic
    fun invalidateIfDrifted(paint: Any?, currentPx: Float) {
        getEntry(paint)?.let { synchronized(it) { it.invalidateIfDrifted(currentPx) } }
    }

    @JvmStatic
    fun snapshotForTest(paint: Any?): Entry? =
        getEntry(paint)?.let { synchronized(it) { Entry(it) } }

    private fun getEntry(paint: Any?): Entry? = paint?.let { synchronized(entries) { entries[it] } }
    private fun getOrCreateEntry(paint: Any): Entry =
        synchronized(entries) { entries[paint] ?: Entry().also { entries[paint] = it } }

    private fun isScaleFactorActive(factor: Float) = factor > 0f && factor != 1f

    class Entry {
        @JvmField
        var basePx = 0f
        @JvmField
        var lastAppliedPx: Float? = null
        @JvmField
        var factorAtApply: Float? = null
        @JvmField
        val basePxBySlot: FloatArray
        @JvmField
        val lastAppliedPxBySlot: Array<Float?>
        @JvmField
        val factorAtApplyBySlot: Array<Float?>

        constructor() : this(
            FloatArray(MAX_SLOTS),
            arrayOfNulls(MAX_SLOTS),
            arrayOfNulls(MAX_SLOTS)
        )

        internal constructor(source: Entry) : this(
            source.basePxBySlot.clone(),
            source.lastAppliedPxBySlot.clone(),
            source.factorAtApplyBySlot.clone()
        )

        private constructor(base: FloatArray, applied: Array<Float?>, factors: Array<Float?>) {
            basePxBySlot = base; lastAppliedPxBySlot = applied; factorAtApplyBySlot =
                factors; syncPrimaryFields()
        }

        private fun findBaseSlot(px: Float) = (0 until MAX_SLOTS).firstOrNull {
            basePxBySlot[it] > 0f && FontFieldRewriteMath.approximatelyEqual(
                px,
                basePxBySlot[it]
            )
        } ?: -1

        private fun findReusableSlot() =
            (0 until MAX_SLOTS).firstOrNull { basePxBySlot[it] <= 0f } ?: MAX_SLOTS - 1

        private fun matchesKnownScaledSize(slot: Int, px: Float, factor: Float) =
            basePxBySlot[slot] > 0f && FontFieldRewriteMath.approximatelyEqual(
                px,
                basePxBySlot[slot] * factor
            )

        private fun isKnownAppliedLocked(px: Float, factor: Float): Boolean {
            repeat(MAX_SLOTS) { slot ->
                if (FontFieldRewriteMath.isKnownAppliedPaintSize(
                        px,
                        factor,
                        lastAppliedPxBySlot[slot],
                        factorAtApplyBySlot[slot]
                    )
                ) {
                    promoteSlot(slot); return true
                }
            }; return false
        }

        fun resolveScaledLocked(px: Float, factor: Float): Float {
            if (isKnownAppliedLocked(
                    px,
                    factor
                )
            ) return px; repeat(MAX_SLOTS) { slot ->
                if (matchesKnownScaledSize(slot, px, factor)) {
                    promoteSlot(slot); return px
                }
            }
            var slot = findBaseSlot(px); if (slot < 0) {
                slot = findReusableSlot(); basePxBySlot[slot] = px; lastAppliedPxBySlot[slot] =
                    null; factorAtApplyBySlot[slot] = null
            }; promoteSlot(slot); return basePxBySlot[0] * factor
        }

        fun invalidateIfDrifted(currentPx: Float) {
            repeat(MAX_SLOTS) { slot ->
                val applied =
                    lastAppliedPxBySlot[slot]; if (applied != null && applied > 0f && !FontFieldRewriteMath.approximatelyEqual(
                    currentPx,
                    applied
                )
            ) {
                lastAppliedPxBySlot[slot] = null; factorAtApplyBySlot[slot] = null
            }
            }; syncPrimaryFields()
        }

        fun promoteSlot(slot: Int) {
            if (slot <= 0) {
                syncPrimaryFields(); return
            }
            val base = basePxBySlot[slot]
            val applied = lastAppliedPxBySlot[slot]
            val factor = factorAtApplyBySlot[slot]; for (i in slot downTo 1) {
                basePxBySlot[i] = basePxBySlot[i - 1]; lastAppliedPxBySlot[i] =
                    lastAppliedPxBySlot[i - 1]; factorAtApplyBySlot[i] = factorAtApplyBySlot[i - 1]
            }; basePxBySlot[0] = base; lastAppliedPxBySlot[0] = applied; factorAtApplyBySlot[0] =
                factor; syncPrimaryFields()
        }

        fun syncPrimaryFields() {
            basePx = basePxBySlot[0]; lastAppliedPx = lastAppliedPxBySlot[0]; factorAtApply =
                factorAtApplyBySlot[0]
        }
    }
}
