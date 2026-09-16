package com.dpis.module.applist

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

class InstalledAppCatalogItem(
    @JvmField val label: String,
    @JvmField val packageName: String,
    @JvmField val systemApp: Boolean,
    @JvmField val hyperOsNativeProxyCandidate: Boolean,
    internal val applicationInfo: ApplicationInfo,
    @JvmField @Volatile var icon: Drawable?,
    @JvmField val firstInstallTime: Long,
    @JvmField val lastUpdateTime: Long,
    @JvmField val labelResolved: Boolean = false,
) {
    fun withResolvedLabel(label: String): InstalledAppCatalogItem =
        InstalledAppCatalogItem(
            label,
            packageName,
            systemApp,
            hyperOsNativeProxyCandidate,
            applicationInfo,
            icon,
            firstInstallTime,
            lastUpdateTime,
            labelResolved = true,
        )
}
