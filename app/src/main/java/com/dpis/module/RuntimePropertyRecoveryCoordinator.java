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
        // Keep the runtime mirrors in sync with the persisted store. Boot/package events
        // are best-effort triggers; the actual source of truth remains the stored config.
        store.migrateWechatViewportToTargetFieldIfNeeded();
        RuntimeDebugPropertySyncer.publishAsync(
                store.isGlobalLogEnabled(),
                store.isFontDebugOverlayEnabled());
        ViewportPropertySyncer.syncConfiguredTargetsAsync(store);
        FontRuntimePropertySyncer.syncConfiguredTargetsAsync(store);
        FontHookDomainPropertySyncer.syncConfiguredTargetsAsync(store);
        WechatTargetFieldPropertySyncer.syncConfiguredTargetsAsync(store);
    }
}
