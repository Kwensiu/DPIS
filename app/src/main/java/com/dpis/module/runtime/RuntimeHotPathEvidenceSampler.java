package com.dpis.module.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeHotPathEvidenceSampler {
    private static final long LOG_MIN_INTERVAL_MILLIS = 2_000L;
    private static final int LOG_EVERY_HITS = 50;
    private static final int MAX_KEYS = 128;

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public RuntimeHotPathEvidenceSampler() {
    }

    public Sample sample(String key, String detail) {
        long now = RuntimeClock.elapsedRealtimeMillis();
        if (!states.containsKey(key) && states.size() >= MAX_KEYS) {
            states.clear();
        }
        State state = states.computeIfAbsent(key, ignored -> new State());
        synchronized (state) {
            state.hitCount++;
            boolean shouldLog = state.hitCount == 1
                    || now - state.lastLoggedAtMillis >= LOG_MIN_INTERVAL_MILLIS
                    || state.hitCount % LOG_EVERY_HITS == 0;
            if (!shouldLog) {
                state.suppressedSinceLastLog++;
                return Sample.skip();
            }
            long suppressedCount = state.suppressedSinceLastLog;
            state.suppressedSinceLastLog = 0L;
            state.lastLoggedAtMillis = now;
            return Sample.emit(detail
                    + ", hitCount=" + state.hitCount
                    + ", suppressedCount=" + suppressedCount);
        }
    }

    public void resetForTest() {
        states.clear();
    }

    public static final class Sample {
        public final boolean emit;
        public final String detail;

        private Sample(boolean emit, String detail) {
            this.emit = emit;
            this.detail = detail;
        }

        static Sample emit(String detail) {
            return new Sample(true, detail);
        }

        static Sample skip() {
            return new Sample(false, "");
        }
    }

    private static final class State {
        long lastLoggedAtMillis;
        long hitCount;
        long suppressedSinceLastLog;
    }
}
