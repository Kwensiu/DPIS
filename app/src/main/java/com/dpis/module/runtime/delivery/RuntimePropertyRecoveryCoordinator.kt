package com.dpis.module.runtime.delivery

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.quirks.WechatDpiPropertySyncer
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.viewport.ViewportPropertySyncer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Replays the persisted per-package runtime mirrors into system properties.
 *
 * The coordinator is intentionally small and idempotent. It does not own persistence;
 * it only re-applies the current store state after process start, package replace, or
 * service reconnection.
 */
object RuntimePropertyRecoveryCoordinator {
    private const val RESYNC_COALESCE_DELAY_MS = 500L
    private val resyncLock = Any()
    private val resyncExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "DPIS-runtime-property-recovery").apply {
                isDaemon = true
            }
        }

    private var pendingStore: DpisConfigStore? = null
    private var pendingResync: ScheduledFuture<*>? = null

    @JvmStatic
    fun resyncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) {
            return
        }
        synchronized(resyncLock) {
            pendingStore = store
            pendingResync?.cancel(false)
            // App start commonly observes a local store before the LSPosed-backed store is
            // available. Publish only the latest snapshot instead of replaying both stores.
            pendingResync = resyncExecutor.schedule(
                ::resyncLatestStore,
                RESYNC_COALESCE_DELAY_MS,
                TimeUnit.MILLISECONDS,
            )
        }
    }

    private fun resyncLatestStore() {
        val store = synchronized(resyncLock) {
            pendingStore.also {
                pendingStore = null
                pendingResync = null
            }
        } ?: return

        // Keep runtime mirrors in sync; persisted config remains the source of truth.
        RuntimeDebugPropertySyncer.publishAsync(
            store.isGlobalLogEnabled(),
            store.isFontDebugOverlayEnabled,
        )
        ViewportPropertySyncer.syncConfiguredTargetsAsync(store)
        FontRuntimePropertySyncer.syncConfiguredTargetsAsync(store)
        FontHookDomainPropertySyncer.syncConfiguredTargetsAsync(store)
        WechatDpiPropertySyncer.syncConfiguredTargetsAsync(store)
    }
}
