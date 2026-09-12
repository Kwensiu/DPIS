package com.dpis.module;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.compose.ui.platform.ComposeView;


import com.dpis.module.appconfig.presentation.AppConfigDialogBinder;

import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.appconfig.landdetail.LandAppDetailPaneBinder;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.InstalledAppCatalogCoordinator;
import com.dpis.module.applist.ScopeState;
import com.dpis.module.applist.presentation.InstalledAppsLoadSession;
import com.dpis.module.applist.presentation.InstalledAppsLoadShell;
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession;
import com.dpis.module.fonts.FontLibraryActivity;

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import com.dpis.module.home.DonateActivity;
import com.dpis.module.home.HomeActivationStateResolver;

import com.dpis.module.home.HomeWorkspaceActions;
import com.dpis.module.home.HomeWorkspaceLayout;
import com.dpis.module.home.HomeWorkspaceLayoutStore;
import com.dpis.module.home.HomeWorkspaceState;
import com.dpis.module.settings.PageSettingsStore;
import com.dpis.module.home.ModeHelpActivity;

import com.dpis.module.quirks.presentation.WechatDpiHelp;
import com.dpis.module.root.RootAccessProbe;
import com.dpis.module.runtime.ModuleRuntimeReloadNoticeCoordinator;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;
import com.dpis.module.runtime.presentation.RuntimeLaunchSession;
import com.dpis.module.runtime.presentation.RuntimeLaunchShell;

import com.dpis.module.settings.presentation.ToolsWorkspace;
import com.dpis.module.settings.presentation.SettingsWorkspaceSession;
import com.dpis.module.settings.SystemScopeCoordinator;
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession;
import com.dpis.module.templates.TemplateWorkspacePresentationSource;
import com.dpis.module.ui.TouchFeedbackBinder;
import com.dpis.module.ui.WatchUiMode;
import com.dpis.module.ui.WatchWorkspaceChromeBinder;

import com.dpis.module.tools.presentation.AppFilterComposeSheet;
import com.dpis.module.updates.UpdatePromptRequest;
import com.dpis.module.updates.presentation.MainUpdateSession;
import com.dpis.module.viewport.ViewportPropertySyncer;




import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import io.github.libxposed.service.XposedService;
import kotlin.Unit;
import com.dpis.module.ui.presentation.MainComposeShellHost;
import com.dpis.module.ui.presentation.MainWorkspacePresentationCoordinator;
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost;
import com.dpis.module.appconfig.presentation.AppConfigSheetSession;
import com.dpis.module.appconfig.presentation.AppConfigSheetShell;
import com.dpis.module.appconfig.presentation.EditorDraftSession;
import com.dpis.module.appconfig.presentation.EditorDraftShell;
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway;
import com.dpis.module.appconfig.presentation.ComposeAppEditorShell;
import com.dpis.module.appconfig.landdetail.LandAppDetailSession;
import com.dpis.module.appconfig.landdetail.LandAppDetailShell;
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticShell;
import com.dpis.module.applist.AppWorkspaceScrollStateStore;
import com.dpis.module.applist.AppWorkspacePresentation;
import com.dpis.module.applist.AppWorkspace;
import com.dpis.module.ui.ConfigEditorDestination;
import com.dpis.module.runtime.ConfigStoreFactory;
import com.dpis.module.config.DpisConfigStore;
import com.dpis.module.diagnostics.DpisLog;
import com.dpis.module.settings.LocalizedActivity;
import com.dpis.module.diagnostics.LogActivity;
import com.dpis.module.ui.MainUiAction;
import com.dpis.module.ui.MainUiState;
import com.dpis.module.ui.MainViewModel;
import com.dpis.module.appconfig.editor.ComposeAppEditorController;
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow;
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator;

public final class MainActivity
        extends LocalizedActivity
        implements DpisApplication.ServiceStateListener {

    private static final long WORKSPACE_TRANSITION_DURATION_MS = 300L;
    private static final float WORKSPACE_CONTENT_ENTER_START_SCALE = 0.96f;
    private static final AccelerateDecelerateInterpolator
            WORKSPACE_CONTENT_ENTER_INTERPOLATOR =
                    new AccelerateDecelerateInterpolator();
    private static final String STATE_CURRENT_QUERY = "state.current_query";
    private static final String STATE_TEMPLATE_QUERY = "state.template_query";
    private static final String STATE_CURRENT_PAGE = "state.current_page";
    private static final String STATE_WORKSPACE_MODE = "state.workspace_mode";
    private static final String STATE_FILTER_SHOW_SYSTEM
            = "state.filter.show_system";
    private static final String STATE_FILTER_INJECTED_ONLY
            = "state.filter.injected_only";
    private static final String STATE_FILTER_WIDTH_ONLY
            = "state.filter.width_only";
    private static final String STATE_FILTER_FONT_ONLY
            = "state.filter.font_only";
    private static final String STATE_FILTER_DISABLED_ONLY
            = "state.filter.disabled_only";
    private static final String STATE_FILTER_TYPEFACE_ONLY
            = "state.filter.typeface_only";
    private static final String STATE_FILTER_HOOK_ONLY
            = "state.filter.hook_only";
    private static final String STATE_FILTER_APP_TYPE = "state.filter.app_type";
    private static final String STATE_FILTER_SORT_ORDER = "state.filter.sort_order";
    private static final String STATE_FILTER_REVERSE = "state.filter.reverse";
    private static final String STATE_REFRESHING_PAGES
            = "state.refreshing_pages";


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
    private AppListFilterStateStore appListFilterStateStore;
    private final AppWorkspaceScrollStateStore appWorkspaceScrollStateStore
            = new AppWorkspaceScrollStateStore();

    private MainViewModel mainViewModel;
    private ComposeAppEditorController composeAppEditorController;
    private ComposeAppEditorSaveWorkflow composeAppEditorSaveWorkflow;
    private MainComposeShellHost composeShellHost;
    private View topContainer;
    private View toolsWorkspaceContainer;
    private View settingsWorkspaceContainer;
    private View landDetailPane;
    private View landDetailDivider;
    private View landDetailEmptyView;
    private FrameLayout landDetailContent;
    private AppListPage landCurrentPage = AppListPage.ALL_APPS;
    private TemplateWorkspaceActivitySession workspaceSession;
    private ToolsWorkspace toolsWorkspace;
    private AppWorkspace appWorkspace;
    private SettingsWorkspaceSession settingsWorkspaceSession;
    private boolean cachedSystemHookEffectiveEnabled;
    private boolean skipNextImmediateServiceReload;
    private MainUiState.WorkspaceMode renderedWorkspaceMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        refreshSystemHookEffectiveEnabled();

        appListFilterStateStore = new AppListFilterStateStore(this);

        RetainedState retainedState
                = (RetainedState) getLastCustomNonConfigurationInstance();
        feedbackDiagnostic = new FeedbackDiagnosticActivitySession(
                new FeedbackDiagnosticShell(this),
                retainedState != null ? retainedState.feedbackDiagnostic : null
        );
        String initialQuery = "";
        String initialTemplateQuery = "";
        TemplateWorkspaceActivitySession.State initialWorkspaceSessionState = null;
        AppListFilterState initialFilterState = appListFilterStateStore.load();
        MainUiState.WorkspaceMode initialWorkspaceMode = MainUiState.WorkspaceMode.valueOf(
                PageSettingsStore.getDefaultStartupPage(this)
        );
        List<AppListItem> initialAppsSnapshot = Collections.emptyList();
        Set<AppListPage> initialRefreshingPages = EnumSet.noneOf(
                AppListPage.class
        );
        if (retainedState != null) {
            initialQuery = retainedState.query;
            initialTemplateQuery = retainedState.templateQuery;
            initialFilterState = retainedState.filterState;
            initialWorkspaceMode = retainedState.workspaceMode;
            initialWorkspaceSessionState = retainedState.workspaceSessionState;
            updateSession.restorePendingPrompt(retainedState.pendingUpdatePrompt);
            appWorkspaceScrollStateStore.restore(retainedState.appListScrollPositions);
            initialRefreshingPages = decodeRefreshingPages(
                    retainedState.refreshingPagePositions
            );
            initialAppsSnapshot = new ArrayList<>(retainedState.appsSnapshot);
            skipNextImmediateServiceReload = !initialAppsSnapshot.isEmpty();
        }
        if (savedInstanceState != null) {
            initialQuery = savedInstanceState.getString(
                    STATE_CURRENT_QUERY,
                    ""
            );
            initialTemplateQuery = savedInstanceState.getString(
                    STATE_TEMPLATE_QUERY,
                    ""
            );
            initialFilterState = new AppListFilterState(
                    parseAppType(savedInstanceState.getString(STATE_FILTER_APP_TYPE),
                            savedInstanceState.getBoolean(STATE_FILTER_SHOW_SYSTEM, false)),
                    savedInstanceState.getBoolean(
                            STATE_FILTER_INJECTED_ONLY,
                            false
                    ),
                    savedInstanceState.getBoolean(STATE_FILTER_DISABLED_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_WIDTH_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_FONT_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_TYPEFACE_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_HOOK_ONLY, false),
                    parseSortOrder(savedInstanceState.getString(STATE_FILTER_SORT_ORDER)),
                    savedInstanceState.getBoolean(STATE_FILTER_REVERSE, false)
            );
            initialWorkspaceMode = MainUiState.WorkspaceMode.fromName(
                    savedInstanceState.getString(STATE_WORKSPACE_MODE)
            );
            initialRefreshingPages = decodeRefreshingPages(
                    savedInstanceState.getIntArray(STATE_REFRESHING_PAGES)
            );
        }
        mainViewModel = new MainViewModel(
                MainUiState.initial(
                        initialQuery,
                        initialTemplateQuery,
                        initialFilterState,
                        initialAppsSnapshot,
                        initialRefreshingPages,
                        initialWorkspaceMode
                )
        );
        initializeWorkspaceSession(initialWorkspaceSessionState, initialTemplateQuery);
        ensureWorkspaceSession().restore(savedInstanceState);
        ComposeEditorScopeRequestCoordinator composeEditorScopeRequestCoordinator = new ComposeEditorScopeRequestCoordinator(
                mainViewModel,
                (item, onApproved) -> systemScopeCoordinator.requestScope(
                        item.packageName,
                        item.label,
                        onApproved,
                        null,
                        false
                ),
                () -> {
                    if (composeShellHost != null) {
                        composeShellHost.refreshApps();
                    }
                },
                () -> showToast(R.string.save_scope_request_notice)
        );
        ComposeAppEditorActivityGateway composeAppEditorGateway = new ComposeAppEditorActivityGateway(
                new ComposeAppEditorShell(this),
                appConfigDialogHost,
                appConfigSaveHandler,
                composeEditorScopeRequestCoordinator,
                wechatDpiHelp
        );
        composeAppEditorSaveWorkflow = new ComposeAppEditorSaveWorkflow(
                composeAppEditorGateway
        );
        composeAppEditorGateway.setSaveWorkflow(composeAppEditorSaveWorkflow);
        composeAppEditorController = new ComposeAppEditorController(
                mainViewModel,
                composeAppEditorGateway
        );

        topContainer = findViewById(R.id.top_container);
        toolsWorkspaceContainer = findViewById(R.id.tools_workspace_container);
        settingsWorkspaceContainer = findViewById(R.id.settings_workspace_container);
        settingsWorkspaceSession = SettingsWorkspaceSession.create(
                this,
                () -> {
                    if (composeShellHost != null) {
                        composeShellHost.refreshSettings();
                    }
                },
                () -> startActivity(new Intent(MainActivity.this, LogActivity.class))
        );
        WatchWorkspaceChromeBinder.applyIfSupported(
                this,
                settingsWorkspaceContainer
        );
        landDetailPane = findViewById(R.id.land_detail_pane);
        landDetailDivider = findViewById(R.id.land_detail_divider);
        landDetailEmptyView = findViewById(R.id.land_detail_empty);
        landDetailContent = findViewById(R.id.land_detail_content);
        ensureWorkspaceSession().attachLegacyViews(
                findViewById(R.id.template_workspace_container),
                findViewById(R.id.template_detail_empty),
                findViewById(R.id.template_detail_content)
        );
        toolsWorkspace = new ToolsWorkspace(
                this,
                () -> {
                    if (composeShellHost != null) composeShellHost.refreshTools();
                },
                () -> showToast(R.string.system_settings_save_failed)
        );
        appWorkspace = new AppWorkspace(new AppWorkspace.Host() {
            @Override public void changeQuery(String query) {
                dispatchMainUiAction(MainUiAction.queryChanged(query));
            }

            @Override public void changePage(AppListPage page) {
                setCurrentAppListPage(page, true);
                if (composeShellHost != null) {
                    composeShellHost.refreshApps();
                }
            }

            @Override public void changeFilters(AppListFilterState filterState) {
                appListFilterStateStore.save(filterState);
                dispatchMainUiAction(MainUiAction.filterChanged(filterState));
            }

            @Override public void refresh(AppListPage page) {
                onPageRefreshRequested(page);
            }

            @Override public void openApp(AppListItem item) {
                if (composeAppEditorController != null) {
                    composeAppEditorController.open(item);
                }
            }

            @Override public void updateScrollPosition(
                    AppListPage page, int index, int scrollOffset) {
                appWorkspaceScrollStateStore.update(page, index, scrollOffset);
            }
        });
        // Workspace navigation is now rendered by the Compose shell in every
        // form factor, including the compact watch radial selector.
        if (savedInstanceState != null) {
            setCurrentAppListPage(
                    AppListPage.fromPosition(
                            savedInstanceState.getInt(STATE_CURRENT_PAGE, 0)
                    ),
                    false
            );
        } else if (retainedState != null) {
            setCurrentAppListPage(
                    AppListPage.fromPosition(retainedState.currentPage),
                    false
            );
        }

        renderMainUiState(requireUiState());
        installComposeWorkspaceShell();
        feedbackDiagnostic.restorePage();
        feedbackDiagnostic.attachHost();
        // The service state callback is not guaranteed to fire on every Wear image.
        // Request the catalog explicitly; MainViewModel coalesces any later service reload.
        requestAppsLoad();
        if (retainedState != null && retainedState.editingPackageName != null) {
            mainViewModel.restoreEditingSession(
                    retainedState.editingPackageName,
                    retainedState.editingDraft,
                    retainedState.savedEditingDraft,
                    retainedState.editingDestination,
                    retainedState.prefillSnapshot,
                    retainedState.prefillInvalidated
            );
            restoreAppEditorForCurrentWorkspace();
        }
        restoreWorkspaceEditorForCurrentConfiguration();
        if (updateSession.showPendingPromptIfAny()) {
            return;
        }
        if (maybeShowModuleRuntimeReloadAdvice()) {
            return;
        }
        if (!updateSession.maybeShowStartupDisclaimerDialog()) {
            updateSession.maybeCheckForUpdatesOnStartup();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSystemHookEffectiveEnabled();
        if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.TEMPLATE) {
            bindWorkspaceSession();
        } else if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
            bindHomeWorkspace();
        } else if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.TOOLS) {
            bindToolsWorkspace();
        } else if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.SETTINGS) {
            bindSettingsWorkspace();
        }
        if (toolsWorkspace != null) {
            toolsWorkspace.onStart();
        }
        if (settingsWorkspaceSession != null) {
            settingsWorkspaceSession.onStart();
        }
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeStartRootAccessProbe();
        if (toolsWorkspace != null) {
            toolsWorkspace.onResume();
        }
        if (settingsWorkspaceSession != null) {
            settingsWorkspaceSession.onResume();
        }
    }

    @Override
    protected void onStop() {
        if (toolsWorkspace != null) {
            toolsWorkspace.onStop();
        }
        if (settingsWorkspaceSession != null) {
            settingsWorkspaceSession.onStop();
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
        if (settingsWorkspaceSession != null) {
            settingsWorkspaceSession.onDestroy();
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
            if (settingsWorkspaceSession != null) {
                settingsWorkspaceSession.onServiceStateChanged();
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
        if (settingsWorkspaceSession != null) {
            settingsWorkspaceSession.onActivityResult(requestCode, resultCode, data);
        }
        if (toolsWorkspace != null) {
            toolsWorkspace.onActivityResult(requestCode, resultCode, data);
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
        MainUiState state = requireUiState();
        outState.putString(STATE_CURRENT_QUERY, state.appQuery);
        outState.putString(STATE_TEMPLATE_QUERY, state.templateQuery);
        outState.putString(STATE_WORKSPACE_MODE, state.workspaceMode.name());
        outState.putBoolean(
                STATE_FILTER_SHOW_SYSTEM,
                state.filterState.showSystemApps()
        );
        outState.putBoolean(
                STATE_FILTER_INJECTED_ONLY,
                state.filterState.injectedOnly()
        );
        outState.putBoolean(
                STATE_FILTER_WIDTH_ONLY,
                state.filterState.widthConfiguredOnly()
        );
        outState.putBoolean(
                STATE_FILTER_FONT_ONLY,
                state.filterState.fontConfiguredOnly()
        );
        outState.putBoolean(
                STATE_FILTER_DISABLED_ONLY,
                state.filterState.disabledOnly()
        );
        outState.putBoolean(
                STATE_FILTER_TYPEFACE_ONLY,
                state.filterState.typefaceConfiguredOnly()
        );
        outState.putBoolean(
                STATE_FILTER_HOOK_ONLY,
                state.filterState.hookConfiguredOnly()
        );
        outState.putString(STATE_FILTER_APP_TYPE, state.filterState.appType().name());
        outState.putString(STATE_FILTER_SORT_ORDER, state.filterState.sortOrder().name());
        outState.putBoolean(STATE_FILTER_REVERSE, state.filterState.reverseOrder());
        outState.putInt(STATE_CURRENT_PAGE, landCurrentPage.position());
        outState.putIntArray(
                STATE_REFRESHING_PAGES,
                captureRefreshingPagePositions()
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
        MainUiState state = requireUiState();
        List<AppListItem> snapshot = state.appsSnapshot();
        int currentPage = landCurrentPage.position();
        EditorDraft draft = editorDraftSession.captureAppConfigDraft();
        if (draft == null && mainViewModel != null) {
            draft = mainViewModel.getEditingDraft();
        }
        return new RetainedState(
                snapshot,
                state.appQuery,
                state.templateQuery,
                state.filterState,
                state.workspaceMode,
                currentPage,
                appWorkspaceScrollStateStore.snapshot(),
                captureRefreshingPagePositions(),
                mainViewModel != null
                        ? mainViewModel.getEditingPackageName()
                        : null,
                draft,
                mainViewModel != null ? mainViewModel.getSavedEditingDraft() : null,
                mainViewModel != null && mainViewModel.getEditorSession() != null
                        ? mainViewModel.getEditorSession().prefillSnapshot
                        : null,
                mainViewModel != null && mainViewModel.getEditorSession() != null
                        && mainViewModel.getEditorSession().prefillInvalidated,
                mainViewModel != null
                        ? mainViewModel.getEditingDestination()
                        : ConfigEditorDestination.MAIN,
                ensureWorkspaceSession().retainedState(),
                feedbackDiagnostic.retainedState(),
                updateSession.getPendingUpdatePrompt()
        );
    }

    private void onPageRefreshRequested(AppListPage page) {
        dispatchMainUiAction(MainUiAction.markPageRefreshing(page));
        installedAppsLoadSession.requestLoad(true);
    }

    private static Set<AppListPage> decodeRefreshingPages(int[] pagePositions) {
        EnumSet<AppListPage> refreshingPages = EnumSet.noneOf(
                AppListPage.class
        );
        if (pagePositions == null) {
            return refreshingPages;
        }
        for (int pagePosition : pagePositions) {
            refreshingPages.add(AppListPage.fromPosition(pagePosition));
        }
        return refreshingPages;
    }

    private int[] captureRefreshingPagePositions() {
        Set<AppListPage> refreshingPages = requireUiState().refreshingPages();
        int[] positions = new int[refreshingPages.size()];
        int index = 0;
        for (AppListPage page : refreshingPages) {
            positions[index++] = page.position();
        }
        return positions;
    }

    private void setCurrentAppListPage(AppListPage page, boolean submit) {
        landCurrentPage = page != null ? page : AppListPage.ALL_APPS;
        if (submit && composeShellHost != null) {
            composeShellHost.refreshApps();
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

    private void dispatchMainUiAction(MainUiAction action) {
        MainViewModel viewModel = mainViewModel;
        if (viewModel == null) {
            return;
        }
        List<MainViewModel.AppsLoadRequest> requests = viewModel.dispatch(action);
        renderMainUiState(viewModel.getState());
        handleAppsLoadRequests(requests);
    }

    private void renderMainUiState(MainUiState state) {
        if (state == null) {
            return;
        }
        if (composeShellHost != null) {
            composeShellHost.render(state);
        }
        applyWorkspaceMode(state.workspaceMode);
        restoreAppEditorForCurrentWorkspace();
    }

    /**
     * Theme 1 keeps the existing workspace root alive inside Compose while later
     * themes replace individual View workspaces. Navigation itself now belongs
     * to the stateless Compose shell and still dispatches through MainUiAction.
     */
    private void installComposeWorkspaceShell() {
        ViewGroup activityContent = findViewById(android.R.id.content);
        if (activityContent == null || activityContent.getChildCount() == 0) {
            return;
        }
        View legacyWorkspaceRoot = activityContent.getChildAt(0);
        if (legacyWorkspaceRoot == null) {
            return;
        }
        activityContent.removeView(legacyWorkspaceRoot);
        ComposeView composeRoot = new ComposeView(this);
        activityContent.addView(
                composeRoot,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        MainWorkspacePresentationCoordinator workspacePresentationCoordinator = new MainWorkspacePresentationCoordinator(
                new MainWorkspacePresentationCoordinator.Content() {
                    @NonNull
                    @Override
                    public HomeWorkspaceState homeState() {
                        return createHomeWorkspaceState();
                    }

                    @NonNull
                    @Override
                    public AppWorkspacePresentation.State appState() {
                        return AppWorkspacePresentation.create(
                                requireUiState(),
                                landCurrentPage,
                                isSystemHookEnabledFromStore(),
                                appWorkspaceScrollStateStore,
                                appWorkspace.actions());
                    }

                    @Override
                    public EditorPresentation.State appEditorState() {
                        return createComposeAppEditorState();
                    }

                    @Override
                    public com.dpis.module.settings.SystemFontScaleToolState toolsState() {
                        return toolsWorkspace != null ? toolsWorkspace.state() : null;
                    }

                    @Override
                    public void changeToolsPending(int percent) {
                        toolsWorkspace.changePending(percent);
                    }

                    @Override
                    public void applyTools() {
                        toolsWorkspace.apply();
                    }

                    @Override
                    public void restoreTools() {
                        toolsWorkspace.restore();
                    }

                    @Override
                    public void requestToolsPermission() {
                        toolsWorkspace.requestPermission();
                    }

                    @Override
                    public com.dpis.module.settings.SettingsActions settings() {
                        return settingsWorkspaceSession;
                    }

                    @NonNull
                    @Override
                    public TemplateWorkspacePresentationSource templateWorkspace() {
                        return ensureWorkspaceSession().presentationSource(
                                query -> {
                                    dispatchMainUiAction(MainUiAction.queryChanged(query));
                                    return Unit.INSTANCE;
                                }
                        );
                    }
                });
        composeShellHost = new MainComposeShellHost(
                composeRoot,
                requireUiState(),
                WatchUiMode.shouldUseCompactUi(this),
                workspacePresentationCoordinator,
                action -> {
                    dispatchMainUiAction(action);
                    return Unit.INSTANCE;
                }
        );
    }

    private void applyWorkspaceMode(MainUiState.WorkspaceMode workspaceMode) {
        MainUiState.WorkspaceMode mode
                = workspaceMode != null ? workspaceMode : MainUiState.WorkspaceMode.HOME;
        boolean enteringToolsWorkspace = mode == MainUiState.WorkspaceMode.TOOLS
                && renderedWorkspaceMode != MainUiState.WorkspaceMode.TOOLS;
        boolean appWorkspace = mode == MainUiState.WorkspaceMode.APP;
        boolean templateWorkspace = mode == MainUiState.WorkspaceMode.TEMPLATE;
        boolean toolsWorkspace = mode == MainUiState.WorkspaceMode.TOOLS;
        boolean settingsWorkspace = mode == MainUiState.WorkspaceMode.SETTINGS;
        setVisible(topContainer, appWorkspace || templateWorkspace);
        boolean animateWorkspace = renderedWorkspaceMode != null
                && renderedWorkspaceMode != mode;
        renderedWorkspaceMode = mode;
        setVisible(toolsWorkspaceContainer, toolsWorkspace);
        setVisible(settingsWorkspaceContainer, settingsWorkspace);
        resetHiddenWorkspacePresentation(mode);
        if (animateWorkspace) {
            animateVisibleWorkspaceContent(mode);
        }
        applyLandscapeDetailVisibility(appWorkspace, templateWorkspace);
        if (templateWorkspace) {
            bindWorkspaceSession();
            restoreWorkspaceEditorForCurrentConfiguration();
        } else if (toolsWorkspace) {
            bindToolsWorkspace(enteringToolsWorkspace);
        } else if (settingsWorkspace) {
            bindSettingsWorkspace();
        }
    }

    private void restoreAppEditorForCurrentWorkspace() {
        if (mainViewModel == null
                || requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP) {
            return;
        }
        // The Compose app workspace restores its editor directly from MainViewModel. Re-entering
        // the legacy route here would stack a View BottomSheetDialog over the Compose sheet after
        // any state render, including the catalog refresh triggered by a successful save.
        if (composeShellHost != null) {
            composeShellHost.refreshApps();
            return;
        }
        String editingPackage = mainViewModel.getEditingPackageName();
        if (editingPackage == null || editingPackage.isBlank()) {
            return;
        }
        if (isLandscapeDetailMode() && landDetailContent.getChildCount() > 0) {
            return;
        }
        for (AppListItem appItem : requireUiState().visibleItems(landCurrentPage)) {
            if (editingPackage.equals(appItem.packageName)) {
                if (isLandscapeDetailMode()) {
                    landAppDetailSession.show(appItem);
                } else {
                    appConfigSheetSession.show(appItem);
                }
                break;
            }
        }
    }

    private void applyLandscapeDetailVisibility(
            boolean appWorkspace,
            boolean templateWorkspace
    ) {
        boolean showDetailPane = isLandscapeDetailMode()
                && (appWorkspace || templateWorkspace);
        setVisible(landDetailPane, showDetailPane);
        setVisible(landDetailDivider, showDetailPane);
        setVisible(landDetailEmptyView, appWorkspace
                && landDetailContent != null
                && landDetailContent.getChildCount() == 0);
        setVisible(landDetailContent, appWorkspace
                && landDetailContent != null
                && landDetailContent.getChildCount() > 0);
        ensureWorkspaceSession().updateLegacyDetailVisibility(templateWorkspace);
    }

    private boolean isLandscapeDetailMode() {
        return landDetailContent != null && landDetailEmptyView != null;
    }

    private void bindWorkspaceSession() {
        ensureWorkspaceSession().present(
                requireUiState().currentQuery(), composeShellHost != null
        );
    }

    private void bindToolsWorkspace() {
        bindToolsWorkspace(false);
    }

    private void bindToolsWorkspace(boolean resetExpandedState) {
        if (composeShellHost != null && toolsWorkspace != null) {
            toolsWorkspace.onResume();
            composeShellHost.refreshTools(resetExpandedState);
            return;
        }
        if (toolsWorkspace != null) {
            toolsWorkspace.bind(toolsWorkspaceContainer);
            if (resetExpandedState) {
                toolsWorkspace.onShown();
            }
        }
    }

    private void restoreWorkspaceEditorForCurrentConfiguration() {
        if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.TEMPLATE) {
            ensureWorkspaceSession().restoreForConfiguration(
                    requireUiState().currentQuery(), composeShellHost != null
            );
        }
    }

    private static AppListFilterState.AppType parseAppType(String value, boolean legacyShowSystem) {
        if (value != null) {
            try {
                return AppListFilterState.AppType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                // Fall through to the legacy boolean representation.
            }
        }
        return legacyShowSystem ? AppListFilterState.AppType.ALL : AppListFilterState.AppType.USER;
    }

    private static AppListFilterState.SortOrder parseSortOrder(String value) {
        if (value != null) {
            try {
                return AppListFilterState.SortOrder.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                // Older saved state did not include ordering.
            }
        }
        return AppListFilterState.SortOrder.NAME;
    }

    private void bindSettingsWorkspace() {
        if (composeShellHost != null) {
            settingsWorkspaceSession.ensureComposeController();
            return;
        }
        if (settingsWorkspaceContainer == null || settingsWorkspaceSession == null) {
            return;
        }
        settingsWorkspaceSession.bindLegacy(settingsWorkspaceContainer);
    }

    private EditorPresentation.State createComposeAppEditorState() {
        return composeAppEditorController != null
                ? composeAppEditorController.createState()
                : null;
    }

    public void refreshComposeApps() {
        if (composeShellHost != null) {
            composeShellHost.refreshApps();
        }
    }

    public MainComposeShellHost composeShell() {
        return composeShellHost;
    }

    public boolean saveComposeEditorForDiagnostic(AppListItem item, EditorDraft draft) {
        return composeAppEditorSaveWorkflow != null
                && composeAppEditorSaveWorkflow.save(item, draft);
    }

    public void markComposeEditorSaved(EditorDraft draft) {
        if (composeAppEditorController != null) {
            composeAppEditorController.markSaved(draft);
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

    private void initializeWorkspaceSession(
            TemplateWorkspaceActivitySession.State initialState,
            String initialQuery
    ) {
        if (workspaceSession == null) {
            workspaceSession = new TemplateWorkspaceActivitySession(
                    this,
                    initialQuery,
                    initialState,
                    () -> {
                        if (composeShellHost != null) {
                            composeShellHost.refreshTemplates();
                        }
                    }
            );
        }
    }

    private TemplateWorkspaceActivitySession ensureWorkspaceSession() {
        initializeWorkspaceSession(
                null,
                requireUiState().currentQuery()
        );
        return workspaceSession;
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void resetHiddenWorkspacePresentation(MainUiState.WorkspaceMode visibleMode) {
        resetWorkspacePresentationUnlessMode(
                toolsWorkspaceContainer, visibleMode, MainUiState.WorkspaceMode.TOOLS);
        resetWorkspacePresentationUnlessMode(
                settingsWorkspaceContainer, visibleMode, MainUiState.WorkspaceMode.SETTINGS);
    }

    private static void resetWorkspacePresentationUnlessMode(
            View view,
            MainUiState.WorkspaceMode visibleMode,
            MainUiState.WorkspaceMode viewMode
    ) {
        if (visibleMode != viewMode) {
            resetWorkspacePresentation(view);
        }
    }

    private static void resetWorkspacePresentation(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
    }

    private void animateVisibleWorkspaceContent(MainUiState.WorkspaceMode mode) {
        View target = workspaceViewForMode(mode);
        if (target == null) {
            return;
        }
        target.animate().cancel();
        target.setAlpha(0f);
        target.setScaleX(WORKSPACE_CONTENT_ENTER_START_SCALE);
        target.setScaleY(WORKSPACE_CONTENT_ENTER_START_SCALE);
        target.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(WORKSPACE_TRANSITION_DURATION_MS)
                .setInterpolator(WORKSPACE_CONTENT_ENTER_INTERPOLATOR)
                .withEndAction(() -> {
                    target.setAlpha(1f);
                    target.setScaleX(1f);
                    target.setScaleY(1f);
                })
                .start();
    }

    private View workspaceViewForMode(MainUiState.WorkspaceMode mode) {
        if (mode == MainUiState.WorkspaceMode.APP) {
            return null;
        }
        if (mode == MainUiState.WorkspaceMode.TEMPLATE) {
            return null;
        }
        if (mode == MainUiState.WorkspaceMode.TOOLS) {
            return toolsWorkspaceContainer;
        }
        if (mode == MainUiState.WorkspaceMode.SETTINGS) {
            return settingsWorkspaceContainer;
        }
        return null;
    }

    private void handleAppsLoadRequests(List<MainViewModel.AppsLoadRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (MainViewModel.AppsLoadRequest request : requests) {
            installedAppsLoadSession.start(request);
        }
    }

    private void showFilterDialog() {
        MainUiState state = requireUiState();
        AppFilterComposeSheet.show(this,
                state.filterState.showSystemApps(),
                state.filterState.injectedOnly(),
                state.filterState.widthConfiguredOnly(),
                state.filterState.fontConfiguredOnly(),
                (showSystem, injectedOnly, widthOnly, fontOnly) -> {
            AppListFilterState filterState = new AppListFilterState(
                    showSystem, injectedOnly, widthOnly, fontOnly
            );
            appListFilterStateStore.save(filterState);
            dispatchMainUiAction(MainUiAction.filterChanged(filterState));
        });
    }

    private boolean maybeShowModuleRuntimeReloadAdvice() {
        return new ModuleRuntimeReloadNoticeCoordinator(this)
                .maybeShow(this::continueStartupDialogsAfterRuntimeReloadAdvice);
    }

    private void continueStartupDialogsAfterRuntimeReloadAdvice() {
        if (!updateSession.maybeShowStartupDisclaimerDialog()) {
            updateSession.maybeCheckForUpdatesOnStartup();
        }
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
        if (isLandscapeDetailMode() && landAppDetailSession.show(item)) {
            return;
        }
        appConfigSheetSession.show(item);
    }

    private void bindHomeWorkspace() {
        if (composeShellHost != null) {
            composeShellHost.refreshHome();
        }
    }

    private HomeWorkspaceState createHomeWorkspaceState() {
        DpisConfigStore configStore = getHookConfigStore();
        int visibleConfiguredAppCount = countUserVisibleConfiguredPackages(
                configStore,
                installedAppsLoadSession.loadScopeState()
        );
        return new HomeWorkspaceState(
                isActivatedForHome(),
                visibleConfiguredAppCount,
                ConfigStoreFactory.createLocalUiFontLibraryStore(
                        this,
                        DpisApplication.getXposedService()
                ).listFonts().size(),
                ensureWorkspaceSession().quickItemCount(),
                RootAccessProbe.cachedResult(),
                updateSession.getHomeUpdateUiState(),
                new HomeWorkspaceLayoutStore(this).load(),
                createHomeWorkspaceActions(),
                PageSettingsStore.isHomeEditButtonVisible(this)
        );
    }

    private boolean isActivatedForHome() {
        boolean libXposedService = HomeActivationStateResolver
                .hasModernLibXposedService(DpisApplication.getXposedService());
        boolean selfLoaded = DpisApplication.isXposedSelfLoaded();
        boolean activated = HomeActivationStateResolver.isActivatedForHome(
                libXposedService,
                selfLoaded);
        DpisLog.i("home activation resolved: libxposedService=" + libXposedService
                + ", selfLoaded=" + selfLoaded
                + ", activated=" + activated);
        return activated;
    }

    private HomeWorkspaceActions createHomeWorkspaceActions() {
        return new HomeWorkspaceActions() {
            @Override
            public void checkForUpdates() {
                updateSession.checkForUpdatesNow();
            }

            @Override
            public void openConfiguredAppsWorkspace() {
                setCurrentAppListPage(AppListPage.CONFIGURED_APPS, false);
                dispatchMainUiAction(
                        MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.APP)
                );
            }

            @Override
            public void openFontLibrary() {
                startActivity(new Intent(MainActivity.this, FontLibraryActivity.class));
            }

            @Override
            public void openTemplateWorkspace() {
                dispatchMainUiAction(
                        MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.TEMPLATE)
                );
            }

            @Override
            public void openModeHelp() {
                startActivity(new Intent(MainActivity.this, ModeHelpActivity.class));
            }

            @Override
            public void openDonate() {
                startActivity(DonateActivity.createIntent(MainActivity.this));
            }

            @Override
            public void saveHomeWorkspaceLayout(HomeWorkspaceLayout layout) {
                new HomeWorkspaceLayoutStore(MainActivity.this).save(layout);
                bindHomeWorkspace();
            }
        };
    }

    static int countUserVisibleConfiguredPackages(DpisConfigStore store,
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
        return landDetailContent;
    }

    public View landDetailEmptyView() {
        return landDetailEmptyView;
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

    private record RetainedState(List<AppListItem> appsSnapshot, String query, String templateQuery,
                                 AppListFilterState filterState,
                                 MainUiState.WorkspaceMode workspaceMode, int currentPage,
                                 int[] appListScrollPositions, int[] refreshingPagePositions,
                                 String editingPackageName, EditorDraft editingDraft,
                                 EditorDraft savedEditingDraft,
                                 EditorDraft prefillSnapshot,
                                 boolean prefillInvalidated,
                                 ConfigEditorDestination editingDestination,
                                 TemplateWorkspaceActivitySession.State workspaceSessionState,
                                 FeedbackDiagnosticActivitySession.State feedbackDiagnostic,
                                 UpdatePromptRequest pendingUpdatePrompt) {

            private RetainedState(
                    List<AppListItem> appsSnapshot,
                    String query,
                    String templateQuery,
                    AppListFilterState filterState,
                    MainUiState.WorkspaceMode workspaceMode,
                    int currentPage,
                    int[] appListScrollPositions,
                    int[] refreshingPagePositions,
                    String editingPackageName,
                    EditorDraft editingDraft,
                    EditorDraft savedEditingDraft,
                    EditorDraft prefillSnapshot,
                    boolean prefillInvalidated,
                    ConfigEditorDestination editingDestination,
                    TemplateWorkspaceActivitySession.State workspaceSessionState,
                    FeedbackDiagnosticActivitySession.State feedbackDiagnostic,
                    UpdatePromptRequest pendingUpdatePrompt
            ) {
                this.appsSnapshot = appsSnapshot;
                this.query = query != null ? query : "";
                this.templateQuery = templateQuery != null ? templateQuery : "";
                this.filterState
                        = filterState != null
                        ? filterState
                        : AppListFilterState.defaultState();
                this.workspaceMode
                        = workspaceMode != null ? workspaceMode : MainUiState.WorkspaceMode.APP;
                this.currentPage = currentPage;
                this.appListScrollPositions = appListScrollPositions != null
                        ? appListScrollPositions.clone()
                        : new int[0];
                this.refreshingPagePositions
                        = refreshingPagePositions != null
                        ? refreshingPagePositions.clone()
                        : new int[0];
                this.editingPackageName = editingPackageName;
                this.editingDraft = editingDraft;
                this.savedEditingDraft = savedEditingDraft;
                this.prefillSnapshot = prefillSnapshot;
                this.prefillInvalidated = prefillInvalidated;
                this.editingDestination = editingDestination != null
                        ? editingDestination
                        : ConfigEditorDestination.MAIN;
                this.workspaceSessionState = workspaceSessionState;
                this.feedbackDiagnostic = feedbackDiagnostic;
                this.pendingUpdatePrompt = pendingUpdatePrompt;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) {
                    return true;
                }
                if (!(object instanceof RetainedState other)) {
                    return false;
                }
                return currentPage == other.currentPage
                        && java.util.Objects.equals(appsSnapshot, other.appsSnapshot)
                        && java.util.Objects.equals(query, other.query)
                        && java.util.Objects.equals(templateQuery, other.templateQuery)
                        && java.util.Objects.equals(filterState, other.filterState)
                        && workspaceMode == other.workspaceMode
                        && java.util.Arrays.equals(appListScrollPositions, other.appListScrollPositions)
                        && java.util.Arrays.equals(refreshingPagePositions, other.refreshingPagePositions)
                        && java.util.Objects.equals(editingPackageName, other.editingPackageName)
                        && java.util.Objects.equals(editingDraft, other.editingDraft)
                        && java.util.Objects.equals(savedEditingDraft, other.savedEditingDraft)
                        && java.util.Objects.equals(prefillSnapshot, other.prefillSnapshot)
                        && prefillInvalidated == other.prefillInvalidated
                        && editingDestination == other.editingDestination
                        && java.util.Objects.equals(workspaceSessionState, other.workspaceSessionState)
                        && java.util.Objects.equals(feedbackDiagnostic, other.feedbackDiagnostic)
                        && java.util.Objects.equals(pendingUpdatePrompt, other.pendingUpdatePrompt);
            }

            @Override
            public int hashCode() {
                int result = java.util.Objects.hash(
                        appsSnapshot, query, templateQuery, filterState, workspaceMode, currentPage,
                        editingPackageName, editingDraft, savedEditingDraft, prefillSnapshot,
                        prefillInvalidated, editingDestination,
                        workspaceSessionState, feedbackDiagnostic, pendingUpdatePrompt);
                result = 31 * result + java.util.Arrays.hashCode(appListScrollPositions);
                return 31 * result + java.util.Arrays.hashCode(refreshingPagePositions);
            }

            @Override
            public String toString() {
                return "RetainedState[appsSnapshot=" + appsSnapshot
                        + ", query=" + query
                        + ", templateQuery=" + templateQuery
                        + ", currentPage=" + currentPage
                        + ", appListScrollPositions="
                        + java.util.Arrays.toString(appListScrollPositions)
                        + ", refreshingPagePositions="
                        + java.util.Arrays.toString(refreshingPagePositions) + "]";
            }
        }

}
