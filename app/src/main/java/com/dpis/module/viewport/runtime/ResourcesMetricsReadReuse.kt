package com.dpis.module.viewport

import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reuses a resolved `Resources.getDisplayMetrics` read while the returned
 * metrics and the write-side generation are unchanged.
 *
 * The one-second window matches the viewport resolver so a runtime property
 * change is not hidden for the rest of the process.
 */
object ResourcesMetricsReadReuse {
    const val REUSE_TTL_NANOS: Long = 1_000_000_000L

    private val entries = Collections.synchronizedMap(WeakHashMap<Any, Entry>())
    private val generation = AtomicInteger()

    @JvmStatic
    fun matchesDisplayMetrics(
        scope: Any?,
        densityDpi: Int,
        densityBits: Int,
        scaledDensityBits: Int,
        widthPx: Int,
        heightPx: Int,
        nowNanos: Long,
    ): Boolean {
        val entry = freshEntry(scope, nowNanos) ?: return false
        return entry.densityDpi == densityDpi &&
                entry.densityBits == densityBits &&
                entry.scaledDensityBits == scaledDensityBits &&
                entry.widthPx == widthPx &&
                entry.heightPx == heightPx
    }

    @JvmStatic
    fun remember(
        scope: Any?,
        densityDpi: Int,
        densityBits: Int,
        scaledDensityBits: Int,
        widthPx: Int,
        heightPx: Int,
        nowNanos: Long,
    ) {
        if (scope == null) {
            return
        }
        entries[scope] = Entry(
            densityDpi,
            densityBits,
            scaledDensityBits,
            widthPx,
            heightPx,
            nowNanos,
            generation.get(),
        )
    }

    @JvmStatic
    fun bump() {
        generation.incrementAndGet()
    }

    @JvmStatic
    fun clearForTest() {
        entries.clear()
        generation.set(0)
    }

    private fun freshEntry(scope: Any?, nowNanos: Long): Entry? {
        if (scope == null) {
            return null
        }
        val entry = entries[scope] ?: return null
        if (nowNanos - entry.rememberedAtNanos >= REUSE_TTL_NANOS) {
            return null
        }
        if (entry.generation != generation.get()) {
            return null
        }
        return entry
    }

    private class Entry(
        val densityDpi: Int,
        val densityBits: Int,
        val scaledDensityBits: Int,
        val widthPx: Int,
        val heightPx: Int,
        val rememberedAtNanos: Long,
        val generation: Int,
    )
}
