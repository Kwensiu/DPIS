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
import androidx.core.view.ViewCompat;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigDialogCoordinator;
import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.appconfig.AppConfigPrefillPreview;
import com.dpis.module.appconfig.AppConfigSaveHandler;
import com.dpis.module.appconfig.EditorDialogStateFactory;
import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.appconfig.EditorPresentation;
import com.dpis.module.appconfig.LandAppDetailPaneBinder;
import com.dpis.module.appconfig.WechatDpiConfig;
import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.InstalledAppCatalogCoordinator;
import com.dpis.module.diagnostics.AppLauncher;
import com.dpis.module.diagnostics.Coordinator;
import com.dpis.module.diagnostics.ExportBuilder;
import com.dpis.module.diagnostics.LogGate;
import com.dpis.module.diagnostics.PackageActions;
import com.dpis.module.diagnostics.PageController;
import com.dpis.module.diagnostics.ResultSheet;
import com.dpis.module.diagnostics.Session;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.fonts.FontLibraryActivity;
import com.dpis.module.fonts.HyperOsNativeAppDetector;
import com.dpis.module.fonts.HyperOsNativeProxyBindMounter;
import com.dpis.module.fonts.hookdomain.FontHookDomainDialog;
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation;
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;
import com.dpis.module.home.DonateActivity;
import com.dpis.module.home.HomeActivationStateResolver;
import com.dpis.module.home.HomeUpdateUiState;
import com.dpis.module.home.HomeWorkspaceActions;
import com.dpis.module.home.HomeWorkspaceLayout;
import com.dpis.module.home.HomeWorkspaceLayoutStore;
import com.dpis.module.home.HomeWorkspaceState;
import com.dpis.module.home.ModeHelpActivity;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;
import com.dpis.module.process.ProcessActionHandler;
import com.dpis.module.quirks.WechatDpiSheetBinder;
import com.dpis.module.root.RootAccessProbe;
import com.dpis.module.runtime.ModuleRuntimeReloadNoticeCoordinator;
import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.runtime.font.FontRuntimePropertySyncer;
import com.dpis.module.settings.StartupDisclaimerStore;
import com.dpis.module.settings.SystemFontScaleToolPresenter;
import com.dpis.module.settings.SystemScopeCoordinator;
import com.dpis.module.settings.ToolsWorkspaceBinder;
import com.dpis.module.templates.TemplateWorkspaceActivitySession;
import com.dpis.module.templates.TemplateWorkspacePresentationSource;
import com.dpis.module.ui.DialogWindowSizer;
import com.dpis.module.ui.TouchFeedbackBinder;
import com.dpis.module.ui.WatchUiMode;
import com.dpis.module.ui.WatchWorkspaceChromeBinder;
import com.dpis.module.ui.WindowInsetsBinder;
import com.dpis.module.ui.compose.AppFilterComposeSheet;
import com.dpis.module.ui.compose.ComposeMessageDialog;
import com.dpis.module.ui.dialog.ConfirmDialog;
import com.dpis.module.updates.GitHubReleaseNotesFetcher;
import com.dpis.module.updates.ReleaseNotesCacheStore;
import com.dpis.module.updates.ReleaseNotesController;
import com.dpis.module.updates.StartupUpdateCheckCoordinator;
import com.dpis.module.updates.StartupUpdateCheckOnce;
import com.dpis.module.updates.StartupUpdateDownloadExecutor;
import com.dpis.module.updates.StartupUpdateManifest;
import com.dpis.module.updates.StartupUpdatePackageHandler;
import com.dpis.module.updates.UpdateAvailableDialog;
import com.dpis.module.updates.UpdateCoordinator;
import com.dpis.module.updates.UpdateDownloadCoordinator;
import com.dpis.module.updates.UpdatePromptDialogCoordinator;
import com.dpis.module.updates.UpdatePromptRequest;
import com.dpis.module.updates.UpdateStateStore;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportPropertySyncer;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.service.XposedService;
import kotlin.Unit;

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
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;
    private static final long INSTALLED_APP_CATALOG_TTL_MS = 60_000L;
    private static final String XIAOMI_GET_INSTALLED_APPS_PERMISSION
            = "com.android.permission.GET_INSTALLED_APPS";
    private static final int REQUEST_XIAOMI_GET_INSTALLED_APPS = 10022;
    private static final int REQUEST_SAVE_FEEDBACK_DIAGNOSTIC = 10024;

    private final UpdateCoordinator updateCoordinator = new UpdateCoordinator();
    private final StartupUpdateDownloadExecutor startupUpdateDownloadExecutor
            = new StartupUpdateDownloadExecutor(
                    UPDATE_CONNECT_TIMEOUT_MS,
                    UPDATE_READ_TIMEOUT_MS,
                    DOWNLOAD_BUFFER_SIZE,
                    DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS
            );
    private UpdateStateStore updateStateStore;
    private UpdateDownloadCoordinator updateDownloadCoordinator;
    private final ProcessActionHandler processActionHandler
            = new ProcessActionHandler(this, this::syncRuntimePropertiesForTargetLaunch);
    private final AppConfigSaveHandler appConfigSaveHandler
            = new AppConfigSaveHandler();
    private Session feedbackDiagnosticSession;
    private final AppLauncher feedbackDiagnosticAppLauncher
            = new AppLauncher(this);
    private final ExecutorService feedbackDiagnosticExportExecutor
            = Executors.newSingleThreadExecutor();
    private final PackageActions feedbackDiagnosticPackageActions
            = new PackageActions(
                    this,
                    feedbackDiagnosticExportExecutor,
                    REQUEST_SAVE_FEEDBACK_DIAGNOSTIC
            );
    private final StartupUpdatePackageHandler startupUpdatePackageHandler
            = new StartupUpdatePackageHandler(this);
    private final ExecutorService startupUpdateExecutor
            = Executors.newSingleThreadExecutor();
    private final SystemScopeCoordinator systemScopeCoordinator
            = new SystemScopeCoordinator(createSystemScopeHost());
    private final InstalledAppCatalogCoordinator installedAppCatalogCoordinator
            = new InstalledAppCatalogCoordinator(
                    createInstalledAppCatalogHost(),
                    INSTALLED_APP_CATALOG_TTL_MS
            );
    private final StartupUpdateCheckCoordinator startupUpdateCheckCoordinator
            = new StartupUpdateCheckCoordinator(
                    createStartupUpdateCheckHost(),
                    updateCoordinator,
                    UPDATE_CONNECT_TIMEOUT_MS,
                    UPDATE_READ_TIMEOUT_MS
            );
    private UpdatePromptDialogCoordinator updatePromptDialogCoordinator;
    private ReleaseNotesController releaseNotesController;
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
    private ToolsWorkspaceBinder toolsWorkspaceBinder;
    private SystemFontScaleToolPresenter composeToolsPresenter;
    private SystemServerSettingsPageController settingsPageController;
    private boolean cachedSystemHookEffectiveEnabled;
    private boolean skipNextImmediateServiceReload;
    private boolean installedAppsPermissionRequestInFlight;
    private boolean pendingInstalledAppsLoadAfterPermission;
    private boolean installedAppsPermissionRequestCompleted;
    private MainUiState.WorkspaceMode renderedWorkspaceMode;
    private HomeUpdateUiState homeUpdateUiState = HomeUpdateUiState.UP_TO_DATE;
    private volatile boolean startupUpdateCheckInProgress;
    private volatile boolean startupUpdateDownloadInProgress;
    private volatile boolean startupUpdateDownloadCancelRequested;
    // A visible update prompt is user work, so retain it across a configuration change.
    private UpdatePromptRequest pendingUpdatePrompt;
    private View activeEditorRoot;
    private String activeEditorPackageName;
    private BottomSheetDialog activeAppEditorDialog;
    private PageController feedbackDiagnosticPageController;
    private FeedbackDiagnosticPageRequest feedbackDiagnosticPageRequest;
    private final Map<String, Integer> pendingRuntimePropertyGenerations = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        refreshSystemHookEffectiveEnabled();

        updateStateStore = new UpdateStateStore(this);
        updateDownloadCoordinator = new UpdateDownloadCoordinator(
                createUpdateDownloadHost(),
                updateCoordinator,
                startupUpdateDownloadExecutor,
                startupUpdateExecutor
        );
        releaseNotesController = new ReleaseNotesController(
                new ReleaseNotesCacheStore(this),
                startupUpdateExecutor,
                this::runOnUiThread,
                GitHubReleaseNotesFetcher::fetchByVersionName,
                System::currentTimeMillis,
                UPDATE_CONNECT_TIMEOUT_MS,
                UPDATE_READ_TIMEOUT_MS
        );
        appListFilterStateStore = new AppListFilterStateStore(this);

        RetainedState retainedState
                = (RetainedState) getLastCustomNonConfigurationInstance();
        feedbackDiagnosticSession = retainedState != null
                && retainedState.feedbackDiagnosticSession != null
                ? retainedState.feedbackDiagnosticSession
                : new Session(getApplicationContext());
        feedbackDiagnosticPageController = new PageController(
                getApplicationContext(),
                feedbackDiagnosticExportExecutor,
                createDiagnosticPageControllerHost()
        );
        String initialQuery = "";
        String initialTemplateQuery = "";
        TemplateWorkspaceActivitySession.State initialWorkspaceSessionState = null;
        AppListFilterState initialFilterState = appListFilterStateStore.load();
        MainUiState.WorkspaceMode initialWorkspaceMode = MainUiState.WorkspaceMode.HOME;
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
            feedbackDiagnosticPageRequest = retainedState.feedbackDiagnosticPageRequest;
            pendingUpdatePrompt = retainedState.pendingUpdatePrompt;
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
                this,
                composeEditorScopeRequestCoordinator
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
        toolsWorkspaceBinder = new ToolsWorkspaceBinder(new ToolsWorkspaceBinder.Host() {
            @Override
            public android.app.Activity activity() {
                return MainActivity.this;
            }

            @Override
            public void applyToolsToolbarInsets(View toolbar) {
                WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false);
            }

            @Override
            public void bindPressHaptic(View view) {
                TouchFeedbackBinder.bindPressHaptic(view);
            }

            @Override
            public void openLogsWhenDiagnosticLogsEnabled() {
                if (LogGate.ensureEnabled(
                        MainActivity.this,
                        () -> startActivity(new Intent(MainActivity.this, LogActivity.class)),
                        null
                )) {
                    startActivity(new Intent(MainActivity.this, LogActivity.class));
                }
            }
        });
        composeToolsPresenter = new SystemFontScaleToolPresenter(this,
                new SystemFontScaleToolPresenter.Listener() {
                    @Override public void onStateChanged(com.dpis.module.settings.SystemFontScaleToolState state) {
                        if (composeShellHost != null) composeShellHost.refreshTools();
                    }
                    @Override public void onWriteFailed() { showToast(R.string.system_settings_save_failed); }
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
        restoreFeedbackDiagnosticPage(retainedState);
        feedbackDiagnosticSession.attachHost(createFeedbackDiagnosticHost());
        // The service state callback is not guaranteed to fire on every Wear image.
        // Request the catalog explicitly; MainViewModel coalesces any later service reload.
        requestAppsLoad();
        if (retainedState != null && retainedState.editingPackageName != null) {
            mainViewModel.restoreEditingSession(
                    retainedState.editingPackageName,
                    retainedState.editingDraft,
                    retainedState.savedEditingDraft,
                    retainedState.editingDestination
            );
            restoreAppEditorForCurrentWorkspace();
        }
        restoreWorkspaceEditorForCurrentConfiguration();
        if (pendingUpdatePrompt != null) {
            showPendingUpdatePrompt();
            return;
        }
        if (maybeShowModuleRuntimeReloadAdvice()) {
            return;
        }
        if (!maybeShowStartupDisclaimerDialog()) {
            maybeCheckForUpdatesOnStartup();
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
        if (composeShellHost != null && composeToolsPresenter != null) {
            composeToolsPresenter.refresh();
        } else if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onStart();
        }
        if (settingsPageController != null) {
            settingsPageController.onStart();
        }
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeStartRootAccessProbe();
        if (composeShellHost != null && composeToolsPresenter != null) {
            composeToolsPresenter.refresh();
        } else if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onResume();
        }
        if (settingsPageController != null) {
            settingsPageController.onResume();
        }
    }

    @Override
    protected void onStop() {
        if (composeShellHost == null && toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onStop();
        }
        if (settingsPageController != null) {
            settingsPageController.onStop();
        }
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (feedbackDiagnosticPageController != null) {
            feedbackDiagnosticPageController.detachHost();
        }
        if (isChangingConfigurations()) {
            feedbackDiagnosticSession.detachHost();
        } else {
            feedbackDiagnosticSession.shutdown();
            feedbackDiagnosticExportExecutor.shutdownNow();
        }
        if (updateDownloadCoordinator != null) {
            updateDownloadCoordinator.shutdown();
        }
        ensureWorkspaceSession().onDestroy();
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
            if (settingsPageController != null) {
                settingsPageController.onServiceStateChanged();
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
        if (settingsPageController != null) {
            settingsPageController.onActivityResult(requestCode, resultCode, data);
        }
        if (composeShellHost != null && composeToolsPresenter != null) {
            composeToolsPresenter.refresh();
        } else if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onActivityResult(requestCode, resultCode, data);
        }
        if (ensureWorkspaceSession().handleActivityResult(requestCode, data)) {
            return;
        }
        if (requestCode == REQUEST_SAVE_FEEDBACK_DIAGNOSTIC
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            feedbackDiagnosticPackageActions.saveFeedbackDiagnosticZip(
                    data.getData(),
                    feedbackDiagnosticSession.diagnosticPackage()
            );
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
                mainViewModel != null
                        ? mainViewModel.getEditingDestination()
                        : ConfigEditorDestination.MAIN,
                ensureWorkspaceSession().retainedState(),
                feedbackDiagnosticSession,
                feedbackDiagnosticPageRequest,
                feedbackDiagnosticPageController.presentation() != null
                        ? feedbackDiagnosticPageController.presentation().getState()
                        : null,
                pendingUpdatePrompt
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

    private void applyLandDetailContentInsets(View detailView) {
        View scrollView = detailView.findViewById(R.id.land_detail_scroll);
        WindowInsetsBinder.applySafeDrawingPadding(scrollView, false, true, false, true);
    }

    void showToast(int messageResId) {
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

    boolean setDpisEnabled(String packageName, boolean enabled) {
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

    MainUiState requireUiState() {
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
                                createComposeAppWorkspaceActions());
                    }

                    @Override
                    public EditorPresentation.State appEditorState() {
                        return createComposeAppEditorState();
                    }

                    @Override
                    public com.dpis.module.settings.SystemFontScaleToolState toolsState() {
                        return composeToolsPresenter != null ? composeToolsPresenter.state() : null;
                    }

                    @Override
                    public void changeToolsPending(int percent) {
                        composeToolsPresenter.selectPendingPercent(percent);
                    }

                    @Override
                    public void applyTools() {
                        composeToolsPresenter.apply();
                    }

                    @Override
                    public void restoreTools() {
                        composeToolsPresenter.restoreDefault();
                    }

                    @Override
                    public void requestToolsPermission() {
                        startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:" + getPackageName())));
                    }

                    @Override
                    public SettingsUiState settingsState() {
                        return ensureComposeSettingsController().presentationState();
                    }

                    @Override
                    public void setSettingsHooks(boolean enabled) {
                        ensureComposeSettingsController().setHooksEnabledFromPresentation(enabled);
                    }

                    @Override
                    public void setSettingsSafeMode(boolean enabled) {
                        ensureComposeSettingsController().presentationController().setSafeModeEnabled(enabled);
                    }

                    @Override
                    public void setSettingsGlobalLog(boolean enabled) {
                        ensureComposeSettingsController().presentationController().setGlobalLogEnabled(enabled);
                    }

                    @Override
                    public void openSettingsLogs() {
                        startActivity(new Intent(MainActivity.this, LogActivity.class));
                    }

                    @Override
                    public void setSettingsLauncherHidden(boolean hidden) {
                        ensureComposeSettingsController().presentationController().setLauncherIconHidden(hidden);
                    }

                    @Override
                    public void setSettingsScale(int percent) {
                        ensureComposeSettingsController().saveInterfaceScaleFromPresentation(percent);
                    }

                    @Override
                    public void openSettingsScaleDetails() {
                        ensureComposeSettingsController().showInterfaceScaleFromPresentation();
                    }

                    @Override
                    public void openSettingsFontDebug() {
                        ensureComposeSettingsController().showFontDebugFromPresentation();
                    }

                    @Override
                    public void openSettingsFontLibrary() {
                        ensureComposeSettingsController().showFontLibraryFromPresentation();
                    }

                    @Override
                    public void openSettingsExperimental() {
                        ensureComposeSettingsController().showExperimentalSettingsFromPresentation();
                    }

                    @Override
                    public void openThemeSettings() {
                        ensureComposeSettingsController().showThemeSettingsFromPresentation();
                    }

                    @Override
                    public void setSettingsLanguage(@NonNull String tag) {
                        ensureComposeSettingsController().setLanguageFromPresentation(tag);
                    }

                    @Override
                    public void openSettingsLanguage() {
                        ensureComposeSettingsController().showLanguageFromPresentation();
                    }

                    @Override
                    public void openSettingsBackup() {
                        ensureComposeSettingsController().showConfigBackupFromPresentation();
                    }

                    @Override
                    public void clearSettingsCache() {
                        ensureComposeSettingsController().clearCacheFromPresentation();
                    }

                    @Override
                    public void openSettingsAbout() {
                        ensureComposeSettingsController().showAboutFromPresentation();
                    }

                    @Override
                    public void openSettingsDonate() {
                        ensureComposeSettingsController().showDonateFromPresentation();
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
                    showEditDetailPane(appItem);
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
        if (composeShellHost != null && composeToolsPresenter != null) {
            composeToolsPresenter.refresh();
            composeShellHost.refreshTools(resetExpandedState);
            return;
        }
        if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.bind(toolsWorkspaceContainer);
            if (resetExpandedState) {
                toolsWorkspaceBinder.onShown();
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
            ensureComposeSettingsController();
            return;
        }
        if (settingsWorkspaceContainer == null || settingsPageController != null) {
            return;
        }
        settingsPageController = new SystemServerSettingsPageController(
                this,
                settingsWorkspaceContainer);
        settingsPageController.bind();
    }

    private SystemServerSettingsPageController ensureComposeSettingsController() {
        if (settingsPageController == null) {
            settingsPageController = new SystemServerSettingsPageController(this, null);
            settingsPageController.initializeComposePresentation();
            settingsPageController.addPresentationListener(state -> {
                if (composeShellHost != null) {
                    composeShellHost.refreshSettings();
                }
            });
        }
        return settingsPageController;
    }

    private AppWorkspacePresentation.Actions createComposeAppWorkspaceActions() {
        return new AppWorkspacePresentation.Actions() {
            @Override public void changeQuery(String query) {
                dispatchMainUiAction(MainUiAction.queryChanged(query));
            }

            @Override public void changePage(AppListPage page) {
                setCurrentAppListPage(page, true);
                if (composeShellHost != null) {
                    // Page selection remains adaptive presentation state, so it can change
                    // without producing a new MainUiState domain snapshot.
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

        };
    }

    private EditorPresentation.State createComposeAppEditorState() {
        return composeAppEditorController != null
                ? composeAppEditorController.createState()
                : null;
    }

    void refreshComposeApps() {
        if (composeShellHost != null) {
            composeShellHost.refreshApps();
        }
    }

    void showComposeWechatDpiHelp() {
        ComposeMessageDialog.show(
                this,
                getString(R.string.dialog_wechat_dpi_help_title),
                getString(R.string.dialog_wechat_dpi_help_message),
                getString(R.string.dialog_close_button)
        );
    }

    private AppConfigDialogBinder.AppConfigDialogState composeEditorDialogState(
            AppListItem item,
            EditorDraft draft
    ) {
        return EditorDialogStateFactory.create(item, draft);
    }

    private void restoreFeedbackDiagnosticPage(RetainedState retainedState) {
        if (retainedState == null
                || retainedState.feedbackDiagnosticPageRequest == null
                || retainedState.feedbackDiagnosticPresentationState == null) {
            return;
        }
        FeedbackDiagnosticPageRequest request = retainedState.feedbackDiagnosticPageRequest;
        showComposeFeedbackDiagnosticPreparation(request.item, request.draft);
        feedbackDiagnosticPageController.restoreState(
                retainedState.feedbackDiagnosticPresentationState
        );
    }

    void showComposeFeedbackDiagnosticPreparation(
            AppListItem item,
            EditorDraft draft
    ) {
        feedbackDiagnosticPageRequest = new FeedbackDiagnosticPageRequest(
                item,
                draft,
                resolvePackageVersionName(item.packageName)
        );
        com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation shown
                = feedbackDiagnosticPageController.show(
                item,
                draft,
                feedbackDiagnosticPageRequest.versionName
        );
        if (shown == null) {
            feedbackDiagnosticPageRequest = null;
        }
    }

    private void showComposeFeedbackDiagnosticConfirmation(
            AppListItem item,
            EditorDraft draft
    ) {
        ConfirmDialog.showWithLabels(
                this,
                getString(R.string.feedback_diagnostic_action),
                getString(R.string.feedback_diagnostic_confirm_message, item.label),
                getString(android.R.string.cancel),
                getString(R.string.feedback_diagnostic_save_and_start_button),
                () -> {
                    if (composeAppEditorSaveWorkflow == null
                            || !composeAppEditorSaveWorkflow.save(item, draft)) {
                        return;
                    }
                    if (composeAppEditorController != null) {
                        composeAppEditorController.markSaved(draft);
                    }
                    boolean started = feedbackDiagnosticSession.start(
                            Coordinator.Request.fromPersisted(
                                    item,
                                    composeEditorDialogState(item, draft),
                                    resolvePackageVersionName(item.packageName),
                                    getHookConfigStore()
                            ),
                            false,
                            30
                    );
                    if (!started) {
                        showToast(R.string.feedback_diagnostic_unavailable);
                    }
                },
                () -> { }
        );
    }

    void syncComposeHyperOsNativeProxyAfterSave(AppListItem item) {
        if (!isHyperOsNativeProxyCandidate(item)) {
            return;
        }
        if (shouldPrepareHyperOsNativeProxyForRestart(item)) {
            executeHyperOsNativeProxyMount(item, true, success -> { });
            return;
        }
        executeHyperOsNativeProxyMount(item, false, success -> { });
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

    private boolean maybeShowStartupDisclaimerDialog() {
        StartupDisclaimerStore store = new StartupDisclaimerStore(this);
        return updatePromptDialogCoordinator().maybeShowStartupDisclaimerDialog(
                new UpdatePromptDialogCoordinator.StartupDisclaimerAcceptance() {
                    @Override
                    public boolean isAccepted() {
                        return store.isAccepted();
                    }

                    @Override
                    public boolean markAccepted() {
                        return store.setAccepted(true);
                    }
                },
                this::maybeCheckForUpdatesOnStartup
        );
    }

    private boolean maybeShowModuleRuntimeReloadAdvice() {
        return new ModuleRuntimeReloadNoticeCoordinator(this)
                .maybeShow(this::continueStartupDialogsAfterRuntimeReloadAdvice);
    }

    private void continueStartupDialogsAfterRuntimeReloadAdvice() {
        if (!maybeShowStartupDisclaimerDialog()) {
            maybeCheckForUpdatesOnStartup();
        }
    }

    private void maybeCheckForUpdatesOnStartup() {
        if (!StartupUpdateCheckOnce.consume()) {
            return;
        }
        startupUpdateCheckCoordinator.maybeCheckForUpdatesOnStartup();
    }

    private void startStartupUpdateDownload(
            String targetVersionName,
            String downloadUrl,
            UpdateAvailableDialog.DialogHandle dialogHandle
    ) {
        updateDownloadCoordinator.startDownload(
                targetVersionName,
                downloadUrl,
                dialogHandle
        );
    }

    private void cancelActiveUpdateDownload() {
        updateDownloadCoordinator.cancelActiveDownload();
    }

    private UpdateCoordinator.State buildUpdateCoordinatorState() {
        return updateStateStore.buildCoordinatorState(
                startupUpdateCheckInProgress,
                startupUpdateDownloadInProgress,
                startupUpdateDownloadCancelRequested
        );
    }

    private void applyStartupCheckState(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        updateStateStore.applyStartupCheckState(state);
        startupUpdateCheckInProgress = state.startupCheckInProgress;
    }

    private void applyDownloadState(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        startupUpdateDownloadInProgress = state.downloadInProgress;
        startupUpdateDownloadCancelRequested = state.downloadCancelRequested;
    }

    private void applyHomeUpdateState(HomeUpdateUiState state) {
        if (state == null) {
            return;
        }
        homeUpdateUiState = state;
        bindHomeWorkspaceIfVisible();
    }

    private void bindHomeWorkspaceIfVisible() {
        if (mainViewModel != null
                && requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
            bindHomeWorkspace();
        }
    }

    private void markPromptedVersion(int versionCode) {
        UpdateCoordinator.State nextState
                = updateCoordinator.markPromptedVersion(
                        buildUpdateCoordinatorState(),
                        versionCode
                );
        updateStateStore.applyPromptedVersion(nextState);
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            showToast(R.string.about_link_open_failed);
            return;
        }
        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
            );
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ignored) {
            showToast(R.string.about_link_open_failed);
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

    private StartupUpdateCheckCoordinator.Host createStartupUpdateCheckHost() {
        return new StartupUpdateCheckCoordinator.Host() {
            @Override
            public boolean isActivityAlive() {
                return !isFinishing() && !isDestroyed();
            }

            @NonNull
            @Override
            public String getManifestUrl() {
                return MainActivity.this.getString(
                        R.string.about_update_manifest_url
                );
            }

            @Override
            public void executeBackground(@NonNull Runnable runnable) {
                startupUpdateExecutor.execute(runnable);
            }

            @Override
            public void runOnUiThread(@NonNull Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }

            @NonNull
            @Override
            public UpdateCoordinator.State buildUpdateCoordinatorState() {
                return MainActivity.this.buildUpdateCoordinatorState();
            }

            @Override
            public void applyStartupCheckState(@NonNull UpdateCoordinator.State state) {
                MainActivity.this.applyStartupCheckState(state);
            }

            @Override
            public int getLocalVersionCode() {
                return BuildConfig.VERSION_CODE;
            }

            @NonNull
            @Override
            public String getLocalVersionName() {
                return BuildConfig.VERSION_NAME;
            }

            @Override
            public void onStartupUpdateCheckStarted() {
                MainActivity.this.applyHomeUpdateState(HomeUpdateUiState.CHECKING);
            }

            @Override
            public void onStartupUpdateAvailable(@NonNull StartupUpdateManifest manifest) {
                MainActivity.this.applyHomeUpdateState(HomeUpdateUiState.available(manifest));
                pendingUpdatePrompt = UpdatePromptRequest.from(manifest);
                showPendingUpdatePrompt();
            }

            @Override
            public void onStartupUpdateUpToDate() {
                MainActivity.this.applyHomeUpdateState(HomeUpdateUiState.UP_TO_DATE);
            }

            @Override
            public void onStartupUpdateCheckFailed() {
                MainActivity.this.applyHomeUpdateState(HomeUpdateUiState.FAILED);
            }
        };
    }

    private UpdatePromptDialogCoordinator updatePromptDialogCoordinator() {
        if (updatePromptDialogCoordinator == null) {
            updatePromptDialogCoordinator = new UpdatePromptDialogCoordinator(
                    this,
                    createUpdatePromptDialogHost(),
                    releaseNotesController
            );
        }
        return updatePromptDialogCoordinator;
    }

    private void showPendingUpdatePrompt() {
        UpdatePromptRequest request = pendingUpdatePrompt;
        if (request != null) {
            updatePromptDialogCoordinator().showUpdateAvailableDialog(request);
        }
    }

    private UpdatePromptDialogCoordinator.Host createUpdatePromptDialogHost() {
        return new UpdatePromptDialogCoordinator.Host() {
            @Override
            public void markPromptedVersion(int versionCode) {
                MainActivity.this.markPromptedVersion(versionCode);
            }

            @Override
            public boolean isDownloadInProgress() {
                return updateDownloadCoordinator.isDownloadInProgress();
            }

            @Override
            public void cancelActiveUpdateDownload() {
                MainActivity.this.cancelActiveUpdateDownload();
            }

            @Override
            public void startStartupUpdateDownload(
                    @NonNull String targetVersionName,
                    @NonNull String downloadUrl,
                    @NonNull UpdateAvailableDialog.DialogHandle dialogHandle
            ) {
                MainActivity.this.startStartupUpdateDownload(
                        targetVersionName,
                        downloadUrl,
                        dialogHandle
                );
            }

            @Override
            public void openUrl(@NonNull String url) {
                MainActivity.this.openUrl(url);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }

            @Override
            public void applyLargeDialogWidth(@NonNull androidx.appcompat.app.AlertDialog dialog) {
                DialogWindowSizer.applyLargeWidth(dialog, MainActivity.this);
            }

            @Override
            public void onUpdatePromptDismissed() {
                if (!isChangingConfigurations()) {
                    pendingUpdatePrompt = null;
                }
            }

            @Override
            public void finishActivity() {
                MainActivity.this.finish();
            }
        };
    }

    private UpdateDownloadCoordinator.Host createUpdateDownloadHost() {
        return new UpdateDownloadCoordinator.Host() {
            @Override
            public boolean isActivityAlive() {
                return !isFinishing() && !isDestroyed();
            }

            @Override
            public Context getContext() {
                return MainActivity.this;
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }

            @Override
            public void onDownloadSuccess(File targetFile) {
                startupUpdatePackageHandler.launchPackageInstaller(targetFile);
            }

            @Override
            public UpdateCoordinator.State buildUpdateCoordinatorState() {
                return MainActivity.this.buildUpdateCoordinatorState();
            }

            @Override
            public void applyDownloadState(UpdateCoordinator.State state) {
                MainActivity.this.applyDownloadState(state);
            }
        };
    }

    private void showEditDialog(AppListItem item) {
        if (mainViewModel != null) {
            mainViewModel.setEditingPackageName(item.packageName);
        }
        activeEditorPackageName = item.packageName;
        if (isLandscapeDetailMode()) {
            showEditDetailPane(item);
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

    private void showEditDetailPane(AppListItem item) {
        if (landDetailContent == null) {
            showEditBottomSheet(item);
            return;
        }
        DpisConfigStore store = getHookConfigStore();
        AppListItem sheetItem = AppConfigPrefillPreview.resolveForEditor(this, item, store);
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.view_land_app_detail,
                landDetailContent,
                false
        );
        applyLandDetailContentInsets(dialogView);
        new LandAppDetailPaneBinder(
                this,
                new LandAppDetailPaneBinder.Actions() {
            @Override
            public void saveDraft(
                    AppListItem editorItem,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Integer viewportValue,
                    String viewportTargetType,
                    Integer fontPercent,
                    String fontMode,
                    String selectedTypefaceId,
                    String draftFontHookDomainsRaw,
                    String viewportApplyMode,
                    boolean viewportApplyModeResetRequested,
                    boolean fontHookDomainsResetRequested,
                    String viewportScaleInput,
                    String viewportAbsoluteInput,
                    boolean dpisEnabled,
                    View root,
                    MaterialButton saveButton
            ) {
                saveAppConfigDraft(
                        editorItem,
                        state,
                        viewportValue,
                        viewportTargetType,
                        fontPercent,
                        fontMode,
                        selectedTypefaceId,
                        draftFontHookDomainsRaw,
                        viewportApplyMode,
                        viewportApplyModeResetRequested,
                        fontHookDomainsResetRequested,
                        viewportScaleInput,
                        viewportAbsoluteInput,
                        dpisEnabled,
                        root,
                        saveButton
                );
            }

            @Override
            public void showTypefaceSelector(
                    AppListItem editorItem,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Runnable onChanged
            ) {
                showLandDetailTypefaceSelector(
                        editorItem,
                        state,
                        onChanged
                );
            }

            @Override
            public void showHookDomains(
                    AppListItem editorItem,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Runnable onChanged
            ) {
                showLandDetailHookDomains(editorItem, state, onChanged);
            }

            @Override
            public void toggleScope(
                    AppListItem editorItem,
                    boolean currentlyInScope,
                    Runnable onTurnedInScope,
                    Runnable onTurnedOutScope
            ) {
                toggleLandDetailScope(
                        editorItem,
                        currentlyInScope,
                        onTurnedInScope,
                        onTurnedOutScope
                );
            }

            @Override
            public boolean setDpisEnabled(
                    String packageName,
                    boolean enabled
            ) {
                boolean saved = MainActivity.this.setDpisEnabled(
                        packageName,
                        enabled
                );
                if (saved) {
                    WechatDpiSheetBinder.publishForDpisState(
                            packageName,
                            enabled
                    );
                    requestAppsLoad();
                }
                return saved;
            }

            @Override
            public void executeProcessAction(
                    AppListItem processItem,
                    AppConfigDialogBinder.ProcessAction action
            ) {
                executeDialogProcessAction(processItem, action);
            }

            @Override
            public void startFeedbackDiagnostic(
                    AppListItem editorItem,
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                MainActivity.this.startFeedbackDiagnostic(editorItem, state);
            }

            @Override
            public void onDraftStateChanged(
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                updateEditingDraft(state);
            }

        }
        ).bind(dialogView, sheetItem, systemHooksEnabled);
        landDetailContent.removeAllViews();
        landDetailContent.addView(
                dialogView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        View scrollView = dialogView.findViewById(R.id.land_detail_scroll);
        if (scrollView != null) {
            ViewCompat.requestApplyInsets(scrollView);
        }
        setVisible(landDetailEmptyView, false);
        setVisible(landDetailContent, true);
        activeEditorRoot = dialogView;
        activeEditorPackageName = item.packageName;
        if (mainViewModel != null && mainViewModel.getEditingDraft() != null) {
            EditorDraft draft = mainViewModel.getEditingDraft();
            applyAppConfigDraft(dialogView, draft);
            LandAppDetailPaneBinder.applyRetainedDraft(
                    this,
                    dialogView,
                    sheetItem,
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
                homeUpdateUiState,
                new HomeWorkspaceLayoutStore(this).load(),
                createHomeWorkspaceActions()
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
                startupUpdateCheckCoordinator.checkForUpdatesNow();
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

    private void saveAppConfigDraft(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Integer viewportValue,
            String viewportTargetType,
            Integer fontPercent,
            String fontMode,
            String selectedTypefaceId,
            String draftFontHookDomainsRaw,
            String viewportApplyMode,
            boolean viewportApplyModeResetRequested,
            boolean fontHookDomainsResetRequested,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            boolean dpisEnabled,
            View root,
            MaterialButton saveButton
    ) {
        saveAppConfigDraftInternal(
                item,
                state,
                viewportValue,
                viewportTargetType,
                fontPercent,
                fontMode,
                selectedTypefaceId,
                draftFontHookDomainsRaw,
                viewportApplyMode,
                viewportApplyModeResetRequested,
                fontHookDomainsResetRequested,
                viewportScaleInput,
                viewportAbsoluteInput,
                dpisEnabled,
                root,
                saveButton
        );
    }

    private boolean saveAppConfigDraftInternal(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Integer viewportValue,
            String viewportTargetType,
            Integer fontPercent,
            String fontMode,
            String selectedTypefaceId,
            String draftFontHookDomainsRaw,
            String viewportApplyMode,
            boolean viewportApplyModeResetRequested,
            boolean fontHookDomainsResetRequested,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            boolean dpisEnabled,
            View root,
            MaterialButton saveButton
    ) {
        if (item == null
                || item.packageName == null
                || item.packageName.isBlank()) {
            return false;
        }
        DpisConfigStore store = getHookConfigStore();
        String normalizedViewportTargetType = ViewportTargetType.normalize(viewportTargetType);
        String rawViewportInput = ViewportTargetType.ABSOLUTE_DP.equals(normalizedViewportTargetType)
                ? viewportAbsoluteInput
                : viewportScaleInput;
        ViewportTargetSpec spec = AppConfigInputValidation.parseViewportTargetSpec(
                rawViewportInput,
                normalizedViewportTargetType
        );
        AppConfigSaveHandler.Result result = saveLandDetailResolvedConfig(
                item,
                spec,
                viewportTargetType,
                viewportApplyMode,
                fontPercent,
                fontMode,
                selectedTypefaceId,
                draftFontHookDomainsRaw,
                viewportApplyModeResetRequested,
                fontHookDomainsResetRequested,
                viewportScaleInput,
                viewportAbsoluteInput
        );
        result = finalizeAppConfigSaveWithRuntimeSync(
                result,
                root,
                item.packageName,
                dpisEnabled,
                store
        );
        if (result.messageResId != 0) {
            showToast(result.messageResId);
        }
        if (!result.success) {
            return false;
        }
        AppConfigDialogBinder.showSaveButtonFeedback(saveButton);
        LandAppDetailPaneBinder.markDraftSaved(root, saveButton);
        requestLandDetailScopeAfterSuccessfulSave(item, state);
        return true;
    }

    private void requestLandDetailScopeAfterSuccessfulSave(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (item == null
                || state == null
                || !state.scopeKnown
                || state.scopeSelected
                || state.scopeRequestPending) {
            return;
        }
        state.scopeRequestPending = true;
        boolean requestStarted = systemScopeCoordinator.requestScope(
                item.packageName,
                item.label,
                () -> state.scopeSelected = true,
                () -> state.scopeRequestPending = false,
                false
        );
        if (requestStarted) {
            showToast(R.string.save_scope_request_notice);
            return;
        }
        state.scopeRequestPending = false;
    }

    AppConfigSaveHandler.Result saveLandDetailResolvedConfig(
            AppListItem item,
            ViewportTargetSpec viewportTargetSpec,
            String viewportTargetType,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontMode,
            String selectedTypefaceId,
            String draftFontHookDomainsRaw,
            boolean viewportApplyModeResetRequested,
            boolean fontHookDomainsResetRequested,
            String viewportScaleInput,
            String viewportAbsoluteInput
    ) {
        return appConfigSaveHandler.saveResolved(
                item,
                viewportTargetSpec,
                viewportTargetType,
                viewportApplyMode,
                viewportApplyModeResetRequested,
                fontScalePercent,
                fontMode,
                selectedTypefaceId,
                draftFontHookDomainsRaw,
                fontHookDomainsResetRequested,
                viewportScaleInput,
                viewportAbsoluteInput,
                isSystemHookEnabledFromStore(),
                getHookConfigStore(),
                null
        );
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

    AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
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

    AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
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
        if (!WechatDpiSheetBinder.save(wechatDpiInput, packageName, dpisEnabled, store)) {
            return AppConfigSaveHandler.Result.failure(
                    WechatDpiSheetBinder.isInputValid(wechatDpiInput)
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

    private void syncRuntimePropertiesForTargetLaunch(String packageName) {
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

    void toggleLandDetailScope(
            AppListItem item,
            boolean currentlyInScope,
            Runnable onTurnedInScope,
            Runnable onTurnedOutScope
    ) {
        if (item == null || !item.scopeKnown) {
            return;
        }
        systemScopeCoordinator.toggleScope(
                item.packageName,
                item.label,
                currentlyInScope,
                () -> {
                    if (onTurnedInScope != null) {
                        onTurnedInScope.run();
                    }
                    requestAppsLoad();
                },
                () -> {
                    if (onTurnedOutScope != null) {
                        onTurnedOutScope.run();
                    }
                    requestAppsLoad();
                }
        );
    }

    private void showLandDetailTypefaceSelector(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onChanged
    ) {
        if (item == null
                || item.packageName == null
                || item.packageName.isBlank()) {
            return;
        }
        MaterialButton selectorAnchor = new MaterialButton(this);
        new AppConfigDialogBinder(
                this,
                createAppConfigDialogHost()
        ).showTypefaceSelector(selectorAnchor, state, onChanged);
    }

    private void showLandDetailHookDomains(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onChanged
    ) {
        if (item == null
                || item.packageName == null
                || item.packageName.isBlank()) {
            return;
        }
        showFontHookDomains(item, state, onChanged);
    }

    public AppConfigDialogBinder.Host createAppConfigDialogHost() {
        return new AppConfigDialogBinder.Host() {
            @Override
            public void toggleScope(
                    AppListItem item,
                    boolean currentlyInScope,
                    Runnable onTurnedInScope,
                    Runnable onTurnedOutScope
            ) {
                systemScopeCoordinator.toggleScope(
                        item.packageName,
                        item.label,
                        currentlyInScope,
                        onTurnedInScope,
                        onTurnedOutScope
                );
            }

            @Override
            public boolean requestScope(
                    AppListItem item,
                    Runnable onTurnedInScope,
                    Runnable onRequestFinished
            ) {
                return systemScopeCoordinator.requestScope(
                        item.packageName,
                        item.label,
                        onTurnedInScope,
                        onRequestFinished,
                        false
                );
            }

            @Override
            public void executeProcessAction(
                    AppListItem item,
                    AppConfigDialogBinder.ProcessAction action
            ) {
                executeDialogProcessAction(item, action);
            }

            @Override
            public void applyHyperOsNativeProxy(
                    AppListItem item,
                    Runnable onFinished
            ) {
                executeHyperOsNativeProxyMount(item, true, onFinished);
            }

            @Override
            public void unmountHyperOsNativeProxy(
                    AppListItem item,
                    Runnable onFinished
            ) {
                executeHyperOsNativeProxyMount(item, false, onFinished);
            }

            @Override
            public boolean isHyperOsNativeProxyCandidate(AppListItem item) {
                return MainActivity.this.isHyperOsNativeProxyCandidate(item);
            }

            @Override
            public boolean setDpisEnabled(String packageName, boolean enabled) {
                return MainActivity.this.setDpisEnabled(packageName, enabled);
            }

            @Override
            public void showFontHookDomains(
                    AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Runnable onStateChanged
            ) {
                MainActivity.this.showFontHookDomains(
                        item,
                        state,
                        onStateChanged
                );
            }

            @Override
            public String getFontHookDomainsButtonText(
                    AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                return MainActivity.this.getFontHookDomainsButtonText(
                        item,
                        state
                );
            }

            @Override
            public void openTypefaceLibrary() {
                MainActivity.this.startActivity(
                        new Intent(MainActivity.this, FontLibraryActivity.class)
                );
            }

            @Override
            public void startFeedbackDiagnostic(
                    AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                MainActivity.this.startFeedbackDiagnostic(item, state);
            }

            @Override
            public AppConfigSaveHandler.Result saveAppConfig(
                    View dialogView,
                    AppListItem item,
                    boolean dpisEnabled,
                    TextInputEditText viewportInput,
                    TextInputEditText fontScaleInput,
                    String viewportMode,
                    String viewportApplyMode,
                    boolean viewportApplyModeResetRequested,
                    String fontMode,
                    String selectedTypefaceId,
                    String draftFontHookDomainsRaw,
                    boolean fontHookDomainsResetRequested,
                    String viewportScaleInput,
                    String viewportAbsoluteInput
            ) {
                refreshSystemHookEffectiveEnabled();
                AppConfigSaveHandler.Result result = appConfigSaveHandler.save(
                        item,
                        viewportInput,
                        fontScaleInput,
                        viewportMode,
                        viewportApplyMode,
                        viewportApplyModeResetRequested,
                        fontMode,
                        selectedTypefaceId,
                        draftFontHookDomainsRaw,
                        fontHookDomainsResetRequested,
                        viewportScaleInput,
                        viewportAbsoluteInput,
                        isSystemHookEnabledFromStore(),
                        getHookConfigStore(),
                        null
                );
                return finalizeAppConfigSaveWithRuntimeSync(
                        result,
                        dialogView,
                        item.packageName,
                        dpisEnabled,
                        getHookConfigStore());
            }

            @Override
            public DpisConfigStore getConfigStore() {
                return MainActivity.this.getHookConfigStore();
            }

            @Override
            public void requestAppsLoad() {
                MainActivity.this.requestAppsLoad();
            }

            @Override
            public void onRuntimeConfigSaved() {
                MainActivity.this.onRuntimeConfigSaved();
            }

            @Override
            public void onDraftStateChanged(
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                updateEditingDraft(state);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }
        };
    }

    private Session.Host createFeedbackDiagnosticHost() {
        return new Session.Host() {
            @Override
            public boolean restartTargetAppForDiagnostic(@NonNull String packageName) {
                syncRuntimePropertiesForTargetLaunch(packageName);
                boolean launched = feedbackDiagnosticAppLauncher
                        .restartForDiagnostic(packageName);
                if (launched && activeAppEditorDialog != null) {
                    activeAppEditorDialog.dismiss();
                }
                return launched;
            }

            @Override
            public boolean systemHooksEnabled() {
                return isSystemHookEnabledFromStore();
            }

            @Override
            public void onRecordingStarted() {
                if (feedbackDiagnosticPageController.presentation() != null) {
                    feedbackDiagnosticPageController.presentation().markRecording();
                }
                showToast(R.string.feedback_diagnostic_started);
            }

            @Override
            public void onStartUnavailable(boolean rootRequired) {
                if (feedbackDiagnosticPageController.presentation() != null) {
                    feedbackDiagnosticPageController.presentation().markStartFailed();
                }
                showToast(rootRequired
                        ? R.string.feedback_diagnostic_root_required
                        : R.string.feedback_diagnostic_unavailable);
            }

            @Override
            public void onPackagingStarted() {
                if (feedbackDiagnosticPageController.presentation() != null) {
                    feedbackDiagnosticPageController.presentation().markPackaging();
                }
            }

            @Override
            public void onPackageReady(
                    @NonNull ExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                showFeedbackDiagnosticReady(diagnosticPackage);
            }

            @Override
            public void onPackagingFailed() {
                if (feedbackDiagnosticPageController.presentation() != null) {
                    feedbackDiagnosticPageController.presentation().showPackagingFailed();
                }
                showToast(R.string.feedback_diagnostic_save_failed);
            }

            @Override
            public void onAutoFinished() {
                showToast(R.string.feedback_diagnostic_auto_finished);
            }
        };
    }

    private void startFeedbackDiagnostic(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (item == null) {
            return;
        }
        if (!LogGate.ensureEnabled(
                this,
                () -> showFeedbackDiagnosticConfirmation(item, state),
                null
        )) {
            return;
        }
        showFeedbackDiagnosticConfirmation(item, state);
    }

    private void showFeedbackDiagnosticConfirmation(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        ConfirmDialog.showWithLabels(
                this,
                getString(R.string.feedback_diagnostic_action),
                getString(
                        R.string.feedback_diagnostic_confirm_message,
                        item.label
                ),
                getString(android.R.string.cancel),
                getString(R.string.feedback_diagnostic_save_and_start_button),
                () -> {
                    AppListItem diagnosticItem = saveCurrentEditorConfigForDiagnostic(item, state);
                    if (diagnosticItem == null) {
                        return;
                    }
                    boolean started = feedbackDiagnosticSession.start(
                            Coordinator.Request.fromPersisted(
                                    diagnosticItem,
                                    state,
                                    resolvePackageVersionName(item.packageName),
                                    getHookConfigStore()
                            ),
                            false,
                            30
                    );
                    if (!started) {
                        showToast(R.string.feedback_diagnostic_unavailable);
                    }
                },
                () -> { }
        );
    }

    private AppListItem saveCurrentEditorConfigForDiagnostic(
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
            return saveLandDetailConfigForDiagnostic(item, state, root);
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

    private AppListItem saveLandDetailConfigForDiagnostic(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            View root
    ) {
        if (!WechatDpiSheetBinder.isInputValid(root)) {
            showToast(R.string.status_save_invalid);
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
        boolean saved = saveAppConfigDraftInternal(
                item,
                state,
                parseEditorPercentOrNull(viewportInput),
                AppConfigDialogBinder.resolveViewportMode(findViewportModeToggle(root)),
                parseEditorPercentOrNull(fontInput),
                AppConfigDialogBinder.resolveFontMode(findFontModeToggle(root)),
                state != null ? state.selectedTypefaceId : null,
                state != null ? state.draftFontHookDomainsRaw : null,
                state != null ? state.viewportApplyMode : ViewportApplyMode.OFF,
                state != null && state.viewportApplyModeResetRequested,
                state != null && state.fontHookDomainsResetRequested,
                state != null ? state.viewportScaleInput : "",
                state != null ? state.viewportAbsoluteInput : "",
                state != null && state.dpisEnabled,
                root,
                root.findViewById(R.id.land_detail_save_button)
        );
        return saved ? item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName))
                : null;
    }

    private Integer readPersistedWechatDpiForDiagnostic(String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null;
        }
        DpisConfigStore store = getHookConfigStore();
        return store != null ? store.getWechatDpi(packageName) : null;
    }

    private static Integer parseEditorPercentOrNull(TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return null;
        }
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void showFeedbackDiagnosticReady(
            ExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        if (feedbackDiagnosticPageController.presentation() == null) {
            showDiagnosticResultSheet(diagnosticPackage);
            return;
        }
        feedbackDiagnosticPageController.presentation().showReady(
                diagnosticPackage.fileName,
                feedbackDiagnosticPackageActions.feedbackDiagnosticSharedCachePath(
                        diagnosticPackage
                ),
                getString(
                        R.string.feedback_diagnostic_package_metadata,
                        formatFeedbackDiagnosticDuration(
                                diagnosticPackage.result.durationMs
                        ),
                        android.text.format.Formatter.formatFileSize(
                                this,
                                diagnosticPackage.zipBytes.length
                        )
                ),
                diagnosticPackage.entries.stream()
                        .map(entry -> new com.dpis.module.ui.compose
                                .FeedbackDiagnosticPreparationPresentation.OutputEntry(
                                entry.name,
                                entry.hasLineCount
                                        ? getString(
                                                R.string.feedback_diagnostic_result_entry_meta,
                                                entry.lineCount,
                                                android.text.format.Formatter.formatFileSize(
                                                        this,
                                                        entry.byteCount
                                                )
                                        )
                                        : android.text.format.Formatter.formatFileSize(
                                                this,
                                                entry.byteCount
                                        )
                        ))
                        .collect(java.util.stream.Collectors.toList())
        );
    }

    private static String formatFeedbackDiagnosticDuration(long durationMs) {
        long safeDurationMs = Math.max(0L, durationMs);
        if (safeDurationMs < 1_000L) {
            return safeDurationMs + " ms";
        }
        if (safeDurationMs < 60_000L) {
            String value = String.format(
                    java.util.Locale.US,
                    "%.1f",
                    safeDurationMs / 1_000.0d
            );
            if (value.endsWith(".0")) {
                value = value.substring(0, value.length() - 2);
            }
            return value + " s";
        }
        long totalSeconds = safeDurationMs / 1_000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return seconds == 0L
                ? minutes + " min"
                : minutes + " min " + seconds + " s";
    }

    private void showDiagnosticResultSheet(
            ExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        new ResultSheet(this, new ResultSheet.Host() {
            @Override
            public void shareFeedbackDiagnostic(
                    @NonNull ExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                feedbackDiagnosticPackageActions.shareFeedbackDiagnostic(diagnosticPackage);
            }

            @Override
            public void saveFeedbackDiagnostic(
                    @NonNull ExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                feedbackDiagnosticPackageActions.launchSaveFeedbackDiagnosticPicker(
                        diagnosticPackage
                );
            }
        }).show(diagnosticPackage);
    }

    private void handleFeedbackDiagnosticPageBack() {
        if (!hasFeedbackDiagnosticStateToClear()) {
            dismissFeedbackDiagnosticPage();
            return;
        }
        ConfirmDialog.showWithLabels(
                this,
                getString(R.string.feedback_diagnostic_action),
                getString(R.string.feedback_diagnostic_exit_confirm_message),
                getString(android.R.string.cancel),
                getString(R.string.feedback_diagnostic_exit_clear_action),
                () -> {
                    feedbackDiagnosticSession.cancel();
                    dismissFeedbackDiagnosticPage();
                },
                () -> { }
        );
    }

    private boolean hasFeedbackDiagnosticStateToClear() {
        return feedbackDiagnosticSession.isRunning()
                || feedbackDiagnosticSession.hasPageState()
                || feedbackDiagnosticSession.diagnosticPackage() != null;
    }

    private void dismissFeedbackDiagnosticPage() {
        feedbackDiagnosticPageController.clear();
        feedbackDiagnosticPageRequest = null;
        if (composeShellHost != null) {
            composeShellHost.dismissDiagnosticPreparation();
        }
    }

    String resolvePackageVersionName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }
        try {
            return getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    private void showFontHookDomains(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onStateChanged
    ) {
        showFontHookDomains(item, state, onStateChanged, isFontHookDomainEditingEnabled());
    }

    private void showFontHookDomains(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onStateChanged,
            boolean fontDomainsEditable
    ) {
        if (item == null
                || item.packageName == null
                || item.packageName.isBlank()) {
            return;
        }
        DpisConfigStore store = getHookConfigStore();
        Set<String> automaticKnownDomains = FontHookDomainRegistry.automaticCustomizableDomains();
        HookDomainOverride currentOverride = resolveFontHookDomainsForDraft(item, state);
        FontHookDomainDialog.show(
                this,
                new FontHookDomainDialog.Host() {
            @Override
            public boolean saveCustom(
                    String packageName,
                    Set<String> selectedKnownDomains,
                    Set<String> automaticKnownDomains,
                    Set<String> unknownDomains
            ) {
                if (state != null) {
                    state.draftFontHookDomainsRaw
                            = HookDomainOverrideStore.rawValueForSelection(
                                    selectedKnownDomains,
                                    automaticKnownDomains,
                                    unknownDomains
                            );
                    state.fontHookDomainsResetRequested
                            = state.draftFontHookDomainsRaw == null;
                }
                if (onStateChanged != null) {
                    onStateChanged.run();
                }
                return true;
            }

            @Override
            public boolean restoreRecommended(String packageName) {
                if (state != null) {
                    state.draftFontHookDomainsRaw = null;
                    state.fontHookDomainsResetRequested = true;
                }
                if (onStateChanged != null) {
                    onStateChanged.run();
                }
                return true;
            }

            @Override
            public boolean saveViewportApplyMode(
                    String packageName,
                    String mode
            ) {
                if (state != null) {
                    state.viewportApplyMode = ViewportApplyMode.normalize(mode);
                    state.viewportApplyModeResetRequested
                            = ViewportApplyMode.OFF.equals(state.viewportApplyMode);
                }
                if (onStateChanged != null) {
                    onStateChanged.run();
                }
                return true;
            }
        },
                item.packageName,
                automaticKnownDomains,
                currentOverride,
                state != null
                        ? state.viewportApplyMode
                        : store.getTargetViewportApplyMode(item.packageName),
                fontDomainsEditable,
                onStateChanged
        );
    }

    private boolean isFontHookDomainEditingEnabled() {
        View root = activeEditorRoot;
        if (root == null
                && landDetailContent != null
                && landDetailContent.getChildCount() > 0) {
            root = landDetailContent.getChildAt(0);
        }
        if (root == null) {
            return false;
        }
        return FontApplyMode.FIELD_REWRITE.equals(
                AppConfigDialogBinder.resolveFontMode(findFontModeToggle(root)));
    }

    String getFontHookDomainsButtonText(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        return FontHookDomainPresentation.forOverride(
                resolveFontHookDomainsForDraft(item, state),
                FontHookDomainRegistry.automaticCustomizableDomains())
                .buttonText(this);
    }

    private HookDomainOverride resolveFontHookDomainsForDraft(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic();
        }
        if (state != null
                && (state.previewFromGlobalPrefill
                        || state.draftFontHookDomainsRaw != null)) {
            return normalizedFontHookDomainsOverride(
                    HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                    FontHookDomainRegistry.automaticCustomizableDomains());
        }
        return normalizedFontHookDomainsOverride(
                new HookDomainOverrideStore(getHookConfigStore()).read(
                        item != null ? item.packageName : null),
                FontHookDomainRegistry.automaticCustomizableDomains());
    }

    private HookDomainOverride normalizedFontHookDomainsOverride(
            HookDomainOverride override,
            Set<String> automaticKnownDomains) {
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                override,
                automaticKnownDomains);
    }

    private void executeHyperOsNativeProxyMount(
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

    void executeDialogProcessAction(
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
    private boolean isHyperOsNativeProxyCandidate(AppListItem item) {
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
        processActionHandler.execute(item, mappedAction);
    }

    boolean isSystemHookEnabledFromStore() {
        return cachedSystemHookEffectiveEnabled;
    }

    private interface HyperOsNativeProxyMountCallback {

        void onFinished(boolean success);
    }

    private void refreshSystemHookEffectiveEnabled() {
        cachedSystemHookEffectiveEnabled
                = SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                        getHookConfigStore()
                );
    }

    public DpisConfigStore getHookConfigStore() {
        return DpisApplication.getActiveHookConfigStore(this);
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

    private void updateEditingDraft(AppConfigDialogBinder.AppConfigDialogState state) {
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

    private void applyAppConfigDraft(View root, EditorDraft draft) {
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
                                 ConfigEditorDestination editingDestination,
                                 TemplateWorkspaceActivitySession.State workspaceSessionState,
                                 Session feedbackDiagnosticSession,
                                 FeedbackDiagnosticPageRequest feedbackDiagnosticPageRequest,
                                 com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation.State
                                         feedbackDiagnosticPresentationState,
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
                    ConfigEditorDestination editingDestination,
                    TemplateWorkspaceActivitySession.State workspaceSessionState,
                    Session feedbackDiagnosticSession,
                    FeedbackDiagnosticPageRequest feedbackDiagnosticPageRequest,
                    com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation.State
                            feedbackDiagnosticPresentationState,
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
                this.editingDestination = editingDestination != null
                        ? editingDestination
                        : ConfigEditorDestination.MAIN;
                this.workspaceSessionState = workspaceSessionState;
                this.feedbackDiagnosticSession = feedbackDiagnosticSession;
                this.feedbackDiagnosticPageRequest = feedbackDiagnosticPageRequest;
                this.feedbackDiagnosticPresentationState = feedbackDiagnosticPresentationState;
                this.pendingUpdatePrompt = pendingUpdatePrompt;
            }
        }

    /** Inputs needed to rebuild the diagnostic page after a configuration change. */
    private record FeedbackDiagnosticPageRequest(
            AppListItem item,
            EditorDraft draft,
            String versionName
    ) {
        private FeedbackDiagnosticPageRequest(
                AppListItem item,
                EditorDraft draft,
                String versionName
        ) {
            this.item = item;
            this.draft = draft;
            this.versionName = versionName != null ? versionName : "";
        }
    }

    private PageController.Host createDiagnosticPageControllerHost() {
        return new PageController.Host() {
            @Override
            public boolean canShowDiagnosticPage() {
                return composeShellHost != null;
            }

            @Override
            public void showDiagnosticPreparation(
                    @NonNull com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation presentation
            ) {
                if (composeShellHost != null) {
                    composeShellHost.showDiagnosticPreparation(presentation);
                }
            }

            @Override
            public void showFallbackConfirmation(
                    @NonNull AppListItem item,
                    @NonNull EditorDraft draft
            ) {
                showComposeFeedbackDiagnosticConfirmation(item, draft);
            }

            @Override
            public void onBackRequested() {
                handleFeedbackDiagnosticPageBack();
            }

            @Override
            public boolean saveAppConfig(@NonNull AppListItem item, @NonNull EditorDraft draft) {
                return composeAppEditorSaveWorkflow != null
                        && composeAppEditorSaveWorkflow.save(item, draft);
            }

            @Override
            public void markAppConfigSaved(@NonNull EditorDraft draft) {
                if (composeAppEditorController != null) {
                    composeAppEditorController.markSaved(draft);
                }
            }

            @Override
            public boolean startDiagnostic(
                    @NonNull AppListItem item,
                    @NonNull EditorDraft draft,
                    @NonNull String versionName,
                    boolean durationEnabled,
                    int durationSeconds
            ) {
                return feedbackDiagnosticSession.start(
                        Coordinator.Request.fromPersisted(
                                item,
                                composeEditorDialogState(item, draft),
                                versionName,
                                getHookConfigStore()
                        ),
                        durationEnabled,
                        durationSeconds
                );
            }

            @Override
            public ExportBuilder.DiagnosticPackage diagnosticPackage() {
                return feedbackDiagnosticSession.diagnosticPackage();
            }

            @Override
            public void saveDiagnosticPackage(
                    @NonNull ExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                feedbackDiagnosticPackageActions.launchSaveFeedbackDiagnosticPicker(
                        diagnosticPackage
                );
            }

            @Override
            public void shareDiagnosticPackage(
                    @NonNull ExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                feedbackDiagnosticPackageActions.shareFeedbackDiagnostic(diagnosticPackage);
            }

            @Override
            public void discardDiagnostic() {
                feedbackDiagnosticSession.cancel();
            }

            @Override
            public void showLsposedExplanation(@NonNull String title, @NonNull String explanation) {
                ComposeMessageDialog.show(
                        MainActivity.this,
                        title,
                        explanation,
                        getString(R.string.dialog_close_button)
                );
            }

            @Override
            public void copyDiagnosticPath(String path) {
                feedbackDiagnosticPackageActions.copyFeedbackDiagnosticPath(path);
            }

            @Override
            public void runOnUiThread(@NonNull Runnable action) {
                MainActivity.this.runOnUiThread(action);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }
        };
    }

}
