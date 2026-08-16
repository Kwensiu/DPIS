package com.dpis.module.runtime.systemserver;

import com.dpis.module.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SystemServerHookLogGate {
    private static final int MAX_LOG_CACHE_ENTRIES = 2048;
    private static final ConcurrentMap<String, String> LAST_PROBE_LOGS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> LAST_PROBE_LOG_TIMESTAMPS =
            new ConcurrentHashMap<>();
    private static final long HOT_ENTRY_LOG_MIN_INTERVAL_MS = 1200L;
    private static final long CORE_ENTRY_LOG_MIN_INTERVAL_MS = 800L;
    private static final long DEFAULT_LOG_MIN_INTERVAL_MS = 400L;

    private SystemServerHookLogGate() {
    }

    static boolean logIfChanged(String key, String message, long minIntervalMs) {
        long nowMs = System.currentTimeMillis();
        String previous = LAST_PROBE_LOGS.get(key);
        Long lastLogMs = LAST_PROBE_LOG_TIMESTAMPS.get(key);
        if (!shouldEmitLog(previous, message, nowMs, lastLogMs, minIntervalMs)) {
            return false;
        }
        LAST_PROBE_LOGS.put(key, message);
        LAST_PROBE_LOG_TIMESTAMPS.put(key, nowMs);
        trimCachesIfNeeded();
        DpisLog.i(message);
        return true;
    }

    static long resolveLogMinIntervalMs(String entryName) {
        if (SystemServerEntryRoute.isHotEntry(entryName)) {
            return HOT_ENTRY_LOG_MIN_INTERVAL_MS;
        }
        if (SystemServerEntryRoute.isCoreLogEntry(entryName)) {
            return CORE_ENTRY_LOG_MIN_INTERVAL_MS;
        }
        return DEFAULT_LOG_MIN_INTERVAL_MS;
    }

    static boolean shouldEmitLog(String previousMessage,
                                 String currentMessage,
                                 long nowMs,
                                 Long lastLogMs,
                                 long minIntervalMs) {
        if (Objects.equals(previousMessage, currentMessage)) {
            return false;
        }
        if (minIntervalMs <= 0L) {
            return true;
        }
        if (lastLogMs == null) {
            return true;
        }
        return nowMs - lastLogMs >= minIntervalMs;
    }

    static boolean shouldLogInterceptEnter(String entryName) {
        return !SystemServerEntryRoute.isHotEntry(entryName);
    }

    private static void trimCachesIfNeeded() {
        if (LAST_PROBE_LOGS.size() <= MAX_LOG_CACHE_ENTRIES
                && LAST_PROBE_LOG_TIMESTAMPS.size() <= MAX_LOG_CACHE_ENTRIES) {
            return;
        }
        LAST_PROBE_LOGS.clear();
        LAST_PROBE_LOG_TIMESTAMPS.clear();
    }
}
