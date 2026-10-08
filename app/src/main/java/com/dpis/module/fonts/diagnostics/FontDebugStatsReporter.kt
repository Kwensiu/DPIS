package com.dpis.module.fonts

import android.app.Application
import android.content.Context
import android.os.Bundle
import com.dpis.module.diagnostics.DpisLog
import java.lang.reflect.Method
import java.util.ArrayDeque
import java.util.Locale

object FontDebugStatsReporter {
    private const val SNAPSHOT_INTERVAL_MS = 500L
    private const val WINDOW_5S = 5
    private const val WINDOW_30S = 30
    private const val TOP_LIMIT = 20
    private val lock = Any()
    private val buckets = ArrayDeque<SecondBucket>()
    private val cumulativeChain = HashMap<String, Int>()
    private val cumulativeChainView = HashMap<String, Int>()
    private var lastSnapshotAt = 0L
    private var totalEvents = 0
    private var pendingSnapshot: Snapshot? = null
    private var cachedAppContext: Context? = null

    @JvmStatic fun resetForTest() = synchronized(lock) {
        buckets.clear(); cumulativeChain.clear(); cumulativeChainView.clear()
        lastSnapshotAt = 0L; totalEvents = 0; pendingSnapshot = null; cachedAppContext = null
    }

    @JvmStatic fun debugTotalEvents(): Int = synchronized(lock) { totalEvents }

    @JvmStatic fun record(chain: String?, viewClass: String?, context: Context?) {
        if (!DpisLog.isLoggingEnabled() || chain.isNullOrEmpty()) return
        recordInternal(chain, viewClass, context)
    }

    @JvmStatic fun recordUnit(unit: Int, viewClass: String?, context: Context?) {
        if (!DpisLog.isLoggingEnabled()) return
        recordInternal("text-size-unit-$unit", viewClass, context)
    }

    private fun recordInternal(chain: String, viewClass: String?, context: Context?) {
        val safeViewClass = viewClass.takeUnless { it.isNullOrEmpty() } ?: "unknown"
        val now = System.currentTimeMillis(); val second = now / 1000L
        val eventContext = resolveContext(context)
        var snapshotToSend: Snapshot?
        synchronized(lock) {
            totalEvents++; appendEvent(second, chain, safeViewClass); pruneOldBuckets(second)
            if (now - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) { pendingSnapshot = buildSnapshot(now, second); lastSnapshotAt = now }
            snapshotToSend = if (eventContext != null) pendingSnapshot.also { pendingSnapshot = null } else null
        }
        snapshotToSend?.let { sendSnapshot(eventContext, it) }
    }

    private fun resolveContext(context: Context?): Context? {
        cachedAppContext?.let { return it }
        val resolved = if (context != null) context.applicationContext ?: context else resolveContextUncached()
        if (resolved is Application) cachedAppContext = resolved
        return resolved
    }

    private fun resolveContextUncached(): Context? = try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val currentApplication: Method = activityThread.getDeclaredMethod("currentApplication")
        (currentApplication.invoke(null) as? Application)
    } catch (_: Throwable) { null }

    private fun appendEvent(second: Long, chain: String, viewClass: String) {
        var tail = buckets.peekLast()
        if (tail == null || tail.second != second) { tail = SecondBucket(second); buckets.addLast(tail) }
        val chainViewKey = "$chain @ $viewClass"
        increment(tail.chainCounts, chain); increment(tail.chainViewCounts, chainViewKey)
        increment(cumulativeChain, chain); increment(cumulativeChainView, chainViewKey)
    }

    private fun pruneOldBuckets(currentSecond: Long) {
        while (buckets.isNotEmpty() && currentSecond - buckets.peekFirst().second >= WINDOW_30S) buckets.removeFirst()
    }

    private fun buildSnapshot(updatedAt: Long, currentSecond: Long): Snapshot {
        val chain5s = mergeWindow(currentSecond, WINDOW_5S, true); val chain30s = mergeWindow(currentSecond, WINDOW_30S, true)
        val view5s = mergeWindow(currentSecond, WINDOW_5S, false); val view30s = mergeWindow(currentSecond, WINDOW_30S, false)
        return Snapshot(formatTopLines(chain5s), formatTopLines(chain30s), formatTopLines(cumulativeChain), formatTopLines(view5s), formatTopLines(view30s), formatTopLines(cumulativeChainView), buildUnitBreakdown(chain5s), totalEvents, updatedAt)
    }

    private fun buildUnitBreakdown(chain5s: Map<String, Int>): String {
        if (chain5s.isEmpty()) return "unit: 0=0 1=0 2=0"
        val units = IntArray(3)
        chain5s.forEach { (key, value) -> (0..2).firstOrNull { key.startsWith("text-size-unit-$it") }?.let { units[it] += value } }
        val total = units.sum(); if (total <= 0) return "unit: 0=0 1=0 2=0"
        val percentages = units.map { kotlin.math.round(it * 100f / total).toInt() }
        return String.format(Locale.US, "unit: 0=%d(%d%%) 1=%d(%d%%) 2=%d(%d%%)", units[0], percentages[0], units[1], percentages[1], units[2], percentages[2])
    }

    private fun mergeWindow(currentSecond: Long, windowSeconds: Int, chainOnly: Boolean): Map<String, Int> {
        val merged = HashMap<String, Int>()
        buckets.filter { currentSecond - it.second < windowSeconds }.forEach { bucket ->
            (if (chainOnly) bucket.chainCounts else bucket.chainViewCounts).forEach { (key, value) -> merged[key] = (merged[key] ?: 0) + value }
        }
        return merged
    }

    private fun formatTopLines(source: Map<String, Int>): String {
        if (source.isEmpty()) return FontDebugStatsSchema.NO_DATA_TEXT
        return source.entries.sortedByDescending { it.value }.take(TOP_LIMIT).joinToString("\n") { String.format(Locale.US, "%4d  %s", it.value, it.key) }
    }

    private fun sendSnapshot(context: Context?, snapshot: Snapshot) {
        if (context == null) return
        FontDebugStatsTransport.sendUpdate(context, Bundle().apply {
            putString(FontDebugStatsStore.EXTRA_CHAIN_5S, snapshot.chain5s); putString(FontDebugStatsStore.EXTRA_CHAIN_30S, snapshot.chain30s); putString(FontDebugStatsStore.EXTRA_CHAIN_ALL, snapshot.chainAll)
            putString(FontDebugStatsStore.EXTRA_CHAIN_VIEW_5S, snapshot.chainView5s); putString(FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S, snapshot.chainView30s); putString(FontDebugStatsStore.EXTRA_CHAIN_VIEW_ALL, snapshot.chainViewAll)
            putString(FontDebugStatsStore.EXTRA_UNIT_BREAKDOWN_5S, snapshot.unitBreakdown5s); putInt(FontDebugStatsStore.EXTRA_EVENT_TOTAL, snapshot.eventTotal); putLong(FontDebugStatsStore.EXTRA_UPDATED_AT, snapshot.updatedAt)
        })
    }

    private fun increment(map: MutableMap<String, Int>, key: String) { map[key] = (map[key] ?: 0) + 1 }
    private class SecondBucket(val second: Long) { val chainCounts = HashMap<String, Int>(); val chainViewCounts = HashMap<String, Int>() }
    private data class Snapshot(val chain5s: String, val chain30s: String, val chainAll: String, val chainView5s: String, val chainView30s: String, val chainViewAll: String, val unitBreakdown5s: String, val eventTotal: Int, val updatedAt: Long)
}
