package com.dpis.module.viewport

import java.lang.ref.WeakReference
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
    private val lastEntry = ThreadLocal<Entry?>()

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
        val localEntry = lastEntry.get()
        if (localEntry != null && localEntry.scope.get() === scope &&
            matchesEntry(
                localEntry,
                densityDpi,
                densityBits,
                scaledDensityBits,
                widthPx,
                heightPx,
                nowNanos,
            )
        ) {
            return true
        }
        val entry = freshEntry(scope, nowNanos) ?: return false
        if (!matchesEntry(
                entry,
                densityDpi,
                densityBits,
                scaledDensityBits,
                widthPx,
                heightPx,
                nowNanos,
            )
        ) {
            return false
        }
        lastEntry.set(entry)
        return true
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
        val entry = Entry(
            scope = WeakReference(scope),
            densityDpi = densityDpi,
            densityBits = densityBits,
            scaledDensityBits = scaledDensityBits,
            widthPx = widthPx,
            heightPx = heightPx,
            rememberedAtNanos = nowNanos,
            generation = generation.get(),
        )
        entries[scope] = entry
        lastEntry.set(entry)
    }

    @JvmStatic
    fun bump() {
        // Configuration reuse shares this generation boundary with metrics
        // reuse so window/display state changes invalidate both paths.
        generation.incrementAndGet()
        lastEntry.remove()
        ResourcesConfigurationReadReuse.invalidate()
    }

    @JvmStatic
    fun clearForTest() {
        entries.clear()
        generation.set(0)
        lastEntry.remove()
        ResourcesConfigurationReadReuse.invalidate()
    }

    private fun freshEntry(scope: Any?, nowNanos: Long): Entry? {
        if (scope == null) {
            return null
        }
        val entry = entries[scope] ?: return null
        return entry.takeIf { isFresh(it, nowNanos) }
    }

    private fun matchesEntry(
        entry: Entry,
        densityDpi: Int,
        densityBits: Int,
        scaledDensityBits: Int,
        widthPx: Int,
        heightPx: Int,
        nowNanos: Long,
    ): Boolean {
        return isFresh(entry, nowNanos) &&
                entry.densityDpi == densityDpi &&
                entry.densityBits == densityBits &&
                entry.scaledDensityBits == scaledDensityBits &&
                entry.widthPx == widthPx &&
                entry.heightPx == heightPx
    }

    private fun isFresh(entry: Entry, nowNanos: Long): Boolean {
        if (nowNanos - entry.rememberedAtNanos >= REUSE_TTL_NANOS) {
            return false
        }
        return entry.generation == generation.get()
    }

    private class Entry(
        val scope: WeakReference<Any>,
        val densityDpi: Int,
        val densityBits: Int,
        val scaledDensityBits: Int,
        val widthPx: Int,
        val heightPx: Int,
        val rememberedAtNanos: Long,
        val generation: Int,
    )
}
