package com.dpis.module.applist.presentation

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.dpis.module.DpisApplication
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.applist.ScopeState
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.ui.MainViewModel

/**
 * Owns Xiaomi installed-apps permission gating, catalog load, and Xposed
 * scope snapshot. [com.dpis.module.MainActivity] forwards lifecycle and
 * ViewModel dispatch.
 */
class InstalledAppsLoadSession @JvmOverloads constructor(
    private val shell: Shell,
    catalogTtlMs: Long = INSTALLED_APP_CATALOG_TTL_MS,
) {
    interface Shell {
        fun activity(): Activity

        fun hookConfigStore(): DpisConfigStore?

        fun dispatchRequestAppsLoad(forceReload: Boolean)

        fun dispatchAppsLoadFinished(requestId: Int, loaded: List<AppListItem>?)
    }

    private val catalogCoordinator = InstalledAppCatalogCoordinator(
        object : InstalledAppCatalogCoordinator.Host {
            override fun getPackageManager(): PackageManager =
                shell.activity().packageManager

            override fun getSelfPackageName(): String =
                shell.activity().packageName
        },
        catalogTtlMs,
    )

    private var permissionRequestInFlight = false
    private var pendingLoadAfterPermission = false
    private var permissionRequestCompleted = false

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
        shell.dispatchRequestAppsLoad(forceInstalledAppCatalogReload)
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
            shell.dispatchRequestAppsLoad(true)
        }
        return true
    }

    fun start(request: MainViewModel.AppsLoadRequest) {
        val requestId = request.requestId
        val forceInstalledAppCatalogReload = request.forceInstalledAppCatalogReload
        Thread({
            var loaded: List<AppListItem>? = null
            try {
                loaded = loadInstalledApps(forceInstalledAppCatalogReload)
            } catch (throwable: Throwable) {
                DpisLog.e("list load failed", throwable)
            }
            val finalLoaded = loaded
            DpisLog.i(
                "app list load finished: requestId=" + requestId
                    + ", loaded=" + (finalLoaded?.size ?: "null")
                    + ", forceReload=" + forceInstalledAppCatalogReload,
            )
            shell.activity().runOnUiThread {
                shell.dispatchAppsLoadFinished(requestId, finalLoaded)
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
        catalogCoordinator.shutdown()
    }

    private fun loadInstalledApps(forceInstalledAppCatalogReload: Boolean): List<AppListItem> {
        val scopeState = loadScopeState()
        return catalogCoordinator.loadInstalledApps(
            forceInstalledAppCatalogReload,
            shell.hookConfigStore(),
            scopeState.packages,
            scopeState.known,
        )
    }

    private fun ensurePermissionBeforeLoad(): Boolean {
        val activity = shell.activity()
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
            shell.activity().packageManager.getPermissionInfo(
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
        const val INSTALLED_APP_CATALOG_TTL_MS = 60_000L
        const val XIAOMI_GET_INSTALLED_APPS_PERMISSION =
            "com.android.permission.GET_INSTALLED_APPS"
        const val REQUEST_XIAOMI_GET_INSTALLED_APPS = 10022
    }
}
