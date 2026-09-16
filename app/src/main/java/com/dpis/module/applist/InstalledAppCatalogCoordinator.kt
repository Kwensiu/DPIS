package com.dpis.module.applist

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.hooks.SystemFrameworkScope
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import java.util.Locale

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
            return catalogLocaleTag(if (locales.isEmpty) null else locales[0])
        }
    }

    private val installedAppCatalogLock = Any()
    private val installedAppCatalogBuildLock = Any()

    private var installedAppCatalog: List<InstalledAppCatalogItem> = emptyList()
    private var catalogInvalidated = true

    /** Kept for legacy View binders that still notify visible icon rows. */
    fun onIconLoadRequested(packageName: String) = Unit

    /** No executor remains after moving icon resolution into the catalog build. */
    fun shutdown() = Unit

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
        return toAppListItems(catalog, store, scopePackages, scopeKnown, forceInstalledAppCatalogReload)
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

            if (shouldUseLauncherVisibilityFallback(
                    installedPackages.map { it.packageName },
                    selfPackageName,
                )
            ) {
                installedPackages = mergeLauncherPackages(packageManager, installedPackages)
                DpisLog.i("launcher visibility fallback returned: size=${installedPackages.size}")
            }

            val cachedLabels = labelStore?.load() ?: CatalogLabelCacheSnapshot.EMPTY
            val snapshot = buildCatalogSnapshot(
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
            val resolved = resolveCatalogItemLabels(
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
        val localeTag = host.getCatalogLocaleTag()
        val merged = mergePersistedLabelRecords(
            store.load(),
            localeTag,
            catalog.mapTo(HashSet()) { it.packageName },
            persistableLabelRecords(catalog),
        )
        store.replace(localeTag, merged)
    }

    private fun isCatalogCacheFresh(): Boolean =
        !catalogInvalidated && installedAppCatalog.isNotEmpty()

    private fun toAppListItems(
        catalog: List<InstalledAppCatalogItem>,
        store: DpisConfigStore?,
        scopePackages: Set<String>?,
        scopeKnown: Boolean,
        forceInstalledAppCatalogReload: Boolean,
    ): List<AppListItem> {
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
                val listItem = createAppListItem(
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
                listItem.firstInstallTime = item.firstInstallTime
                listItem.lastUpdateTime = item.lastUpdateTime
                result += listItem
            } else {
                val listItem = createUnconfiguredAppListItem(
                    item.label,
                    item.packageName,
                    inScope,
                    scopeKnown,
                    item.systemApp,
                    item.hyperOsNativeProxyCandidate,
                    true,
                )
                listItem.firstInstallTime = item.firstInstallTime
                listItem.lastUpdateTime = item.lastUpdateTime
                result += listItem
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

    companion object {
        @JvmStatic
        fun unresolvedCatalogLabel(applicationInfo: ApplicationInfo?, packageName: String): String {
            val nonLocalized = applicationInfo?.nonLocalizedLabel?.toString()?.trim()
            return if (!nonLocalized.isNullOrEmpty()) nonLocalized else packageName
        }

        @JvmStatic
        fun catalogLocaleTag(locale: Locale?): String =
            (locale ?: Locale.getDefault()).toLanguageTag()

        @JvmStatic
        fun buildCatalogSnapshot(
            packages: List<PackageInfo>,
            selfPackageName: String,
            cachedLabels: CatalogLabelCacheSnapshot,
            localeTag: String,
        ): List<InstalledAppCatalogItem> =
            packages.mapNotNull { packageInfo ->
                val unresolved = unresolvedCatalogLabel(
                    packageInfo.applicationInfo,
                    packageInfo.packageName,
                )
                val cached = cachedLabels.resolvedLabel(
                    localeTag,
                    packageInfo.packageName,
                    packageInfo.lastUpdateTime,
                )
                createCatalogItem(
                    packageInfo,
                    selfPackageName,
                    cached ?: unresolved,
                    labelResolved = cached != null,
                )
            }.let(::sortedCatalog)

        @JvmStatic
        fun resolveCatalogItemLabels(
            items: List<InstalledAppCatalogItem>,
            loadLabel: (ApplicationInfo) -> String?,
        ): List<InstalledAppCatalogItem> =
            items.map { item ->
                if (item.labelResolved) {
                    item
                } else {
                    val label = loadLabel(item.applicationInfo)?.trim().orEmpty()
                    if (label.isEmpty()) item else item.withResolvedLabel(label)
                }
            }.let(::sortedCatalog)

        private fun sortedCatalog(items: List<InstalledAppCatalogItem>): List<InstalledAppCatalogItem> =
            items.sortedWith(
                compareBy<InstalledAppCatalogItem> { it.label.lowercase(Locale.ROOT) }
                    .thenBy { it.packageName },
            )

        @JvmStatic
        fun persistableLabelRecords(
            catalog: List<InstalledAppCatalogItem>,
        ): Map<String, CatalogLabelRecord> {
            val records = LinkedHashMap<String, CatalogLabelRecord>()
            for (item in catalog) {
                if (!item.labelResolved) continue
                val label = item.label.trim()
                if (label.isEmpty()) continue
                records[item.packageName] = CatalogLabelRecord(label, item.lastUpdateTime)
            }
            return records
        }

        @JvmStatic
        fun mergePersistedLabelRecords(
            existing: CatalogLabelCacheSnapshot,
            localeTag: String,
            installedPackages: Set<String>,
            updates: Map<String, CatalogLabelRecord>,
        ): Map<String, CatalogLabelRecord> {
            val merged = LinkedHashMap<String, CatalogLabelRecord>()
            if (existing.localeTag == localeTag) {
                existing.records.forEach { (packageName, record) ->
                    if (packageName in installedPackages) {
                        merged[packageName] = record
                    }
                }
            }
            updates.forEach { (packageName, record) ->
                if (packageName in installedPackages) {
                    merged[packageName] = record
                }
            }
            return merged
        }

        @JvmStatic
        fun createCatalogItem(
            packageInfo: PackageInfo,
            selfPackageName: String,
            label: String,
            labelResolved: Boolean,
        ): InstalledAppCatalogItem? {
            val packageName = packageInfo.packageName
            if (packageName.isBlank() || packageName == selfPackageName) return null
            val applicationInfo = packageInfo.applicationInfo
                ?: ApplicationInfo().also { it.packageName = packageName }
            return InstalledAppCatalogItem(
                label,
                packageName,
                isSystemApp(applicationInfo),
                false,
                applicationInfo,
                null,
                packageInfo.firstInstallTime,
                packageInfo.lastUpdateTime,
                labelResolved,
            )
        }

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
            return store.getConfiguredPackages().filterNotNull().toSet()
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

        @JvmStatic
        fun countUserVisibleConfiguredPackages(
            store: DpisConfigStore?,
            scopeState: ScopeState?,
        ): Int {
            val safeScopeState = scopeState ?: ScopeState(emptySet(), false)
            return userVisibleConfiguredPackages(
                store,
                safeScopeState.packages,
                safeScopeState.known,
            ).size
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
            val fontHookDomainsRaw = store?.getPackageFontHookDomainsRaw(packageName)
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
                fontHookDomainsRaw,
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
            packageNames: Collection<String>?,
            selfPackageName: String,
        ): Boolean = packageNames.isNullOrEmpty() || packageNames.none {
            it != selfPackageName
        }

        @JvmStatic
        fun isInstalledCatalogChangeAction(action: String?): Boolean = when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE,
            Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE,
            -> true
            else -> false
        }

        @JvmStatic
        fun shouldInvalidateInstalledCatalog(action: String?): Boolean =
            isInstalledCatalogChangeAction(action) || action == Intent.ACTION_LOCALE_CHANGED

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

        private fun isSystemApp(applicationInfo: ApplicationInfo): Boolean =
            applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
    }
}
