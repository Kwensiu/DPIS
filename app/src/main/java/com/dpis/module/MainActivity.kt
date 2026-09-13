package com.dpis.module

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDraft
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
import com.dpis.module.root.RootAccessProbe
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
import com.dpis.module.ui.presentation.MainWorkspaceShell
import com.dpis.module.updates.presentation.MainUpdateSession
import com.dpis.module.viewport.ViewportPropertySyncer

class MainActivity :
    LocalizedActivity(),
    DpisApplication.ServiceStateListener {

    internal val updateSession = MainUpdateSession(this, ::bindHomeWorkspaceIfVisible)
    internal val wechatHelp = WechatDpiHelp(this) { mainWorkspaceSession.composeShell() }
    private val runtimeLaunchSession = RuntimeLaunchSession(RuntimeLaunchShell(this))
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
    internal val mainWorkspaceSession = MainWorkspaceSession(MainWorkspaceShell(this))
    internal val homeWorkspaceSession = HomeWorkspaceSession(HomeWorkspaceShell(this))
    internal val hostWiringSession = MainHostWiringSession(MainHostWiringShell(this))
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
        refreshSystemHookEffectiveEnabled()
        mainWorkspaceSession.bindForLifecycle(requireUiState().workspaceMode)
        hostWiringSession.toolsWorkspace?.onStart()
        hostWiringSession.settingsWorkspaceSession?.onStart()
        DpisApplication.addServiceStateListener(this, true)
    }

    override fun onResume() {
        super.onResume()
        maybeStartRootAccessProbe()
        hostWiringSession.toolsWorkspace?.onResume()
        hostWiringSession.settingsWorkspaceSession?.onResume()
    }

    override fun onStop() {
        hostWiringSession.toolsWorkspace?.onStop()
        hostWiringSession.settingsWorkspaceSession?.onStop()
        DpisApplication.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        startupSession.feedbackDiagnostic?.onDestroy(isChangingConfigurations)
        updateSession.shutdown()
        ensureWorkspaceSession().onDestroy()
        hostWiringSession.settingsWorkspaceSession?.onDestroy()
        installedAppsLoadSession.shutdown()
        super.onDestroy()
    }

    override fun onServiceStateChanged() {
        runOnUiThread {
            refreshSystemHookEffectiveEnabled()
            if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
                mainWorkspaceSession.bindHomeWorkspace()
            }
            hostWiringSession.settingsWorkspaceSession?.onServiceStateChanged()
            if (startupSession.consumeSkipNextImmediateServiceReload()) {
                return@runOnUiThread
            }
            requestAppsLoad()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        hostWiringSession.settingsWorkspaceSession?.onActivityResult(
            requestCode,
            resultCode,
            data,
        )
        hostWiringSession.toolsWorkspace?.onActivityResult(requestCode, resultCode, data)
        if (ensureWorkspaceSession().handleActivityResult(requestCode, data)) {
            return
        }
        if (startupSession.feedbackDiagnostic?.handleActivityResult(
                requestCode,
                resultCode,
                data,
            ) == true
        ) {
            return
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        startupSession.saveInstanceState(
            outState,
            requireUiState(),
            currentAppListPage.position(),
        )
        ensureWorkspaceSession().saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        installedAppsLoadSession.onRequestPermissionsResult(requestCode)
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return startupSession.retain(
            requireUiState(),
            currentAppListPage.position(),
            scrollStateStore.snapshot(),
            editorDraftSession.captureAppConfigDraft(),
            startupSession.viewModel,
            ensureWorkspaceSession().retainedState(),
            startupSession.feedbackDiagnostic?.retainedState(),
            updateSession.pendingUpdatePrompt,
        )
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
        onRuntimeConfigSaved()
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

    fun saveComposeEditorForDiagnostic(item: AppListItem?, draft: EditorDraft?): Boolean {
        val workflow = hostWiringSession.composeAppEditorSaveWorkflow ?: return false
        return item != null && draft != null && workflow.save(item, draft)
    }

    fun markComposeEditorSaved(draft: EditorDraft?) {
        hostWiringSession.composeAppEditorController?.markSaved(draft)
    }

    fun showComposeFeedbackDiagnosticPreparation(item: AppListItem?, draft: EditorDraft?) {
        if (item == null || draft == null) {
            return
        }
        startupSession.feedbackDiagnostic?.showPreparation(item, draft)
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

    private fun maybeStartRootAccessProbe() {
        RootAccessProbe.refreshAsync {
            runOnUiThread {
                if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
                    mainWorkspaceSession.bindHomeWorkspace()
                }
            }
        }
    }

    fun finalizeAppConfigSaveWithRuntimeSync(
        saveResult: AppConfigSaveHandler.Result?,
        configRoot: View?,
        packageName: String?,
        dpisEnabled: Boolean,
        store: DpisConfigStore?,
    ): AppConfigSaveHandler.Result =
        runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
            saveResult,
            configRoot,
            packageName,
            dpisEnabled,
            store,
        )

    fun finalizeAppConfigSaveWithRuntimeSync(
        saveResult: AppConfigSaveHandler.Result?,
        wechatDpiInput: String?,
        packageName: String?,
        dpisEnabled: Boolean,
        store: DpisConfigStore?,
    ): AppConfigSaveHandler.Result =
        runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
            saveResult,
            wechatDpiInput,
            packageName,
            dpisEnabled,
            store,
        )

    fun onRuntimeConfigSaved() {
        runtimeLaunchSession.onRuntimeConfigSaved()
    }

    fun syncRuntimePropertiesForTargetLaunch(packageName: String?) {
        runtimeLaunchSession.syncRuntimePropertiesForTargetLaunch(packageName)
    }

    fun startFeedbackDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        startupSession.feedbackDiagnostic?.startFromViewEditor(item, state)
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

    fun executeHyperOsNativeProxyMount(
        item: AppListItem?,
        apply: Boolean,
        onFinished: Runnable?,
    ) {
        runtimeLaunchSession.executeHyperOsNativeProxyMount(item, apply, onFinished)
    }

    fun executeDialogProcessAction(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction?,
    ) {
        runtimeLaunchSession.executeDialogProcessAction(item, action)
    }

    /** The catalogue intentionally does not preload metadata for every installed package. */
    fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean =
        runtimeLaunchSession.isHyperOsNativeProxyCandidate(item)

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
