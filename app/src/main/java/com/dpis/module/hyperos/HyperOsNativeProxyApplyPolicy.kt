package com.dpis.module.hyperos

import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore

/**
 * Decides when HyperOS native-proxy bind mounts should apply, roll back, or run before restart.
 *
 * Candidate detection may use the list-item flag or a live metadata lookup. Active config is
 * stored viewport, font scale, or WeChat DPI — not typeface id or list-item font fields.
 */
object HyperOsNativeProxyApplyPolicy {
    fun isCandidate(
        item: AppListItem?,
        metadataLookup: (String) -> Boolean,
    ): Boolean {
        if (item == null) return false
        return item.hyperOsNativeProxyCandidate || metadataLookup(item.packageName)
    }

    fun hasActiveStoredConfig(store: DpisConfigStore, packageName: String): Boolean {
        val viewportTargetSpec = store.getTargetViewportSpec(packageName)
        val fontScalePercent = store.getTargetFontScalePercent(packageName)
        return viewportTargetSpec.isEnabled ||
            fontScalePercent != null ||
            store.hasTargetAppSpecificConfig(packageName)
    }

    fun shouldApply(store: DpisConfigStore?, packageName: String?): Boolean {
        if (store == null || packageName.isNullOrBlank()) return false
        return store.isTargetDpisEnabled(packageName) &&
            hasActiveStoredConfig(store, packageName)
    }

    fun shouldPrepareForRestart(
        item: AppListItem?,
        store: DpisConfigStore?,
        metadataLookup: (String) -> Boolean,
    ): Boolean {
        val current = item ?: return false
        return isCandidate(current, metadataLookup) &&
            shouldApply(store, current.packageName)
    }
}
