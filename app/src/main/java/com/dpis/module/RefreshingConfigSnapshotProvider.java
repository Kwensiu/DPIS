package com.dpis.module;

import android.os.SystemClock;

import java.util.function.Supplier;

public final class RefreshingConfigSnapshotProvider implements PerAppDisplayConfigSource.SnapshotProvider {
    private static final long FAILURE_LOG_MIN_INTERVAL_MILLIS = 30_000L;

    interface Clock {
        long nowMillis();
    }

    private final Supplier<ConfigSnapshot> loader;
    private final long ttlMillis;
    private final Clock clock;
    private volatile ConfigSnapshot snapshot;
    private volatile long loadedAtMillis;
    private volatile long lastRefreshAttemptAtMillis = Long.MIN_VALUE;
    private volatile long lastFailureLoggedAtMillis;
    private volatile String lastFailureMessage;

    public RefreshingConfigSnapshotProvider(Supplier<ConfigSnapshot> loader, long ttlMillis) {
        this(loader, ttlMillis, SystemClock::elapsedRealtime);
    }

    public RefreshingConfigSnapshotProvider(Supplier<ConfigSnapshot> loader,
                                     long ttlMillis,
                                     Clock clock) {
        this.loader = loader;
        this.ttlMillis = Math.max(0L, ttlMillis);
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    @Override
    public ConfigSnapshot get() {
        long now = clock.nowMillis();
        ConfigSnapshot current = snapshot;
        if (!shouldRefresh(current, now)) {
            return current;
        }
        synchronized (this) {
            now = clock.nowMillis();
            current = snapshot;
            if (!shouldRefresh(current, now)) {
                return current;
            }
            try {
                lastRefreshAttemptAtMillis = now;
                ConfigSnapshot loaded = loader != null ? loader.get() : null;
                snapshot = loaded != null ? loaded : ConfigSnapshot.empty();
                loadedAtMillis = now;
                lastFailureMessage = null;
                return snapshot;
            } catch (Throwable throwable) {
                logFailureIfNeeded(throwable, now);
                return current != null ? current : ConfigSnapshot.empty();
            }
        }
    }

    private boolean shouldRefresh(ConfigSnapshot current, long nowMillis) {
        if (ttlMillis == 0L) {
            return true;
        }
        if (current != null) {
            return (nowMillis - loadedAtMillis) >= ttlMillis;
        }
        return lastRefreshAttemptAtMillis == Long.MIN_VALUE
                || (nowMillis - lastRefreshAttemptAtMillis) >= ttlMillis;
    }

    private void logFailureIfNeeded(Throwable throwable, long nowMillis) {
        String message = throwable.getClass().getName() + ": " + throwable.getMessage();
        if (message.equals(lastFailureMessage)
                && (nowMillis - lastFailureLoggedAtMillis) < FAILURE_LOG_MIN_INTERVAL_MILLIS) {
            return;
        }
        lastFailureMessage = message;
        lastFailureLoggedAtMillis = nowMillis;
        DpisLog.e("config snapshot refresh failed: " + message, throwable);
    }
}
