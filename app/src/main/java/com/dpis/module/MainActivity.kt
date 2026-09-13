package com.dpis.module

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.landdetail.LandAppDetailPaneBinder
import com.dpis.module.appconfig.landdetail.LandAppDetailSession
import com.dpis.module.appconfig.landdetail.LandAppDetailShell
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.presentation.AppConfigSheetSession
import com.dpis.module.appconfig.presentation.AppConfigSheetShell
import com.dpis.module.appconfig.presentation.EditorDraftSession
import com.dpis.module.appconfig.presentation.EditorDraftShell
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppWorkspaceScrollStateStore
import com.dpis.module.applist.presentation.AppListFilterSession
import com.dpis.module.applist.presentation.AppListFilterShell
import com.dpis.module.applist.presentation.InstalledAppsLoadSession
import com.dpis.module.applist.presentation.InstalledAppsLoadShell
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.home.presentation.HomeWorkspaceSession
import com.dpis.module.home.presentation.HomeWorkspaceShell
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.runtime.presentation.RuntimeLaunchSession
import com.dpis.module.runtime.presentation.RuntimeLaunchShell
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.SystemScopeCoordinator
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel
import com.dpis.module.ui.presentation.MainHostWiringSession
import com.dpis.module.ui.presentation.MainHostWiringShell
import com.dpis.module.ui.presentation.MainStartupSession
import com.dpis.module.ui.presentation.MainWorkspaceSession
import com.dpis.module.updates.presentation.MainUpdateSession
import com.dpis.module.viewport.ViewportPropertySyncer

class MainActivity :
    LocalizedActivity(),
    DpisApplication.ServiceStateListener {

    internal val updateSession = MainUpdateSession(this, ::bindHomeWorkspaceIfVisible)
    internal val wechatHelp = WechatDpiHelp(this) { mainWorkspaceSession.composeShell() }
    internal val runtimeLaunchSession = RuntimeLaunchSession(RuntimeLaunchShell(this))
    internal val saveHandler = AppConfigSaveHandler()
    private val systemScopeCoordinator = SystemScopeCoordinator(
        object : SystemScopeCoordinator.Host {
            override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                showToast(messageResId, *formatArgs)
            }

            override fun requestAppsLoad() {
                this@MainActivity.requestAppsLoad()
            }

            override fun runOnUiThread(runnable: Runnable) {
                this@MainActivity.runOnUiThread(runnable)
            }
        },
    )
    internal val dialogHost = AppConfigDialogActivityHost(
        this,
        saveHandler,
        systemScopeCoordinator,
    )
    internal val landDetailSession = LandAppDetailSession(
        LandAppDetailShell(this),
        saveHandler,
        systemScopeCoordinator,
        dialogHost,
    )
    internal val sheetSession = AppConfigSheetSession(
        AppConfigSheetShell(this),
        dialogHost,
    )
    internal val editorDraftSession = EditorDraftSession(
        EditorDraftShell(this),
        dialogHost,
    )
    internal val installedAppsLoadSession =
        InstalledAppsLoadSession(InstalledAppsLoadShell(this))
    private val appListFilterSession = AppListFilterSession(AppListFilterShell(this))
    internal val hostWiringSession = MainHostWiringSession(MainHostWiringShell(this))
    internal val mainWorkspaceSession = MainWorkspaceSession(this, hostWiringSession)
    internal val homeWorkspaceSession = HomeWorkspaceSession(HomeWorkspaceShell(this))
    internal val startupSession = MainStartupSession(
        this,
        updateSession,
        hostWiringSession,
        mainWorkspaceSession,
    )
    internal val scrollStateStore = AppWorkspaceScrollStateStore()

    internal var currentAppListPage = AppListPage.ALL_APPS
        private set
    private var workspaceSession: TemplateWorkspaceActivitySession? = null
    private var cachedSystemHookEffectiveEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)
        refreshSystemHookEffectiveEnabled()
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

    fun onPageRefreshRequested(page: AppListPage?) {
        dispatchMainUiAction(MainUiAction.markPageRefreshing(page))
        installedAppsLoadSession.requestLoad(true)
    }

    fun setCurrentAppListPage(page: AppListPage?, submit: Boolean) {
        currentAppListPage = page ?: AppListPage.ALL_APPS
        if (submit) {
            mainWorkspaceSession.refreshApps()
        }
    }

    fun requestAppsLoad() {
        installedAppsLoadSession.requestLoad(false)
    }

    fun dispatchInstalledAppsLoad(forceReload: Boolean) {
        dispatchMainUiAction(MainUiAction.requestAppsLoad(forceReload))
    }

    fun dispatchInstalledAppsLoadFinished(requestId: Int, loaded: List<AppListItem>?) {
        dispatchMainUiAction(MainUiAction.appsLoadFinished(requestId, loaded))
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

    fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean {
        val store = hookConfigStore
        if (store == null || packageName == null) {
            showToast(R.string.status_save_requires_init)
            return false
        }
        if (!store.setTargetDpisEnabled(packageName, enabled)) {
            showToast(R.string.system_settings_save_failed)
            return false
        }
        if (!enabled) {
            FontRuntimePropertySyncer.clearTargetAsync(packageName)
            FontHookDomainPropertySyncer.clearTargetAsync(packageName)
            ViewportPropertySyncer.clearTargetAsync(packageName)
        }
        showToast(
            if (enabled) {
                R.string.dialog_dpis_enabled_status
            } else {
                R.string.dialog_dpis_disabled_status
            },
        )
        runtimeLaunchSession.onRuntimeConfigSaved()
        return true
    }

    fun requireUiState(): MainUiState {
        val viewModel = startupSession.viewModel ?: return MainUiState.initial(
            "",
            AppListFilterState.defaultState(),
            emptyList(),
            emptySet(),
        )
        return viewModel.state
    }

    fun dispatchMainUiAction(action: MainUiAction?) {
        val viewModel = startupSession.viewModel ?: return
        val requests = viewModel.dispatch(action)
        mainWorkspaceSession.render(viewModel.state)
        handleAppsLoadRequests(requests)
    }

    fun initializeWorkspaceSession(
        initialState: TemplateWorkspaceActivitySession.State?,
        initialQuery: String?,
    ) {
        if (workspaceSession == null) {
            workspaceSession = TemplateWorkspaceActivitySession(
                this,
                initialQuery.orEmpty(),
                initialState,
            ) { mainWorkspaceSession.refreshTemplates() }
        }
    }

    fun ensureWorkspaceSession(): TemplateWorkspaceActivitySession {
        initializeWorkspaceSession(null, requireUiState().currentQuery())
        return checkNotNull(workspaceSession)
    }

    private fun handleAppsLoadRequests(requests: List<MainViewModel.AppsLoadRequest>?) {
        if (requests.isNullOrEmpty()) {
            return
        }
        for (request in requests) {
            installedAppsLoadSession.start(request)
        }
    }

    fun showFilterDialog() {
        appListFilterSession.show()
    }

    fun applyAppListFilter(filterState: AppListFilterState?) {
        if (filterState != null) {
            appListFilterSession.apply(filterState)
        }
    }

    private fun bindHomeWorkspaceIfVisible() {
        if (startupSession.viewModel != null &&
            requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME
        ) {
            mainWorkspaceSession.bindHomeWorkspace()
        }
    }

    fun saveCurrentEditorConfigForDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ): AppListItem? {
        if (item == null) {
            return null
        }
        val root = editorDraftSession.activeEditorRoot()
        if (root == null || item.packageName != editorDraftSession.activeEditorPackageName()) {
            return item
        }
        if (AppConfigDialogBinder.viewsFor(root) != null) {
            return sheetSession.saveForDiagnostic(item, root)
        }
        if (LandAppDetailPaneBinder.stateFor(root) != null) {
            return landDetailSession.saveForDiagnostic(item, state, root)
        }
        return item
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

    val isSystemHookEnabledFromStore: Boolean
        get() = cachedSystemHookEffectiveEnabled

    fun refreshSystemHookEffectiveEnabled() {
        cachedSystemHookEffectiveEnabled =
            SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(hookConfigStore)
    }

    val hookConfigStore: DpisConfigStore?
        get() = DpisApplication.getActiveHookConfigStore(this)

    fun requestEditorScope(item: AppListItem, onApproved: Runnable?): Boolean =
        systemScopeCoordinator.requestScope(
            item.packageName,
            item.label,
            onApproved,
            null,
            false,
        )
}
