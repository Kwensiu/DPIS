package com.dpis.module;

/**
 * Replays the persisted per-package runtime mirrors into system properties.
 *
 * <p>The coordinator is intentionally small and idempotent. It does not own persistence;
 * it only re-applies the current store state after process start, package replace, or
 * service reconnection.</p>
 */
final class RuntimePropertyRecoveryCoordinator {
    private RuntimePropertyRecoveryCoordinator() {
    }

    static void resyncConfiguredTargetsAsync(DpiConfigStore store) {
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
