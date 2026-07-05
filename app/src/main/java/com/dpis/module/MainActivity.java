package com.dpis.module;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation;

import com.dpis.module.fonts.hookdomain.FontHookDomainDialog;

import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.fonts.FontLibraryStore;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.viewport.ViewportPropertySyncer;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.AppListPagerAdapter;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;

import com.dpis.module.quirks.WechatDpiSheetBinder;

import com.dpis.module.templates.QuickTemplateSortDialog;

import com.dpis.module.templates.GlobalPrefillStore;
import com.dpis.module.templates.GlobalPrefillEditorBinder;
import com.dpis.module.templates.GlobalPrefillSheetDialog;
import com.dpis.module.templates.QuickTemplateApplyAdapters;
import com.dpis.module.templates.QuickTemplateEditorBinder;
import com.dpis.module.templates.QuickTemplateEditSheetDialog;
import com.dpis.module.templates.QuickTemplateTargetSelectionActivity;
import com.dpis.module.templates.QuickTemplateTargetsBinder;
import com.dpis.module.templates.TemplateWorkspaceBinder;

import com.dpis.module.templates.QuickTemplateStore;

import com.dpis.module.templates.TemplateConfigValue;

import com.dpis.module.applist.AppListFilterStateStore;

import com.dpis.module.applist.AppListFilterState;

import com.dpis.module.appconfig.WechatDpiConfig;

import com.dpis.module.fonts.HyperOsNativeProxyBindMounter;
import com.dpis.module.diagnostics.FeedbackDiagnosticAppLauncher;

import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.ui.WindowInsetsBinder;

import com.dpis.module.ui.DialogWindowSizer;

import com.dpis.module.home.HomeUpdateUiState;
import com.dpis.module.home.HomeWorkspaceBinder;

import com.dpis.module.settings.ToolsWorkspaceBinder;
import com.dpis.module.settings.StartupDisclaimerStore;

import com.dpis.module.templates.QuickTemplateTargetCarrierState;

import com.dpis.module.templates.QuickTemplateTargetSelectionContract;

import com.dpis.module.templates.QuickTemplateApplyConfirmationMessage;
import com.dpis.module.templates.QuickTemplateApplyCoordinator;

import com.dpis.module.root.RootAccessProbe;

import com.dpis.module.ui.MaxHeightNestedScrollView;

import com.dpis.module.ui.FormInputFocusBinder;

import com.dpis.module.updates.UpdateStateStore;

import com.dpis.module.updates.UpdatePromptDialogCoordinator;

import com.dpis.module.updates.UpdateDownloadCoordinator;

import com.dpis.module.updates.UpdateCoordinator;

import com.dpis.module.updates.StartupUpdatePackageHandler;

import com.dpis.module.updates.StartupUpdateManifest;

import com.dpis.module.updates.StartupUpdateDownloadExecutor;

import com.dpis.module.updates.StartupUpdateCheckOnce;

import com.dpis.module.updates.StartupUpdateCheckCoordinator;

import com.dpis.module.updates.ReleaseNotesController;

import com.dpis.module.updates.ReleaseNotesCacheStore;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Process;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.dpis.module.appconfig.AppConfigDialogCoordinator;
import com.dpis.module.updates.GitHubReleaseNotesFetcher;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.dpis.module.updates.ReleaseNotesMarkdownRenderer;
import io.github.libxposed.service.XposedService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity
        extends LocalizedActivity
        implements DpisApplication.ServiceStateListener {

    private static final long MODE_TOGGLE_ANIM_DURATION_MS = 200L;
    private static final long WORKSPACE_TRANSITION_DURATION_MS = 300L;
    private static final float WORKSPACE_CONTENT_ENTER_START_SCALE = 0.96f;
    private static final AccelerateDecelerateInterpolator
            WORKSPACE_CONTENT_ENTER_INTERPOLATOR =
                    new AccelerateDecelerateInterpolator();
    private static final long SEARCH_FAB_ANIM_DURATION_MS = 160L;
    private static final float SEARCH_FAB_HIDDEN_SCALE = 0.92f;
    private static final int SEARCH_FAB_SCROLL_TRIGGER_DY = 8;
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
    private static final String STATE_PAGE_SCROLL_STATES
            = "state.page_scroll_states";
    private static final String STATE_REFRESHING_PAGES
            = "state.refreshing_pages";
    private static final String STATE_TEMPLATE_DETAIL_KIND
            = "state.template_detail.kind";
    private static final String STATE_TEMPLATE_DETAIL_ID
            = "state.template_detail.id";
    private static final String STATE_QUICK_TEMPLATE_TARGETS_ACTIVITY_STARTED
            = "state.quick_template.targets_activity_started";
    private static final String STATE_GLOBAL_PREFILL_DRAFT
            = "state.global_prefill.draft";
    private static final String STATE_QUICK_TEMPLATE_DRAFT
            = "state.quick_template.draft";
    private static final String STATE_DRAFT_NAME = "name";
    private static final String STATE_DRAFT_VIEWPORT_INPUT = "viewport_input";
    private static final String STATE_DRAFT_VIEWPORT_MODE = "viewport_mode";
    private static final String STATE_DRAFT_VIEWPORT_APPLY_MODE = "viewport_apply_mode";
    private static final String STATE_DRAFT_FONT_INPUT = "font_input";
    private static final String STATE_DRAFT_FONT_MODE = "font_mode";
    private static final String STATE_DRAFT_TYPEFACE_ID = "typeface_id";
    private static final String STATE_DRAFT_FONT_HOOK_DOMAINS = "font_hook_domains";
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;
    private static final long INSTALLED_APP_CATALOG_TTL_MS = 60_000L;
    private static final int FIRST_SCREEN_ICON_WARMUP_LIMIT = 48;
    private static final long ICON_REFRESH_DEBOUNCE_MS = 120L;
    private static final String XIAOMI_GET_INSTALLED_APPS_PERMISSION
            = "com.android.permission.GET_INSTALLED_APPS";
    private static final int REQUEST_XIAOMI_GET_INSTALLED_APPS = 10022;
    private static final int REQUEST_QUICK_TEMPLATE_TARGETS = 10023;
    private static final int REQUEST_SAVE_FEEDBACK_DIAGNOSTIC = 10024;
    private static final String SHARED_FEEDBACK_DIAGNOSTIC_DIRECTORY_NAME
            = "shared-feedback-diagnostics";

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
    private final FeedbackDiagnosticCoordinator feedbackDiagnosticCoordinator
            = new FeedbackDiagnosticCoordinator(createFeedbackDiagnosticHost());
    private final FeedbackDiagnosticAppLauncher feedbackDiagnosticAppLauncher
            = new FeedbackDiagnosticAppLauncher(this);
    private final FeedbackDiagnosticExportBuilder feedbackDiagnosticExportBuilder
            = new FeedbackDiagnosticExportBuilder(this);
    private final ExecutorService feedbackDiagnosticExportExecutor
            = Executors.newSingleThreadExecutor();
    private final StartupUpdatePackageHandler startupUpdatePackageHandler
            = new StartupUpdatePackageHandler(this);
    private final ExecutorService startupUpdateExecutor
            = Executors.newSingleThreadExecutor();
    private final SystemScopeCoordinator systemScopeCoordinator
            = new SystemScopeCoordinator(createSystemScopeHost());
    private final InstalledAppCatalogCoordinator installedAppCatalogCoordinator
            = new InstalledAppCatalogCoordinator(
                    createInstalledAppCatalogHost(),
                    INSTALLED_APP_CATALOG_TTL_MS,
                    FIRST_SCREEN_ICON_WARMUP_LIMIT,
                    ICON_REFRESH_DEBOUNCE_MS
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

    private MainViewModel mainViewModel;
    private AppListPagerAdapter pagerAdapter;
    private ViewPager2 appPager;
    private TabLayout filterTabs;
    private View topContainer;
    private View homeWorkspaceContainer;
    private View templateWorkspaceContainer;
    private View toolsWorkspaceContainer;
    private View settingsWorkspaceContainer;
    private View landListPageView;
    private View landDetailPane;
    private View landDetailDivider;
    private View landDetailEmptyView;
    private FrameLayout landDetailContent;
    private View templateDetailEmptyView;
    private FrameLayout templateDetailContent;
    private TemplateDetailSelection templateDetailSelection
            = TemplateDetailSelection.none();
    private AppListPagerAdapter.AppListPageController landListController;
    private AppListPage landCurrentPage = AppListPage.ALL_APPS;
    private final SparseArray<Parcelable> landScrollStates
            = new SparseArray<>();
    private HomeWorkspaceBinder homeWorkspaceBinder;
    private TemplateWorkspaceBinder templateWorkspaceBinder;
    private ToolsWorkspaceBinder toolsWorkspaceBinder;
    private SettingsWorkspaceBinder settingsWorkspaceBinder;
    private NavigationBarView workspaceSwitch;
    private SparseArray<Parcelable> restoredPageScrollStates;
    private EditText searchInput;
    private ImageButton searchClearButton;
    private FloatingActionButton searchFocusFab;
    private boolean searchFabHidden;
    private boolean suppressSearchQueryChange;
    private boolean updatingWorkspaceSelection;
    private boolean updatingFilterTabSelection;
    private ImageButton searchFilterButton;
    private boolean cachedSystemHookEffectiveEnabled;
    private boolean skipNextImmediateServiceReload;
    private boolean installedAppsPermissionRequestInFlight;
    private boolean pendingInstalledAppsLoadAfterPermission;
    private boolean installedAppsPermissionRequestCompleted;
    private MainWorkspaceMode renderedWorkspaceMode;
    private boolean rootAccessProbeInFlight;
    private HomeUpdateUiState homeUpdateUiState = HomeUpdateUiState.UP_TO_DATE;
    private volatile boolean startupUpdateCheckInProgress;
    private volatile boolean startupUpdateDownloadInProgress;
    private volatile boolean startupUpdateDownloadCancelRequested;
    private View activeEditorRoot;
    private String activeEditorPackageName;
    private BottomSheetDialog activeAppEditorDialog;
    private GlobalPrefillSheetDialog activeGlobalPrefillSheetDialog;
    private QuickTemplateEditSheetDialog activeQuickTemplateEditSheetDialog;
    private GlobalPrefillEditorBinder activeGlobalPrefillEditorBinder;
    private QuickTemplateEditorBinder activeQuickTemplateEditorBinder;
    private QuickTemplateTargetsBinder activeQuickTemplateTargetsBinder;
    private FeedbackDiagnosticCoordinator.Result pendingFeedbackDiagnosticResult;
    private FeedbackDiagnosticExportBuilder.DiagnosticPackage pendingFeedbackDiagnosticPackage;
    private androidx.appcompat.app.AlertDialog activeFeedbackDiagnosticPackagingDialog;
    private final Map<String, Integer> pendingRuntimePropertyGenerations = new HashMap<>();
    private boolean mainActivityResumed;
    private GlobalPrefillEditorBinder.Draft retainedGlobalPrefillDraft;
    private QuickTemplateEditorBinder.Draft retainedQuickTemplateDraft;
    private boolean templateSheetMigrationInProgress;
    // TODO: Promote quick-template target carrier decisions into a full state machine.
    private boolean quickTemplateTargetSelectionActivityStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        searchFocusFab = findViewById(R.id.search_focus_fab);
        applyInsets();
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
                = (RetainedState) getLastNonConfigurationInstance();
        String initialQuery = "";
        String initialTemplateQuery = "";
        AppListFilterState initialFilterState = appListFilterStateStore.load();
        MainWorkspaceMode initialWorkspaceMode = MainWorkspaceMode.HOME;
        List<AppListItem> initialAppsSnapshot = Collections.emptyList();
        Set<AppListPage> initialRefreshingPages = EnumSet.noneOf(
                AppListPage.class
        );
        if (retainedState != null) {
            initialQuery = retainedState.query;
            initialTemplateQuery = retainedState.templateQuery;
            initialFilterState = retainedState.filterState;
            initialWorkspaceMode = retainedState.workspaceMode;
            templateDetailSelection = retainedState.templateDetailSelection;
            quickTemplateTargetSelectionActivityStarted =
                    retainedState.quickTemplateTargetSelectionActivityStarted;
            retainedGlobalPrefillDraft = retainedState.globalPrefillDraft;
            retainedQuickTemplateDraft = retainedState.quickTemplateDraft;
            homeUpdateUiState = retainedState.homeUpdateUiState;
            restoredPageScrollStates = retainedState.pageScrollStates;
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
                    savedInstanceState.getBoolean(STATE_FILTER_SHOW_SYSTEM, false),
                    savedInstanceState.getBoolean(
                            STATE_FILTER_INJECTED_ONLY,
                            false
                    ),
                    savedInstanceState.getBoolean(STATE_FILTER_WIDTH_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_FONT_ONLY, false)
            );
            initialWorkspaceMode = MainWorkspaceMode.fromName(
                    savedInstanceState.getString(STATE_WORKSPACE_MODE)
            );
            restoredPageScrollStates
                    = savedInstanceState.getSparseParcelableArray(
                            STATE_PAGE_SCROLL_STATES
                    );
            initialRefreshingPages = decodeRefreshingPages(
                    savedInstanceState.getIntArray(STATE_REFRESHING_PAGES)
            );
            templateDetailSelection = restoreTemplateDetailSelection(savedInstanceState);
            quickTemplateTargetSelectionActivityStarted = savedInstanceState.getBoolean(
                    STATE_QUICK_TEMPLATE_TARGETS_ACTIVITY_STARTED,
                    false
            );
            retainedGlobalPrefillDraft = restoreGlobalPrefillDraft(
                    savedInstanceState.getBundle(STATE_GLOBAL_PREFILL_DRAFT)
            );
            retainedQuickTemplateDraft = restoreQuickTemplateDraft(
                    savedInstanceState.getBundle(STATE_QUICK_TEMPLATE_DRAFT)
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

        searchFilterButton = findViewById(R.id.search_filter_button);
        appPager = findViewById(R.id.app_pager);
        filterTabs = findViewById(R.id.filter_tabs);
        topContainer = findViewById(R.id.top_container);
        homeWorkspaceContainer = findViewById(R.id.home_workspace_container);
        templateWorkspaceContainer = findViewById(
                R.id.template_workspace_container
        );
        toolsWorkspaceContainer = findViewById(R.id.tools_workspace_container);
        settingsWorkspaceContainer = findViewById(R.id.settings_workspace_container);
        landListPageView = findViewById(R.id.land_app_list_page);
        landDetailPane = findViewById(R.id.land_detail_pane);
        landDetailDivider = findViewById(R.id.land_detail_divider);
        landDetailEmptyView = findViewById(R.id.land_detail_empty);
        landDetailContent = findViewById(R.id.land_detail_content);
        templateDetailEmptyView = findViewById(R.id.template_detail_empty);
        templateDetailContent = findViewById(R.id.template_detail_content);
        templateWorkspaceBinder = new TemplateWorkspaceBinder(
                this,
                createTemplateWorkspaceActions(),
                createQuickTemplateActions()
        );
        homeWorkspaceBinder = new HomeWorkspaceBinder(this);
        toolsWorkspaceBinder = new ToolsWorkspaceBinder(new ToolsWorkspaceBinder.Host() {
            @Override
            public android.app.Activity activity() {
                return MainActivity.this;
            }

            @Override
            public void applyToolsToolbarInsets(View toolbar) {
                WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);
            }

            @Override
            public void bindPressHaptic(View view) {
                TouchFeedbackBinder.bindPressHaptic(view);
            }

            @Override
            public void openLogsWhenDiagnosticLogsEnabled() {
                if (DiagnosticLogGate.ensureEnabled(
                        MainActivity.this,
                        () -> startActivity(new Intent(MainActivity.this, LogActivity.class)),
                        null
                )) {
                    startActivity(new Intent(MainActivity.this, LogActivity.class));
                }
            }
        });
        settingsWorkspaceBinder = new SettingsWorkspaceBinder(this);
        workspaceSwitch = findViewById(R.id.workspace_switch);
        workspaceSwitch.setSaveFromParentEnabled(false);
        bindLandscapeWorkspaceRailItemHeight();
        if (appPager != null) {
            pagerAdapter = new AppListPagerAdapter(
                    this::showEditDialog,
                    this::onPageRefreshRequested,
                    this::onPageListScrolled,
                    this::onIconLoadRequested,
                    this::isSystemHookEnabledFromStore
            );
            pagerAdapter.restorePageScrollStates(restoredPageScrollStates);
            appPager.setAdapter(pagerAdapter);
        } else {
            restoreLandscapeScrollStates(restoredPageScrollStates);
        }
        bindLandscapeListController();
        applyRefreshingStatesToPager();
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

        bindFilterTabs();
        bindWorkspaceSwitch();
        searchFilterButton.setOnClickListener(v -> showFilterDialog());
        bindFabTouchFeedback(searchFocusFab);
        searchFocusFab.setOnClickListener(v
                -> focusSearchInputAndShowKeyboard()
        );

        searchInput = findViewById(R.id.search_input);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                clearSearchFocus();
                return true;
            }
            return false;
        });
        searchClearButton = findViewById(R.id.search_clear_button);
        searchInput.addTextChangedListener(
                new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                String query = s != null ? s.toString() : "";
                if (!suppressSearchQueryChange) {
                    dispatchMainUiAction(MainUiAction.queryChanged(query));
                }
                searchClearButton.setVisibility(
                        query.isEmpty() ? View.GONE : View.VISIBLE
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        }
        );
        searchClearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
        });
        String restoredQuery = requireUiState().currentQuery();
        if (!restoredQuery.isEmpty()) {
            searchInput.setText(restoredQuery);
            searchInput.setSelection(restoredQuery.length());
        }
        searchInput.setOnFocusChangeListener((view, hasFocus)
                -> updateSearchHint()
        );
        renderMainUiState(requireUiState());
        if (retainedState != null && retainedState.editingPackageName != null) {
            mainViewModel.setEditingPackageName(
                    retainedState.editingPackageName
            );
            mainViewModel.setEditingDraft(retainedState.editingDraft);
            restoreAppEditorForCurrentWorkspace();
        }
        restoreTemplateEditorForCurrentConfiguration();
        if (maybeShowModuleRuntimeReloadAdvice()) {
            return;
        }
        if (!maybeShowStartupDisclaimerDialog()) {
            maybeCheckForUpdatesOnStartup();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                && searchInput != null
                && searchInput.hasFocus()) {
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            if (!FormInputFocusBinder.isInsideAny(
                    rawX,
                    rawY,
                    searchInput,
                    searchFocusFab
            )) {
                clearSearchFocus();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSystemHookEffectiveEnabled();
        refreshVisibleAppListStatuses();
        if (requireUiState().workspaceMode == MainWorkspaceMode.TEMPLATE) {
            bindTemplateWorkspace();
        } else if (requireUiState().workspaceMode == MainWorkspaceMode.HOME) {
            bindHomeWorkspace();
        } else if (requireUiState().workspaceMode == MainWorkspaceMode.TOOLS) {
            bindToolsWorkspace();
        } else if (requireUiState().workspaceMode == MainWorkspaceMode.SETTINGS) {
            bindSettingsWorkspace();
        }
        if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onStart();
        }
        if (settingsWorkspaceBinder != null) {
            settingsWorkspaceBinder.onStart();
        }
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainActivityResumed = true;
        feedbackDiagnosticCoordinator.onDpisResumed();
        maybeShowPendingFeedbackDiagnosticResult();
        if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onResume();
        }
        if (settingsWorkspaceBinder != null) {
            settingsWorkspaceBinder.onResume();
        }
    }

    @Override
    protected void onStop() {
        mainActivityResumed = false;
        if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onStop();
        }
        if (settingsWorkspaceBinder != null) {
            settingsWorkspaceBinder.onStop();
        }
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        feedbackDiagnosticCoordinator.shutdown();
        feedbackDiagnosticExportExecutor.shutdownNow();
        if (updateDownloadCoordinator != null) {
            updateDownloadCoordinator.shutdown();
        }
        disposeActiveQuickTemplateTargetsBinder();
        installedAppCatalogCoordinator.shutdown();
        super.onDestroy();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(() -> {
            refreshSystemHookEffectiveEnabled();
            refreshVisibleAppListStatuses();
            if (requireUiState().workspaceMode == MainWorkspaceMode.HOME) {
                bindHomeWorkspace();
            }
            if (settingsWorkspaceBinder != null) {
                settingsWorkspaceBinder.onServiceStateChanged();
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
        if (settingsWorkspaceBinder != null) {
            settingsWorkspaceBinder.onActivityResult(requestCode, resultCode, data);
        }
        if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_QUICK_TEMPLATE_TARGETS) {
            quickTemplateTargetSelectionActivityStarted = false;
            if (QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                    isLandscapeDetailMode(),
                    hasPendingQuickTemplateTargets(),
                    quickTemplateTargetCloseReason(data)
            )) {
                clearTemplateDetailSelection();
            }
            return;
        }
        if (requestCode == REQUEST_SAVE_FEEDBACK_DIAGNOSTIC
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            saveFeedbackDiagnosticZip(data.getData());
        }
    }

    private boolean hasPendingQuickTemplateTargets() {
        return templateDetailSelection != null
                && templateDetailSelection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS;
    }

    private static QuickTemplateTargetCarrierState.CloseReason quickTemplateTargetCloseReason(
            Intent data
    ) {
        String reason = data != null
                ? data.getStringExtra(QuickTemplateTargetSelectionContract.EXTRA_CLOSE_REASON)
                : null;
        return QuickTemplateTargetSelectionContract.closeReasonFrom(reason);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
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
        if (appPager != null) {
            outState.putInt(STATE_CURRENT_PAGE, appPager.getCurrentItem());
        } else {
            outState.putInt(STATE_CURRENT_PAGE, landCurrentPage.position());
        }
        SparseArray<Parcelable> pageScrollStates = captureAppListScrollStates();
        if (pageScrollStates != null) {
            outState.putSparseParcelableArray(
                    STATE_PAGE_SCROLL_STATES,
                    pageScrollStates
            );
        }
        outState.putIntArray(
                STATE_REFRESHING_PAGES,
                captureRefreshingPagePositions()
        );
        captureTemplateEditorDraft();
        saveTemplateDetailSelection(outState, templateDetailSelection);
        outState.putBoolean(
                STATE_QUICK_TEMPLATE_TARGETS_ACTIVITY_STARTED,
                quickTemplateTargetSelectionActivityStarted
        );
        if (retainedGlobalPrefillDraft != null) {
            outState.putBundle(
                    STATE_GLOBAL_PREFILL_DRAFT,
                    saveGlobalPrefillDraft(retainedGlobalPrefillDraft)
            );
        }
        if (retainedQuickTemplateDraft != null) {
            outState.putBundle(
                    STATE_QUICK_TEMPLATE_DRAFT,
                    saveQuickTemplateDraft(retainedQuickTemplateDraft)
            );
        }
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
    public Object onRetainNonConfigurationInstance() {
        MainUiState state = requireUiState();
        List<AppListItem> snapshot = state.appsSnapshot();
        int currentPage
                = appPager != null
                        ? appPager.getCurrentItem()
                        : landCurrentPage.position();
        SparseArray<Parcelable> pageScrollStates = captureAppListScrollStates();
        AppConfigDraft draft = captureAppConfigDraft();
        if (draft == null && mainViewModel != null) {
            draft = mainViewModel.getEditingDraft();
        }
        captureTemplateEditorDraft();
        return new RetainedState(
                snapshot,
                state.appQuery,
                state.templateQuery,
                state.filterState,
                state.workspaceMode,
                currentPage,
                pageScrollStates,
                captureRefreshingPagePositions(),
                mainViewModel != null
                        ? mainViewModel.getEditingPackageName()
                        : null,
                draft,
                templateDetailSelection,
                quickTemplateTargetSelectionActivityStarted,
                retainedGlobalPrefillDraft,
                retainedQuickTemplateDraft,
                homeUpdateUiState
        );
    }

    private void onPageRefreshRequested(AppListPage page) {
        dispatchMainUiAction(MainUiAction.markPageRefreshing(page));
        requestAppsLoad(true);
    }

    private void onPageListScrolled(AppListPage page, int dy) {
        if (dy >= SEARCH_FAB_SCROLL_TRIGGER_DY) {
            hideSearchFocusFab();
            return;
        }
        if (dy <= -SEARCH_FAB_SCROLL_TRIGGER_DY) {
            showSearchFocusFab();
        }
    }

    private void onIconLoadRequested(String packageName) {
        installedAppCatalogCoordinator.onIconLoadRequested(packageName);
    }

    private void refreshVisibleAppListStatuses() {
        if (pagerAdapter != null) {
            pagerAdapter.refreshVisibleStatuses();
        }
        if (landListController != null) {
            landListController.refreshStatuses();
        }
    }

    private SparseArray<Parcelable> captureAppListScrollStates() {
        if (pagerAdapter != null) {
            return pagerAdapter.capturePageScrollStates();
        }
        captureCurrentLandscapeScrollState();
        SparseArray<Parcelable> states = new SparseArray<>();
        for (int i = 0; i < landScrollStates.size(); i++) {
            states.put(landScrollStates.keyAt(i), landScrollStates.valueAt(i));
        }
        return states;
    }

    private void restoreLandscapeScrollStates(SparseArray<Parcelable> states) {
        landScrollStates.clear();
        if (states == null) {
            return;
        }
        for (int i = 0; i < states.size(); i++) {
            landScrollStates.put(states.keyAt(i), states.valueAt(i));
        }
    }

    private void captureCurrentLandscapeScrollState() {
        if (landListController != null) {
            Parcelable landState = landListController.captureScrollState();
            if (landState != null) {
                landScrollStates.put(landCurrentPage.position(), landState);
            }
        }
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

    private void bindLandscapeListController() {
        if (landListPageView == null) {
            return;
        }
        SwipeRefreshLayout swipeRefreshLayout = landListPageView.findViewById(
                R.id.page_swipe_refresh
        );
        RecyclerView recyclerView = landListPageView.findViewById(
                R.id.page_list
        );
        landListController = new AppListPagerAdapter.AppListPageController(
                swipeRefreshLayout,
                recyclerView,
                this::showEditDialog,
                this::onPageRefreshRequested,
                this::onPageListScrolled,
                this::onIconLoadRequested,
                this::isSystemHookEnabledFromStore
        );
        landListController.setSwipeRefreshEnabled(false);
    }

    private void bindFilterTabs() {
        if (filterTabs == null) {
            return;
        }
        if (appPager != null) {
            new TabLayoutMediator(filterTabs, appPager, (tab, position)
                    -> tab.setText(
                            getString(AppListPage.fromPosition(position).titleRes())
                    )
            ).attach();
            return;
        }
        filterTabs.removeAllTabs();
        for (AppListPage page : AppListPage.values()) {
            TabLayout.Tab tab = filterTabs
                    .newTab()
                    .setText(getString(page.titleRes()));
            filterTabs.addTab(tab, page == landCurrentPage);
        }
        filterTabs.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (updatingFilterTabSelection) {
                    return;
                }
                setCurrentAppListPage(
                        AppListPage.fromPosition(tab.getPosition()),
                        true
                );
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        }
        );
    }

    private void setCurrentAppListPage(AppListPage page, boolean submit) {
        AppListPage nextPage = page != null ? page : AppListPage.ALL_APPS;
        if (appPager != null) {
            appPager.setCurrentItem(nextPage.position(), false);
            if (submit) {
                applyFilter();
                applyRefreshingStatesToPager();
            }
            return;
        }
        if (landCurrentPage != nextPage) {
            captureCurrentLandscapeScrollState();
        }
        landCurrentPage = nextPage;
        if (filterTabs != null) {
            TabLayout.Tab selectedTab = filterTabs.getTabAt(
                    nextPage.position()
            );
            if (selectedTab != null && !selectedTab.isSelected()) {
                updatingFilterTabSelection = true;
                try {
                    selectedTab.select();
                } finally {
                    updatingFilterTabSelection = false;
                }
            }
        }
        if (submit) {
            applyFilter();
            applyRefreshingStatesToPager();
        }
    }

    private void applyRefreshingStatesToPager() {
        MainUiState state = requireUiState();
        if (pagerAdapter != null) {
            for (AppListPage page : AppListPage.values()) {
                pagerAdapter.setRefreshing(page, state.isRefreshing(page));
            }
        }
        if (landListController != null) {
            landListController.setRefreshing(
                    state.isRefreshing(landCurrentPage)
            );
        }
    }

    private void requestAppsLoad() {
        requestAppsLoad(false);
    }

    private void requestAppsLoad(boolean forceInstalledAppCatalogReload) {
        if (!ensureInstalledAppsPermissionBeforeLoad()) {
            pendingInstalledAppsLoadAfterPermission = true;
            return;
        }
        dispatchMainUiAction(
                MainUiAction.requestAppsLoad(forceInstalledAppCatalogReload)
        );
    }

    private boolean ensureInstalledAppsPermissionBeforeLoad() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || installedAppsPermissionRequestCompleted
                || !isXiaomiInstalledAppsPermissionDeclared()) {
            return true;
        }
        try {
            if (checkPermission(
                    XIAOMI_GET_INSTALLED_APPS_PERMISSION,
                    Process.myPid(),
                    Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            if (!installedAppsPermissionRequestInFlight) {
                installedAppsPermissionRequestInFlight = true;
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

    private void startAppsLoad(MainUiEffect.StartAppsLoad start) {
        int requestId = start.requestId;
        boolean forceInstalledAppCatalogReload
                = start.forceInstalledAppCatalogReload;
        new Thread(() -> {
            List<AppListItem> loaded = null;
            try {
                loaded = loadInstalledApps(forceInstalledAppCatalogReload);
            } catch (Throwable throwable) {
                DpisLog.e("list load failed", throwable);
            }
            List<AppListItem> finalLoaded = loaded;
            runOnUiThread(() -> onAppsLoadFinished(requestId, finalLoaded));
        }, "dpis-load-apps-" + requestId).start();
    }

    private void onAppsLoadFinished(int requestId, List<AppListItem> loaded) {
        dispatchMainUiAction(MainUiAction.appsLoadFinished(requestId, loaded));
    }

    private void applyInsets() {
        View topContainer = findViewById(R.id.top_container);
        WindowInsetsBinder.applySafeDrawingPadding(topContainer, false, true, false, false);
        View homeWorkspace = findViewById(R.id.home_workspace_container);
        WindowInsetsBinder.applySafeDrawingPadding(homeWorkspace, true, true, true, true);
        WindowInsetsBinder.applyNavigationBarMargins(searchFocusFab);
    }

    private void applyLandDetailContentInsets(View detailView) {
        View scrollView = detailView.findViewById(R.id.land_detail_scroll);
        WindowInsetsBinder.applySafeDrawingPadding(scrollView, false, true, false, true);
    }

    private void focusSearchInputAndShowKeyboard() {
        if (searchInput == null) {
            return;
        }
        showSearchFocusFab();
        searchInput.requestFocus();
        Editable current = searchInput.getText();
        if (current != null) {
            searchInput.setSelection(current.length());
        }
        searchInput.setHint("");
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE
        );
        if (imm != null) {
            searchInput.post(()
                    -> imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
            );
        }
    }

    private void bindFabTouchFeedback(FloatingActionButton fab) {
        TouchFeedbackBinder.bindPressScaleAndHaptic(fab);
    }

    private void hideSearchFocusFab() {
        if (searchFocusFab == null || searchFabHidden) {
            return;
        }
        searchFabHidden = true;
        searchFocusFab.animate().cancel();
        searchFocusFab.setClickable(false);
        searchFocusFab
                .animate()
                .translationY(getResources().getDimensionPixelSize(
                        R.dimen.floating_actions_hide_offset_y))
                .alpha(0f)
                .scaleX(SEARCH_FAB_HIDDEN_SCALE)
                .scaleY(SEARCH_FAB_HIDDEN_SCALE)
                .setDuration(SEARCH_FAB_ANIM_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (searchFabHidden) {
                        searchFocusFab.setVisibility(View.INVISIBLE);
                    }
                })
                .start();
    }

    private void showSearchFocusFab() {
        if (searchFocusFab == null || !searchFabHidden) {
            return;
        }
        searchFabHidden = false;
        searchFocusFab.animate().cancel();
        searchFocusFab.setVisibility(View.VISIBLE);
        searchFocusFab.setClickable(true);
        searchFocusFab
                .animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(SEARCH_FAB_ANIM_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void setSearchFocusFabVisible(boolean visible) {
        if (searchFocusFab == null) {
            return;
        }
        searchFocusFab.animate().cancel();
        searchFabHidden = false;
        searchFocusFab.setClickable(visible);
        searchFocusFab.setAlpha(1f);
        searchFocusFab.setScaleX(1f);
        searchFocusFab.setScaleY(1f);
        searchFocusFab.setTranslationY(0f);
        searchFocusFab.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void clearSearchFocus() {
        if (searchInput == null) {
            return;
        }
        Editable current = searchInput.getText();
        if (current == null || current.length() == 0) {
            searchInput.setHint(getString(R.string.search_hint));
        }
        FormInputFocusBinder.clearFocusAndHideIme(
                findViewById(android.R.id.content),
                searchInput
        );
    }

    private void showToast(int messageResId) {
        showToast(getString(messageResId));
    }

    private void showToast(int messageResId, Object... formatArgs) {
        showToast(getString(messageResId, formatArgs));
    }

    private void showToast(CharSequence message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean setDpisEnabled(String packageName, boolean enabled) {
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

    private List<AppListItem> loadInstalledApps(
            boolean forceInstalledAppCatalogReload
    ) {
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
            if (scope != null) {
                scopePackages.addAll(scope);
                return new ScopeState(scopePackages, true);
            }
        } catch (RuntimeException ignored) {
            scopePackages.clear();
        }
        return new ScopeState(scopePackages, false);
    }

    private void applyFilter() {
        MainUiState state = requireUiState();
        if (pagerAdapter != null) {
            for (AppListPage page : AppListPage.values()) {
                pagerAdapter.submitPage(page, state.visibleItems(page));
            }
        }
        if (landListController != null) {
            landListController.bind(
                    landCurrentPage,
                    state.visibleItems(landCurrentPage),
                    landScrollStates.get(landCurrentPage.position()),
                    state.isRefreshing(landCurrentPage)
            );
        }
    }

    private MainUiState requireUiState() {
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
        List<MainUiEffect> effects = viewModel.dispatch(action);
        renderMainUiState(viewModel.getState());
        handleMainUiEffects(effects);
    }

    private void renderMainUiState(MainUiState state) {
        if (state == null) {
            return;
        }
        syncSearchInputWithState(state);
        applyWorkspaceMode(state.workspaceMode);
        applyFilter();
        applyRefreshingStatesToPager();
        restoreAppEditorForCurrentWorkspace();
    }

    private void bindWorkspaceSwitch() {
        if (workspaceSwitch == null) {
            return;
        }
        selectWorkspaceItem(workspaceButtonId(requireUiState().workspaceMode));
        workspaceSwitch.setOnItemSelectedListener(item -> {
            if (updatingWorkspaceSelection) {
                return true;
            }
            dispatchMainUiAction(
                    MainUiAction.workspaceModeChanged(
                            workspaceModeForButtonId(item.getItemId())
                    )
            );
            return true;
        });
    }

    private void bindLandscapeWorkspaceRailItemHeight() {
        if (!(workspaceSwitch instanceof NavigationRailView)) {
            return;
        }
        workspaceSwitch.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) ->
                syncLandscapeWorkspaceRailItemHeight());
        workspaceSwitch.post(this::syncLandscapeWorkspaceRailItemHeight);
    }

    private void syncLandscapeWorkspaceRailItemHeight() {
        if (!(workspaceSwitch instanceof NavigationRailView)
                || workspaceSwitch.getMenu() == null
                || workspaceSwitch.getMenu().size() == 0) {
            return;
        }
        NavigationRailView railView = (NavigationRailView) workspaceSwitch;
        View railViewport = (View) workspaceSwitch.getParent();
        int railViewportHeight = railViewport != null
                ? railViewport.getHeight()
                : workspaceSwitch.getHeight();
        if (railViewportHeight <= 0) {
            return;
        }
        int availableHeight = Math.max(
                1,
                railViewportHeight - railView.getPaddingTop() - railView.getPaddingBottom()
        );
        int itemHeight = Math.max(
                1,
                availableHeight / railView.getMenu().size()
        );
        if (railView.getItemMinimumHeight() != itemHeight) {
            railView.setItemMinimumHeight(itemHeight);
        }
    }

    private void applyWorkspaceMode(MainWorkspaceMode workspaceMode) {
        MainWorkspaceMode mode
                = workspaceMode != null ? workspaceMode : MainWorkspaceMode.HOME;
        boolean enteringToolsWorkspace = mode == MainWorkspaceMode.TOOLS
                && renderedWorkspaceMode != MainWorkspaceMode.TOOLS;
        boolean appWorkspace = mode == MainWorkspaceMode.APP;
        boolean homeWorkspace = mode == MainWorkspaceMode.HOME;
        boolean templateWorkspace = mode == MainWorkspaceMode.TEMPLATE;
        boolean toolsWorkspace = mode == MainWorkspaceMode.TOOLS;
        boolean settingsWorkspace = mode == MainWorkspaceMode.SETTINGS;
        setVisible(topContainer, appWorkspace || templateWorkspace);
        setVisible(filterTabs, appWorkspace);
        boolean floatingActionsVisible
                = appWorkspace && !isLandscapeDetailMode();
        setSearchFocusFabVisible(floatingActionsVisible);
        if (searchFilterButton != null) {
            searchFilterButton.setEnabled(appWorkspace);
            searchFilterButton.setVisibility(
                    appWorkspace ? View.VISIBLE : View.GONE
            );
        }
        applySearchClearButtonPosition(appWorkspace);
        boolean animateWorkspace = renderedWorkspaceMode != null
                && renderedWorkspaceMode != mode;
        renderedWorkspaceMode = mode;
        setVisible(appPager, appWorkspace);
        setVisible(landListPageView, appWorkspace);
        setVisible(homeWorkspaceContainer, homeWorkspace);
        setVisible(templateWorkspaceContainer, templateWorkspace);
        setVisible(toolsWorkspaceContainer, toolsWorkspace);
        setVisible(settingsWorkspaceContainer, settingsWorkspace);
        resetHiddenWorkspacePresentation(mode);
        if (animateWorkspace) {
            animateVisibleWorkspaceContent(mode);
        }
        applyLandscapeDetailVisibility(appWorkspace, templateWorkspace);
        if (workspaceSwitch != null
                && workspaceSwitch.getSelectedItemId() != workspaceButtonId(mode)) {
            selectWorkspaceItem(workspaceButtonId(mode));
        }
        updateSearchHint(mode);
        if (homeWorkspace) {
            bindHomeWorkspace();
        } else if (templateWorkspace) {
            bindTemplateWorkspace();
            restoreTemplateEditorForCurrentConfiguration();
        } else if (toolsWorkspace) {
            bindToolsWorkspace(enteringToolsWorkspace);
        } else if (settingsWorkspace) {
            bindSettingsWorkspace();
        }
    }

    private void applySearchClearButtonPosition(boolean filterButtonVisible) {
        if (searchClearButton == null) {
            return;
        }
        int start = getResources().getDimensionPixelSize(filterButtonVisible
                ? R.dimen.main_search_action_pair_padding
                : R.dimen.main_search_action_icon_padding_start);
        int end = getResources().getDimensionPixelSize(filterButtonVisible
                ? R.dimen.main_search_action_pair_padding
                : R.dimen.main_search_action_icon_padding_end);
        int vertical = getResources().getDimensionPixelSize(
                R.dimen.main_search_action_icon_padding_vertical);
        ViewCompat.setPaddingRelative(searchClearButton, start, vertical, end, vertical);
    }

    private void restoreAppEditorForCurrentWorkspace() {
        if (mainViewModel == null
                || requireUiState().workspaceMode != MainWorkspaceMode.APP) {
            return;
        }
        String editingPackage = mainViewModel.getEditingPackageName();
        if (editingPackage == null || editingPackage.isBlank()) {
            return;
        }
        if (isLandscapeDetailMode()
                && landDetailContent != null
                && landDetailContent.getChildCount() > 0) {
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
        setVisible(templateDetailEmptyView, templateWorkspace
                && (templateDetailSelection == null
                || templateDetailSelection.kind == TemplateDetailKind.NONE));
        setVisible(templateDetailContent, templateWorkspace
                && templateDetailSelection != null
                && templateDetailSelection.kind != TemplateDetailKind.NONE);
    }

    private boolean isLandscapeDetailMode() {
        return landDetailContent != null && landDetailEmptyView != null;
    }

    private void bindTemplateWorkspace() {
        if (templateWorkspaceBinder != null) {
            templateWorkspaceBinder.bind(
                    templateWorkspaceContainer,
                    requireUiState().currentQuery()
            );
        }
    }

    private void syncSearchInputWithState(MainUiState state) {
        if (searchInput == null || state == null) {
            return;
        }
        String query = state.currentQuery();
        Editable current = searchInput.getText();
        String currentQuery = current != null ? current.toString() : "";
        if (query.equals(currentQuery)) {
            return;
        }
        suppressSearchQueryChange = true;
        searchInput.setText(query);
        suppressSearchQueryChange = false;
        searchInput.setSelection(query.length());
    }

    private void bindToolsWorkspace() {
        bindToolsWorkspace(false);
    }

    private void bindToolsWorkspace(boolean resetExpandedState) {
        if (toolsWorkspaceBinder != null) {
            toolsWorkspaceBinder.bind(toolsWorkspaceContainer);
            if (resetExpandedState) {
                toolsWorkspaceBinder.onShown();
            }
        }
    }

    private void restoreTemplateDetailPane() {
        if (!isLandscapeDetailMode()
                || templateDetailContent == null
                || templateDetailSelection == null
                || templateDetailSelection.kind == TemplateDetailKind.NONE) {
            applyLandscapeDetailVisibility(false, true);
            return;
        }
        if (templateDetailContent.getChildCount() > 0) {
            applyLandscapeDetailVisibility(false, true);
            return;
        }
        showTemplateDetailPane(templateDetailSelection);
    }

    private void showGlobalPrefillEditor() {
        templateDetailSelection = TemplateDetailSelection.globalPrefill();
        retainedQuickTemplateDraft = null;
        disposeActiveQuickTemplateTargetsBinder();
        if (!isLandscapeDetailMode()) {
            showGlobalPrefillSheet();
            return;
        }
        showTemplateDetailPane(templateDetailSelection);
    }

    private void showGlobalPrefillSheet() {
        if (activeGlobalPrefillSheetDialog != null
                && activeGlobalPrefillSheetDialog.isShowing()) {
            return;
        }
        activeGlobalPrefillSheetDialog = new GlobalPrefillSheetDialog(
                this,
                this::onTemplateEditorUpdated,
                () -> {
                    if (activeGlobalPrefillSheetDialog != null) {
                        retainedGlobalPrefillDraft = activeGlobalPrefillSheetDialog.snapshotDraft();
                    }
                    activeGlobalPrefillSheetDialog = null;
                    if (!templateSheetMigrationInProgress) {
                        clearTemplateDetailSelection();
                    }
                },
                retainedGlobalPrefillDraft
        );
        activeGlobalPrefillSheetDialog.show();
    }

    private void showQuickTemplateSheet(String templateId) {
        if (activeQuickTemplateEditSheetDialog != null
                && activeQuickTemplateEditSheetDialog.isShowing()) {
            return;
        }
        activeQuickTemplateEditSheetDialog = new QuickTemplateEditSheetDialog(
                this,
                templateId,
                this::onTemplateEditorUpdated,
                () -> {
                    if (activeQuickTemplateEditSheetDialog != null) {
                        retainedQuickTemplateDraft =
                                activeQuickTemplateEditSheetDialog.snapshotDraft();
                    }
                    activeQuickTemplateEditSheetDialog = null;
                    if (!templateSheetMigrationInProgress) {
                        clearTemplateDetailSelection();
                    }
                },
                retainedQuickTemplateDraft
        );
        activeQuickTemplateEditSheetDialog.show();
    }

    private void captureTemplateEditorDraft() {
        if (activeGlobalPrefillSheetDialog != null) {
            retainedGlobalPrefillDraft = activeGlobalPrefillSheetDialog.snapshotDraft();
        }
        if (activeQuickTemplateEditSheetDialog != null) {
            retainedQuickTemplateDraft = activeQuickTemplateEditSheetDialog.snapshotDraft();
        }
        if (activeGlobalPrefillEditorBinder != null) {
            retainedGlobalPrefillDraft = activeGlobalPrefillEditorBinder.snapshotDraft();
        }
        if (activeQuickTemplateEditorBinder != null) {
            retainedQuickTemplateDraft = activeQuickTemplateEditorBinder.snapshotDraft();
        }
    }

    private static void saveTemplateDetailSelection(
            Bundle outState,
            TemplateDetailSelection selection
    ) {
        TemplateDetailSelection normalized = selection != null
                ? selection
                : TemplateDetailSelection.none();
        outState.putString(STATE_TEMPLATE_DETAIL_KIND, normalized.kind.name());
        outState.putString(STATE_TEMPLATE_DETAIL_ID, normalized.templateId);
    }

    private static TemplateDetailSelection restoreTemplateDetailSelection(
            Bundle savedInstanceState
    ) {
        if (savedInstanceState == null) {
            return TemplateDetailSelection.none();
        }
        TemplateDetailKind kind = TemplateDetailKind.fromName(
                savedInstanceState.getString(STATE_TEMPLATE_DETAIL_KIND)
        );
        if (kind == TemplateDetailKind.GLOBAL_PREFILL) {
            return TemplateDetailSelection.globalPrefill();
        }
        if (kind == TemplateDetailKind.QUICK_TEMPLATE) {
            return TemplateDetailSelection.quickTemplate(
                    savedInstanceState.getString(STATE_TEMPLATE_DETAIL_ID)
            );
        }
        if (kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS) {
            return TemplateDetailSelection.quickTemplateTargets(
                    savedInstanceState.getString(STATE_TEMPLATE_DETAIL_ID)
            );
        }
        return TemplateDetailSelection.none();
    }

    // Saved-instance recovery must keep unsaved template editor input, not just
    // reopen the same detail page after process recreation.
    private static Bundle saveGlobalPrefillDraft(GlobalPrefillEditorBinder.Draft draft) {
        Bundle bundle = new Bundle();
        if (draft == null) {
            return bundle;
        }
        bundle.putString(STATE_DRAFT_VIEWPORT_INPUT, draft.viewportInput);
        bundle.putString(STATE_DRAFT_VIEWPORT_MODE, draft.viewportMode);
        bundle.putString(STATE_DRAFT_VIEWPORT_APPLY_MODE, draft.viewportApplyMode);
        bundle.putString(STATE_DRAFT_FONT_INPUT, draft.fontInput);
        bundle.putString(STATE_DRAFT_FONT_MODE, draft.fontMode);
        bundle.putString(STATE_DRAFT_TYPEFACE_ID, draft.selectedTypefaceId);
        bundle.putString(STATE_DRAFT_FONT_HOOK_DOMAINS, draft.draftFontHookDomainsRaw);
        return bundle;
    }

    private static GlobalPrefillEditorBinder.Draft restoreGlobalPrefillDraft(Bundle bundle) {
        if (bundle == null || bundle.isEmpty()) {
            return null;
        }
        return new GlobalPrefillEditorBinder.Draft(
                bundle.getString(STATE_DRAFT_VIEWPORT_INPUT),
                bundle.getString(STATE_DRAFT_VIEWPORT_MODE),
                bundle.getString(STATE_DRAFT_VIEWPORT_APPLY_MODE),
                bundle.getString(STATE_DRAFT_FONT_INPUT),
                bundle.getString(STATE_DRAFT_FONT_MODE),
                bundle.getString(STATE_DRAFT_TYPEFACE_ID),
                bundle.getString(STATE_DRAFT_FONT_HOOK_DOMAINS)
        );
    }

    private static Bundle saveQuickTemplateDraft(QuickTemplateEditorBinder.Draft draft) {
        Bundle bundle = new Bundle();
        if (draft == null) {
            return bundle;
        }
        bundle.putString(STATE_DRAFT_NAME, draft.nameInput);
        bundle.putString(STATE_DRAFT_VIEWPORT_INPUT, draft.viewportInput);
        bundle.putString(STATE_DRAFT_VIEWPORT_MODE, draft.viewportMode);
        bundle.putString(STATE_DRAFT_VIEWPORT_APPLY_MODE, draft.viewportApplyMode);
        bundle.putString(STATE_DRAFT_FONT_INPUT, draft.fontInput);
        bundle.putString(STATE_DRAFT_FONT_MODE, draft.fontMode);
        bundle.putString(STATE_DRAFT_TYPEFACE_ID, draft.selectedTypefaceId);
        bundle.putString(STATE_DRAFT_FONT_HOOK_DOMAINS, draft.draftFontHookDomainsRaw);
        return bundle;
    }

    private static QuickTemplateEditorBinder.Draft restoreQuickTemplateDraft(Bundle bundle) {
        if (bundle == null || bundle.isEmpty()) {
            return null;
        }
        return new QuickTemplateEditorBinder.Draft(
                bundle.getString(STATE_DRAFT_NAME),
                bundle.getString(STATE_DRAFT_VIEWPORT_INPUT),
                bundle.getString(STATE_DRAFT_VIEWPORT_MODE),
                bundle.getString(STATE_DRAFT_VIEWPORT_APPLY_MODE),
                bundle.getString(STATE_DRAFT_FONT_INPUT),
                bundle.getString(STATE_DRAFT_FONT_MODE),
                bundle.getString(STATE_DRAFT_TYPEFACE_ID),
                bundle.getString(STATE_DRAFT_FONT_HOOK_DOMAINS)
        );
    }

    private void closeActiveTemplateSheetForMigration() {
        templateSheetMigrationInProgress = true;
        if (activeGlobalPrefillSheetDialog != null) {
            retainedGlobalPrefillDraft = activeGlobalPrefillSheetDialog.snapshotDraft();
            activeGlobalPrefillSheetDialog.dismiss();
            activeGlobalPrefillSheetDialog = null;
        }
        if (activeQuickTemplateEditSheetDialog != null) {
            retainedQuickTemplateDraft = activeQuickTemplateEditSheetDialog.snapshotDraft();
            activeQuickTemplateEditSheetDialog.dismiss();
            activeQuickTemplateEditSheetDialog = null;
        }
        templateSheetMigrationInProgress = false;
    }

    private void disposeActiveQuickTemplateTargetsBinder() {
        if (activeQuickTemplateTargetsBinder != null) {
            activeQuickTemplateTargetsBinder.dispose();
            activeQuickTemplateTargetsBinder = null;
        }
    }

    private void showQuickTemplateEditor(String templateId) {
        templateDetailSelection = TemplateDetailSelection.quickTemplate(templateId);
        retainedGlobalPrefillDraft = null;
        disposeActiveQuickTemplateTargetsBinder();
        if (!isLandscapeDetailMode()) {
            showQuickTemplateSheet(templateId);
            return;
        }
        showTemplateDetailPane(templateDetailSelection);
    }

    private void showQuickTemplateTargets(String templateId) {
        templateDetailSelection = TemplateDetailSelection.quickTemplateTargets(templateId);
        retainedGlobalPrefillDraft = null;
        retainedQuickTemplateDraft = null;
        if (!isLandscapeDetailMode()) {
            startQuickTemplateTargetSelectionActivity(templateId);
            return;
        }
        showTemplateDetailPane(templateDetailSelection);
    }

    private void startQuickTemplateTargetSelectionActivity(String templateId) {
        if (!QuickTemplateTargetCarrierState.shouldStartPortraitActivity(
                isLandscapeDetailMode(),
                templateDetailSelection != null
                        && templateDetailSelection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS,
                quickTemplateTargetSelectionActivityStarted
        )) {
            return;
        }
        Intent intent = new Intent(
                MainActivity.this,
                QuickTemplateTargetSelectionActivity.class
        );
        intent.putExtra(
                QuickTemplateTargetSelectionContract.EXTRA_TEMPLATE_ID,
                templateId
        );
        quickTemplateTargetSelectionActivityStarted = true;
        startActivityForResult(intent, REQUEST_QUICK_TEMPLATE_TARGETS);
    }

    private void restoreTemplateEditorForCurrentConfiguration() {
        if (requireUiState().workspaceMode != MainWorkspaceMode.TEMPLATE
                || templateDetailSelection == null
                || templateDetailSelection.kind == TemplateDetailKind.NONE) {
            return;
        }
        TemplateDetailSelection selection = templateDetailSelection;
        if (isLandscapeDetailMode()) {
            if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS) {
                quickTemplateTargetSelectionActivityStarted = false;
            }
            closeActiveTemplateSheetForMigration();
            restoreTemplateDetailPane();
            return;
        }
        if (selection.kind == TemplateDetailKind.GLOBAL_PREFILL) {
            showGlobalPrefillSheet();
            return;
        }
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE) {
            showQuickTemplateSheet(selection.templateId);
            return;
        }
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS) {
            startQuickTemplateTargetSelectionActivity(selection.templateId);
        }
    }

    private void clearTemplateDetailSelection() {
        templateDetailSelection = TemplateDetailSelection.none();
        retainedGlobalPrefillDraft = null;
        retainedQuickTemplateDraft = null;
        quickTemplateTargetSelectionActivityStarted = false;
        disposeActiveQuickTemplateTargetsBinder();
        if (templateDetailContent != null) {
            templateDetailContent.removeAllViews();
        }
        activeGlobalPrefillEditorBinder = null;
        activeQuickTemplateEditorBinder = null;
    }

    private void showTemplateDetailPane(TemplateDetailSelection selection) {
        if (templateDetailContent == null || selection == null) {
            return;
        }
        templateDetailSelection = selection;
        disposeActiveQuickTemplateTargetsBinder();
        templateDetailContent.removeAllViews();
        View detailView = inflateTemplateDetailView(selection);
        boolean bound = bindTemplateDetailView(selection, detailView);
        if (!bound) {
            templateDetailSelection = TemplateDetailSelection.none();
            templateDetailContent.removeAllViews();
            bindTemplateWorkspace();
            applyLandscapeDetailVisibility(false, true);
            return;
        }
        templateDetailContent.addView(
                detailView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        ViewCompat.requestApplyInsets(detailView);
        applyLandscapeDetailVisibility(false, true);
    }

    private View inflateTemplateDetailView(TemplateDetailSelection selection) {
        int layoutRes;
        if (selection.kind == TemplateDetailKind.GLOBAL_PREFILL) {
            layoutRes = R.layout.view_land_global_prefill_detail;
        } else if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS) {
            layoutRes = R.layout.view_land_quick_template_targets_detail;
        } else {
            layoutRes = R.layout.view_land_quick_template_detail;
        }
        return LayoutInflater.from(this).inflate(
                layoutRes,
                templateDetailContent,
                false
        );
    }

    private boolean bindTemplateDetailView(
            TemplateDetailSelection selection,
            View detailView
    ) {
        if (selection.kind == TemplateDetailKind.GLOBAL_PREFILL) {
            applyTemplateDetailInsets(
                    detailView,
                    R.id.global_prefill_scroll
            );
            activeGlobalPrefillEditorBinder = GlobalPrefillEditorBinder.bind(
                    this,
                    detailView,
                    this::onTemplateEditorUpdated,
                    null,
                    false,
                    retainedGlobalPrefillDraft
            );
            activeQuickTemplateEditorBinder = null;
            return activeGlobalPrefillEditorBinder != null;
        }
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE) {
            applyTemplateDetailInsets(
                    detailView,
                    R.id.quick_template_edit_scroll
            );
            activeQuickTemplateEditorBinder = QuickTemplateEditorBinder.bind(
                    this,
                    detailView,
                    selection.templateId,
                    this::onTemplateEditorUpdated,
                    null,
                    false,
                    retainedQuickTemplateDraft
            );
            activeGlobalPrefillEditorBinder = null;
            return activeQuickTemplateEditorBinder != null;
        }
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS) {
            WindowInsetsBinder.applySafeDrawingPadding(
                    detailView,
                    false,
                    true,
                    false,
                    true
            );
            activeQuickTemplateTargetsBinder = new QuickTemplateTargetsBinder(
                    this,
                    detailView,
                    createQuickTemplateTargetsHost()
            );
            activeGlobalPrefillEditorBinder = null;
            activeQuickTemplateEditorBinder = null;
            return activeQuickTemplateTargetsBinder.bind(selection.templateId);
        }
        return false;
    }

    private QuickTemplateTargetsBinder.Host createQuickTemplateTargetsHost() {
        return new QuickTemplateTargetsBinder.Host() {
            @Override
            public PackageManager getPackageManager() {
                return MainActivity.this.getPackageManager();
            }

            @Override
            public String getSelfPackageName() {
                return MainActivity.this.getPackageName();
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }

            @Override
            public View getIconRefreshAnchor() {
                return templateDetailContent != null
                        ? templateDetailContent.findViewById(R.id.quick_template_targets_list)
                        : null;
            }

            @Override
            public void onSaved() {
                bindTemplateWorkspace();
            }

            @Override
            public void onMissingTemplate() {
                clearTemplateDetailSelection();
                applyLandscapeDetailVisibility(false, true);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }
        };
    }

    private void applyTemplateDetailInsets(View detailView, int scrollViewId) {
        View scrollView = detailView != null
                ? detailView.findViewById(scrollViewId)
                : null;
        WindowInsetsBinder.applySafeDrawingPadding(
                scrollView,
                false,
                true,
                false,
                true
        );
    }

    private void onTemplateEditorUpdated() {
        bindTemplateWorkspace();
        if (templateDetailSelection != null
                && templateDetailSelection.kind == TemplateDetailKind.GLOBAL_PREFILL) {
            retainedGlobalPrefillDraft = null;
        } else if (templateDetailSelection != null
                && templateDetailSelection.kind == TemplateDetailKind.QUICK_TEMPLATE) {
            retainedQuickTemplateDraft = null;
        }
        if (!isLandscapeDetailMode()
                || requireUiState().workspaceMode != MainWorkspaceMode.TEMPLATE
                || templateDetailSelection == null
                || templateDetailSelection.kind == TemplateDetailKind.NONE) {
            return;
        }
        TemplateDetailSelection selection = templateDetailSelection;
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE
                && selection.templateId == null
                && activeQuickTemplateEditorBinder != null) {
            String savedTemplateId = activeQuickTemplateEditorBinder.currentTemplateId();
            if (savedTemplateId != null && !savedTemplateId.isBlank()) {
                templateDetailSelection = TemplateDetailSelection.quickTemplate(savedTemplateId);
                selection = templateDetailSelection;
            }
        }
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE
                && selection.templateId == null) {
            templateDetailSelection = TemplateDetailSelection.none();
            templateDetailContent.removeAllViews();
            applyLandscapeDetailVisibility(false, true);
            return;
        }
        if (selection.kind == TemplateDetailKind.QUICK_TEMPLATE
                && selection.templateId != null
                && new QuickTemplateStore(
                        getSharedPreferences(
                                DpisConfigStore.GROUP,
                                Context.MODE_PRIVATE
                        )
                ).read(selection.templateId) == null) {
            templateDetailSelection = TemplateDetailSelection.none();
            templateDetailContent.removeAllViews();
            applyLandscapeDetailVisibility(false, true);
            return;
        }
        showTemplateDetailPane(selection);
    }

    private void bindSettingsWorkspace() {
        if (settingsWorkspaceBinder != null) {
            settingsWorkspaceBinder.bind(settingsWorkspaceContainer);
        }
    }

    private void updateSearchHint() {
        updateSearchHint(requireUiState().workspaceMode);
    }

    private void updateSearchHint(MainWorkspaceMode workspaceMode) {
        if (searchInput == null) {
            return;
        }
        boolean templateWorkspace = workspaceMode == MainWorkspaceMode.TEMPLATE;
        CharSequence current = searchInput.getText();
        boolean empty = current == null || current.length() == 0;
        if (templateWorkspace) {
            if (searchInput.hasFocus() || empty) {
                searchInput.setHint(getString(R.string.template_search_hint));
            }
            return;
        }
        if (searchInput.hasFocus()) {
            searchInput.setHint("");
            return;
        }
        if (empty) {
            searchInput.setHint(getString(R.string.search_hint));
        }
    }

    private void selectWorkspaceItem(int itemId) {
        if (workspaceSwitch == null) {
            return;
        }
        updatingWorkspaceSelection = true;
        try {
            workspaceSwitch.setSelectedItemId(itemId);
        } finally {
            updatingWorkspaceSelection = false;
        }
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void resetHiddenWorkspacePresentation(MainWorkspaceMode visibleMode) {
        resetWorkspacePresentationUnlessMode(appPager, visibleMode, MainWorkspaceMode.APP);
        resetWorkspacePresentationUnlessMode(
                landListPageView, visibleMode, MainWorkspaceMode.APP);
        resetWorkspacePresentationUnlessMode(
                homeWorkspaceContainer, visibleMode, MainWorkspaceMode.HOME);
        resetWorkspacePresentationUnlessMode(
                templateWorkspaceContainer, visibleMode, MainWorkspaceMode.TEMPLATE);
        resetWorkspacePresentationUnlessMode(
                toolsWorkspaceContainer, visibleMode, MainWorkspaceMode.TOOLS);
        resetWorkspacePresentationUnlessMode(
                settingsWorkspaceContainer, visibleMode, MainWorkspaceMode.SETTINGS);
    }

    private static void resetWorkspacePresentationUnlessMode(
            View view,
            MainWorkspaceMode visibleMode,
            MainWorkspaceMode viewMode
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

    private void animateVisibleWorkspaceContent(MainWorkspaceMode mode) {
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

    private View workspaceViewForMode(MainWorkspaceMode mode) {
        if (mode == MainWorkspaceMode.APP) {
            return appPager != null ? appPager : landListPageView;
        }
        if (mode == MainWorkspaceMode.HOME) {
            return homeWorkspaceContainer;
        }
        if (mode == MainWorkspaceMode.TEMPLATE) {
            return templateWorkspaceContainer;
        }
        if (mode == MainWorkspaceMode.TOOLS) {
            return toolsWorkspaceContainer;
        }
        if (mode == MainWorkspaceMode.SETTINGS) {
            return settingsWorkspaceContainer;
        }
        return null;
    }

    private static int workspaceButtonId(MainWorkspaceMode workspaceMode) {
        if (workspaceMode == MainWorkspaceMode.TEMPLATE) {
            return R.id.workspace_template_button;
        }
        if (workspaceMode == MainWorkspaceMode.HOME) {
            return R.id.workspace_home_button;
        }
        if (workspaceMode == MainWorkspaceMode.TOOLS) {
            return R.id.workspace_tools_button;
        }
        if (workspaceMode == MainWorkspaceMode.SETTINGS) {
            return R.id.workspace_settings_button;
        }
        return R.id.workspace_app_button;
    }

    private static MainWorkspaceMode workspaceModeForButtonId(int checkedId) {
        if (checkedId == R.id.workspace_template_button) {
            return MainWorkspaceMode.TEMPLATE;
        }
        if (checkedId == R.id.workspace_home_button) {
            return MainWorkspaceMode.HOME;
        }
        if (checkedId == R.id.workspace_tools_button) {
            return MainWorkspaceMode.TOOLS;
        }
        if (checkedId == R.id.workspace_settings_button) {
            return MainWorkspaceMode.SETTINGS;
        }
        return MainWorkspaceMode.APP;
    }

    private void handleMainUiEffects(List<MainUiEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        for (MainUiEffect effect : effects) {
            if (effect instanceof MainUiEffect.StartAppsLoad) {
                startAppsLoad((MainUiEffect.StartAppsLoad) effect);
            }
        }
    }

    private void showFilterDialog() {
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_list_filters,
                root,
                false
        );
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        MaterialSwitch showSystemSwitch = dialogView.findViewById(
                R.id.filter_show_system_switch
        );
        MaterialSwitch injectedOnlySwitch = dialogView.findViewById(
                R.id.filter_injected_only_switch
        );
        MaterialSwitch widthOnlySwitch = dialogView.findViewById(
                R.id.filter_width_only_switch
        );
        MaterialSwitch fontOnlySwitch = dialogView.findViewById(
                R.id.filter_font_only_switch
        );
        MainUiState state = requireUiState();

        showSystemSwitch.setChecked(state.filterState.showSystemApps());
        injectedOnlySwitch.setChecked(state.filterState.injectedOnly());
        widthOnlySwitch.setChecked(state.filterState.widthConfiguredOnly());
        fontOnlySwitch.setChecked(state.filterState.fontConfiguredOnly());

        android.widget.CompoundButton.OnCheckedChangeListener listener = (
                buttonView,
                isChecked) -> {
            AppListFilterState filterState = new AppListFilterState(
                    showSystemSwitch.isChecked(),
                    injectedOnlySwitch.isChecked(),
                    widthOnlySwitch.isChecked(),
                    fontOnlySwitch.isChecked()
            );
            appListFilterStateStore.save(filterState);
            dispatchMainUiAction(MainUiAction.filterChanged(filterState));
        };
        showSystemSwitch.setOnCheckedChangeListener(listener);
        injectedOnlySwitch.setOnCheckedChangeListener(listener);
        widthOnlySwitch.setOnCheckedChangeListener(listener);
        fontOnlySwitch.setOnCheckedChangeListener(listener);
        dialog.show();
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
        if (!ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(this)) {
            return false;
        }
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_module_runtime_reload_advice,
                null,
                false
        );
        MaterialButton ackButton = dialogView.findViewById(
                R.id.module_runtime_reload_ack_button
        );
        androidx.appcompat.app.AlertDialog dialog
                = new MaterialAlertDialogBuilder(this).setView(dialogView).create();
        dialog.setCanceledOnTouchOutside(true);
        ackButton.setOnClickListener(v -> {
            ModuleRuntimeReloadAdvisor.markReloadAdviceAcknowledged(this);
            dialog.dismiss();
            continueStartupDialogsAfterRuntimeReloadAdvice();
        });
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
        return true;
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

    private void retryHomeUpdateCheck() {
        startupUpdateCheckCoordinator.checkForUpdatesNow();
    }

    private void startStartupUpdateDownload(
            String targetVersionName,
            String downloadUrl,
            androidx.appcompat.app.AlertDialog dialog,
            MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView
    ) {
        updateDownloadCoordinator.startDownload(
                targetVersionName,
                downloadUrl,
                dialog,
                primaryButton,
                cancelButton,
                progressView,
                progressTextView
        );
    }

    private void startHomeUpdateDownload() {
        HomeUpdateUiState current = homeUpdateUiState;
        if (current == null || current.status != HomeUpdateUiState.Status.AVAILABLE) {
            return;
        }
        markPromptedVersion(current.versionCode);
        updateDownloadCoordinator.startHomeDownload(
                current.versionName,
                current.apkUrl,
                new UpdateDownloadCoordinator.HomeDownloadListener() {
                    @Override
                    public void onStarted() {
                        homeUpdateUiState = current.asDownloading(0);
                        bindHomeWorkspaceIfVisible();
                    }

                    @Override
                    public void onProgress(int progress) {
                        homeUpdateUiState = current.asDownloading(progress);
                        bindHomeWorkspaceIfVisible();
                    }

                    @Override
                    public void onSucceeded(File targetFile) {
                        homeUpdateUiState = current.asInstallReady(targetFile);
                        bindHomeWorkspaceIfVisible();
                    }

                    @Override
                    public void onFinished() {
                        if (homeUpdateUiState.status == HomeUpdateUiState.Status.DOWNLOADING) {
                            homeUpdateUiState = current.asAvailable();
                            bindHomeWorkspaceIfVisible();
                        }
                    }
                });
    }

    private void installHomeDownloadedUpdate() {
        HomeUpdateUiState current = homeUpdateUiState;
        if (current == null || current.status != HomeUpdateUiState.Status.INSTALL_READY) {
            return;
        }
        File apkFile = current.downloadedApkPath.isEmpty()
                ? null
                : new File(current.downloadedApkPath);
        if (apkFile == null || !apkFile.exists()) {
            homeUpdateUiState = current.asAvailable();
            bindHomeWorkspaceIfVisible();
            showToast(R.string.about_update_download_failed);
            return;
        }
        startupUpdatePackageHandler.launchPackageInstaller(apkFile);
    }

    private void showHomeUpdateReleaseNotesDialog() {
        HomeUpdateUiState current = homeUpdateUiState;
        if (current == null || !current.showsUpdateActionCard()) {
            return;
        }
        MaxHeightNestedScrollView scrollView = new MaxHeightNestedScrollView(this);
        scrollView.setMaxHeightFraction(0.62f);
        MaterialTextView textView = new MaterialTextView(this);
        textView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        textView.setTextColor(MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                getColor(android.R.color.black)));
        textView.setLineSpacing(
                getResources().getDimension(R.dimen.dialog_text_line_spacing),
                1f);
        int padding = getResources().getDimensionPixelSize(
                R.dimen.dialog_surface_padding_horizontal);
        scrollView.setPadding(padding, padding / 2, padding, 0);
        scrollView.addView(textView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.about_update_release_notes_title)
                        .setView(scrollView)
                        .setPositiveButton(R.string.about_update_action_cancel_dialog, null)
                        .create();
        String embedded = current.releaseNotes;
        if (!embedded.isEmpty()) {
            textView.setText(ReleaseNotesMarkdownRenderer.render(
                    this,
                    embedded,
                    getResources().getConfiguration().getLocales().get(0)));
        } else {
            textView.setText(R.string.about_update_release_notes_loading);
        }
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, this);
        if (embedded.isEmpty()) {
            loadHomeReleaseNotes(textView, dialog, current.versionName);
        }
    }

    private void loadHomeReleaseNotes(MaterialTextView textView,
            androidx.appcompat.app.AlertDialog dialog,
            String versionName) {
        releaseNotesController.load(versionName, false,
                new ReleaseNotesController.Listener() {
                    @Override
                    public boolean isAlive() {
                        return !isFinishing()
                                && !isDestroyed()
                                && dialog.isShowing();
                    }

                    @Override
                    public void onBody(String body) {
                        textView.setText(ReleaseNotesMarkdownRenderer.render(
                                MainActivity.this,
                                body,
                                getResources().getConfiguration().getLocales().get(0)));
                    }

                    @Override
                    public void onEmptyBody() {
                        textView.setText(R.string.about_update_release_notes_empty);
                    }

                    @Override
                    public void onFailure() {
                        textView.setText(R.string.about_update_release_notes_failed);
                    }
                });
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
                && requireUiState().workspaceMode == MainWorkspaceMode.HOME) {
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
            @Override
            public PackageManager getPackageManager() {
                return MainActivity.this.getPackageManager();
            }

            @Override
            public String getSelfPackageName() {
                return MainActivity.this.getPackageName();
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }

            @Override
            public View getIconRefreshAnchor() {
                return appPager != null
                        ? appPager
                        : findViewById(android.R.id.content);
            }

            @Override
            public void requestAppsLoad() {
                MainActivity.this.requestAppsLoad();
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
            public void runOnUiThread(Runnable runnable) {
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

            @Override
            public String getManifestUrl() {
                return MainActivity.this.getString(
                        R.string.about_update_manifest_url
                );
            }

            @Override
            public void executeBackground(Runnable runnable) {
                startupUpdateExecutor.execute(runnable);
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }

            @Override
            public UpdateCoordinator.State buildUpdateCoordinatorState() {
                return MainActivity.this.buildUpdateCoordinatorState();
            }

            @Override
            public void applyStartupCheckState(UpdateCoordinator.State state) {
                MainActivity.this.applyStartupCheckState(state);
            }

            @Override
            public int getLocalVersionCode() {
                return BuildConfig.VERSION_CODE;
            }

            @Override
            public String getLocalVersionName() {
                return BuildConfig.VERSION_NAME;
            }

            @Override
            public void onStartupUpdateCheckStarted() {
                MainActivity.this.applyHomeUpdateState(HomeUpdateUiState.CHECKING);
            }

            @Override
            public void onStartupUpdateAvailable(StartupUpdateManifest manifest) {
                MainActivity.this.applyHomeUpdateState(
                        HomeUpdateUiState.available(manifest)
                );
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

    private UpdatePromptDialogCoordinator.Host createUpdatePromptDialogHost() {
        return new UpdatePromptDialogCoordinator.Host() {
            @Override
            public void showDialogIdleState(
                    MaterialButton primaryButton,
                    MaterialButton cancelButton,
                    LinearProgressIndicator progressView,
                    MaterialTextView progressTextView
            ) {
                UpdateDownloadCoordinator.showDialogIdleState(
                        primaryButton,
                        cancelButton,
                        progressView,
                        progressTextView
                );
            }

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
                    String targetVersionName,
                    String downloadUrl,
                    androidx.appcompat.app.AlertDialog dialog,
                    MaterialButton primaryButton,
                    MaterialButton cancelButton,
                    LinearProgressIndicator progressView,
                    MaterialTextView progressTextView
            ) {
                MainActivity.this.startStartupUpdateDownload(
                        targetVersionName,
                        downloadUrl,
                        dialog,
                        primaryButton,
                        cancelButton,
                        progressView,
                        progressTextView
                );
            }

            @Override
            public void openUrl(String url) {
                MainActivity.this.openUrl(url);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }

            @Override
            public void applyLargeDialogWidth(androidx.appcompat.app.AlertDialog dialog) {
                DialogWindowSizer.applyLargeWidth(dialog, MainActivity.this);
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
        TemplateConfigValue globalPrefill = new GlobalPrefillStore(
                getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
        ).read();
        AppListItem sheetItem = AppConfigPrefillPreview.applyIfEligible(
                item,
                store,
                globalPrefill
        );
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
        AppConfigDraft draft = mainViewModel != null
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
        TemplateConfigValue globalPrefill = new GlobalPrefillStore(
                getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
        ).read();
        AppListItem sheetItem = AppConfigPrefillPreview.applyIfEligible(
                item,
                store,
                globalPrefill
        );
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
            AppConfigDraft draft = mainViewModel.getEditingDraft();
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
        if (homeWorkspaceBinder != null) {
            maybeStartRootAccessProbe();
            homeWorkspaceBinder.bind(
                    homeWorkspaceContainer,
                    createHomeWorkspaceState()
            );
        }
    }

    private HomeWorkspaceBinder.State createHomeWorkspaceState() {
        DpisConfigStore configStore = getHookConfigStore();
        int visibleConfiguredAppCount = countUserVisibleConfiguredPackages(
                configStore,
                loadScopeState()
        );
        return new HomeWorkspaceBinder.State(
                HomeActivationStateResolver.isActivatedForHome(),
                visibleConfiguredAppCount,
                ConfigStoreFactory.createLocalUiFontLibraryStore(
                        this,
                        DpisApplication.getXposedService()
                ).listFonts().size(),
                new QuickTemplateStore(
                        getSharedPreferences(
                                DpisConfigStore.GROUP,
                                Context.MODE_PRIVATE
                        )
                ).readAll().size(),
                RootAccessProbe.cachedResult(),
                homeUpdateUiState,
                createHomeWorkspaceActions()
        );
    }

    private HomeWorkspaceBinder.Actions createHomeWorkspaceActions() {
        return new HomeWorkspaceBinder.Actions() {
            @Override
            public void retryUpdateCheck() {
                retryHomeUpdateCheck();
            }

            @Override
            public void showReleaseNotes() {
                showHomeUpdateReleaseNotesDialog();
            }

            @Override
            public void startUpdateDownload() {
                startHomeUpdateDownload();
            }

            @Override
            public void installDownloadedUpdate() {
                installHomeDownloadedUpdate();
            }

            @Override
            public void openConfiguredAppsWorkspace() {
                setCurrentAppListPage(AppListPage.CONFIGURED_APPS, false);
                dispatchMainUiAction(
                        MainUiAction.workspaceModeChanged(MainWorkspaceMode.APP)
                );
            }

            @Override
            public void openFontLibrary() {
                startActivity(new Intent(MainActivity.this, FontLibraryActivity.class));
            }

            @Override
            public void openTemplateWorkspace() {
                dispatchMainUiAction(
                        MainUiAction.workspaceModeChanged(MainWorkspaceMode.TEMPLATE)
                );
            }
        };
    }

    static int countUserVisibleConfiguredPackages(DpisConfigStore store,
            ScopeState scopeState) {
        Set<String> packageNames = new HashSet<>();
        if (store != null) {
            packageNames.addAll(store.getConfiguredPackages());
        }
        ScopeState safeScopeState = scopeState != null
                ? scopeState
                : new ScopeState(Collections.emptySet(), false);
        if (safeScopeState.known) {
            packageNames.addAll(safeScopeState.packages);
        }
        int count = 0;
        for (String packageName : packageNames) {
            if (InstalledAppCatalogCoordinator.isUserVisibleConfiguredPackage(
                    store,
                    packageName,
                    safeScopeState.known,
                    safeScopeState.packages.contains(packageName))) {
                count++;
            }
        }
        return count;
    }

    static final class ScopeState {
        final Set<String> packages;
        final boolean known;

        ScopeState(Set<String> packages, boolean known) {
            this.packages = packages != null ? packages : Collections.emptySet();
            this.known = known;
        }
    }

    private void maybeStartRootAccessProbe() {
        if (rootAccessProbeInFlight
                || RootAccessProbe.cachedResult().status
                        != RootAccessProbe.Status.UNKNOWN) {
            return;
        }
        rootAccessProbeInFlight = true;
        startupUpdateExecutor.execute(() -> {
            RootAccessProbe.Result result = RootAccessProbe.probe();
            runOnUiThread(() -> {
                rootAccessProbeInFlight = false;
                if (requireUiState().workspaceMode == MainWorkspaceMode.HOME) {
                    bindHomeWorkspace();
                }
            });
        });
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

    private AppConfigSaveHandler.Result saveLandDetailResolvedConfig(
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

    private AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync(
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
        if (result == null || !result.success) {
            return result;
        }
        scheduleRuntimePropertiesForTargetLaunch(packageName);
        return result;
    }

    private void onRuntimeConfigSaved() {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        requestAppsLoad();
    }

    private void scheduleRuntimePropertiesForTargetLaunch(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        int generation;
        synchronized (pendingRuntimePropertyGenerations) {
            generation = pendingRuntimePropertyGenerations.getOrDefault(packageName, 0) + 1;
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

    private void toggleLandDetailScope(
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

    private TemplateWorkspaceBinder.GlobalPrefillActions createTemplateWorkspaceActions() {
        return new TemplateWorkspaceBinder.GlobalPrefillActions() {
            @Override
            public void edit() {
                showGlobalPrefillEditor();
            }
        };
    }

    private TemplateWorkspaceBinder.QuickTemplateActions createQuickTemplateActions() {
        return new TemplateWorkspaceBinder.QuickTemplateActions() {
            @Override
            public void apply(String templateId) {
                applyQuickTemplate(templateId);
            }

            @Override
            public void edit(String templateId) {
                showQuickTemplateEditor(templateId);
            }

            @Override
            public void select(String templateId) {
                showQuickTemplateTargets(templateId);
            }

            @Override
            public void create() {
                showQuickTemplateEditor(null);
            }

            @Override
            public void sort(List<QuickTemplateStore.QuickTemplate> templates) {
                QuickTemplateSortDialog.show(
                        MainActivity.this,
                        templates,
                        new QuickTemplateSortDialog.Host() {
                    @Override
                    public boolean saveOrder(List<String> orderedIds) {
                        return new QuickTemplateStore(
                                getSharedPreferences(
                                        DpisConfigStore.GROUP,
                                        Context.MODE_PRIVATE
                                )
                        ).reorder(orderedIds);
                    }

                    @Override
                    public void onOrderSaved() {
                        showToast(R.string.quick_template_sort_saved);
                        bindTemplateWorkspace();
                    }

                    @Override
                    public void showToast(int messageResId) {
                        MainActivity.this.showToast(messageResId);
                    }
                }
                );
            }
        };
    }

    private void applyQuickTemplate(String templateId) {
        QuickTemplateStore store = new QuickTemplateStore(
                getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
        );
        QuickTemplateStore.QuickTemplate template = store.read(templateId);
        if (template == null) {
            showToast(R.string.quick_template_target_missing);
            bindTemplateWorkspace();
            return;
        }
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                QuickTemplateApplyAdapters.from(getHookConfigStore());
        QuickTemplateApplyCoordinator.TargetPackageFilter installedPackageFilter
                = this::isInstalledTemplateTargetPackage;
        QuickTemplateApplyCoordinator.Plan plan = coordinator.plan(
                template,
                installedPackageFilter
        );
        if (plan.targetCount <= 0) {
            showToast(R.string.quick_template_apply_empty_selection);
            return;
        }
        String message = QuickTemplateApplyConfirmationMessage.format(
                plan.targetCount,
                plan.overwriteCount,
                new QuickTemplateApplyConfirmationMessage.Strings() {
                    @Override
                    public String plain(int targetCount) {
                        return getString(
                                R.string.quick_template_apply_confirm_message,
                                targetCount
                        );
                    }

                    @Override
                    public String overwrite(int targetCount, int overwriteCount) {
                        return getString(
                                R.string.quick_template_apply_confirm_message_overwrite,
                                targetCount,
                                overwriteCount
                        );
                    }

                    @Override
                    public String scopeNote() {
                        return getString(R.string.quick_template_apply_scope_note);
                    }
                });
        androidx.appcompat.app.AlertDialog dialog
                = new MaterialAlertDialogBuilder(this)
                        .setTitle(
                                getString(
                                        R.string.quick_template_apply_confirm_title,
                                        template.name
                                )
                        )
                        .setMessage(message)
                        .setNegativeButton(
                                R.string.dialog_process_action_confirm_negative,
                                null
                        )
                        .setPositiveButton(
                                R.string.template_workspace_action_apply,
                                (unusedDialog, which)
                                -> finishQuickTemplateApply(
                                        coordinator,
                                        template,
                                        installedPackageFilter
                                )
                        )
                        .create();
        dialog.setOnShowListener(d -> {
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(
                            androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
                    )
            );
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(
                            androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
                    )
            );
        });
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
    }

    private void finishQuickTemplateApply(
            QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator,
            QuickTemplateStore.QuickTemplate template
    ) {
        finishQuickTemplateApply(coordinator, template, null);
    }

    private void finishQuickTemplateApply(
            QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator,
            QuickTemplateStore.QuickTemplate template,
            QuickTemplateApplyCoordinator.TargetPackageFilter targetPackageFilter
    ) {
        QuickTemplateApplyCoordinator.Result result = coordinator.apply(
                template,
                targetPackageFilter
        );
        if (result.emptySelection) {
            showToast(R.string.quick_template_apply_empty_selection);
            return;
        }
        if (result.failureCount() > 0) {
            showToast(
                    R.string.quick_template_apply_result_partial,
                    result.successCount(),
                    result.failureCount()
            );
        } else {
            showToast(
                    R.string.quick_template_apply_result_success,
                    result.successCount()
            );
        }
        if (result.successCount() > 0) {
            onRuntimeConfigSaved();
        }
        new BatchScopeRequestCoordinator(
                createBatchScopeRequestHost()
        ).requestMissingScope(result.successfulPackages);
        bindTemplateWorkspace();
    }

    private boolean isInstalledTemplateTargetPackage(String packageName) {
        if (packageName == null
                || packageName.isBlank()
                || getPackageName().equals(packageName)) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageManager().getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                );
            } else {
                getPackageManager().getApplicationInfo(packageName, 0);
            }
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private BatchScopeRequestCoordinator.Host createBatchScopeRequestHost() {
        return new BatchScopeRequestCoordinator.Host() {
            @Override
            public void showToast(int messageResId, Object... formatArgs) {
                MainActivity.this.showToast(messageResId, formatArgs);
            }

            @Override
            public void requestAppsLoad() {
                MainActivity.this.requestAppsLoad();
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }
        };
    }

    private AppConfigDialogBinder.Host createAppConfigDialogHost() {
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

    private FeedbackDiagnosticCoordinator.Host createFeedbackDiagnosticHost() {
        return new FeedbackDiagnosticCoordinator.Host() {
            @Override
            public boolean restartTargetAppForDiagnostic(String packageName) {
                syncRuntimePropertiesForTargetLaunch(packageName);
                boolean launched = feedbackDiagnosticAppLauncher
                        .restartForDiagnostic(packageName);
                if (launched && activeAppEditorDialog != null) {
                    activeAppEditorDialog.dismiss();
                }
                return launched;
            }

            @Override
            public String dpisPackageName() {
                return getPackageName();
            }

            @Override
            public RootAccessProbe.Result rootAccess() {
                return RootAccessProbe.cachedResult();
            }

            @Override
            public boolean systemHooksEnabled() {
                return isSystemHookEnabledFromStore();
            }

            @Override
            public long currentTimeMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public void onFeedbackDiagnosticStarted() {
                showToast(R.string.feedback_diagnostic_started);
            }

            @Override
            public void onFeedbackDiagnosticUnavailable() {
                showToast(R.string.feedback_diagnostic_unavailable);
            }

            @Override
            public void onFeedbackDiagnosticRootRequired() {
                showToast(R.string.feedback_diagnostic_root_required);
            }

            @Override
            public void onFeedbackDiagnosticFinished(
                    FeedbackDiagnosticCoordinator.Result result
            ) {
                pendingFeedbackDiagnosticResult = result;
                maybeShowPendingFeedbackDiagnosticResult();
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
        if (!DiagnosticLogGate.ensureEnabled(
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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.feedback_diagnostic_action)
                .setMessage(getString(
                        R.string.feedback_diagnostic_confirm_message,
                        item.label
                ))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.feedback_diagnostic_save_and_start_button, (dialog, which) -> {
                    AppListItem diagnosticItem = saveCurrentEditorConfigForDiagnostic(item, state);
                    if (diagnosticItem == null) {
                        return;
                    }
                    boolean started = feedbackDiagnosticCoordinator.start(
                            FeedbackDiagnosticCoordinator.Request.fromPersisted(
                                    diagnosticItem,
                                    state,
                                    resolvePackageVersionName(item.packageName),
                                    getHookConfigStore()
                            )
                    );
                    if (!started) {
                        showToast(R.string.feedback_diagnostic_unavailable);
                    }
                })
                .show();
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

    private void maybeShowPendingFeedbackDiagnosticResult() {
        FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
                = pendingFeedbackDiagnosticPackage;
        if (diagnosticPackage != null && mainActivityResumed) {
            pendingFeedbackDiagnosticPackage = null;
            dismissFeedbackDiagnosticPackagingDialog();
            showFeedbackDiagnosticResultSheet(diagnosticPackage);
            return;
        }
        FeedbackDiagnosticCoordinator.Result result = pendingFeedbackDiagnosticResult;
        if (result == null || !mainActivityResumed) {
            return;
        }
        pendingFeedbackDiagnosticResult = null;
        showFeedbackDiagnosticPackagingDialog();
        feedbackDiagnosticExportExecutor.execute(() -> {
            FeedbackDiagnosticExportBuilder.DiagnosticPackage built = null;
            try {
                built = feedbackDiagnosticExportBuilder.buildPackage(result);
            } catch (IOException | RuntimeException ignored) {
                built = null;
            }
            FeedbackDiagnosticExportBuilder.DiagnosticPackage finalBuilt = built;
            runOnUiThread(() -> {
                dismissFeedbackDiagnosticPackagingDialog();
                if (finalBuilt == null) {
                    Toast.makeText(
                            this,
                            R.string.feedback_diagnostic_save_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                if (!mainActivityResumed) {
                    pendingFeedbackDiagnosticPackage = finalBuilt;
                    return;
                }
                showFeedbackDiagnosticResultSheet(finalBuilt);
            });
        });
    }

    private void showFeedbackDiagnosticResultSheet(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        new FeedbackDiagnosticResultSheet(this, new FeedbackDiagnosticResultSheet.Host() {
            @Override
            public void shareFeedbackDiagnostic(
                    FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                MainActivity.this.shareFeedbackDiagnostic(diagnosticPackage);
            }

            @Override
            public void saveFeedbackDiagnostic(
                    FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                MainActivity.this.launchSaveFeedbackDiagnosticPicker(diagnosticPackage);
            }
        }).show(diagnosticPackage);
    }

    private void showFeedbackDiagnosticPackagingDialog() {
        dismissFeedbackDiagnosticPackagingDialog();
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_feedback_diagnostic_packaging, null, false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
        activeFeedbackDiagnosticPackagingDialog = dialog;
    }

    private void dismissFeedbackDiagnosticPackagingDialog() {
        if (activeFeedbackDiagnosticPackagingDialog != null) {
            activeFeedbackDiagnosticPackagingDialog.dismiss();
            activeFeedbackDiagnosticPackagingDialog = null;
        }
    }

    private String resolvePackageVersionName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }
        try {
            return getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    @SuppressWarnings("deprecation")
    private void launchSaveFeedbackDiagnosticPicker(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        pendingFeedbackDiagnosticPackage = diagnosticPackage;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(FeedbackDiagnosticExportBuilder.MIME_TYPE)
                .putExtra(
                        Intent.EXTRA_TITLE,
                        diagnosticPackage.fileName
                );
        try {
            startActivityForResult(intent, REQUEST_SAVE_FEEDBACK_DIAGNOSTIC);
        } catch (ActivityNotFoundException error) {
            pendingFeedbackDiagnosticPackage = null;
            Toast.makeText(
                    this,
                    R.string.feedback_diagnostic_save_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void saveFeedbackDiagnosticZip(Uri uri) {
        FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
                = pendingFeedbackDiagnosticPackage;
        pendingFeedbackDiagnosticPackage = null;
        if (uri == null || diagnosticPackage == null) {
            Toast.makeText(this, R.string.feedback_diagnostic_save_failed, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        feedbackDiagnosticExportExecutor.execute(() -> {
            boolean success;
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Unable to open diagnostic output");
                }
                outputStream.write(diagnosticPackage.zipBytes);
                success = true;
            } catch (IOException | RuntimeException error) {
                success = false;
            }
            boolean finalSuccess = success;
            runOnUiThread(() -> Toast.makeText(
                    this,
                    finalSuccess
                            ? R.string.feedback_diagnostic_save_success
                            : R.string.feedback_diagnostic_save_failed,
                    Toast.LENGTH_SHORT
            ).show());
        });
    }

    private void shareFeedbackDiagnostic(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        feedbackDiagnosticExportExecutor.execute(() -> {
            Uri uri = null;
            boolean success = false;
            try {
                File file = writeSharedFeedbackDiagnosticZip(diagnosticPackage);
                uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        file
                );
                success = true;
            } catch (IOException | RuntimeException error) {
                success = false;
            }
            Uri finalUri = uri;
            boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (!finalSuccess || finalUri == null) {
                    Toast.makeText(
                            this,
                            R.string.feedback_diagnostic_share_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                launchFeedbackDiagnosticShareSheet(finalUri);
            });
        });
    }

    private File writeSharedFeedbackDiagnosticZip(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) throws IOException {
        File directory = new File(getCacheDir(), SHARED_FEEDBACK_DIAGNOSTIC_DIRECTORY_NAME);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create diagnostic share directory");
        }
        File file = new File(directory, diagnosticPackage.fileName);
        try (OutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(diagnosticPackage.zipBytes);
        }
        return file;
    }

    private void launchFeedbackDiagnosticShareSheet(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(FeedbackDiagnosticExportBuilder.MIME_TYPE)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(
                    intent,
                    getString(R.string.feedback_diagnostic_share_action)
            ));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(
                    this,
                    R.string.feedback_diagnostic_share_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showFontHookDomains(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onStateChanged
    ) {
        if (item == null
                || item.packageName == null
                || item.packageName.isBlank()) {
            return;
        }
        DpisConfigStore store = getHookConfigStore();
        Set<String> automaticKnownDomains = recommendedTemplateFontHookDomains();
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
                isFontHookDomainEditingEnabled(),
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

    private String getFontHookDomainsButtonText(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        return FontHookDomainPresentation.forOverride(
                resolveFontHookDomainsForDraft(item, state),
                recommendedTemplateFontHookDomains())
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
                    recommendedTemplateFontHookDomains());
        }
        return normalizedFontHookDomainsOverride(
                new HookDomainOverrideStore(getHookConfigStore()).read(
                        item != null ? item.packageName : null),
                recommendedTemplateFontHookDomains());
    }

    private HookDomainOverride normalizedFontHookDomainsOverride(
            HookDomainOverride override,
            Set<String> automaticKnownDomains) {
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                override,
                automaticKnownDomains);
    }

    private Set<String> recommendedTemplateFontHookDomains() {
        // The custom hook-chain editor owns the compat/field-rewrite route.
        // System-mode font routes are scheduled separately and must not share
        // this user-editable switch state. This template is intentionally not
        // package-specific; built-in app routes must not become implicit custom
        // hook-chain selections.
        return FontHookDomainRegistry.recommendedTemplateKnownDomains();
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

    private void executeDialogProcessAction(
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
        if (item == null || !item.hyperOsNativeProxyCandidate) {
            return false;
        }
        DpisConfigStore store = getHookConfigStore();
        return (store.isTargetDpisEnabled(item.packageName)
                && hasActiveStoredConfig(store, item.packageName));
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

    private boolean isSystemHookEnabledFromStore() {
        return cachedSystemHookEffectiveEnabled;
    }

    private interface HyperOsNativeProxyMountCallback {

        void onFinished(boolean success);
    }

    private void refreshSystemHookEffectiveEnabled() {
        cachedSystemHookEffectiveEnabled
                = systemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                        getHookConfigStore()
                );
    }

    private DpisConfigStore getHookConfigStore() {
        return DpisApplication.getActiveHookConfigStore(this);
    }

    private AppConfigDraft captureAppConfigDraft() {
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
        if (state != null && state.packageName != null && !state.packageName.isBlank()) {
            packageName = state.packageName;
        }
        if ((packageName == null || packageName.isBlank()) && mainViewModel != null) {
            packageName = mainViewModel.getEditingPackageName();
        }
        AppConfigDraft current = mainViewModel != null
                ? mainViewModel.getEditingDraft()
                : null;
        boolean useCurrentState = current != null
                && current.packageName != null
                && current.packageName.equals(packageName);
        AppConfigDraft draft = new AppConfigDraft(
                packageName,
                viewportText,
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
                WechatDpiSheetBinder.captureDraft(root)
        );
        return draft;
    }

    private void updateEditingDraft(AppConfigDialogBinder.AppConfigDialogState state) {
        if (mainViewModel == null || state == null) {
            return;
        }
        AppConfigDraft captured = captureAppConfigDraft();
        if (captured != null) {
            mainViewModel.setEditingDraft(captured);
            return;
        }
        AppConfigDraft current = mainViewModel.getEditingDraft();
        String packageName = state.packageName != null && !state.packageName.isBlank()
                ? state.packageName
                : mainViewModel.getEditingPackageName();
        AppConfigDraft draft = new AppConfigDraft(
                packageName,
                current != null ? current.viewportInput : "",
                current != null ? current.viewportMode : ViewportTargetType.RELATIVE_SCALE,
                current != null ? current.fontInput : "",
                current != null ? current.fontMode : FontApplyMode.SYSTEM_EMULATION,
                state.selectedTypefaceId,
                state.draftFontHookDomainsRaw,
                state.viewportApplyMode,
                state.fontHookDomainsResetRequested,
                state.viewportApplyModeResetRequested,
                current != null ? current.wechatDpiInput : null
        );
        mainViewModel.setEditingDraft(draft);
    }

    private void applyAppConfigDraft(View root, AppConfigDraft draft) {
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
        if (viewportInput != null && draft.viewportInput != null) {
            viewportInput.setText(draft.viewportInput);
        }
        if (fontInput != null && draft.fontInput != null) {
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

    static final class AppConfigDraft {

        final String packageName;
        final String viewportInput;
        final String viewportMode;
        final String fontInput;
        final String fontMode;
        final String selectedTypefaceId;
        final String draftFontHookDomainsRaw;
        final String viewportApplyMode;
        final boolean fontHookDomainsResetRequested;
        final boolean viewportApplyModeResetRequested;
        final String wechatDpiInput;

        AppConfigDraft(
                String packageName,
                String viewportInput,
                String viewportMode,
                String fontInput,
                String fontMode,
                String selectedTypefaceId,
                String draftFontHookDomainsRaw,
                String viewportApplyMode,
                boolean fontHookDomainsResetRequested,
                boolean viewportApplyModeResetRequested,
                String wechatDpiInput
        ) {
            this.packageName = packageName;
            this.viewportInput = viewportInput;
            this.viewportMode = viewportMode;
            this.fontInput = fontInput;
            this.fontMode = fontMode;
            this.selectedTypefaceId = selectedTypefaceId;
            this.draftFontHookDomainsRaw = draftFontHookDomainsRaw;
            this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
            this.fontHookDomainsResetRequested = fontHookDomainsResetRequested;
            this.viewportApplyModeResetRequested = viewportApplyModeResetRequested;
            this.wechatDpiInput = wechatDpiInput;
        }
    }

    private static final class RetainedState {

        final List<AppListItem> appsSnapshot;
        final String query;
        final String templateQuery;
        final AppListFilterState filterState;
        final MainWorkspaceMode workspaceMode;
        final int currentPage;
        final SparseArray<Parcelable> pageScrollStates;
        final int[] refreshingPagePositions;
        final String editingPackageName;
        final AppConfigDraft editingDraft;
        final TemplateDetailSelection templateDetailSelection;
        final boolean quickTemplateTargetSelectionActivityStarted;
        final GlobalPrefillEditorBinder.Draft globalPrefillDraft;
        final QuickTemplateEditorBinder.Draft quickTemplateDraft;
        final HomeUpdateUiState homeUpdateUiState;

        RetainedState(
                List<AppListItem> appsSnapshot,
                String query,
                String templateQuery,
                AppListFilterState filterState,
                MainWorkspaceMode workspaceMode,
                int currentPage,
                SparseArray<Parcelable> pageScrollStates,
                int[] refreshingPagePositions,
                String editingPackageName,
                AppConfigDraft editingDraft,
                TemplateDetailSelection templateDetailSelection,
                boolean quickTemplateTargetSelectionActivityStarted,
                GlobalPrefillEditorBinder.Draft globalPrefillDraft,
                QuickTemplateEditorBinder.Draft quickTemplateDraft,
                HomeUpdateUiState homeUpdateUiState
        ) {
            this.appsSnapshot = appsSnapshot;
            this.query = query != null ? query : "";
            this.templateQuery = templateQuery != null ? templateQuery : "";
            this.filterState
                    = filterState != null
                            ? filterState
                            : AppListFilterState.defaultState();
            this.workspaceMode
                    = workspaceMode != null ? workspaceMode : MainWorkspaceMode.APP;
            this.currentPage = currentPage;
            this.pageScrollStates
                    = pageScrollStates != null ? pageScrollStates.clone() : null;
            this.refreshingPagePositions
                    = refreshingPagePositions != null
                            ? refreshingPagePositions.clone()
                            : new int[0];
            this.editingPackageName = editingPackageName;
            this.editingDraft = editingDraft;
            this.templateDetailSelection = templateDetailSelection != null
                    ? templateDetailSelection
                    : TemplateDetailSelection.none();
            this.quickTemplateTargetSelectionActivityStarted =
                    quickTemplateTargetSelectionActivityStarted;
            this.globalPrefillDraft = globalPrefillDraft;
            this.quickTemplateDraft = quickTemplateDraft;
            this.homeUpdateUiState = homeUpdateUiState != null
                    ? homeUpdateUiState
                    : HomeUpdateUiState.UP_TO_DATE;
        }
    }

    private enum TemplateDetailKind {
        NONE,
        GLOBAL_PREFILL,
        QUICK_TEMPLATE,
        QUICK_TEMPLATE_TARGETS;

        static TemplateDetailKind fromName(String name) {
            if (name == null) {
                return NONE;
            }
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return NONE;
            }
        }
    }

    private static final class TemplateDetailSelection {

        final TemplateDetailKind kind;
        final String templateId;

        private TemplateDetailSelection(
                TemplateDetailKind kind,
                String templateId
        ) {
            this.kind = kind != null ? kind : TemplateDetailKind.NONE;
            this.templateId = templateId;
        }

        static TemplateDetailSelection none() {
            return new TemplateDetailSelection(TemplateDetailKind.NONE, null);
        }

        static TemplateDetailSelection globalPrefill() {
            return new TemplateDetailSelection(
                    TemplateDetailKind.GLOBAL_PREFILL,
                    null
            );
        }

        static TemplateDetailSelection quickTemplate(String templateId) {
            if (templateId != null && templateId.isBlank()) {
                return none();
            }
            return new TemplateDetailSelection(
                    TemplateDetailKind.QUICK_TEMPLATE,
                    templateId
            );
        }

        static TemplateDetailSelection quickTemplateTargets(String templateId) {
            if (templateId == null || templateId.isBlank()) {
                return none();
            }
            return new TemplateDetailSelection(
                    TemplateDetailKind.QUICK_TEMPLATE_TARGETS,
                    templateId
            );
        }
    }
}
