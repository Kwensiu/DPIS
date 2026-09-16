package com.dpis.module.applist.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.CatalogLabelCacheSnapshot
import com.dpis.module.applist.InstalledAppCatalogItem
import com.dpis.module.applist.InstalledAppCatalogPolicy
import com.dpis.module.applist.InstalledAppCatalogLabelStore
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import java.io.File

class InstalledAppCatalogCoordinator(
    private val host: Host,
    private val labelStore: InstalledAppCatalogLabelStore? = null,
) {
    interface Host {
        fun getPackageManager(): PackageManager

        fun getSelfPackageName(): String

        fun getCatalogLocaleTag(): String
    }

    class ContextHost(private val context: Context) : Host {
        override fun getPackageManager(): PackageManager = context.packageManager

        override fun getSelfPackageName(): String = context.packageName

        override fun getCatalogLocaleTag(): String {
            val locales = context.resources.configuration.locales
            return InstalledAppCatalogPolicy.catalogLocaleTag(
                if (locales.isEmpty) null else locales[0],
            )
        }
    }

    private val installedAppCatalogLock = Any()
    private val installedAppCatalogBuildLock = Any()

    private var installedAppCatalog: List<InstalledAppCatalogItem> = emptyList()
    private var catalogInvalidated = true

    fun invalidate() {
        synchronized(installedAppCatalogLock) {
            catalogInvalidated = true
        }
    }

    fun labelsResolved(): Boolean = synchronized(installedAppCatalogLock) {
        installedAppCatalog.isNotEmpty() && installedAppCatalog.all { it.labelResolved }
    }

    /**
     * Complete catalog with resolved labels. Target pickers keep showing one finished list.
     */
    fun loadInstalledAppCatalog(forceInstalledAppCatalogReload: Boolean): List<InstalledAppCatalogItem> {
        getInstalledAppCatalog(
            host.getPackageManager(),
            host.getSelfPackageName(),
            forceInstalledAppCatalogReload,
        )
        return resolveCatalogLabels(host.getPackageManager())
    }

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
        val catalog = getInstalledAppCatalog(
            host.getPackageManager(),
            host.getSelfPackageName(),
            forceInstalledAppCatalogReload,
        )
        val items = InstalledAppCatalogPolicy.toAppListItems(
            catalog,
            store,
            scopePackages,
            scopeKnown,
        )
        DpisLog.i(
            "app list items built: catalog=${catalog.size}, result=${items.size}, " +
                "forceReload=$forceInstalledAppCatalogReload, scopeKnown=$scopeKnown",
        )
        return items
    }

    fun resolveInstalledAppLabels(
        store: DpisConfigStore?,
        scopePackages: Set<String>?,
        scopeKnown: Boolean,
    ): List<AppListItem> {
        resolveCatalogLabels(host.getPackageManager())
        return loadInstalledApps(false, store, scopePackages, scopeKnown)
    }

    private fun getInstalledAppCatalog(
        packageManager: PackageManager,
        selfPackageName: String,
        forceReload: Boolean,
    ): List<InstalledAppCatalogItem> {
        synchronized(installedAppCatalogLock) {
            if (!forceReload && isCatalogCacheFresh()) {
                DpisLog.i("installed app catalog cache hit: size=${installedAppCatalog.size}")
                return installedAppCatalog
            }
        }
        synchronized(installedAppCatalogBuildLock) {
            synchronized(installedAppCatalogLock) {
                if (!forceReload && isCatalogCacheFresh()) {
                    return installedAppCatalog
                }
            }

            var installedPackages = queryInstalledPackages(packageManager)
            DpisLog.i(
                "installed packages query returned: size=${installedPackages.size}, " +
                    "sdk=${Build.VERSION.SDK_INT}, forceReload=$forceReload",
            )

            if (InstalledAppCatalogPolicy.shouldUseLauncherVisibilityFallback(
                    installedPackages.map { it.packageName },
                    selfPackageName,
                )
            ) {
                installedPackages = mergeLauncherPackages(packageManager, installedPackages)
                DpisLog.i("launcher visibility fallback returned: size=${installedPackages.size}")
            }

            val cachedLabels = labelStore?.load() ?: CatalogLabelCacheSnapshot.EMPTY
            val snapshot = InstalledAppCatalogPolicy.buildCatalogSnapshot(
                installedPackages,
                selfPackageName,
                cachedLabels,
                host.getCatalogLocaleTag(),
            )
            val reusedLabels = snapshot.count { it.labelResolved }
            DpisLog.i(
                "installed app catalog rebuilt: raw=${installedPackages.size}, " +
                    "catalog=${snapshot.size}, reusedLabels=$reusedLabels",
            )
            synchronized(installedAppCatalogLock) {
                installedAppCatalog = snapshot
                catalogInvalidated = false
                return installedAppCatalog
            }
        }
    }

    private fun resolveCatalogLabels(packageManager: PackageManager): List<InstalledAppCatalogItem> {
        synchronized(installedAppCatalogBuildLock) {
            val current = synchronized(installedAppCatalogLock) { installedAppCatalog }
            if (current.isEmpty() || current.all { it.labelResolved }) {
                return current
            }
            val resolved = InstalledAppCatalogPolicy.resolveCatalogItemLabels(
                current,
            ) { applicationInfo ->
                try {
                    packageManager.getApplicationLabel(applicationInfo).toString()
                } catch (_: RuntimeException) {
                    null
                }
            }
            synchronized(installedAppCatalogLock) {
                if (catalogInvalidated) {
                    return current
                }
                installedAppCatalog = resolved
            }
            persistResolvedLabels(resolved)
            return resolved
        }
    }

    private fun persistResolvedLabels(catalog: List<InstalledAppCatalogItem>) {
        val store = labelStore ?: return
        store.mergeReplace(
            host.getCatalogLocaleTag(),
            catalog.mapTo(HashSet()) { it.packageName },
            InstalledAppCatalogPolicy.persistableLabelRecords(catalog),
        )
    }

    private fun isCatalogCacheFresh(): Boolean =
        !catalogInvalidated && installedAppCatalog.isNotEmpty()

    companion object {
        @JvmStatic
        fun labelStore(context: Context): InstalledAppCatalogLabelStore {
            val filesDir = context.applicationContext?.filesDir ?: context.filesDir
            return InstalledAppCatalogLabelStore.shared(
                File(filesDir, InstalledAppCatalogLabelStore.FILE_NAME),
            )
        }

        private fun queryInstalledPackages(packageManager: PackageManager): List<PackageInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }

        private fun mergeLauncherPackages(
            packageManager: PackageManager,
            installedPackages: List<PackageInfo>,
        ): List<PackageInfo> {
            val merged = LinkedHashMap<String, PackageInfo>()
            installedPackages.forEach { packageInfo ->
                merged[packageInfo.packageName] = packageInfo
            }
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
                val application = resolveInfo.activityInfo?.applicationInfo ?: return@forEach
                if (merged.containsKey(application.packageName)) return@forEach
                merged[application.packageName] = packageInfoForLauncherFallback(
                    packageManager,
                    application,
                )
            }
            return merged.values.toList()
        }

        private fun packageInfoForLauncherFallback(
            packageManager: PackageManager,
            application: ApplicationInfo,
        ): PackageInfo {
            val queried = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        application.packageName,
                        PackageManager.PackageInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(application.packageName, 0)
                }
            }.getOrNull()
            if (queried != null) return queried
            return PackageInfo().also {
                it.packageName = application.packageName
                it.applicationInfo = application
            }
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
    }
}
