package com.dpis.module.runtime.systemserver.diagnostics

import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.systemserver.hook.EntryRoute
import java.util.concurrent.ConcurrentHashMap

object HookLogGate {
    private const val MAX_LOG_CACHE_ENTRIES = 2048
    private val LAST_PROBE_LOGS = ConcurrentHashMap<String, String>()
    private val LAST_PROBE_LOG_TIMESTAMPS = ConcurrentHashMap<String, Long>()
    private const val HOT_ENTRY_LOG_MIN_INTERVAL_MS = 1200L
    private const val CORE_ENTRY_LOG_MIN_INTERVAL_MS = 800L
    private const val DEFAULT_LOG_MIN_INTERVAL_MS = 400L

    @JvmStatic
    fun logIfChanged(key: String?, message: String?, minIntervalMs: Long): Boolean {
        val nowMs = System.currentTimeMillis()
        val lookupKey = key!!
        val previous = LAST_PROBE_LOGS[lookupKey]
        val lastLogMs = LAST_PROBE_LOG_TIMESTAMPS[lookupKey]
        if (!shouldEmitLog(previous, message, nowMs, lastLogMs, minIntervalMs)) {
            return false
        }
        LAST_PROBE_LOGS[lookupKey] = message!!
        LAST_PROBE_LOG_TIMESTAMPS[lookupKey] = nowMs
        trimCachesIfNeeded()
        DpisLog.i(message)
        return true
    }

    @JvmStatic
    fun resolveLogMinIntervalMs(entryName: String?): Long {
        if (EntryRoute.isHotEntry(entryName)) {
            return HOT_ENTRY_LOG_MIN_INTERVAL_MS
        }
        if (EntryRoute.isCoreLogEntry(entryName)) {
            return CORE_ENTRY_LOG_MIN_INTERVAL_MS
        }
        return DEFAULT_LOG_MIN_INTERVAL_MS
    }

    @JvmStatic
    fun shouldEmitLog(
        previousMessage: String?,
        currentMessage: String?,
        nowMs: Long,
        lastLogMs: Long?,
        minIntervalMs: Long,
    ): Boolean {
        if (previousMessage == currentMessage) {
            return false
        }
        if (minIntervalMs <= 0L) {
            return true
        }
        if (lastLogMs == null) {
            return true
        }
        return nowMs - lastLogMs >= minIntervalMs
    }

    @JvmStatic
    fun shouldLogInterceptEnter(entryName: String?): Boolean = !EntryRoute.isHotEntry(entryName)

    private fun trimCachesIfNeeded() {
        if (LAST_PROBE_LOGS.size <= MAX_LOG_CACHE_ENTRIES &&
            LAST_PROBE_LOG_TIMESTAMPS.size <= MAX_LOG_CACHE_ENTRIES
        ) {
            return
        }
        LAST_PROBE_LOGS.clear()
        LAST_PROBE_LOG_TIMESTAMPS.clear()
    }
}
