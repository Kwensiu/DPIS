package com.dpis.module;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.compose.ui.platform.ComposeView;


import com.dpis.module.appconfig.presentation.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigDialogCoordinator;
import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.appconfig.AppConfigPrefillPreview;
import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.appconfig.landdetail.LandAppDetailPaneBinder;
import com.dpis.module.appconfig.WechatDpiConfig;
import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.InstalledAppCatalogCoordinator;
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.fonts.FontLibraryActivity;
import com.dpis.module.fonts.HyperOsNativeAppDetector;
import com.dpis.module.fonts.device.HyperOsNativeProxyBindMounter;

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import com.dpis.module.home.DonateActivity;
import com.dpis.module.home.HomeActivationStateResolver;

import com.dpis.module.home.HomeWorkspaceActions;
import com.dpis.module.home.HomeWorkspaceLayout;
import com.dpis.module.home.HomeWorkspaceLayoutStore;
import com.dpis.module.home.HomeWorkspaceState;
import com.dpis.module.settings.PageSettingsStore;
import com.dpis.module.home.ModeHelpActivity;

import com.dpis.module.process.presentation.ProcessActionConfirm;
import com.dpis.module.process.presentation.ProcessActionHandler;
import com.dpis.module.quirks.presentation.WechatDpiHelp;
import com.dpis.module.quirks.WechatDpiEditor;
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder;
import com.dpis.module.root.RootAccessProbe;
import com.dpis.module.runtime.ModuleRuntimeReloadNoticeCoordinator;
import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

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
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportPropertySyncer;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.github.libxposed.service.XposedService;
import kotlin.Unit;
import com.dpis.module.ui.presentation.MainComposeShellHost;
import com.dpis.module.ui.presentation.MainWorkspacePresentationCoordinator;
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost;
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
    private static final long INSTALLED_APP_CATALOG_TTL_MS = 60_000L;
    private static final String XIAOMI_GET_INSTALLED_APPS_PERMISSION
            = "com.android.permission.GET_INSTALLED_APPS";
    private static final int REQUEST_XIAOMI_GET_INSTALLED_APPS = 10022;


    private final MainUpdateSession updateSession
            = new MainUpdateSession(this, this::bindHomeWorkspaceIfVisible);
    private final WechatDpiHelp wechatDpiHelp
            = new WechatDpiHelp(this, this::composeShell);
    private final ProcessActionHandler processActionHandler
            = new ProcessActionHandler(
                    this,
                    this::syncRuntimePropertiesForTargetLaunch,
                    new ProcessActionConfirm(this, this::composeShell));
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
    private final InstalledAppCatalogCoordinator installedAppCatalogCoordinator
            = new InstalledAppCatalogCoordinator(
                    createInstalledAppCatalogHost(),
                    INSTALLED_APP_CATALOG_TTL_MS
            );
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
    private boolean installedAppsPermissionRequestInFlight;
    private boolean pendingInstalledAppsLoadAfterPermission;
    private boolean installedAppsPermissionRequestCompleted;
    private MainUiState.WorkspaceMode renderedWorkspaceMode;
    private View activeEditorRoot;
    private String activeEditorPackageName;
    private BottomSheetDialog activeAppEditorDialog;
    private final Map<String, Integer> pendingRuntimePropertyGenerations = new HashMap<>();

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
        installedAppCatalogCoordinator.shutdown();
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
        if (requestCode != REQUEST_XIAOMI_GET_INSTALLED_APPS) {
            return;
        }
        installedAppsPermissionRequestInFlight = false;
        boolean shouldReload = pendingInstalledAppsLoadAfterPermission;
        pendingInstalledAppsLoadAfterPermission = false;
        installedAppsPermissionRequestCompleted = true;
        if (shouldReload) {
            dispatchMainUiAction(MainUiAction.requestAppsLoad(true));
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        MainUiState state = requireUiState();
        List<AppListItem> snapshot = state.appsSnapshot();
        int currentPage = landCurrentPage.position();
        EditorDraft draft = captureAppConfigDraft();
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
        requestAppsLoad(true);
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
        requestAppsLoad(false);
    }

    private void requestAppsLoad(boolean forceInstalledAppCatalogReload) {
        boolean permissionReady = ensureInstalledAppsPermissionBeforeLoad();
        DpisLog.i("app list load permission gate: ready=" + permissionReady
                + ", forceReload=" + forceInstalledAppCatalogReload);
        if (!permissionReady) {
            pendingInstalledAppsLoadAfterPermission = true;
            return;
        }
        dispatchMainUiAction(
                MainUiAction.requestAppsLoad(forceInstalledAppCatalogReload)
        );
    }

    private boolean ensureInstalledAppsPermissionBeforeLoad() {
        boolean xiaomiPermissionDeclared = isXiaomiInstalledAppsPermissionDeclared();
        DpisLog.i("installed apps permission state: sdk=" + Build.VERSION.SDK_INT
                + ", requestCompleted=" + installedAppsPermissionRequestCompleted
                + ", requestInFlight=" + installedAppsPermissionRequestInFlight
                + ", xiaomiPermissionDeclared=" + xiaomiPermissionDeclared);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || installedAppsPermissionRequestCompleted
                || !xiaomiPermissionDeclared) {
            return true;
        }
        try {
            int permissionState = checkPermission(
                    XIAOMI_GET_INSTALLED_APPS_PERMISSION,
                    Process.myPid(),
                    Process.myUid()
            );
            DpisLog.i("installed apps permission check: granted="
                    + (permissionState == PackageManager.PERMISSION_GRANTED));
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            if (!installedAppsPermissionRequestInFlight) {
                installedAppsPermissionRequestInFlight = true;
                DpisLog.i("installed apps permission request started");
                requestPermissions(
                        new String[]{XIAOMI_GET_INSTALLED_APPS_PERMISSION},
                        REQUEST_XIAOMI_GET_INSTALLED_APPS
                );
            }
            return false;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private boolean isXiaomiInstalledAppsPermissionDeclared() {
        try {
            getPackageManager().getPermissionInfo(
                    XIAOMI_GET_INSTALLED_APPS_PERMISSION,
                    0
            );
            return true;
        } catch (PackageManager.NameNotFoundException
                | RuntimeException ignored) {
            return false;
        }
    }

    private void startAppsLoad(MainViewModel.AppsLoadRequest request) {
        int requestId = request.requestId;
        boolean forceInstalledAppCatalogReload
                = request.forceInstalledAppCatalogReload;
        new Thread(() -> {
            List<AppListItem> loaded = null;
            try {
                loaded = loadInstalledApps(forceInstalledAppCatalogReload);
            } catch (Throwable throwable) {
                DpisLog.e("list load failed", throwable);
            }
            List<AppListItem> finalLoaded = loaded;
            DpisLog.i("app list load finished: requestId=" + requestId
                    + ", loaded=" + (finalLoaded == null ? "null" : finalLoaded.size())
                    + ", forceReload=" + forceInstalledAppCatalogReload);
            runOnUiThread(() -> onAppsLoadFinished(requestId, finalLoaded));
        }, "dpis-load-apps-" + requestId).start();
    }

    private void onAppsLoadFinished(int requestId, List<AppListItem> loaded) {
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

    private List<AppListItem> loadInstalledApps(boolean forceInstalledAppCatalogReload) {
        ScopeState scopeState = loadScopeState();
        return installedAppCatalogCoordinator.loadInstalledApps(
                forceInstalledAppCatalogReload,
                getHookConfigStore(),
                scopeState.packages,
                scopeState.known
        );
    }

    private ScopeState loadScopeState() {
        Set<String> scopePackages = new HashSet<>();
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            return new ScopeState(scopePackages, false);
        }
        try {
            List<String> scope = service.getScope();
            scopePackages.addAll(scope);
            return new ScopeState(scopePackages, true);
        } catch (RuntimeException ignored) {
            scopePackages.clear();
        }
        return new ScopeState(scopePackages, false);
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
                    showEditBottomSheet(appItem);
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
        if (activeAppEditorDialog != null) {
            activeAppEditorDialog.dismiss();
        }
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
            startAppsLoad(request);
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

    private InstalledAppCatalogCoordinator.Host createInstalledAppCatalogHost() {
        return new InstalledAppCatalogCoordinator.Host() {
            @NonNull
            @Override
            public PackageManager getPackageManager() {
                return MainActivity.this.getPackageManager();
            }

            @NonNull
            @Override
            public String getSelfPackageName() {
                return MainActivity.this.getPackageName();
            }

        };
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
        activeEditorPackageName = item.packageName;
        if (isLandscapeDetailMode() && landAppDetailSession.show(item)) {
            return;
        }
        showEditBottomSheet(item);
    }

    private void showEditBottomSheet(AppListItem item) {
        if (activeAppEditorDialog != null && activeAppEditorDialog.isShowing()) {
            return;
        }
        DpisConfigStore store = getHookConfigStore();
        AppListItem sheetItem = AppConfigPrefillPreview.resolveForEditor(this, item, store);
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_app_config,
                root,
                false
        );
        AppConfigDialogBinder binder = new AppConfigDialogBinder(
                this,
                createAppConfigDialogHost()
        );
        binder.bind(
                dialogView,
                sheetItem,
                systemHooksEnabled
        );
        EditorDraft draft = mainViewModel != null
                ? mainViewModel.getEditingDraft()
                : null;
        if (draft != null) {
            applyAppConfigDraft(dialogView, draft);
            binder.applyRetainedDraft(
                    dialogView,
                    sheetItem,
                    systemHooksEnabled,
                    draft.selectedTypefaceId,
                    draft.draftFontHookDomainsRaw,
                    draft.viewportApplyMode,
                    draft.fontHookDomainsResetRequested,
                    draft.viewportApplyModeResetRequested
            );
            WechatDpiSheetBinder.applyDraft(
                    dialogView,
                    draft.wechatDpiInput
            );
        }
        activeEditorRoot = dialogView;
        activeEditorPackageName = item.packageName;
        BottomSheetDialog dialog = new AppConfigDialogCoordinator(this).show(
                dialogView
        );
        activeAppEditorDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (activeEditorRoot == dialogView) {
                activeEditorRoot = null;
                activeEditorPackageName = null;
            }
            if (activeAppEditorDialog == dialog) {
                activeAppEditorDialog = null;
            }
            if (mainViewModel != null && !isChangingConfigurations()) {
                mainViewModel.clearEditingPackageName();
                mainViewModel.clearEditingDraft();
            }
        });
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
                loadScopeState()
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

    record ScopeState(Set<String> packages, boolean known) {
            ScopeState(Set<String> packages, boolean known) {
                this.packages = packages != null ? packages : Collections.emptySet();
                this.known = known;
            }
        }

    private void maybeStartRootAccessProbe() {
        RootAccessProbe.refreshAsync(result -> runOnUiThread(() -> {
            if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
                bindHomeWorkspace();
            }
        }));
    }

    private AppConfigSaveHandler.Result finalizeAppConfigSaveWithWechatDpi(
            AppConfigSaveHandler.Result saveResult,
            View configRoot,
            String packageName,
            boolean dpisEnabled,
            DpisConfigStore store) {
        if (saveResult == null) {
            return AppConfigSaveHandler.Result.failure(R.string.system_settings_save_failed);
        }
        if (!saveResult.success) {
            return saveResult;
        }
        if (!WechatDpiSheetBinder.save(configRoot, packageName, dpisEnabled, store)) {
            return AppConfigSaveHandler.Result.failure(
                    WechatDpiSheetBinder.isInputValid(configRoot)
                            ? R.string.system_settings_save_failed
                            : R.string.status_save_invalid);
        }
        onRuntimeConfigSaved();
        return saveResult;
    }

    public AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
            AppConfigSaveHandler.Result saveResult,
            View configRoot,
            String packageName,
            boolean dpisEnabled,
            DpisConfigStore store) {
        AppConfigSaveHandler.Result result = finalizeAppConfigSaveWithWechatDpi(
                saveResult,
                configRoot,
                packageName,
                dpisEnabled,
                store);
        if (!result.success) {
            return result;
        }
        scheduleRuntimePropertiesForTargetLaunch(packageName);
        return result;
    }

    public AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
            AppConfigSaveHandler.Result saveResult,
            String wechatDpiInput,
            String packageName,
            boolean dpisEnabled,
            DpisConfigStore store) {
        if (saveResult == null) {
            return AppConfigSaveHandler.Result.failure(R.string.system_settings_save_failed);
        }
        if (!saveResult.success) {
            return saveResult;
        }
        if (!WechatDpiEditor.save(wechatDpiInput, packageName, dpisEnabled, store)) {
            return AppConfigSaveHandler.Result.failure(
                    WechatDpiEditor.isInputValid(wechatDpiInput)
                            ? R.string.system_settings_save_failed
                            : R.string.status_save_invalid);
        }
        onRuntimeConfigSaved();
        scheduleRuntimePropertiesForTargetLaunch(packageName);
        return saveResult;
    }

    public void onRuntimeConfigSaved() {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        requestAppsLoad();
    }

    private void scheduleRuntimePropertiesForTargetLaunch(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        int generation;
        synchronized (pendingRuntimePropertyGenerations) {
            Integer currentGeneration = pendingRuntimePropertyGenerations.get(packageName);
            generation = (currentGeneration != null ? currentGeneration : 0) + 1;
            pendingRuntimePropertyGenerations.put(packageName, generation);
        }
        Thread syncThread = new Thread(
                () -> syncRuntimePropertiesForTargetLaunch(packageName, generation),
                "dpis-runtime-property-target-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    public void syncRuntimePropertiesForTargetLaunch(String packageName) {
        Integer generation;
        synchronized (pendingRuntimePropertyGenerations) {
            generation = pendingRuntimePropertyGenerations.get(packageName);
        }
        if (generation == null) {
            return;
        }
        syncRuntimePropertiesForTargetLaunch(packageName, generation);
    }

    private void syncRuntimePropertiesForTargetLaunch(String packageName, int generation) {
        DpisConfigStore store = getHookConfigStore();
        ViewportPropertySyncer.syncTarget(packageName, store);
        FontRuntimePropertySyncer.syncTarget(packageName, store);
        synchronized (pendingRuntimePropertyGenerations) {
            Integer currentGeneration = pendingRuntimePropertyGenerations.get(packageName);
            if (currentGeneration != null && currentGeneration == generation) {
                pendingRuntimePropertyGenerations.remove(packageName);
            }
        }
    }

    private String viewportScaleDraftFor(
            AppListItem item,
            ViewportTargetSpec activeSpec
    ) {
        if (activeSpec != null && activeSpec.isRelativeScale()) {
            return AppConfigInputValidation.formatScaleMilliPercentInput(activeSpec.scaleMilliPercent());
        }
        if (item.viewportScaleMilliPercent != null) {
            return AppConfigInputValidation.formatScaleMilliPercentInput(item.viewportScaleMilliPercent);
        }
        return "";
    }

    private String viewportAbsoluteDraftFor(
            AppListItem item,
            ViewportTargetSpec activeSpec
    ) {
        if (activeSpec != null && activeSpec.isAbsoluteDp()) {
            return String.valueOf(activeSpec.absoluteWidthDp());
        }
        if (item.viewportWidthDp != null) {
            return String.valueOf(item.viewportWidthDp);
        }
        return "";
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
        View root = activeEditorRoot;
        if (root == null || !item.packageName.equals(activeEditorPackageName)) {
            return item;
        }
        if (AppConfigDialogBinder.viewsFor(root) != null) {
            return saveDialogConfigForDiagnostic(item, root);
        }
        if (LandAppDetailPaneBinder.stateFor(root) != null) {
            return landAppDetailSession.saveForDiagnostic(item, state, root);
        }
        return item;
    }

    private AppListItem saveDialogConfigForDiagnostic(AppListItem item, View root) {
        AppConfigDialogBinder.AppConfigDialogViews views
                = AppConfigDialogBinder.viewsFor(root);
        AppConfigDialogBinder.AppConfigDialogState state
                = AppConfigDialogBinder.stateFor(root);
        if (views == null || state == null) {
            return item;
        }
        if (!AppConfigDialogBinder.updateSaveButtonState(root, views)) {
            showToast(R.string.status_save_invalid);
            return null;
        }
        AppConfigSaveHandler.Result result = createAppConfigDialogHost().saveAppConfig(
                root,
                item,
                state.dpisEnabled,
                views.viewportInputView,
                views.fontInputView,
                AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
                state.viewportApplyMode,
                state.viewportApplyModeResetRequested,
                AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
                state.selectedTypefaceId,
                state.draftFontHookDomainsRaw,
                state.fontHookDomainsResetRequested,
                state.viewportScaleInput,
                state.viewportAbsoluteInput
        );
        if (result.messageResId != 0) {
            showToast(result.messageResId);
        }
        if (!result.success) {
            return null;
        }
        // Keep feedback diagnostic on the same save aftermath as the sheet save button.
        // Otherwise this side path can persist config but skip scope/proxy preparation.
        state.previewFromGlobalPrefill = false;
        state.draftFontHookDomainsRaw = null;
        state.fontHookDomainsResetRequested = false;
        state.viewportApplyModeResetRequested = false;
        state.captureSavedDraft(views, false);
        AppConfigDialogBinder.showSaveButtonFeedback(views.saveButton);
        AppConfigDialogBinder binder = new AppConfigDialogBinder(this, createAppConfigDialogHost());
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        AppConfigDialogBinder.AppConfigDialogActionStyle style
                = AppConfigDialogBinder.captureDialogActionStyle(views.scopeButton);
        binder.refreshDialogState(views, state, style, systemHooksEnabled, item);
        binder.syncHyperOsNativeProxyAfterSave(item, views, state);
        binder.requestScopeAfterSuccessfulSave(root, item, views, state, style, systemHooksEnabled);
        return item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName));
    }

    private Integer readPersistedWechatDpiForDiagnostic(String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null;
        }
        DpisConfigStore store = getHookConfigStore();
        return store != null ? store.getWechatDpi(packageName) : null;
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

    public View currentEditorRoot() {
        View root = activeEditorRoot;
        if (root == null
                && landDetailContent != null
                && landDetailContent.getChildCount() > 0) {
            root = landDetailContent.getChildAt(0);
        }
        return root;
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
        executeHyperOsNativeProxyMount(item, apply, ignored -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    private void executeHyperOsNativeProxyMount(
            AppListItem item,
            boolean apply,
            HyperOsNativeProxyMountCallback onFinished
    ) {
        new Thread(() -> {
            HyperOsNativeProxyBindMounter.MountPlan plan
                    = HyperOsNativeProxyBindMounter.createPlan(
                            this,
                            item.packageName
                    );
            HyperOsNativeProxyBindMounter.MountResult result = apply
                    ? HyperOsNativeProxyBindMounter.apply(plan)
                    : HyperOsNativeProxyBindMounter.unmount(plan);
            DpisLog.i(
                    "HyperOS Native Proxy "
                    + (apply ? "apply" : "rollback")
                    + " package="
                    + item.packageName
                    + " success="
                    + result.success()
                    + " output="
                    + result.output()
            );
            int messageResId = apply
                    ? R.string.dialog_hyperos_native_proxy_apply_failed
                    : R.string.dialog_hyperos_native_proxy_unmount_failed;
            runOnUiThread(() -> {
                if (!result.success()) {
                    showToast(messageResId);
                }
                if (onFinished != null) {
                    onFinished.onFinished(result.success());
                }
            });
        }, "DPIS-HyperOsNativeProxyMount").start();
    }

    public void executeDialogProcessAction(
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action
    ) {
        if (action == AppConfigDialogBinder.ProcessAction.RESTART
                && shouldPrepareHyperOsNativeProxyForRestart(item)) {
            // Re-prepare before restart because APK updates can leave an old bind mount
            // pointing at a deleted module native library.
            executeHyperOsNativeProxyMount(item, true, success -> {
                if (success) {
                    executeDialogProcessActionAfterHyperOsProxyReady(
                            item,
                            action
                    );
                }
            });
            return;
        }
        executeDialogProcessActionAfterHyperOsProxyReady(item, action);
    }

    private boolean shouldPrepareHyperOsNativeProxyForRestart(
            AppListItem item
    ) {
        if (!isHyperOsNativeProxyCandidate(item)) {
            return false;
        }
        DpisConfigStore store = getHookConfigStore();
        return (store.isTargetDpisEnabled(item.packageName)
                && hasActiveStoredConfig(store, item.packageName));
    }

    /** The catalogue intentionally does not preload metadata for every installed package. */
    public boolean isHyperOsNativeProxyCandidate(AppListItem item) {
        return item != null && (item.hyperOsNativeProxyCandidate
                || HyperOsNativeAppDetector.isNativeProxyCandidate(
                        getPackageManager(), item.packageName));
    }

    private static boolean hasActiveStoredConfig(
            DpisConfigStore store,
            String packageName
    ) {
        ViewportTargetSpec viewportTargetSpec = store.getTargetViewportSpec(
                packageName
        );
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        return (viewportTargetSpec.isEnabled()
                || fontScalePercent != null
                || store.hasTargetAppSpecificConfig(packageName));
    }

    private void executeDialogProcessActionAfterHyperOsProxyReady(
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action
    ) {
        ProcessActionHandler.Action mappedAction = switch (action) {
            case START ->
                ProcessActionHandler.Action.START;
            case RESTART ->
                ProcessActionHandler.Action.RESTART;
            case STOP ->
                ProcessActionHandler.Action.STOP;
        };
        if (item != null) {
            processActionHandler.execute(item, mappedAction);
        }
    }

    public boolean isSystemHookEnabledFromStore() {
        return cachedSystemHookEffectiveEnabled;
    }

    private interface HyperOsNativeProxyMountCallback {

        void onFinished(boolean success);
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
        return mainViewModel != null ? mainViewModel.getEditingDraft() : null;
    }

    public void rememberActiveEditor(View root, String packageName) {
        activeEditorRoot = root;
        activeEditorPackageName = packageName;
    }

    private EditorDraft captureAppConfigDraft() {
        View root = activeEditorRoot;
        String packageName = activeEditorPackageName;
        if (root == null
                && landDetailContent != null
                && landDetailContent.getChildCount() > 0) {
            root = landDetailContent.getChildAt(0);
            packageName = mainViewModel != null
                    ? mainViewModel.getEditingPackageName()
                    : packageName;
        }
        if (root == null) {
            return null;
        }
        TextInputEditText viewportInput = findEditorInput(
                root,
                R.id.land_detail_viewport_input,
                R.id.dialog_viewport_input
        );
        TextInputEditText fontInput = findEditorInput(
                root,
                R.id.land_detail_font_scale_input,
                R.id.dialog_font_scale_input
        );
        AppConfigDialogBinder.AppConfigDialogState state = findEditorState(root);
        String viewportText
                = viewportInput != null && viewportInput.getText() != null
                ? viewportInput.getText().toString()
                : "";
        String fontText
                = fontInput != null && fontInput.getText() != null
                ? fontInput.getText().toString()
                : "";
        String viewportMode = viewportInput != null
                ? AppConfigDialogBinder.resolveViewportMode(findViewportModeToggle(root))
                : ViewportTargetType.RELATIVE_SCALE;
        String fontMode = fontInput != null
                ? AppConfigDialogBinder.resolveFontMode(findFontModeToggle(root))
                : FontApplyMode.SYSTEM_EMULATION;
        if (state != null && !state.packageName.isBlank()) {
            packageName = state.packageName;
        }
        if ((packageName == null || packageName.isBlank()) && mainViewModel != null) {
            packageName = mainViewModel.getEditingPackageName();
        }
        EditorDraft current = mainViewModel != null
                ? mainViewModel.getEditingDraft()
                : null;
        boolean useCurrentState = current != null && current.packageName.equals(packageName);
        return new EditorDraft(
                packageName,
                viewportText,
                state != null ? state.viewportScaleInput
                        : useCurrentState ? current.viewportScaleInput : "",
                state != null ? state.viewportAbsoluteInput
                        : useCurrentState ? current.viewportAbsoluteInput : "",
                viewportMode,
                fontText,
                fontMode,
                state != null ? state.selectedTypefaceId
                        : useCurrentState ? current.selectedTypefaceId : null,
                state != null ? state.draftFontHookDomainsRaw
                        : useCurrentState ? current.draftFontHookDomainsRaw : null,
                state != null ? state.viewportApplyMode
                        : useCurrentState ? current.viewportApplyMode : ViewportApplyMode.OFF,
                state != null
                        ? state.fontHookDomainsResetRequested
                        : useCurrentState && current.fontHookDomainsResetRequested,
                state != null
                        ? state.viewportApplyModeResetRequested
                        : useCurrentState && current.viewportApplyModeResetRequested,
                WechatDpiSheetBinder.captureDraft(root),
                state != null ? state.scopeSelected
                        : useCurrentState && current.scopeSelected,
                state != null ? state.dpisEnabled
                        : useCurrentState && current.dpisEnabled
        );
    }

    public void updateEditingDraft(AppConfigDialogBinder.AppConfigDialogState state) {
        if (mainViewModel == null || state == null) {
            return;
        }
        EditorDraft captured = captureAppConfigDraft();
        if (captured != null) {
            mainViewModel.setEditingDraft(captured);
            return;
        }
        EditorDraft current = mainViewModel.getEditingDraft();
        String packageName = !state.packageName.isBlank()
                ? state.packageName
                : mainViewModel.getEditingPackageName();
        EditorDraft draft = new EditorDraft(
                packageName,
                current != null ? current.viewportInput : "",
                current != null ? current.viewportScaleInput : "",
                current != null ? current.viewportAbsoluteInput : "",
                current != null ? current.viewportMode : ViewportTargetType.RELATIVE_SCALE,
                current != null ? current.fontInput : "",
                current != null ? current.fontMode : FontApplyMode.SYSTEM_EMULATION,
                state.selectedTypefaceId,
                state.draftFontHookDomainsRaw,
                state.viewportApplyMode,
                state.fontHookDomainsResetRequested,
                state.viewportApplyModeResetRequested,
                current != null ? current.wechatDpiInput : null,
                state.scopeSelected,
                state.dpisEnabled
        );
        mainViewModel.setEditingDraft(draft);
    }

    public void applyAppConfigDraft(View root, EditorDraft draft) {
        if (draft == null || root == null) {
            return;
        }
        TextInputEditText viewportInput = findEditorInput(
                root,
                R.id.land_detail_viewport_input,
                R.id.dialog_viewport_input
        );
        TextInputEditText fontInput = findEditorInput(
                root,
                R.id.land_detail_font_scale_input,
                R.id.dialog_font_scale_input
        );
        AppConfigDialogBinder.ModeToggle viewportToggle
                = findViewportModeToggle(root);
        AppConfigDialogBinder.ModeToggle fontToggle = findFontModeToggle(root);
        TextInputLayout viewportInputLayout = findEditorInputLayout(
                root,
                R.id.land_detail_viewport_input_layout,
                R.id.dialog_viewport_input_layout
        );
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportToggle,
                draft.viewportMode,
                false
        );
        if (viewportInputLayout != null) {
            if (root.findViewById(R.id.dialog_viewport_input_layout) != null) {
                new AppConfigDialogBinder(this, createAppConfigDialogHost())
                        .bindViewportInputHint(
                                viewportInputLayout,
                                draft.viewportMode
                        );
            } else {
                viewportInputLayout.setHint(
                        ViewportTargetType.RELATIVE_SCALE.equals(
                                ViewportTargetType.normalize(draft.viewportMode)
                        )
                                ? R.string.dialog_viewport_hint_scale
                                : R.string.dialog_viewport_hint_absolute
                );
            }
        }
        AppConfigDialogBinder.bindFontModeToggle(
                fontToggle,
                draft.fontMode,
                false
        );
        if (viewportInput != null) {
            viewportInput.setText(draft.viewportInput);
        }
        if (fontInput != null) {
            fontInput.setText(draft.fontInput);
        }
    }

    private TextInputEditText findEditorInput(
            View root,
            int landId,
            int dialogId
    ) {
        TextInputEditText input = root.findViewById(landId);
        return input != null ? input : root.findViewById(dialogId);
    }

    private TextInputLayout findEditorInputLayout(
            View root,
            int landId,
            int dialogId
    ) {
        TextInputLayout inputLayout = root.findViewById(landId);
        return inputLayout != null ? inputLayout : root.findViewById(dialogId);
    }

    private AppConfigDialogBinder.ModeToggle findViewportModeToggle(View root) {
        View landContainer = root.findViewById(
                R.id.land_detail_viewport_mode_toggle_button
        );
        if (landContainer != null) {
            return new AppConfigDialogBinder.ModeToggle(
                    landContainer,
                    root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                    root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                    root.findViewById(R.id.land_detail_viewport_mode_width_label)
            );
        }
        return new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.dialog_viewport_mode_toggle_button),
                root.findViewById(R.id.dialog_viewport_mode_toggle_thumb),
                root.findViewById(R.id.dialog_viewport_mode_system_label),
                root.findViewById(R.id.dialog_viewport_mode_compat_label)
        );
    }

    private AppConfigDialogBinder.ModeToggle findFontModeToggle(View root) {
        View landContainer = root.findViewById(
                R.id.land_detail_font_mode_toggle_button
        );
        if (landContainer != null) {
            return new AppConfigDialogBinder.ModeToggle(
                    landContainer,
                    root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                    root.findViewById(R.id.land_detail_font_mode_system_label),
                    root.findViewById(R.id.land_detail_font_mode_compat_label)
            );
        }
        return new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.dialog_font_mode_toggle_button),
                root.findViewById(R.id.dialog_font_mode_toggle_thumb),
                root.findViewById(R.id.dialog_font_mode_system_label),
                root.findViewById(R.id.dialog_font_mode_compat_label)
        );
    }

    private AppConfigDialogBinder.AppConfigDialogState findEditorState(
            View root
    ) {
        AppConfigDialogBinder.AppConfigDialogState dialogState
                = AppConfigDialogBinder.stateFor(root);
        return dialogState != null
                ? dialogState
                : LandAppDetailPaneBinder.stateFor(root);
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
