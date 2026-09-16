package com.dpis.module.applist.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.dpis.module.DpisApplication
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.InstalledAppCatalogCoordinator as CatalogPolicy
import com.dpis.module.applist.presentation.InstalledAppCatalogCoordinator
import com.dpis.module.applist.ScopeState
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.ui.MainViewModel

/**
 * Owns Xiaomi installed-apps permission gating, catalog load, and Xposed
 * scope snapshot.
 */
class InstalledAppsLoadSession(
    private val activity: MainActivity,
    private val dispatchInstalledAppsLoad: (Boolean) -> Unit,
    private val dispatchInstalledAppsLoadSnapshot: (Int, List<AppListItem>?) -> Unit,
    private val dispatchInstalledAppsLoadFinished: (Int, List<AppListItem>?) -> Unit,
) {
    private val catalogCoordinator by lazy {
        InstalledAppCatalogCoordinator(
            InstalledAppCatalogCoordinator.ContextHost(activity),
            InstalledAppCatalogCoordinator.labelStore(activity),
        )
    }

    private var permissionRequestInFlight = false
    private var pendingLoadAfterPermission = false
    private var permissionRequestCompleted = false
    private var packageChangesRegistered = false

    private val packageCatalogReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!CatalogPolicy.shouldInvalidateInstalledCatalog(intent?.action)) {
                return
            }
            catalogCoordinator.invalidate()
            requestLoad(true)
        }
    }

    fun attachPackageCatalogMonitor() {
        if (packageChangesRegistered) return
        val applicationContext = activity.applicationContext
        ContextCompat.registerReceiver(
            applicationContext,
            packageCatalogReceiver,
            packageCatalogFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            applicationContext,
            packageCatalogReceiver,
            externalCatalogFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )
        packageChangesRegistered = true
    }

    fun requestLoad(forceInstalledAppCatalogReload: Boolean) {
        val permissionReady = ensurePermissionBeforeLoad()
        DpisLog.i(
            "app list load permission gate: ready=" + permissionReady
                + ", forceReload=" + forceInstalledAppCatalogReload,
        )
        if (!permissionReady) {
            pendingLoadAfterPermission = true
            return
        }
        dispatchInstalledAppsLoad(forceInstalledAppCatalogReload)
    }

    fun onRequestPermissionsResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST_XIAOMI_GET_INSTALLED_APPS) {
            return false
        }
        permissionRequestInFlight = false
        val shouldReload = pendingLoadAfterPermission
        pendingLoadAfterPermission = false
        permissionRequestCompleted = true
        if (shouldReload) {
            dispatchInstalledAppsLoad(true)
        }
        return true
    }

    fun start(request: MainViewModel.AppsLoadRequest) {
        val requestId = request.requestId
        val forceInstalledAppCatalogReload = request.forceInstalledAppCatalogReload
        Thread({
            var snapshot: List<AppListItem>? = null
            var settled: List<AppListItem>? = null
            try {
                snapshot = loadInstalledApps(forceInstalledAppCatalogReload)
                if (catalogCoordinator.labelsResolved()) {
                    settled = snapshot
                    snapshot = null
                } else {
                    activity.runOnUiThread {
                        dispatchInstalledAppsLoadSnapshot(requestId, snapshot)
                    }
                    settled = resolveInstalledAppLabels()
                }
            } catch (throwable: Throwable) {
                DpisLog.e("list load failed", throwable)
            }
            val finalLoaded = settled
            DpisLog.i(
                "app list load finished: requestId=" + requestId
                    + ", loaded=" + (finalLoaded?.size ?: "null")
                    + ", forceReload=" + forceInstalledAppCatalogReload,
            )
            activity.runOnUiThread {
                dispatchInstalledAppsLoadFinished(requestId, finalLoaded)
            }
        }, "dpis-load-apps-$requestId").start()
    }

    fun loadScopeState(): ScopeState {
        val scopePackages = HashSet<String>()
        val service = DpisApplication.xposedService
        if (service == null) {
            return ScopeState(scopePackages, false)
        }
        return try {
            scopePackages.addAll(service.scope)
            ScopeState(scopePackages, true)
        } catch (_: RuntimeException) {
            scopePackages.clear()
            ScopeState(scopePackages, false)
        }
    }

    fun shutdown() {
        if (packageChangesRegistered) {
            try {
                activity.applicationContext.unregisterReceiver(packageCatalogReceiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered with the application context.
            }
            packageChangesRegistered = false
        }
        catalogCoordinator.shutdown()
    }

    private fun loadInstalledApps(forceInstalledAppCatalogReload: Boolean): List<AppListItem> {
        val scopeState = loadScopeState()
        return catalogCoordinator.loadInstalledApps(
            forceInstalledAppCatalogReload,
            activity.hookConfigStore,
            scopeState.packages,
            scopeState.known,
        )
    }

    private fun resolveInstalledAppLabels(): List<AppListItem> {
        val scopeState = loadScopeState()
        return catalogCoordinator.resolveInstalledAppLabels(
            activity.hookConfigStore,
            scopeState.packages,
            scopeState.known,
        )
    }

    private fun ensurePermissionBeforeLoad(): Boolean {
        val xiaomiPermissionDeclared = isXiaomiPermissionDeclared()
        DpisLog.i(
            "installed apps permission state: sdk=" + Build.VERSION.SDK_INT
                + ", requestCompleted=" + permissionRequestCompleted
                + ", requestInFlight=" + permissionRequestInFlight
                + ", xiaomiPermissionDeclared=" + xiaomiPermissionDeclared,
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || permissionRequestCompleted
            || !xiaomiPermissionDeclared
        ) {
            return true
        }
        return try {
            val permissionState = activity.checkPermission(
                XIAOMI_GET_INSTALLED_APPS_PERMISSION,
                Process.myPid(),
                Process.myUid(),
            )
            DpisLog.i(
                "installed apps permission check: granted="
                    + (permissionState == PackageManager.PERMISSION_GRANTED),
            )
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                return true
            }
            if (!permissionRequestInFlight) {
                permissionRequestInFlight = true
                DpisLog.i("installed apps permission request started")
                activity.requestPermissions(
                    arrayOf(XIAOMI_GET_INSTALLED_APPS_PERMISSION),
                    REQUEST_XIAOMI_GET_INSTALLED_APPS,
                )
            }
            false
        } catch (_: RuntimeException) {
            true
        }
    }

    private fun isXiaomiPermissionDeclared(): Boolean {
        return try {
            activity.packageManager.getPermissionInfo(
                XIAOMI_GET_INSTALLED_APPS_PERMISSION,
                0,
            )
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    companion object {
        const val XIAOMI_GET_INSTALLED_APPS_PERMISSION =
            "com.android.permission.GET_INSTALLED_APPS"
        const val REQUEST_XIAOMI_GET_INSTALLED_APPS = 10022

        private fun packageCatalogFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        private fun externalCatalogFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
    }
}
