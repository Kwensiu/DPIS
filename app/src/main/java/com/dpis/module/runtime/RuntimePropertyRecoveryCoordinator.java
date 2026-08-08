package com.dpis.module.runtime;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.*;

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.viewport.ViewportPropertySyncer;

import com.dpis.module.quirks.WechatDpiPropertySyncer;

import com.dpis.module.runtime.RuntimeDebugPropertySyncer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Replays the persisted per-package runtime mirrors into system properties.
 *
 * <p>The coordinator is intentionally small and idempotent. It does not own persistence;
 * it only re-applies the current store state after process start, package replace, or
 * service reconnection.</p>
 */
public final class RuntimePropertyRecoveryCoordinator {
    private static final long RESYNC_COALESCE_DELAY_MS = 500L;
    private static final Object RESYNC_LOCK = new Object();
    private static final ScheduledExecutorService RESYNC_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "DPIS-runtime-property-recovery");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private static DpisConfigStore pendingStore;
    private static ScheduledFuture<?> pendingResync;

    private RuntimePropertyRecoveryCoordinator() {
    }

    public static void resyncConfiguredTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        synchronized (RESYNC_LOCK) {
            pendingStore = store;
            if (pendingResync != null) {
                pendingResync.cancel(false);
            }
            // App start commonly observes a local store before the LSPosed-backed store is
            // available. Publish only the latest snapshot instead of replaying both stores.
            pendingResync = RESYNC_EXECUTOR.schedule(
                    RuntimePropertyRecoveryCoordinator::resyncLatestStore,
                    RESYNC_COALESCE_DELAY_MS,
                    TimeUnit.MILLISECONDS);
        }
    }

    private static void resyncLatestStore() {
        DpisConfigStore store;
        synchronized (RESYNC_LOCK) {
            store = pendingStore;
            pendingStore = null;
            pendingResync = null;
        }
        if (store == null) {
            return;
        }
        // Keep runtime mirrors in sync; persisted config remains the source of truth.
        RuntimeDebugPropertySyncer.publishAsync(
                store.isGlobalLogEnabled(),
                store.isFontDebugOverlayEnabled());
        ViewportPropertySyncer.syncConfiguredTargetsAsync(store);
        FontRuntimePropertySyncer.syncConfiguredTargetsAsync(store);
        FontHookDomainPropertySyncer.syncConfiguredTargetsAsync(store);
        WechatDpiPropertySyncer.syncConfiguredTargetsAsync(store);
    }
}
