package com.dpis.module.applist.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Process
import com.dpis.module.applist.InstalledAppCatalogPolicy
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.root.RootInstalledPackageProvider
import com.dpis.module.root.RootShellInstalledPackageProvider

interface InstalledAppSource {
    fun load(): List<PackageInfo>
}

/**
 * Loads packages for the DPIS process user. Root supplies the authoritative user package set;
 * PackageManager remains a compatibility fallback for devices without root access.
 */
class RootPreferredInstalledAppSource(
    private val packageManager: PackageManager,
    private val selfPackageName: String,
    private val userId: Int,
    private val rootPackageProvider: RootInstalledPackageProvider =
        RootShellInstalledPackageProvider(packageManager),
) : InstalledAppSource {
    override fun load(): List<PackageInfo> {
        val rootPackages = rootPackageProvider.query(userId)
        if (rootPackages != null) {
            DpisLog.i(
                "installed packages loaded from root provider: user=$userId, " +
                        "result=${rootPackages.size}",
            )
            return rootPackages
        }

        var packages = queryPackageManager()
        DpisLog.i("installed packages root user set unavailable: user=$userId")
        if (InstalledAppCatalogPolicy.shouldUseLauncherVisibilityFallback(
                packages.map { it.packageName },
                selfPackageName,
            )
        ) {
            packages = mergeLauncherPackages(packages)
            DpisLog.i("launcher visibility fallback returned: size=${packages.size}")
        }
        packages = InstalledAppCatalogPolicy.filterInstalledPackagesForUser(packages, userId)
        DpisLog.i(
            "installed packages loaded from PackageManager fallback: " +
                    "user=$userId, size=${packages.size}",
        )
        return packages
    }

    private fun queryPackageManager(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }

    private fun mergeLauncherPackages(installedPackages: List<PackageInfo>): List<PackageInfo> {
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
            merged[application.packageName] = packageInfoForLauncherFallback(application)
        }
        return merged.values.toList()
    }

    private fun packageInfoForLauncherFallback(application: ApplicationInfo): PackageInfo {
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

    companion object {
        fun forContext(context: Context): RootPreferredInstalledAppSource =
            RootPreferredInstalledAppSource(
                context.packageManager,
                context.packageName,
                Process.myUid() / USER_ID_RANGE,
            )

        private const val USER_ID_RANGE = 100000
    }
}
