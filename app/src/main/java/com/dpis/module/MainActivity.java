package com.dpis.module;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.dpis.module.appconfig.presentation.AppConfigDialogBinder;

import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.appconfig.landdetail.LandAppDetailPaneBinder;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.InstalledAppCatalogCoordinator;
import com.dpis.module.applist.ScopeState;
import com.dpis.module.applist.presentation.AppListFilterSession;
import com.dpis.module.applist.presentation.AppListFilterShell;
import com.dpis.module.applist.presentation.InstalledAppsLoadSession;
import com.dpis.module.applist.presentation.InstalledAppsLoadShell;
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession;


import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import com.dpis.module.home.HomeUpdateUiState;
import com.dpis.module.home.HomeWorkspaceState;
import com.dpis.module.home.presentation.HomeWorkspaceSession;
import com.dpis.module.home.presentation.HomeWorkspaceShell;
import com.dpis.module.quirks.presentation.WechatDpiHelp;
import com.dpis.module.root.RootAccessProbe;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;
import com.dpis.module.runtime.presentation.RuntimeLaunchSession;
import com.dpis.module.runtime.presentation.RuntimeLaunchShell;

import com.dpis.module.settings.presentation.ToolsWorkspace;
import com.dpis.module.settings.presentation.SettingsWorkspaceSession;
import com.dpis.module.settings.SystemScopeCoordinator;
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession;
import com.dpis.module.ui.TouchFeedbackBinder;



import com.dpis.module.updates.presentation.MainUpdateSession;
import com.dpis.module.viewport.ViewportPropertySyncer;




import java.util.Collections;
import java.util.List;

import com.dpis.module.ui.presentation.MainComposeShellHost;
import com.dpis.module.ui.presentation.MainHostWiringSession;
import com.dpis.module.ui.presentation.MainHostWiringShell;
import com.dpis.module.ui.presentation.MainLaunchSession;
import com.dpis.module.ui.presentation.MainLaunchShell;
import com.dpis.module.ui.presentation.MainRetainedState;
import com.dpis.module.ui.presentation.MainStartupSession;
import com.dpis.module.ui.presentation.MainWorkspaceSession;
import com.dpis.module.ui.presentation.MainWorkspaceShell;
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost;
import com.dpis.module.appconfig.presentation.AppConfigSheetSession;
import com.dpis.module.appconfig.presentation.AppConfigSheetShell;
import com.dpis.module.appconfig.presentation.EditorDraftSession;
import com.dpis.module.appconfig.presentation.EditorDraftShell;

import com.dpis.module.appconfig.landdetail.LandAppDetailSession;
import com.dpis.module.appconfig.landdetail.LandAppDetailShell;
import com.dpis.module.applist.AppWorkspaceScrollStateStore;
import com.dpis.module.applist.AppWorkspace;

import com.dpis.module.config.DpisConfigStore;

import com.dpis.module.settings.LocalizedActivity;

import com.dpis.module.ui.MainUiAction;
import com.dpis.module.ui.MainUiState;
import com.dpis.module.ui.MainViewModel;
import com.dpis.module.appconfig.editor.ComposeAppEditorController;
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow;


public final class MainActivity
        extends LocalizedActivity
        implements DpisApplication.ServiceStateListener {

    private final MainStartupSession startupSession = new MainStartupSession();
    private final MainUpdateSession updateSession
            = new MainUpdateSession(this, this::bindHomeWorkspaceIfVisible);
    private final WechatDpiHelp wechatDpiHelp
            = new WechatDpiHelp(this, this::composeShell);
    private final RuntimeLaunchSession runtimeLaunchSession
            = new RuntimeLaunchSession(new RuntimeLaunchShell(this));
    private final AppConfigSaveHandler appConfigSaveHandler
            = new AppConfigSaveHandler();
    private FeedbackDiagnosticActivitySession feedbackDiagnostic;
    private final SystemScopeCoordinator systemScopeCoordinator
            = new SystemScopeCoordinator(createSystemScopeHost());
    private final AppConfigDialogActivityHost appConfigDialogHost
            = new AppConfigDialogActivityHost(
                    this,
                    appConfigSaveHandler,
                    systemScopeCoordinator
            );
    private final LandAppDetailSession landAppDetailSession
            = new LandAppDetailSession(
                    new LandAppDetailShell(this),
                    appConfigSaveHandler,
                    systemScopeCoordinator,
                    appConfigDialogHost
            );
    private final AppConfigSheetSession appConfigSheetSession
            = new AppConfigSheetSession(
                    new AppConfigSheetShell(this),
                    appConfigDialogHost
            );
    private final EditorDraftSession editorDraftSession
            = new EditorDraftSession(
                    new EditorDraftShell(this),
                    appConfigDialogHost
            );
    private final InstalledAppsLoadSession installedAppsLoadSession
            = new InstalledAppsLoadSession(new InstalledAppsLoadShell(this));
    private final AppListFilterSession appListFilterSession
            = new AppListFilterSession(new AppListFilterShell(this));
    private final MainWorkspaceSession mainWorkspaceSession
            = new MainWorkspaceSession(new MainWorkspaceShell(this));
    private final HomeWorkspaceSession homeWorkspaceSession
            = new HomeWorkspaceSession(new HomeWorkspaceShell(this));
    private final MainHostWiringSession hostWiringSession
            = new MainHostWiringSession(new MainHostWiringShell(this));
    private final MainLaunchSession launchSession
            = new MainLaunchSession(
                    new MainLaunchShell(this),
                    startupSession,
                    updateSession,
                    hostWiringSession,
                    mainWorkspaceSession
            );
    private AppListFilterStateStore appListFilterStateStore;
    private final AppWorkspaceScrollStateStore appWorkspaceScrollStateStore
            = new AppWorkspaceScrollStateStore();

    private MainViewModel mainViewModel;
    private AppListPage landCurrentPage = AppListPage.ALL_APPS;
    private TemplateWorkspaceActivitySession workspaceSession;
    private boolean cachedSystemHookEffectiveEnabled;
    private boolean skipNextImmediateServiceReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        refreshSystemHookEffectiveEnabled();
        launchSession.launch(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSystemHookEffectiveEnabled();
        mainWorkspaceSession.bindForLifecycle(requireUiState().workspaceMode);
        if (toolsWorkspace() != null) {
            toolsWorkspace().onStart();
        }
        if (settingsWorkspaceSession() != null) {
            settingsWorkspaceSession().onStart();
        }
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeStartRootAccessProbe();
        if (toolsWorkspace() != null) {
            toolsWorkspace().onResume();
        }
        if (settingsWorkspaceSession() != null) {
            settingsWorkspaceSession().onResume();
        }
    }

    @Override
    protected void onStop() {
        if (toolsWorkspace() != null) {
            toolsWorkspace().onStop();
        }
        if (settingsWorkspaceSession() != null) {
            settingsWorkspaceSession().onStop();
        }
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (feedbackDiagnostic != null) {
            feedbackDiagnostic.onDestroy(isChangingConfigurations());
        }
        updateSession.shutdown();
        ensureWorkspaceSession().onDestroy();
        if (settingsWorkspaceSession() != null) {
            settingsWorkspaceSession().onDestroy();
        }
        installedAppsLoadSession.shutdown();
        super.onDestroy();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(() -> {
            refreshSystemHookEffectiveEnabled();
            if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
                bindHomeWorkspace();
            }
            if (settingsWorkspaceSession() != null) {
                settingsWorkspaceSession().onServiceStateChanged();
            }
            if (skipNextImmediateServiceReload) {
                skipNextImmediateServiceReload = false;
                return;
            }
            requestAppsLoad();
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (settingsWorkspaceSession() != null) {
            settingsWorkspaceSession().onActivityResult(requestCode, resultCode, data);
        }
        if (toolsWorkspace() != null) {
            toolsWorkspace().onActivityResult(requestCode, resultCode, data);
        }
        if (ensureWorkspaceSession().handleActivityResult(requestCode, data)) {
            return;
        }
        if (feedbackDiagnostic != null
                && feedbackDiagnostic.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        startupSession.saveInstanceState(
                outState,
                requireUiState(),
                landCurrentPage.position()
        );
        ensureWorkspaceSession().saveState(outState);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );
        installedAppsLoadSession.onRequestPermissionsResult(requestCode);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return startupSession.retain(
                requireUiState(),
                landCurrentPage.position(),
                appWorkspaceScrollStateStore.snapshot(),
                editorDraftSession.captureAppConfigDraft(),
                mainViewModel,
                ensureWorkspaceSession().retainedState(),
                feedbackDiagnostic.retainedState(),
                updateSession.getPendingUpdatePrompt()
        );
    }

    public void onPageRefreshRequested(AppListPage page) {
        dispatchMainUiAction(MainUiAction.markPageRefreshing(page));
        installedAppsLoadSession.requestLoad(true);
    }

    public void setCurrentAppListPage(AppListPage page, boolean submit) {
        landCurrentPage = page != null ? page : AppListPage.ALL_APPS;
        if (submit) {
            mainWorkspaceSession.refreshApps();
        }
    }

    public void requestAppsLoad() {
        installedAppsLoadSession.requestLoad(false);
    }

    public void dispatchInstalledAppsLoad(boolean forceReload) {
        dispatchMainUiAction(MainUiAction.requestAppsLoad(forceReload));
    }

    public void dispatchInstalledAppsLoadFinished(
            int requestId,
            List<AppListItem> loaded
    ) {
        dispatchMainUiAction(MainUiAction.appsLoadFinished(requestId, loaded));
    }

    public void showToast(int messageResId) {
        showToast(getString(messageResId));
    }

    public void showToast(int messageResId, Object... formatArgs) {
        showToast(getString(messageResId, formatArgs));
    }

    private void showToast(CharSequence message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public boolean setDpisEnabled(String packageName, boolean enabled) {
        DpisConfigStore store = getHookConfigStore();
        if (store == null) {
            showToast(R.string.status_save_requires_init);
            return false;
        }
        if (!store.setTargetDpisEnabled(packageName, enabled)) {
            showToast(R.string.system_settings_save_failed);
            return false;
        }
        if (!enabled) {
            FontRuntimePropertySyncer.clearTargetAsync(packageName);
            FontHookDomainPropertySyncer.clearTargetAsync(packageName);
            ViewportPropertySyncer.clearTargetAsync(packageName);
        }
        showToast(
                enabled
                        ? R.string.dialog_dpis_enabled_status
                        : R.string.dialog_dpis_disabled_status
        );
        onRuntimeConfigSaved();
        return true;
    }

    public MainUiState requireUiState() {
        MainViewModel viewModel = mainViewModel;
        if (viewModel == null) {
            return MainUiState.initial(
                    "",
                    AppListFilterState.defaultState(),
                    Collections.emptyList(),
                    Collections.emptySet()
            );
        }
        return viewModel.getState();
    }

    public void dispatchMainUiAction(MainUiAction action) {
        MainViewModel viewModel = mainViewModel;
        if (viewModel == null) {
            return;
        }
        List<MainViewModel.AppsLoadRequest> requests = viewModel.dispatch(action);
        renderMainUiState(viewModel.getState());
        handleAppsLoadRequests(requests);
    }

    private void renderMainUiState(MainUiState state) {
        mainWorkspaceSession.render(state);
    }

    public void refreshComposeApps() {
        mainWorkspaceSession.refreshApps();
    }

    public MainComposeShellHost composeShell() {
        return mainWorkspaceSession.composeShell();
    }

    public boolean saveComposeEditorForDiagnostic(AppListItem item, EditorDraft draft) {
        return hostWiringSession.getComposeAppEditorSaveWorkflow() != null
                && hostWiringSession.getComposeAppEditorSaveWorkflow().save(item, draft);
    }

    public void markComposeEditorSaved(EditorDraft draft) {
        if (composeAppEditorController() != null) {
            composeAppEditorController().markSaved(draft);
        }
    }

    public void dismissActiveEditorDialog() {
        appConfigSheetSession.dismiss();
    }

    public void clearEditingSession() {
        editorDraftSession.clearEditingSession();
    }

    public MainViewModel editorViewModel() {
        return mainViewModel;
    }

    public void showComposeFeedbackDiagnosticPreparation(
            AppListItem item,
            EditorDraft draft
    ) {
        feedbackDiagnostic.showPreparation(item, draft);
    }

    public void initializeWorkspaceSession(
            TemplateWorkspaceActivitySession.State initialState,
            String initialQuery
    ) {
        if (workspaceSession == null) {
            workspaceSession = new TemplateWorkspaceActivitySession(
                    this,
                    initialQuery,
                    initialState,
                    () -> mainWorkspaceSession.refreshTemplates()
            );
        }
    }

    public TemplateWorkspaceActivitySession ensureWorkspaceSession() {
        initializeWorkspaceSession(
                null,
                requireUiState().currentQuery()
        );
        return workspaceSession;
    }

    private void handleAppsLoadRequests(List<MainViewModel.AppsLoadRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (MainViewModel.AppsLoadRequest request : requests) {
            installedAppsLoadSession.start(request);
        }
    }

    public void showFilterDialog() {
        appListFilterSession.show();
    }

    public void applyAppListFilter(AppListFilterState filterState) {
        appListFilterSession.apply(filterState);
    }

    private void bindHomeWorkspaceIfVisible() {
        if (mainViewModel != null
                && requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
            bindHomeWorkspace();
        }
    }

    private SystemScopeCoordinator.Host createSystemScopeHost() {
        return new SystemScopeCoordinator.Host() {
            @Override
            public void showToast(int messageResId, Object... formatArgs) {
                MainActivity.this.showToast(messageResId, formatArgs);
            }

            @Override
            public void requestAppsLoad() {
                MainActivity.this.requestAppsLoad();
            }

            @Override
            public void runOnUiThread(@NonNull Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }
        };
    }

    private void showEditDialog(AppListItem item) {
        if (mainViewModel != null) {
            mainViewModel.setEditingPackageName(item.packageName);
        }
        editorDraftSession.rememberActiveEditor(
                editorDraftSession.activeEditorRoot(),
                item.packageName
        );
        if (mainWorkspaceSession.isLandscapeDetailMode() && landAppDetailSession.show(item)) {
            return;
        }
        appConfigSheetSession.show(item);
    }

    public HomeWorkspaceState createHomeWorkspaceState() {
        return homeWorkspaceSession.createState();
    }

    public ScopeState loadInstalledAppScopeState() {
        return installedAppsLoadSession.loadScopeState();
    }

    public HomeUpdateUiState homeUpdateUiState() {
        return updateSession.getHomeUpdateUiState();
    }

    public void checkForUpdatesNow() {
        updateSession.checkForUpdatesNow();
    }

    public void bindHomeWorkspace() {
        mainWorkspaceSession.bindHomeWorkspace();
    }

    public static int countUserVisibleConfiguredPackages(DpisConfigStore store,
            ScopeState scopeState) {
        ScopeState safeScopeState = scopeState != null
                ? scopeState
                : new ScopeState(Collections.emptySet(), false);
        return InstalledAppCatalogCoordinator.userVisibleConfiguredPackages(
                store,
                safeScopeState.packages,
                safeScopeState.known
        ).size();
    }

    private void maybeStartRootAccessProbe() {
        RootAccessProbe.refreshAsync(result -> runOnUiThread(() -> {
            if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
                bindHomeWorkspace();
            }
        }));
    }

    public AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
            AppConfigSaveHandler.Result saveResult,
            View configRoot,
            String packageName,
            boolean dpisEnabled,
            DpisConfigStore store) {
        return runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
                saveResult,
                configRoot,
                packageName,
                dpisEnabled,
                store);
    }

    public AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
            AppConfigSaveHandler.Result saveResult,
            String wechatDpiInput,
            String packageName,
            boolean dpisEnabled,
            DpisConfigStore store) {
        return runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
                saveResult,
                wechatDpiInput,
                packageName,
                dpisEnabled,
                store);
    }

    public void onRuntimeConfigSaved() {
        runtimeLaunchSession.onRuntimeConfigSaved();
    }

    public void syncRuntimePropertiesForTargetLaunch(String packageName) {
        runtimeLaunchSession.syncRuntimePropertiesForTargetLaunch(packageName);
    }

    public void toggleLandDetailScope(
            AppListItem item,
            boolean currentlyInScope,
            Runnable onTurnedInScope,
            Runnable onTurnedOutScope
    ) {
        landAppDetailSession.toggleScope(
                item,
                currentlyInScope,
                onTurnedInScope,
                onTurnedOutScope
        );
    }

    public AppConfigDialogBinder.Host createAppConfigDialogHost() {
        return appConfigDialogHost;
    }

    public void startFeedbackDiagnostic(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        feedbackDiagnostic.startFromViewEditor(item, state);
    }

    public AppListItem saveCurrentEditorConfigForDiagnostic(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (item == null) {
            return null;
        }
        View root = editorDraftSession.activeEditorRoot();
        if (root == null || !item.packageName.equals(editorDraftSession.activeEditorPackageName())) {
            return item;
        }
        if (AppConfigDialogBinder.viewsFor(root) != null) {
            return appConfigSheetSession.saveForDiagnostic(item, root);
        }
        if (LandAppDetailPaneBinder.stateFor(root) != null) {
            return landAppDetailSession.saveForDiagnostic(item, state, root);
        }
        return item;
    }

    public String resolvePackageVersionName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }
        try {
            return getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    public String getFontHookDomainsButtonText(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        return appConfigDialogHost.fontHookDomainsButtonText(item, state);
    }

    public void executeHyperOsNativeProxyMount(
            AppListItem item,
            boolean apply,
            Runnable onFinished
    ) {
        runtimeLaunchSession.executeHyperOsNativeProxyMount(item, apply, onFinished);
    }

    public void executeDialogProcessAction(
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action
    ) {
        runtimeLaunchSession.executeDialogProcessAction(item, action);
    }

    /** The catalogue intentionally does not preload metadata for every installed package. */
    public boolean isHyperOsNativeProxyCandidate(AppListItem item) {
        return runtimeLaunchSession.isHyperOsNativeProxyCandidate(item);
    }

    public boolean isSystemHookEnabledFromStore() {
        return cachedSystemHookEffectiveEnabled;
    }

    public void refreshSystemHookEffectiveEnabled() {
        cachedSystemHookEffectiveEnabled
                = SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                        getHookConfigStore()
                );
    }

    public DpisConfigStore getHookConfigStore() {
        return DpisApplication.getActiveHookConfigStore(this);
    }

    public FrameLayout landDetailContent() {
        return hostWiringSession.getLandDetailContent();
    }

    public View landDetailEmptyView() {
        return hostWiringSession.getLandDetailEmptyView();
    }

    public ComposeAppEditorController composeAppEditorController() {
        return hostWiringSession.getComposeAppEditorController();
    }

    public AppWorkspace appWorkspace() {
        return hostWiringSession.getAppWorkspace();
    }

    public ToolsWorkspace toolsWorkspace() {
        return hostWiringSession.getToolsWorkspace();
    }

    public SettingsWorkspaceSession settingsWorkspaceSession() {
        return hostWiringSession.getSettingsWorkspaceSession();
    }

    public AppListPage landCurrentPage() {
        return landCurrentPage;
    }

    public AppWorkspaceScrollStateStore appWorkspaceScrollStateStore() {
        return appWorkspaceScrollStateStore;
    }

    public LandAppDetailSession landAppDetailSession() {
        return landAppDetailSession;
    }

    public AppConfigSheetSession appConfigSheetSession() {
        return appConfigSheetSession;
    }

    public View topContainer() {
        return hostWiringSession.getTopContainer();
    }

    public View toolsWorkspaceContainer() {
        return hostWiringSession.getToolsWorkspaceContainer();
    }

    public View settingsWorkspaceContainer() {
        return hostWiringSession.getSettingsWorkspaceContainer();
    }

    public View landDetailPane() {
        return hostWiringSession.getLandDetailPane();
    }

    public View landDetailDivider() {
        return hostWiringSession.getLandDetailDivider();
    }

    public AppConfigDialogActivityHost appConfigDialogHost() {
        return appConfigDialogHost;
    }

    public AppConfigSaveHandler appConfigSaveHandler() {
        return appConfigSaveHandler;
    }

    public WechatDpiHelp wechatDpiHelp() {
        return wechatDpiHelp;
    }

    public boolean requestEditorScope(AppListItem item, Runnable onApproved) {
        return systemScopeCoordinator.requestScope(
                item.packageName,
                item.label,
                onApproved,
                null,
                false
        );
    }

    public void refreshComposeSettings() {
        mainWorkspaceSession.refreshSettings();
    }

    public void refreshComposeTools() {
        mainWorkspaceSession.refreshTools();
    }

    @SuppressWarnings("deprecation")
    public MainRetainedState lastRetainedState() {
        Object retained = getLastCustomNonConfigurationInstance();
        return retained instanceof MainRetainedState
                ? (MainRetainedState) retained
                : null;
    }

    public void attachAppListFilterStateStore(AppListFilterStateStore store) {
        appListFilterStateStore = store;
    }

    public void setFeedbackDiagnostic(FeedbackDiagnosticActivitySession session) {
        feedbackDiagnostic = session;
    }

    public void restoreAppListScrollPositions(int[] positions) {
        appWorkspaceScrollStateStore.restore(positions);
    }

    public void setSkipNextImmediateServiceReload(boolean skip) {
        skipNextImmediateServiceReload = skip;
    }

    public void setMainViewModel(MainViewModel viewModel) {
        mainViewModel = viewModel;
    }

    public void restoreFeedbackDiagnosticPage() {
        feedbackDiagnostic.restorePage();
    }

    public void attachFeedbackDiagnosticHost() {
        feedbackDiagnostic.attachHost();
    }

    public void saveAppListFilterState(AppListFilterState filterState) {
        appListFilterStateStore.save(filterState);
    }

    public void updateAppListScrollPosition(
            AppListPage page,
            int index,
            int scrollOffset
    ) {
        appWorkspaceScrollStateStore.update(page, index, scrollOffset);
    }

    public void attachTemplateLegacyViews(
            View workspaceContainer,
            View detailEmpty,
            FrameLayout detailContent
    ) {
        ensureWorkspaceSession().attachLegacyViews(
                workspaceContainer,
                detailEmpty,
                detailContent
        );
    }

    public EditorDraft currentEditingDraft() {
        return editorDraftSession.currentEditingDraft();
    }

    public void rememberActiveEditor(View root, String packageName) {
        editorDraftSession.rememberActiveEditor(root, packageName);
    }

    public void updateEditingDraft(AppConfigDialogBinder.AppConfigDialogState state) {
        editorDraftSession.updateEditingDraft(state);
    }

    public void applyAppConfigDraft(View root, EditorDraft draft) {
        editorDraftSession.applyAppConfigDraft(root, draft);
    }

    public View currentEditorRoot() {
        return editorDraftSession.currentEditorRoot();
    }

}
