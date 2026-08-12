package com.dpis.module.applist

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.settings.SystemFrameworkScope
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import java.util.Locale

class InstalledAppCatalogCoordinator(
    private val host: Host,
    private val installedAppCatalogTtlMs: Long,
) {
    interface Host {
        fun getPackageManager(): PackageManager

        fun getSelfPackageName(): String
    }

    private val installedAppCatalogLock = Any()
    private val installedAppCatalogBuildLock = Any()

    private var installedAppCatalog: List<InstalledAppCatalogItem> = emptyList()
    private var installedAppCatalogLoadedAtMs = 0L

    /** Kept for legacy View binders that still notify visible icon rows. */
    fun onIconLoadRequested(packageName: String) = Unit

    /** No executor remains after moving icon resolution into the catalog build. */
    fun shutdown() = Unit

    fun loadInstalledAppCatalog(forceInstalledAppCatalogReload: Boolean): List<InstalledAppCatalogItem> =
        getInstalledAppCatalog(
            host.getPackageManager(),
            host.getSelfPackageName(),
            forceInstalledAppCatalogReload,
        )

    /**
     * Legacy target pickers display the full list at once. Keep their rows complete while
     * the Compose app workspace resolves icons only for the page it is about to show.
     */
    fun loadInstalledAppCatalogWithIcons(
        forceInstalledAppCatalogReload: Boolean,
    ): List<InstalledAppCatalogItem> {
        val catalog = loadInstalledAppCatalog(forceInstalledAppCatalogReload)
        val packageManager = host.getPackageManager()
        catalog.forEach { loadItemIcon(packageManager, it) }
        return catalog
    }

    fun loadInstalledApps(
        forceInstalledAppCatalogReload: Boolean,
        store: DpisConfigStore?,
        scopePackages: Set<String>?,
        scopeKnown: Boolean,
    ): List<AppListItem> {
        val catalog = loadInstalledAppCatalog(forceInstalledAppCatalogReload)
        val configuredPackages = userVisibleConfiguredPackages(
            store,
            scopePackages,
            scopeKnown,
        )
        val result = ArrayList<AppListItem>(catalog.size)
        for (item in catalog) {
            val inScope = scopePackages?.contains(item.packageName) == true
            if (item.packageName in configuredPackages) {
                // Configured rows retain compatibility-aware values used when reopening editors.
                result += createAppListItem(
                    store,
                    scopePackages,
                    scopeKnown,
                    item.label,
                    item.packageName,
                    item.systemApp,
                    item.hyperOsNativeProxyCandidate,
                    true,
                    null,
                )
            } else {
                result += createUnconfiguredAppListItem(
                    item.label,
                    item.packageName,
                    inScope,
                    scopeKnown,
                    item.systemApp,
                    item.hyperOsNativeProxyCandidate,
                    true,
                )
            }
        }
        for (packageName in configuredPackagesMissingFromCatalog(configuredPackages, catalog)) {
            result += createAppListItem(
                store,
                scopePackages,
                scopeKnown,
                packageName,
                packageName,
                false,
                false,
                false,
                null,
            )
        }
        DpisLog.i(
            "app list items built: catalog=${catalog.size}, result=${result.size}, " +
                "forceReload=$forceInstalledAppCatalogReload, scopeKnown=$scopeKnown",
        )
        return result
    }

    private fun getInstalledAppCatalog(
        packageManager: PackageManager,
        selfPackageName: String,
        forceReload: Boolean,
    ): List<InstalledAppCatalogItem> {
        var now = SystemClock.elapsedRealtime()
        synchronized(installedAppCatalogLock) {
            if (!forceReload && isCatalogCacheFresh(now)) {
                DpisLog.i("installed app catalog cache hit: size=${installedAppCatalog.size}")
                return installedAppCatalog
            }
        }
        synchronized(installedAppCatalogBuildLock) {
            now = SystemClock.elapsedRealtime()
            synchronized(installedAppCatalogLock) {
                if (!forceReload && isCatalogCacheFresh(now)) {
                    return installedAppCatalog
                }
            }

            var installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
            DpisLog.i(
                "installed applications query returned: size=${installedApps.size}, " +
                    "sdk=${Build.VERSION.SDK_INT}, forceReload=$forceReload",
            )

            if (shouldUseLauncherVisibilityFallback(installedApps, selfPackageName)) {
                installedApps = mergeLauncherApplications(packageManager, installedApps)
                DpisLog.i("launcher visibility fallback returned: size=${installedApps.size}")
            }

            val snapshot = installedApps
                .asSequence()
                .filter { it.packageName != selfPackageName }
                .map { applicationInfo ->
                    InstalledAppCatalogItem(
                        packageManager.getApplicationLabel(applicationInfo).toString(),
                        applicationInfo.packageName,
                        isSystemApp(applicationInfo),
                        false,
                        applicationInfo,
                        null,
                    )
                }
                .sortedWith(
                    compareBy<InstalledAppCatalogItem> { it.label.lowercase(Locale.ROOT) }
                        .thenBy { it.packageName },
                )
                .toList()
            DpisLog.i(
                "installed app catalog rebuilt: raw=${installedApps.size}, catalog=${snapshot.size}",
            )
            synchronized(installedAppCatalogLock) {
                installedAppCatalog = snapshot
                installedAppCatalogLoadedAtMs = now
                return installedAppCatalog
            }
        }
    }

    private fun isCatalogCacheFresh(now: Long): Boolean =
        installedAppCatalog.isNotEmpty() &&
            now - installedAppCatalogLoadedAtMs <= installedAppCatalogTtlMs

    companion object {
        private fun configuredPackagesMissingFromCatalog(
            configuredPackages: Set<String>,
            catalog: List<InstalledAppCatalogItem>,
        ): List<String> {
            if (configuredPackages.isEmpty()) return emptyList()
            val installedPackages = catalog.mapTo(HashSet()) { it.packageName }
            return configuredPackages.filterNotTo(ArrayList()) { it in installedPackages }.sorted()
        }

        /**
         * Validates only candidates recovered from persistent state, avoiding store reads for
         * every ordinary installed application.
         */
        @JvmStatic
        fun userVisibleConfiguredPackages(store: DpisConfigStore?): Set<String> {
            if (store == null) return emptySet()
            return store.configuredPackages
                .filterTo(HashSet()) {
                    !SystemFrameworkScope.isFrameworkScopePackage(it) &&
                        store.hasUserVisiblePackageConfig(it)
                }
        }

        /**
         * Keeps scope-only packages aligned with the home configured-app count.
         * A known scope is user-visible configured state even when no package
         * field has been persisted yet.
         */
        @JvmStatic
        fun userVisibleConfiguredPackages(
            store: DpisConfigStore?,
            scopePackages: Set<String>?,
            scopeKnown: Boolean,
        ): Set<String> {
            val configured = userVisibleConfiguredPackages(store).toMutableSet()
            if (scopeKnown) {
                scopePackages.orEmpty()
                    .filterNotTo(configured) { SystemFrameworkScope.isFrameworkScopePackage(it) }
            }
            return configured
        }

        /** An unconfigured row has no persisted package state to decode. */
        @JvmStatic
        fun createUnconfiguredAppListItem(
            label: String,
            packageName: String,
            inScope: Boolean,
            scopeKnown: Boolean,
            systemApp: Boolean,
            hyperOsNativeProxyCandidate: Boolean,
            installed: Boolean,
        ): AppListItem = AppListItem(
            label,
            packageName,
            inScope,
            scopeKnown,
            null,
            null,
            ViewportApplyMode.OFF,
            ViewportTargetType.OFF,
            ViewportTargetSpec.off(),
            null,
            FontApplyMode.OFF,
            null,
            false,
            null,
            true,
            isUserVisibleConfiguredPackage(
                null,
                packageName,
                scopeKnown,
                inScope,
            ),
            installed,
            systemApp,
            hyperOsNativeProxyCandidate,
            null,
        )

        @JvmStatic
        fun createAppListItem(
            store: DpisConfigStore?,
            scopePackages: Set<String>?,
            scopeKnown: Boolean,
            label: String,
            packageName: String,
            systemApp: Boolean,
            hyperOsNativeProxyCandidate: Boolean,
            installed: Boolean,
            icon: Drawable?,
        ): AppListItem {
            val viewportWidth = store?.getTargetViewportWidthDp(packageName)
            val viewportScale = store?.getTargetViewportScaleMilliPercent(packageName)
            val targetSpec = store?.getTargetViewportSpec(packageName) ?: ViewportTargetSpec.off()
            val targetType = store?.getTargetViewportType(packageName) ?: ViewportTargetType.OFF
            val viewportMode = store?.getTargetViewportApplyMode(packageName) ?: ViewportApplyMode.OFF
            val fontScale = store?.getTargetFontScalePercent(packageName)
            val fontMode = store?.getTargetFontApplyMode(packageName) ?: FontApplyMode.OFF
            val typefaceId = store?.getTargetTypefaceId(packageName)
            val appSpecificConfigActive = store?.hasTargetAppSpecificConfig(packageName) == true
            val wechatDpi = store?.getWechatDpi(packageName)
            val dpisEnabled = store?.isTargetDpisEnabled(packageName) != false
            val inScope = scopePackages?.contains(packageName) == true
            val configured = isUserVisibleConfiguredPackage(
                store,
                packageName,
                scopeKnown,
                inScope,
            )
            return AppListItem(
                label,
                packageName,
                inScope,
                scopeKnown,
                viewportWidth,
                viewportScale,
                viewportMode,
                targetType,
                targetSpec,
                fontScale,
                fontMode,
                typefaceId,
                appSpecificConfigActive,
                wechatDpi,
                dpisEnabled,
                configured,
                installed,
                systemApp,
                hyperOsNativeProxyCandidate,
                icon,
            )
        }

        @JvmStatic
        fun isUserVisibleConfiguredPackage(
            store: DpisConfigStore?,
            packageName: String,
            scopeKnown: Boolean,
            inScope: Boolean,
        ): Boolean =
            !SystemFrameworkScope.isFrameworkScopePackage(packageName) &&
                ((scopeKnown && inScope) || store?.hasUserVisiblePackageConfig(packageName) == true)

        @JvmStatic
        fun shouldUseLauncherVisibilityFallback(
            applications: List<ApplicationInfo>?,
            selfPackageName: String,
        ): Boolean = applications.isNullOrEmpty() || applications.none {
            it.packageName != selfPackageName
        }

        private fun mergeLauncherApplications(
            packageManager: PackageManager,
            installedApplications: List<ApplicationInfo>,
        ): List<ApplicationInfo> {
            val merged = LinkedHashMap<String, ApplicationInfo>()
            installedApplications.forEach { merged[it.packageName] = it }
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launcherActivities: List<ResolveInfo> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        launcherIntent,
                        PackageManager.ResolveInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(launcherIntent, 0)
                }
            launcherActivities.forEach { resolveInfo ->
                resolveInfo.activityInfo?.applicationInfo?.let { application ->
                    merged[application.packageName] = application
                }
            }
            return merged.values.toList()
        }

        private fun loadApplicationIcon(
            packageManager: PackageManager,
            applicationInfo: ApplicationInfo,
        ): Drawable? = try {
            applicationInfo.loadIcon(packageManager)
        } catch (_: RuntimeException) {
            null
        }

        private fun loadItemIcon(
            packageManager: PackageManager,
            item: InstalledAppCatalogItem,
        ): Drawable? {
            item.icon?.let { return it }
            return loadApplicationIcon(packageManager, item.applicationInfo)?.also { item.icon = it }
        }

        private fun isSystemApp(applicationInfo: ApplicationInfo): Boolean =
            applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
    }
}
