package com.dpis.module.config

import com.dpis.module.viewport.ViewportTargetSpec

interface ConfigSnapshotStore {
    fun getConfiguredPackages(): MutableSet<String?>
    fun isTargetDpisEnabled(packageName: String): Boolean
    fun getTargetViewportSpec(packageName: String): ViewportTargetSpec
    fun getTargetViewportApplyMode(packageName: String): String?
    fun getTargetFontScalePercent(packageName: String): Int?
    fun getTargetFontApplyMode(packageName: String): String?
    fun getTargetTypefaceId(packageName: String): String?
    fun getPackageFontHookDomainsRaw(packageName: String?): String?
    fun isSystemServerHooksEnabled(): Boolean
    fun isSystemServerSafeModeEnabled(): Boolean
    fun isGlobalLogEnabled(): Boolean
    fun hasSystemServerHooksEnabled(): Boolean
    fun hasSystemServerSafeModeEnabled(): Boolean
    fun hasGlobalLogEnabled(): Boolean
}
