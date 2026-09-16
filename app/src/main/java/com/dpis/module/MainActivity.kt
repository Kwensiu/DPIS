package com.dpis.module

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppWorkspaceScrollStateStore
import com.dpis.module.applist.presentation.InstalledAppsLoadSession
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.home.presentation.HomeWorkspaceSession
import com.dpis.module.ui.presentation.SecondaryActivityNavigator
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.runtime.presentation.RuntimeLaunchSession
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.hooks.SystemScopeCoordinator
import com.dpis.module.ui.presentation.MainHostWiringSession
import com.dpis.module.ui.presentation.MainStartupSession
import com.dpis.module.ui.presentation.MainWorkspaceSession
import com.dpis.module.updates.presentation.MainUpdateSession

class MainActivity :
    LocalizedActivity(),
    DpisApplication.ServiceStateListener {

    internal val updateSession: MainUpdateSession = MainUpdateSession(this) {
        startupSession.bindHomeWorkspaceIfVisible()
    }
    internal val wechatHelp = WechatDpiHelp(this) { mainWorkspaceSession.composeShell() }
    internal val runtimeLaunchSession = RuntimeLaunchSession(
        this,
        requestAppsLoad = { startupSession.requestAppsLoad() },
        composeShell = { mainWorkspaceSession.composeShell() },
    )
    internal val saveHandler = AppConfigSaveHandler()
    internal val systemScopeCoordinator: SystemScopeCoordinator = SystemScopeCoordinator(
        object : SystemScopeCoordinator.Host {
            override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                showToast(messageResId, *formatArgs)
            }

            override fun requestAppsLoad() {
                startupSession.requestAppsLoad()
            }

            override fun runOnUiThread(runnable: Runnable) {
                this@MainActivity.runOnUiThread(runnable)
            }
        },
    )
    internal val installedAppsLoadSession = InstalledAppsLoadSession(
        this,
        dispatchInstalledAppsLoad = { startupSession.dispatchInstalledAppsLoad(it) },
        dispatchInstalledAppsLoadFinished = { requestId, loaded ->
            startupSession.dispatchInstalledAppsLoadFinished(requestId, loaded)
        },
    )
    internal val secondaryNavigation = SecondaryActivityNavigator(this)
    internal val hostWiringSession = MainHostWiringSession(this, secondaryNavigation)
    internal val mainWorkspaceSession = MainWorkspaceSession(this, hostWiringSession)
    internal val homeWorkspaceSession = HomeWorkspaceSession(
        this,
        loadScopeState = { installedAppsLoadSession.loadScopeState() },
        quickItemCount = { startupSession.ensureWorkspaceSession().quickItemCount() },
        homeUpdateUiState = { updateSession.homeUpdateUiState },
        checkForUpdatesNow = { updateSession.checkForUpdatesNow() },
        setCurrentAppListPage = { page, submit ->
            startupSession.setCurrentAppListPage(page, submit)
        },
        dispatch = { startupSession.dispatch(it) },
        bindHomeWorkspace = { mainWorkspaceSession.bindHomeWorkspace() },
        secondaryNavigation = secondaryNavigation,
    )
    internal val startupSession: MainStartupSession = MainStartupSession(
        this,
        updateSession,
        hostWiringSession,
        mainWorkspaceSession,
    )
    internal val scrollStateStore = AppWorkspaceScrollStateStore()

    override fun onUnhandledTaskRootBack() {
        moveTaskToBack(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startupSession.launch(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        startupSession.onStart()
    }

    override fun onResume() {
        super.onResume()
        startupSession.onResume()
    }

    override fun handleAppearanceChangeOnResume(): Boolean {
        val shell = mainWorkspaceSession.composeShell() ?: return false
        shell.applyAppearanceInPlace()
        return true
    }

    override fun onStop() {
        startupSession.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        startupSession.onDestroy()
        super.onDestroy()
    }

    override fun onServiceStateChanged() {
        runOnUiThread { startupSession.onServiceStateChanged() }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        startupSession.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        startupSession.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startupSession.onRequestPermissionsResult(requestCode)
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return startupSession.retainNonConfigurationInstance()
    }

    fun showToast(messageResId: Int) {
        showToast(getString(messageResId))
    }

    fun showToast(messageResId: Int, vararg formatArgs: Any?) {
        showToast(getString(messageResId, *formatArgs))
    }

    private fun showToast(message: CharSequence?) {
        if (isFinishing || isDestroyed) {
            return
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun resolvePackageVersionName(packageName: String?): String {
        if (packageName.isNullOrBlank()) {
            return ""
        }
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    val hookConfigStore: DpisConfigStore?
        get() = DpisApplication.getActiveHookConfigStore(this)
}
