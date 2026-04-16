package com.dpis.module;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class SystemServerHookLogGate {
    private static final ConcurrentMap<String, String> LAST_PROBE_LOGS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> LAST_PROBE_LOG_TIMESTAMPS =
            new ConcurrentHashMap<>();
    private static final long HOT_ENTRY_LOG_MIN_INTERVAL_MS = 1200L;
    private static final long DEFAULT_LOG_MIN_INTERVAL_MS = 0L;

    private SystemServerHookLogGate() {
    }

    static void logIfChanged(String key, String message, long minIntervalMs) {
        long nowMs = System.currentTimeMillis();
        String previous = LAST_PROBE_LOGS.get(key);
        Long lastLogMs = LAST_PROBE_LOG_TIMESTAMPS.get(key);
        if (!shouldEmitLog(previous, message, nowMs, lastLogMs, minIntervalMs)) {
            return;
        }
        LAST_PROBE_LOGS.put(key, message);
        LAST_PROBE_LOG_TIMESTAMPS.put(key, nowMs);
        DpisLog.i(message);
    }

    static long resolveLogMinIntervalMs(String entryName) {
        if (isHotEntry(entryName)) {
            return HOT_ENTRY_LOG_MIN_INTERVAL_MS;
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
        return !"display-policy-layout".equals(entryName)
                && !"relayout-dispatch".equals(entryName);
    }

    static boolean isHotEntry(String entryName) {
        return "display-policy-layout".equals(entryName)
                || "relayout-dispatch".equals(entryName);
    }
}
