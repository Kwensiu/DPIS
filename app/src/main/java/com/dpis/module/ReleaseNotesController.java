package com.dpis.module;

import java.util.concurrent.Executor;

final class ReleaseNotesController {
    interface Fetcher {
        String fetch(String versionName, int connectTimeoutMs, int readTimeoutMs) throws Exception;
    }

    interface Clock {
        long nowMs();
    }

    interface UiDispatcher {
        void post(Runnable runnable);
    }

    interface Listener {
        boolean isAlive();

        void onBody(String body);

        void onEmptyBody();

        void onFailure();
    }

    private final ReleaseNotesCacheStore cacheStore;
    private final Executor executor;
    private final UiDispatcher uiDispatcher;
    private final Fetcher fetcher;
    private final Clock clock;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    ReleaseNotesController(ReleaseNotesCacheStore cacheStore,
            Executor executor,
            UiDispatcher uiDispatcher,
            Fetcher fetcher,
            Clock clock,
            int connectTimeoutMs,
            int readTimeoutMs) {
        if (cacheStore == null || executor == null || uiDispatcher == null
                || fetcher == null || clock == null) {
            throw new IllegalArgumentException("Release notes dependencies must not be null");
        }
        this.cacheStore = cacheStore;
        this.executor = executor;
        this.uiDispatcher = uiDispatcher;
        this.fetcher = fetcher;
        this.clock = clock;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    void load(String versionName, boolean hasEmbeddedReleaseNotes, Listener listener) {
        if (listener == null) {
            return;
        }
        String cachedBody = cacheStore.getValidBody(versionName, clock.nowMs());
        if (cachedBody != null) {
            dispatchCachedBody(cachedBody, hasEmbeddedReleaseNotes, listener);
            return;
        }
        executor.execute(() -> fetchAndDispatch(versionName, hasEmbeddedReleaseNotes, listener));
    }

    private void dispatchCachedBody(String cachedBody,
            boolean hasEmbeddedReleaseNotes,
            Listener listener) {
        uiDispatcher.post(() -> {
            if (!listener.isAlive()) {
                return;
            }
            if (cachedBody.trim().isEmpty()) {
                if (!hasEmbeddedReleaseNotes) {
                    listener.onEmptyBody();
                }
            } else {
                listener.onBody(cachedBody);
            }
        });
    }

    private void fetchAndDispatch(String versionName,
            boolean hasEmbeddedReleaseNotes,
            Listener listener) {
        try {
            String body = fetcher.fetch(versionName, connectTimeoutMs, readTimeoutMs);
            if (body == null || body.trim().isEmpty()) {
                cacheStore.put(versionName, "", clock.nowMs());
                dispatchEmptyBody(hasEmbeddedReleaseNotes, listener);
                return;
            }
            cacheStore.put(versionName, body, clock.nowMs());
            dispatchBody(body, listener);
        } catch (Exception ignored) {
            dispatchFailure(hasEmbeddedReleaseNotes, listener);
        }
    }

    private void dispatchBody(String body, Listener listener) {
        uiDispatcher.post(() -> {
            if (listener.isAlive()) {
                listener.onBody(body);
            }
        });
    }

    private void dispatchEmptyBody(boolean hasEmbeddedReleaseNotes, Listener listener) {
        uiDispatcher.post(() -> {
            if (listener.isAlive() && !hasEmbeddedReleaseNotes) {
                listener.onEmptyBody();
            }
        });
    }

    private void dispatchFailure(boolean hasEmbeddedReleaseNotes, Listener listener) {
        uiDispatcher.post(() -> {
            if (listener.isAlive() && !hasEmbeddedReleaseNotes) {
                listener.onFailure();
            }
        });
    }
}
