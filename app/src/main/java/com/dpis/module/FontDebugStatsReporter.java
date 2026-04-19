package com.dpis.module;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FontDebugStatsReporter {
    private static final Object LOCK = new Object();
    private static final long SNAPSHOT_INTERVAL_MS = 500L;
    private static final int WINDOW_5S = 5;
    private static final int WINDOW_30S = 30;
    private static final int TOP_LIMIT = 20;

    private static final ArrayDeque<SecondBucket> BUCKETS = new ArrayDeque<>();
    private static final Map<String, Integer> CUMULATIVE_CHAIN = new HashMap<>();
    private static final Map<String, Integer> CUMULATIVE_CHAIN_VIEW = new HashMap<>();
    private static long lastSnapshotAt;
    private static int totalEvents;
    private static Snapshot pendingSnapshot;

    private FontDebugStatsReporter() {
    }

    static void record(String chain, String viewClass, Context context) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        if (chain == null || chain.isEmpty()) {
            return;
        }
        String safeViewClass = (viewClass == null || viewClass.isEmpty())
                ? "unknown"
                : viewClass;
        long now = System.currentTimeMillis();
        long second = now / 1000L;
        Context eventContext = resolveContext(context);

        Snapshot snapshotToSend = null;
        synchronized (LOCK) {
            totalEvents++;
            appendEvent(second, chain, safeViewClass);
            pruneOldBuckets(second);

            if (now - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) {
                pendingSnapshot = buildSnapshot(now, second);
                lastSnapshotAt = now;
            }
            if (eventContext != null && pendingSnapshot != null) {
                snapshotToSend = pendingSnapshot;
                pendingSnapshot = null;
            }
        }
        if (snapshotToSend != null) {
            sendSnapshot(eventContext, snapshotToSend);
        }
    }

    private static Context resolveContext(Context context) {
        if (context != null) {
            Context app = context.getApplicationContext();
            if (app != null) {
                return app;
            }
            return context;
        }
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            Object app = currentApplication.invoke(null);
            if (app instanceof Application application) {
                return application;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void appendEvent(long second, String chain, String viewClass) {
        SecondBucket tail = BUCKETS.peekLast();
        if (tail == null || tail.second != second) {
            tail = new SecondBucket(second);
            BUCKETS.addLast(tail);
        }
        String chainViewKey = toChainViewKey(chain, viewClass);
        increment(tail.chainCounts, chain);
        increment(tail.chainViewCounts, chainViewKey);
        increment(CUMULATIVE_CHAIN, chain);
        increment(CUMULATIVE_CHAIN_VIEW, chainViewKey);
    }

    private static void pruneOldBuckets(long currentSecond) {
        while (!BUCKETS.isEmpty()) {
            SecondBucket head = BUCKETS.peekFirst();
            if (head == null || currentSecond - head.second < WINDOW_30S) {
                break;
            }
            BUCKETS.removeFirst();
        }
    }

    private static Snapshot buildSnapshot(long updatedAt, long currentSecond) {
        Map<String, Integer> chain5s = mergeWindow(currentSecond, WINDOW_5S, true);
        Map<String, Integer> chain30s = mergeWindow(currentSecond, WINDOW_30S, true);
        Map<String, Integer> chainView5s = mergeWindow(currentSecond, WINDOW_5S, false);
        Map<String, Integer> chainView30s = mergeWindow(currentSecond, WINDOW_30S, false);

        return new Snapshot(
                formatTopLines(chain5s),
                formatTopLines(chain30s),
                formatTopLines(CUMULATIVE_CHAIN),
                formatTopLines(chainView5s),
                formatTopLines(chainView30s),
                formatTopLines(CUMULATIVE_CHAIN_VIEW),
                buildUnitBreakdown(chain5s),
                totalEvents,
                updatedAt
        );
    }

    private static String buildUnitBreakdown(Map<String, Integer> chain5s) {
        if (chain5s == null || chain5s.isEmpty()) {
            return "unit: 0=0 1=0 2=0";
        }
        int unit0 = 0;
        int unit1 = 0;
        int unit2 = 0;
        for (Map.Entry<String, Integer> entry : chain5s.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue() == null ? 0 : entry.getValue();
            if (key == null || value <= 0) {
                continue;
            }
            if (key.startsWith("text-size-unit-0")) {
                unit0 += value;
            } else if (key.startsWith("text-size-unit-1")) {
                unit1 += value;
            } else if (key.startsWith("text-size-unit-2")) {
                unit2 += value;
            }
        }
        int total = unit0 + unit1 + unit2;
        if (total <= 0) {
            return "unit: 0=0 1=0 2=0";
        }
        int p0 = Math.round((unit0 * 100f) / total);
        int p1 = Math.round((unit1 * 100f) / total);
        int p2 = Math.round((unit2 * 100f) / total);
        return String.format(Locale.US,
                "unit: 0=%d(%d%%) 1=%d(%d%%) 2=%d(%d%%)",
                unit0, p0, unit1, p1, unit2, p2);
    }

    private static Map<String, Integer> mergeWindow(long currentSecond,
                                                    int windowSeconds,
                                                    boolean chainOnly) {
        Map<String, Integer> merged = new HashMap<>();
        for (SecondBucket bucket : BUCKETS) {
            if (currentSecond - bucket.second >= windowSeconds) {
                continue;
            }
            Map<String, Integer> source = chainOnly ? bucket.chainCounts : bucket.chainViewCounts;
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                incrementBy(merged, entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    private static String formatTopLines(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return "暂无数据";
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(source.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getValue(), left.getValue()));

        StringBuilder builder = new StringBuilder();
        int limit = Math.min(TOP_LIMIT, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            builder.append(String.format(Locale.US, "%4d  %s", entry.getValue(), entry.getKey()));
            if (i < limit - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private static void sendSnapshot(Context context, Snapshot snapshot) {
        if (context == null || snapshot == null) {
            return;
        }
        Intent intent = new Intent(FontDebugStatsStore.ACTION_STATS_UPDATE);
        intent.setPackage("com.dpis.module");
        intent.putExtra(FontDebugStatsStore.EXTRA_CHAIN_5S, snapshot.chain5s);
        intent.putExtra(FontDebugStatsStore.EXTRA_CHAIN_30S, snapshot.chain30s);
        intent.putExtra(FontDebugStatsStore.EXTRA_CHAIN_ALL, snapshot.chainAll);
        intent.putExtra(FontDebugStatsStore.EXTRA_CHAIN_VIEW_5S, snapshot.chainView5s);
        intent.putExtra(FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S, snapshot.chainView30s);
        intent.putExtra(FontDebugStatsStore.EXTRA_CHAIN_VIEW_ALL, snapshot.chainViewAll);
        intent.putExtra(FontDebugStatsStore.EXTRA_UNIT_BREAKDOWN_5S, snapshot.unitBreakdown5s);
        intent.putExtra(FontDebugStatsStore.EXTRA_EVENT_TOTAL, snapshot.eventTotal);
        intent.putExtra(FontDebugStatsStore.EXTRA_UPDATED_AT, snapshot.updatedAt);
        try {
            context.sendBroadcast(intent);
        } catch (Throwable ignored) {
        }
    }

    private static void increment(Map<String, Integer> map, String key) {
        incrementBy(map, key, 1);
    }

    private static void incrementBy(Map<String, Integer> map, String key, int delta) {
        Integer old = map.get(key);
        map.put(key, (old == null ? 0 : old) + delta);
    }

    private static String toChainViewKey(String chain, String viewClass) {
        return chain + " @ " + viewClass;
    }

    private static final class SecondBucket {
        final long second;
        final Map<String, Integer> chainCounts = new HashMap<>();
        final Map<String, Integer> chainViewCounts = new HashMap<>();

        SecondBucket(long second) {
            this.second = second;
        }
    }

    private static final class Snapshot {
        final String chain5s;
        final String chain30s;
        final String chainAll;
        final String chainView5s;
        final String chainView30s;
        final String chainViewAll;
        final String unitBreakdown5s;
        final int eventTotal;
        final long updatedAt;

        Snapshot(String chain5s,
                 String chain30s,
                 String chainAll,
                 String chainView5s,
                 String chainView30s,
                 String chainViewAll,
                 String unitBreakdown5s,
                 int eventTotal,
                 long updatedAt) {
            this.chain5s = chain5s;
            this.chain30s = chain30s;
            this.chainAll = chainAll;
            this.chainView5s = chainView5s;
            this.chainView30s = chainView30s;
            this.chainViewAll = chainViewAll;
            this.unitBreakdown5s = unitBreakdown5s;
            this.eventTotal = eventTotal;
            this.updatedAt = updatedAt;
        }
    }
}
