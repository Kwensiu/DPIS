package com.dpis.module.fonts

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.round

/** Bounds caller-stack detail on the Paint hot path without changing counters. */
object PaintDiagnosticCallerSampler {
    private const val INITIAL_SAMPLES = 2
    private const val PERIODIC_SAMPLE_INTERVAL = 32
    private const val MAX_KEYS = 128
    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    @JvmStatic
    fun shouldCapture(paintClassName: String?, incomingPx: Float): Boolean {
        val key =
            "${paintClassName.takeUnless { it.isNullOrEmpty() } ?: "unknown"}|${round(incomingPx * 10f) / 10f}"
        val count = counts[key] ?: synchronized(counts) {
            counts[key] ?: if (counts.size >= MAX_KEYS) return false else AtomicInteger().also {
                counts[key] = it
            }
        }
        val ordinal = count.getAndIncrement()
        return ordinal < INITIAL_SAMPLES || ordinal % PERIODIC_SAMPLE_INTERVAL == 0
    }

    @JvmStatic
    fun resetForTest() = counts.clear()
}
