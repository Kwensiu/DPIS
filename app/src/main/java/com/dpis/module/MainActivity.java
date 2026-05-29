package com.dpis.module;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
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
import android.view.LayoutInflater;
import android.view.KeyEvent;
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

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends LocalizedActivity implements DpisApplication.ServiceStateListener {
    private static final long MODE_TOGGLE_ANIM_DURATION_MS = 200L;
    private static final long SEARCH_FAB_ANIM_DURATION_MS = 180L;
    private static final int SEARCH_FAB_SCROLL_TRIGGER_DY = 8;
    private static final String STATE_CURRENT_QUERY = "state.current_query";
    private static final String STATE_CURRENT_PAGE = "state.current_page";
    private static final String STATE_WORKSPACE_MODE = "state.workspace_mode";
    private static final String STATE_FILTER_SHOW_SYSTEM = "state.filter.show_system";
    private static final String STATE_FILTER_INJECTED_ONLY = "state.filter.injected_only";
    private static final String STATE_FILTER_WIDTH_ONLY = "state.filter.width_only";
    private static final String STATE_FILTER_FONT_ONLY = "state.filter.font_only";
    private static final String STATE_PAGE_SCROLL_STATES = "state.page_scroll_states";
    private static final String STATE_REFRESHING_PAGES = "state.refreshing_pages";
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;
    private static final long INSTALLED_APP_CATALOG_TTL_MS = 60_000L;
    private static final int FIRST_SCREEN_ICON_WARMUP_LIMIT = 48;
    private static final long ICON_REFRESH_DEBOUNCE_MS = 120L;
    private static final String XIAOMI_GET_INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS";
    private static final int REQUEST_XIAOMI_GET_INSTALLED_APPS = 10022;

    private final UpdateCoordinator updateCoordinator = new UpdateCoordinator();
    private final StartupUpdateDownloadExecutor startupUpdateDownloadExecutor = new StartupUpdateDownloadExecutor(
            UPDATE_CONNECT_TIMEOUT_MS,
            UPDATE_READ_TIMEOUT_MS,
            DOWNLOAD_BUFFER_SIZE,
            DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS);
    private UpdateStateStore updateStateStore;
    private UpdateDownloadCoordinator updateDownloadCoordinator;
    private final ProcessActionHandler processActionHandler = new ProcessActionHandler(this);
    private final AppConfigSaveHandler appConfigSaveHandler = new AppConfigSaveHandler();
    private final StartupUpdatePackageHandler startupUpdatePackageHandler = new StartupUpdatePackageHandler(this);
    private final ExecutorService startupUpdateExecutor = Executors.newSingleThreadExecutor();
    private final SystemScopeCoordinator systemScopeCoordinator = new SystemScopeCoordinator(createSystemScopeHost());
    private final InstalledAppCatalogCoordinator installedAppCatalogCoordinator = new InstalledAppCatalogCoordinator(
            createInstalledAppCatalogHost(),
            INSTALLED_APP_CATALOG_TTL_MS,
            FIRST_SCREEN_ICON_WARMUP_LIMIT,
            ICON_REFRESH_DEBOUNCE_MS);
    private final StartupUpdateCheckCoordinator startupUpdateCheckCoordinator = new StartupUpdateCheckCoordinator(
            createStartupUpdateCheckHost(),
            updateCoordinator,
            UPDATE_CONNECT_TIMEOUT_MS,
            UPDATE_READ_TIMEOUT_MS);
    private StartupUpdateDialogCoordinator startupUpdateDialogCoordinator;
    private ReleaseNotesController releaseNotesController;
    private AppListFilterStateStore appListFilterStateStore;

    private MainViewModel mainViewModel;
    private AppListPagerAdapter pagerAdapter;
    private ViewPager2 appPager;
    private TabLayout filterTabs;
    private View appWorkspaceDivider;
    private View templateWorkspaceContainer;
    private TemplateWorkspaceBinder templateWorkspaceBinder;
    private NavigationBarView workspaceSwitch;
    private SparseArray<Parcelable> restoredPageScrollStates;
    private EditText searchInput;
    private FloatingActionButton searchFocusFab;
    private FloatingActionButton helpFab;
    private boolean searchFabHidden;
    private boolean updatingWorkspaceSelection;
    private ImageButton searchFilterButton;
    private boolean cachedSystemHookEffectiveEnabled;
    private boolean skipNextImmediateServiceReload;
    private boolean installedAppsPermissionRequestInFlight;
    private boolean pendingInstalledAppsLoadAfterPermission;
    private boolean installedAppsPermissionRequestCompleted;
    private volatile boolean startupUpdateCheckInProgress;
    private volatile boolean startupUpdateDownloadInProgress;
    private volatile boolean startupUpdateDownloadCancelRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        searchFocusFab = findViewById(R.id.search_focus_fab);
        helpFab = findViewById(R.id.help_fab);
        applyInsets();
        refreshSystemHookEffectiveEnabled();

        updateStateStore = new UpdateStateStore(this);
        updateDownloadCoordinator = new UpdateDownloadCoordinator(
                createUpdateDownloadHost(),
                updateCoordinator,
                startupUpdateDownloadExecutor,
                startupUpdatePackageHandler,
                startupUpdateExecutor);
        releaseNotesController = new ReleaseNotesController(
                new ReleaseNotesCacheStore(this),
                startupUpdateExecutor,
                this::runOnUiThread,
                GitHubReleaseNotesFetcher::fetchByVersionName,
                System::currentTimeMillis,
                UPDATE_CONNECT_TIMEOUT_MS,
                UPDATE_READ_TIMEOUT_MS);
        appListFilterStateStore = new AppListFilterStateStore(this);

        RetainedState retainedState = (RetainedState) getLastNonConfigurationInstance();
        String initialQuery = "";
        AppListFilterState initialFilterState = appListFilterStateStore.load();
        MainWorkspaceMode initialWorkspaceMode = MainWorkspaceMode.APP;
        List<AppListItem> initialAppsSnapshot = Collections.emptyList();
        Set<AppListPage> initialRefreshingPages = EnumSet.noneOf(AppListPage.class);
        if (retainedState != null) {
            initialQuery = retainedState.query;
            initialFilterState = retainedState.filterState;
            initialWorkspaceMode = retainedState.workspaceMode;
            restoredPageScrollStates = retainedState.pageScrollStates;
            initialRefreshingPages = decodeRefreshingPages(retainedState.refreshingPagePositions);
            initialAppsSnapshot = new ArrayList<>(retainedState.appsSnapshot);
            skipNextImmediateServiceReload = !initialAppsSnapshot.isEmpty();
        }
        if (savedInstanceState != null) {
            initialQuery = savedInstanceState.getString(STATE_CURRENT_QUERY, "");
            initialFilterState = new AppListFilterState(
                    savedInstanceState.getBoolean(STATE_FILTER_SHOW_SYSTEM, false),
                    savedInstanceState.getBoolean(STATE_FILTER_INJECTED_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_WIDTH_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_FONT_ONLY, false));
            initialWorkspaceMode = MainWorkspaceMode.fromName(
                    savedInstanceState.getString(STATE_WORKSPACE_MODE));
            restoredPageScrollStates = savedInstanceState.getSparseParcelableArray(STATE_PAGE_SCROLL_STATES);
            initialRefreshingPages = decodeRefreshingPages(
                    savedInstanceState.getIntArray(STATE_REFRESHING_PAGES));
        }
        mainViewModel = new MainViewModel(MainUiState.initial(
                initialQuery,
                initialFilterState,
                initialAppsSnapshot,
                initialRefreshingPages,
                initialWorkspaceMode));

        searchFilterButton = findViewById(R.id.search_filter_button);
        appPager = findViewById(R.id.app_pager);
        filterTabs = findViewById(R.id.filter_tabs);
        appWorkspaceDivider = findViewById(R.id.app_workspace_divider);
        templateWorkspaceContainer = findViewById(R.id.template_workspace_container);
        templateWorkspaceBinder = new TemplateWorkspaceBinder(this, createTemplateWorkspaceActions(),
                createQuickTemplateActions());
        workspaceSwitch = findViewById(R.id.workspace_switch);
        pagerAdapter = new AppListPagerAdapter(
                this::showEditDialog,
                this::onPageRefreshRequested,
                this::onPageListScrolled,
                this::onIconLoadRequested,
                this::isSystemHookEnabledFromStore);
        pagerAdapter.restorePageScrollStates(restoredPageScrollStates);
        appPager.setAdapter(pagerAdapter);
        applyRefreshingStatesToPager();
        if (savedInstanceState != null) {
            appPager.setCurrentItem(savedInstanceState.getInt(STATE_CURRENT_PAGE, 0), false);
        } else if (retainedState != null) {
            appPager.setCurrentItem(retainedState.currentPage, false);
        }

        new TabLayoutMediator(filterTabs, appPager,
                (tab, position) -> tab.setText(getString(AppListPage.fromPosition(position).titleRes())))
                .attach();
        bindWorkspaceSwitch();
        searchFilterButton.setOnClickListener(v -> showFilterDialog());
        bindFabTouchFeedback(searchFocusFab);
        bindFabTouchFeedback(helpFab);
        helpFab.setOnClickListener(v -> showHelpTutorialDialog());
        searchFocusFab.setOnClickListener(v -> focusSearchInputAndShowKeyboard());

        searchInput = findViewById(R.id.search_input);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchInput.clearFocus();
                return false;
            }
            return false;
        });
        ImageButton searchClearButton = findViewById(R.id.search_clear_button);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString() : "";
                dispatchMainUiAction(MainUiAction.queryChanged(query));
                searchClearButton.setVisibility(
                        query.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchClearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
        });
        String restoredQuery = requireUiState().query;
        if (!restoredQuery.isEmpty()) {
            searchInput.setText(restoredQuery);
            searchInput.setSelection(restoredQuery.length());
        }
        searchInput.setOnFocusChangeListener((view, hasFocus) -> updateSearchHint());

        View systemSettingsButton = findViewById(R.id.system_settings_button);
        systemSettingsButton
                .setOnClickListener(v -> startActivity(new Intent(this, SystemServerSettingsActivity.class)));

        renderMainUiState(requireUiState());
        if (maybeShowModuleRuntimeReloadAdvice()) {
            return;
        }
        if (!maybeShowStartupDisclaimerDialog()) {
            maybeCheckForUpdatesOnStartup();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && searchInput != null && searchInput.hasFocus()) {
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            if (!isTouchInsideView(rawX, rawY, searchInput)) {
                if (isTouchInsideView(rawX, rawY, searchFocusFab)) {
                    return super.dispatchTouchEvent(event);
                }
                if (isTouchInsideView(rawX, rawY, helpFab)) {
                    return super.dispatchTouchEvent(event);
                }
                clearSearchFocus();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSystemHookEffectiveEnabled();
        if (pagerAdapter != null) {
            pagerAdapter.refreshVisibleStatuses();
        }
        if (requireUiState().workspaceMode == MainWorkspaceMode.TEMPLATE) {
            bindTemplateWorkspace();
        }
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (updateDownloadCoordinator != null) {
            updateDownloadCoordinator.shutdown();
        }
        installedAppCatalogCoordinator.shutdown();
        super.onDestroy();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(() -> {
            refreshSystemHookEffectiveEnabled();
            if (pagerAdapter != null) {
                pagerAdapter.refreshVisibleStatuses();
            }
            if (skipNextImmediateServiceReload) {
                skipNextImmediateServiceReload = false;
                return;
            }
            requestAppsLoad();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        MainUiState state = requireUiState();
        outState.putString(STATE_CURRENT_QUERY, state.query);
        outState.putString(STATE_WORKSPACE_MODE, state.workspaceMode.name());
        outState.putBoolean(STATE_FILTER_SHOW_SYSTEM, state.filterState.showSystemApps);
        outState.putBoolean(STATE_FILTER_INJECTED_ONLY, state.filterState.injectedOnly);
        outState.putBoolean(STATE_FILTER_WIDTH_ONLY, state.filterState.widthConfiguredOnly);
        outState.putBoolean(STATE_FILTER_FONT_ONLY, state.filterState.fontConfiguredOnly);
        if (appPager != null) {
            outState.putInt(STATE_CURRENT_PAGE, appPager.getCurrentItem());
        }
        if (pagerAdapter != null) {
            outState.putSparseParcelableArray(
                    STATE_PAGE_SCROLL_STATES,
                    pagerAdapter.capturePageScrollStates());
        }
        outState.putIntArray(STATE_REFRESHING_PAGES, captureRefreshingPagePositions());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        int currentPage = appPager != null ? appPager.getCurrentItem() : 0;
        SparseArray<Parcelable> pageScrollStates = pagerAdapter != null
                ? pagerAdapter.capturePageScrollStates()
                : restoredPageScrollStates;
        return new RetainedState(
                snapshot,
                state.query,
                state.filterState,
                state.workspaceMode,
                currentPage,
                pageScrollStates,
                captureRefreshingPagePositions());
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

    private static Set<AppListPage> decodeRefreshingPages(int[] pagePositions) {
        EnumSet<AppListPage> refreshingPages = EnumSet.noneOf(AppListPage.class);
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

    private void applyRefreshingStatesToPager() {
        if (pagerAdapter == null) {
            return;
        }
        MainUiState state = requireUiState();
        for (AppListPage page : AppListPage.values()) {
            pagerAdapter.setRefreshing(page, state.isRefreshing(page));
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
        dispatchMainUiAction(MainUiAction.requestAppsLoad(forceInstalledAppCatalogReload));
    }

    private boolean ensureInstalledAppsPermissionBeforeLoad() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || installedAppsPermissionRequestCompleted
                || !isXiaomiInstalledAppsPermissionDeclared()) {
            return true;
        }
        try {
            if (checkPermission(XIAOMI_GET_INSTALLED_APPS_PERMISSION,
                    Process.myPid(),
                    Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            if (!installedAppsPermissionRequestInFlight) {
                installedAppsPermissionRequestInFlight = true;
                requestPermissions(
                        new String[] { XIAOMI_GET_INSTALLED_APPS_PERMISSION },
                        REQUEST_XIAOMI_GET_INSTALLED_APPS);
            }
            return false;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private boolean isXiaomiInstalledAppsPermissionDeclared() {
        try {
            getPackageManager().getPermissionInfo(XIAOMI_GET_INSTALLED_APPS_PERMISSION, 0);
            return true;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return false;
        }
    }

    private void startAppsLoad(MainUiEffect.StartAppsLoad start) {
        int requestId = start.requestId;
        boolean forceInstalledAppCatalogReload = start.forceInstalledAppCatalogReload;
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
        final int baseTopPadding = topContainer.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(topContainer, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return windowInsets;
        });
        ViewGroup.MarginLayoutParams searchLayoutParams = (ViewGroup.MarginLayoutParams) searchFocusFab
                .getLayoutParams();
        ViewGroup.MarginLayoutParams helpLayoutParams = (ViewGroup.MarginLayoutParams) helpFab.getLayoutParams();
        final int baseSearchBottomMargin = searchLayoutParams.bottomMargin;
        final int baseSearchEndMargin = searchLayoutParams.getMarginEnd();
        final int baseHelpEndMargin = helpLayoutParams.getMarginEnd();
        final int floatingActionsGapPx = getResources().getDimensionPixelSize(R.dimen.floating_actions_gap);
        ViewCompat.setOnApplyWindowInsetsListener(searchFocusFab, (view, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int sideInset = Math.max(navigationBars.left, navigationBars.right);
            ViewGroup.MarginLayoutParams searchParams = (ViewGroup.MarginLayoutParams) searchFocusFab.getLayoutParams();
            searchParams.bottomMargin = baseSearchBottomMargin + navigationBars.bottom;
            searchParams.setMarginEnd(baseSearchEndMargin + sideInset);
            searchFocusFab.setLayoutParams(searchParams);
            ViewGroup.MarginLayoutParams helpParams = (ViewGroup.MarginLayoutParams) helpFab.getLayoutParams();
            int searchFabSizePx = resolveSearchFabSizePx();
            helpParams.bottomMargin = searchParams.bottomMargin + searchFabSizePx + floatingActionsGapPx;
            helpParams.setMarginEnd(baseHelpEndMargin + sideInset);
            helpFab.setLayoutParams(helpParams);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(topContainer);
        ViewCompat.requestApplyInsets(searchFocusFab);
    }

    private int resolveSearchFabSizePx() {
        if (searchFocusFab == null) {
            return getResources().getDimensionPixelSize(
                    com.google.android.material.R.dimen.design_fab_size_normal);
        }
        int measuredHeight = searchFocusFab.getMeasuredHeight();
        if (measuredHeight > 0) {
            return measuredHeight;
        }
        int height = searchFocusFab.getHeight();
        if (height > 0) {
            return height;
        }
        ViewGroup.LayoutParams layoutParams = searchFocusFab.getLayoutParams();
        if (layoutParams != null && layoutParams.height > 0) {
            return layoutParams.height;
        }
        return getResources().getDimensionPixelSize(
                com.google.android.material.R.dimen.design_fab_size_normal);
    }

    private static boolean isTouchInsideView(int rawX, int rawY, View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        Rect outRect = new Rect();
        view.getGlobalVisibleRect(outRect);
        return outRect.contains(rawX, rawY);
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
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            searchInput.post(() -> imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT));
        }
    }

    private void bindFabTouchFeedback(FloatingActionButton fab) {
        TouchFeedbackBinder.bindPressScaleAndHaptic(fab);
    }

    private void hideSearchFocusFab() {
        if (searchFocusFab == null || helpFab == null || searchFabHidden) {
            return;
        }
        searchFabHidden = true;
        searchFocusFab.animate().cancel();
        helpFab.animate().cancel();
        ViewGroup.MarginLayoutParams searchLayoutParams = (ViewGroup.MarginLayoutParams) searchFocusFab
                .getLayoutParams();
        ViewGroup.MarginLayoutParams helpLayoutParams = (ViewGroup.MarginLayoutParams) helpFab.getLayoutParams();
        float searchTargetTranslationY = searchFocusFab.getHeight() + searchLayoutParams.bottomMargin;
        float helpTargetTranslationY = helpFab.getHeight() + helpLayoutParams.bottomMargin;
        searchFocusFab.animate()
                .translationY(searchTargetTranslationY)
                .alpha(0f)
                .setDuration(SEARCH_FAB_ANIM_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withStartAction(() -> searchFocusFab.setClickable(false))
                .start();
        helpFab.animate()
                .translationY(helpTargetTranslationY)
                .alpha(0f)
                .setDuration(SEARCH_FAB_ANIM_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withStartAction(() -> helpFab.setClickable(false))
                .start();
    }

    private void showSearchFocusFab() {
        if (searchFocusFab == null || helpFab == null || !searchFabHidden) {
            return;
        }
        searchFabHidden = false;
        searchFocusFab.animate().cancel();
        helpFab.animate().cancel();
        searchFocusFab.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(SEARCH_FAB_ANIM_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withStartAction(() -> searchFocusFab.setClickable(true))
                .start();
        helpFab.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(SEARCH_FAB_ANIM_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withStartAction(() -> helpFab.setClickable(true))
                .start();
    }

    private void clearSearchFocus() {
        if (searchInput == null) {
            return;
        }
        searchInput.clearFocus();
        Editable current = searchInput.getText();
        if (current == null || current.length() == 0) {
            searchInput.setHint(getString(R.string.search_hint));
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void clearDialogInputFocus(View fallbackFocusView,
            TextInputEditText viewportInputView,
            TextInputEditText fontInputView) {
        // Clear focus from inputs so cursor disappears
        if (viewportInputView != null) {
            viewportInputView.clearFocus();
        }
        if (fontInputView != null) {
            fontInputView.clearFocus();
        }
        // Hide keyboard using fallbackFocusView's window token (the dialog root)
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && fallbackFocusView != null) {
            imm.hideSoftInputFromWindow(fallbackFocusView.getWindowToken(), 0);
        }
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

    private void showHelpTutorialDialog() {
        HelpTutorialDialog.show(this);
    }

    private boolean setDpisEnabled(String packageName, boolean enabled) {
        DpiConfigStore store = getUiConfigStore();
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
        showToast(enabled ? R.string.dialog_dpis_enabled_status : R.string.dialog_dpis_disabled_status);
        requestAppsLoad();
        return true;
    }

    private List<AppListItem> loadInstalledApps(boolean forceInstalledAppCatalogReload) {
        Set<String> scopePackages = new HashSet<>();
        boolean scopeKnown = false;
        XposedService service = DpisApplication.getXposedService();
        if (service != null) {
            try {
                List<String> scope = service.getScope();
                if (scope != null) {
                    scopePackages.addAll(scope);
                    scopeKnown = true;
                }
            } catch (RuntimeException ignored) {
                scopePackages.clear();
            }
        }
        return installedAppCatalogCoordinator.loadInstalledApps(
                forceInstalledAppCatalogReload,
                getUiConfigStore(),
                scopePackages,
                scopeKnown);
    }

    private void applyFilter() {
        if (pagerAdapter == null) {
            return;
        }
        MainUiState state = requireUiState();
        for (AppListPage page : AppListPage.values()) {
            pagerAdapter.submitPage(page, state.visibleItems(page));
        }
    }

    private MainUiState requireUiState() {
        MainViewModel viewModel = mainViewModel;
        if (viewModel == null) {
            return MainUiState.initial(
                    "",
                    AppListFilterState.defaultState(),
                    Collections.emptyList(),
                    Collections.emptySet());
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
        applyWorkspaceMode(state.workspaceMode);
        applyFilter();
        applyRefreshingStatesToPager();
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
            dispatchMainUiAction(MainUiAction.workspaceModeChanged(
                    workspaceModeForButtonId(item.getItemId())));
            return true;
        });
    }

    private void applyWorkspaceMode(MainWorkspaceMode workspaceMode) {
        MainWorkspaceMode mode = workspaceMode != null ? workspaceMode : MainWorkspaceMode.APP;
        boolean appWorkspace = mode == MainWorkspaceMode.APP;
        setVisible(filterTabs, appWorkspace);
        setVisible(appWorkspaceDivider, appWorkspace);
        setVisible(appPager, appWorkspace);
        setVisible(templateWorkspaceContainer, !appWorkspace);
        setVisible(searchFocusFab, appWorkspace);
        setVisible(helpFab, appWorkspace);
        if (searchFilterButton != null) {
            searchFilterButton.setEnabled(appWorkspace);
            searchFilterButton.setVisibility(appWorkspace ? View.VISIBLE : View.GONE);
        }
        if (workspaceSwitch != null && workspaceSwitch.getSelectedItemId() != workspaceButtonId(mode)) {
            selectWorkspaceItem(workspaceButtonId(mode));
        }
        updateSearchHint(mode);
        if (!appWorkspace) {
            bindTemplateWorkspace();
        }
    }

    private void bindTemplateWorkspace() {
        if (templateWorkspaceBinder != null) {
            templateWorkspaceBinder.bind(templateWorkspaceContainer, requireUiState().query);
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

    private static int workspaceButtonId(MainWorkspaceMode workspaceMode) {
        if (workspaceMode == MainWorkspaceMode.TEMPLATE) {
            return R.id.workspace_template_button;
        }
        return R.id.workspace_app_button;
    }

    private static MainWorkspaceMode workspaceModeForButtonId(int checkedId) {
        if (checkedId == R.id.workspace_template_button) {
            return MainWorkspaceMode.TEMPLATE;
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
                R.layout.dialog_list_filters, root, false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        MaterialSwitch showSystemSwitch = dialogView.findViewById(R.id.filter_show_system_switch);
        MaterialSwitch injectedOnlySwitch = dialogView.findViewById(R.id.filter_injected_only_switch);
        MaterialSwitch widthOnlySwitch = dialogView.findViewById(R.id.filter_width_only_switch);
        MaterialSwitch fontOnlySwitch = dialogView.findViewById(R.id.filter_font_only_switch);
        MainUiState state = requireUiState();

        showSystemSwitch.setChecked(state.filterState.showSystemApps);
        injectedOnlySwitch.setChecked(state.filterState.injectedOnly);
        widthOnlySwitch.setChecked(state.filterState.widthConfiguredOnly);
        fontOnlySwitch.setChecked(state.filterState.fontConfiguredOnly);

        android.widget.CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            AppListFilterState filterState = new AppListFilterState(
                    showSystemSwitch.isChecked(),
                    injectedOnlySwitch.isChecked(),
                    widthOnlySwitch.isChecked(),
                    fontOnlySwitch.isChecked());
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
        return startupUpdateDialogCoordinator().maybeShowStartupDisclaimerDialog(
                getUiConfigStore(),
                this::maybeCheckForUpdatesOnStartup);
    }

    private boolean maybeShowModuleRuntimeReloadAdvice() {
        if (!ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(this)) {
            return false;
        }
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_module_runtime_reload_advice, null, false);
        MaterialButton ackButton = dialogView.findViewById(R.id.module_runtime_reload_ack_button);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        ackButton.setOnClickListener(v -> {
            ModuleRuntimeReloadAdvisor.markReloadAdviceAcknowledged(this);
            dialog.dismiss();
            continueStartupDialogsAfterRuntimeReloadAdvice();
        });
        dialog.show();
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

    private void launchStartupUpdateDialog(StartupUpdateManifest manifest) {
        startupUpdateDialogCoordinator().showUpdateAvailableDialog(
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                manifest.versionName,
                manifest.versionCode,
                manifest.apkUrl,
                manifest.releasePage,
                manifest.releaseNotes);
    }

    private void startStartupUpdateDownload(String targetVersionName,
            String downloadUrl,
            androidx.appcompat.app.AlertDialog dialog,
            MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        updateDownloadCoordinator.startDownload(
                targetVersionName,
                downloadUrl,
                dialog,
                primaryButton,
                cancelButton,
                progressView,
                progressTextView);
    }

    private void cancelActiveUpdateDownload() {
        updateDownloadCoordinator.cancelActiveDownload();
    }

    private UpdateCoordinator.State buildUpdateCoordinatorState() {
        return updateStateStore.buildCoordinatorState(
                startupUpdateCheckInProgress,
                startupUpdateDownloadInProgress,
                startupUpdateDownloadCancelRequested);
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

    private void markPromptedVersion(int versionCode) {
        UpdateCoordinator.State nextState = updateCoordinator.markPromptedVersion(
                buildUpdateCoordinatorState(),
                versionCode);
        updateStateStore.applyPromptedVersion(nextState);
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            showToast(R.string.about_link_open_failed);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
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
                return appPager != null ? appPager : findViewById(android.R.id.content);
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
                return MainActivity.this.getString(R.string.about_update_manifest_url);
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
            public void launchStartupUpdateDialog(StartupUpdateManifest manifest) {
                MainActivity.this.launchStartupUpdateDialog(manifest);
            }
        };
    }

    private StartupUpdateDialogCoordinator startupUpdateDialogCoordinator() {
        if (startupUpdateDialogCoordinator == null) {
            startupUpdateDialogCoordinator = new StartupUpdateDialogCoordinator(
                    this,
                    createStartupUpdateDialogHost(),
                    releaseNotesController);
        }
        return startupUpdateDialogCoordinator;
    }

    private StartupUpdateDialogCoordinator.Host createStartupUpdateDialogHost() {
        return new StartupUpdateDialogCoordinator.Host() {
            @Override
            public void showDialogIdleState(MaterialButton primaryButton,
                    MaterialButton cancelButton,
                    LinearProgressIndicator progressView,
                    MaterialTextView progressTextView) {
                UpdateDownloadCoordinator.showDialogIdleState(
                        primaryButton,
                        cancelButton,
                        progressView,
                        progressTextView);
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
            public void startStartupUpdateDownload(String targetVersionName,
                    String downloadUrl,
                    androidx.appcompat.app.AlertDialog dialog,
                    MaterialButton primaryButton,
                    MaterialButton cancelButton,
                    LinearProgressIndicator progressView,
                    MaterialTextView progressTextView) {
                MainActivity.this.startStartupUpdateDownload(
                        targetVersionName,
                        downloadUrl,
                        dialog,
                        primaryButton,
                        cancelButton,
                        progressView,
                        progressTextView);
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
        if (pagerAdapter != null) {
            pagerAdapter.refreshVisibleStatuses();
        }
        DpiConfigStore store = getUiConfigStore();
        TemplateConfigValue globalPrefill = new GlobalPrefillStore(
                getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE)).read();
        AppListItem sheetItem = AppConfigPrefillPreview.applyIfEligible(
                item, store, globalPrefill);
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_app_config, root, false);
        new AppConfigDialogBinder(this, createAppConfigDialogHost()).bind(
                dialogView, sheetItem, systemHooksEnabled);
        new AppConfigDialogCoordinator(this).show(dialogView);
    }

    private TemplateWorkspaceBinder.GlobalPrefillActions createTemplateWorkspaceActions() {
        return new TemplateWorkspaceBinder.GlobalPrefillActions() {
            @Override
            public void edit() {
                GlobalPrefillSheetDialog.show(MainActivity.this, MainActivity.this::bindTemplateWorkspace);
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
                QuickTemplateEditSheetDialog.show(
                        MainActivity.this, templateId, MainActivity.this::bindTemplateWorkspace);
            }

            @Override
            public void select(String templateId) {
                Intent intent = new Intent(MainActivity.this, QuickTemplateTargetSelectionActivity.class);
                intent.putExtra(QuickTemplateTargetSelectionActivity.EXTRA_TEMPLATE_ID, templateId);
                startActivity(intent);
            }

            @Override
            public void create() {
                QuickTemplateEditSheetDialog.show(
                        MainActivity.this, null, MainActivity.this::bindTemplateWorkspace);
            }

            @Override
            public void sort(List<QuickTemplateStore.QuickTemplate> templates) {
                QuickTemplateSortDialog.show(MainActivity.this, templates, new QuickTemplateSortDialog.Host() {
                    @Override
                    public boolean saveOrder(List<String> orderedIds) {
                        return new QuickTemplateStore(
                                getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE))
                                .reorder(orderedIds);
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
                });
            }
        };
    }

    private void applyQuickTemplate(String templateId) {
        QuickTemplateStore store = new QuickTemplateStore(
                getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE));
        QuickTemplateStore.QuickTemplate template = store.read(templateId);
        if (template == null) {
            showToast(R.string.quick_template_target_missing);
            bindTemplateWorkspace();
            return;
        }
        QuickTemplateApplyCoordinator coordinator = new QuickTemplateApplyCoordinator(
                new DpiConfigStore(getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE)));
        QuickTemplateApplyCoordinator.TargetPackageFilter installedPackageFilter =
                this::isInstalledTemplateTargetPackage;
        QuickTemplateApplyCoordinator.Plan plan = coordinator.plan(template, installedPackageFilter);
        if (plan.targetCount <= 0) {
            showToast(R.string.quick_template_apply_empty_selection);
            return;
        }
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.quick_template_apply_confirm_title, template.name))
                .setMessage(getString(
                        R.string.quick_template_apply_confirm_message,
                        plan.targetCount,
                        plan.overwriteCount))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.template_workspace_action_apply,
                        (unusedDialog, which) -> finishQuickTemplateApply(
                                coordinator, template, installedPackageFilter))
                .create();
        dialog.setOnShowListener(d -> {
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE));
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE));
        });
        dialog.show();
    }

    private void finishQuickTemplateApply(QuickTemplateApplyCoordinator coordinator,
            QuickTemplateStore.QuickTemplate template) {
        finishQuickTemplateApply(coordinator, template, null);
    }

    private void finishQuickTemplateApply(QuickTemplateApplyCoordinator coordinator,
            QuickTemplateStore.QuickTemplate template,
            QuickTemplateApplyCoordinator.TargetPackageFilter targetPackageFilter) {
        QuickTemplateApplyCoordinator.Result result = coordinator.apply(template, targetPackageFilter);
        if (result.emptySelection) {
            showToast(R.string.quick_template_apply_empty_selection);
            return;
        }
        if (result.failureCount() > 0) {
            showToast(R.string.quick_template_apply_result_partial,
                    result.successCount(),
                    result.failureCount());
        } else {
            showToast(R.string.quick_template_apply_result_success, result.successCount());
        }
        new BatchScopeRequestCoordinator(createBatchScopeRequestHost())
                .requestMissingScope(result.successfulPackages);
        bindTemplateWorkspace();
    }

    private boolean isInstalledTemplateTargetPackage(String packageName) {
        if (packageName == null || packageName.isBlank() || getPackageName().equals(packageName)) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageManager().getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0));
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
            public void clearDialogInputFocus(View fallbackFocusView,
                    TextInputEditText viewportInputView,
                    TextInputEditText fontInputView) {
                MainActivity.this.clearDialogInputFocus(
                        fallbackFocusView, viewportInputView, fontInputView);
            }

            @Override
            public void toggleScope(AppListItem item,
                    boolean currentlyInScope,
                    Runnable onTurnedInScope,
                    Runnable onTurnedOutScope) {
                systemScopeCoordinator.toggleScope(
                        item.packageName,
                        item.label,
                        currentlyInScope,
                        onTurnedInScope,
                        onTurnedOutScope);
            }

            @Override
            public boolean requestScope(AppListItem item,
                    Runnable onTurnedInScope,
                    Runnable onRequestFinished) {
                return systemScopeCoordinator.requestScope(
                        item.packageName,
                        item.label,
                        onTurnedInScope,
                        onRequestFinished,
                        false);
            }

            @Override
            public void executeProcessAction(AppListItem item, AppConfigDialogBinder.ProcessAction action) {
                executeDialogProcessAction(item, action);
            }

            @Override
            public void applyHyperOsNativeProxy(AppListItem item, Runnable onFinished) {
                executeHyperOsNativeProxyMount(item, true, onFinished);
            }

            @Override
            public void unmountHyperOsNativeProxy(AppListItem item, Runnable onFinished) {
                executeHyperOsNativeProxyMount(item, false, onFinished);
            }

            @Override
            public boolean setDpisEnabled(String packageName, boolean enabled) {
                return MainActivity.this.setDpisEnabled(packageName, enabled);
            }

            @Override
            public void showFontHookDomains(AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Runnable onStateChanged) {
                MainActivity.this.showFontHookDomains(item, state, onStateChanged);
            }

            @Override
            public String getFontHookDomainsButtonText(AppListItem item,
                    boolean previewFromGlobalPrefill,
                    String previewFontHookDomainsRaw) {
                return MainActivity.this.getFontHookDomainsButtonText(
                        item, previewFromGlobalPrefill, previewFontHookDomainsRaw);
            }

            @Override
            public void openTypefaceLibrary() {
                MainActivity.this.startActivity(new Intent(MainActivity.this, FontLibraryActivity.class));
            }

            @Override
            public int[] saveAppConfig(AppListItem item,
                    TextInputEditText viewportInput,
                    TextInputEditText fontScaleInput,
                    String viewportMode,
                    String viewportApplyMode,
                    String fontMode,
                    String selectedTypefaceId,
                    String previewFontHookDomainsRaw,
                    String viewportScaleInput,
                    String viewportAbsoluteInput) {
                refreshSystemHookEffectiveEnabled();
                return appConfigSaveHandler.save(
                        item,
                        viewportInput,
                        fontScaleInput,
                        viewportMode,
                        viewportApplyMode,
                        fontMode,
                        selectedTypefaceId,
                        previewFontHookDomainsRaw,
                        viewportScaleInput,
                        viewportAbsoluteInput,
                        isSystemHookEnabledFromStore(),
                        getUiConfigStore(),
                        MainActivity.this::requestAppsLoad);
            }

            @Override
            public DpiConfigStore getConfigStore() {
                return MainActivity.this.getUiConfigStore();
            }

            @Override
            public void requestAppsLoad() {
                MainActivity.this.requestAppsLoad();
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }
        };
    }

    private void showFontHookDomains(AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onStateChanged) {
        if (item == null || item.packageName == null || item.packageName.isBlank()) {
            return;
        }
        DpiConfigStore store = getUiConfigStore();
        Set<String> automaticKnownDomains = resolveAutomaticFontHookDomains(store, item.packageName);
        boolean previewMode = state != null && state.previewFromGlobalPrefill;
        HookDomainOverride currentOverride = previewMode
                ? HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw)
                : new HookDomainOverrideStore(store).read(item.packageName);
        FontHookDomainDialog.show(this,
                new FontHookDomainDialog.Host() {
                    @Override
                    public boolean saveCustom(String packageName,
                            Set<String> selectedKnownDomains,
                            Set<String> automaticKnownDomains,
                            Set<String> unknownDomains) {
                        if (previewMode) {
                            if (state != null) {
                                state.previewFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection(
                                        selectedKnownDomains,
                                        automaticKnownDomains,
                                        unknownDomains);
                            }
                            if (onStateChanged != null) {
                                onStateChanged.run();
                            }
                            return true;
                        }
                        HookDomainOverrideStore overrideStore = new HookDomainOverrideStore(store);
                        boolean saved = overrideStore.saveCustomIfDifferentFromAutomatic(
                                packageName,
                                selectedKnownDomains,
                                automaticKnownDomains,
                                unknownDomains);
                        if (saved) {
                            HookDomainOverride override = overrideStore.read(packageName);
                            if (override.customPathEnabled) {
                                FontHookDomainPropertySyncer.publishTargetAsync(
                                        packageName,
                                        override.enabledKnownDomains);
                            } else {
                                FontHookDomainPropertySyncer.clearTargetAsync(packageName);
                            }
                            publishFontRuntimeTarget(packageName, store);
                            requestAppsLoad();
                        }
                        return saved;
                    }

                    @Override
                    public boolean restoreRecommended(String packageName) {
                        if (previewMode) {
                            if (state != null) {
                                state.previewFontHookDomainsRaw = null;
                            }
                            if (onStateChanged != null) {
                                onStateChanged.run();
                            }
                            return true;
                        }
                        boolean restored = new HookDomainOverrideStore(store).restoreRecommended(packageName);
                        if (restored) {
                            FontHookDomainPropertySyncer.clearTargetAsync(packageName);
                            publishFontRuntimeTarget(packageName, store);
                            requestAppsLoad();
                        }
                        return restored;
                    }

                    @Override
                    public boolean saveViewportApplyMode(String packageName, String mode) {
                        if (previewMode) {
                            if (state != null) {
                                state.viewportApplyMode = ViewportApplyMode.normalize(mode);
                            }
                            if (onStateChanged != null) {
                                onStateChanged.run();
                            }
                            return true;
                        }
                        boolean saved = store.setTargetViewportApplyMode(packageName, mode);
                        if (saved) {
                            ViewportPropertySyncer.syncConfiguredTargetsAsync(store);
                            requestAppsLoad();
                        }
                        return saved;
                    }
                },
                item.packageName,
                automaticKnownDomains,
                currentOverride,
                previewMode ? state.viewportApplyMode : store.getTargetViewportApplyMode(item.packageName),
                onStateChanged);
    }

    private static void publishFontRuntimeTarget(String packageName, DpiConfigStore store) {
        if (store == null || packageName == null || packageName.isBlank()) {
            return;
        }
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        if (!store.isTargetDpisEnabled(packageName)
                || fontScalePercent == null
                || fontScalePercent <= 0) {
            FontRuntimePropertySyncer.clearTargetAsync(packageName);
            return;
        }
        FontRuntimePropertySyncer.publishTargetAsync(
                packageName,
                fontScalePercent,
                store.getTargetFontApplyMode(packageName),
                FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName));
    }

    private String getFontHookDomainsButtonText(AppListItem item,
            boolean previewFromGlobalPrefill,
            String previewFontHookDomainsRaw) {
        HookDomainOverride override = previewFromGlobalPrefill
                ? HookDomainOverrideStore.fromRaw(previewFontHookDomainsRaw)
                : new HookDomainOverrideStore(getUiConfigStore()).read(item != null ? item.packageName : null);
        if (!override.customPathEnabled) {
            return getString(R.string.dialog_font_hook_domains_title);
        }
        int selectedCount = FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                override.enabledKnownDomains).size();
        int totalCount = FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size();
        return getString(R.string.dialog_font_hook_domains_title_with_count,
                selectedCount, totalCount);
    }

    private Set<String> resolveAutomaticFontHookDomains(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isBlank()) {
            return new LinkedHashSet<>();
        }
        HookRuntimePolicy policy = HookRuntimePolicy.fromStore(store);
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                policy,
                packageName,
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                HookDomainOverride.automatic(),
                AppProcessHookInstaller.resolveDebugFontOverrideForPackage(packageName));
        return FontHookDomainRegistry.orderedCustomizableSubset(
                parseKnownDomainCsv(plan.hookDomains));
    }

    private static Set<String> parseKnownDomainCsv(String rawDomains) {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        if (rawDomains == null || rawDomains.isBlank()) {
            return domains;
        }
        for (String part : rawDomains.split(",")) {
            String id = part == null ? "" : part.trim();
            if (FontHookDomainRegistry.isKnown(id)) {
                domains.add(id);
            }
        }
        return FontHookDomainRegistry.orderedKnownSubset(domains);
    }

    private void executeHyperOsNativeProxyMount(
            AppListItem item, boolean apply, Runnable onFinished) {
        executeHyperOsNativeProxyMount(item, apply, ignored -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    private void executeHyperOsNativeProxyMount(
            AppListItem item, boolean apply, HyperOsNativeProxyMountCallback onFinished) {
        new Thread(() -> {
            HyperOsNativeProxyBindMounter.MountPlan plan = HyperOsNativeProxyBindMounter.createPlan(this,
                    item.packageName);
            HyperOsNativeProxyBindMounter.MountResult result = apply
                    ? HyperOsNativeProxyBindMounter.apply(plan)
                    : HyperOsNativeProxyBindMounter.unmount(plan);
            DpisLog.i("HyperOS Native Proxy " + (apply ? "apply" : "rollback")
                    + " package=" + item.packageName
                    + " success=" + result.success
                    + " output=" + result.output);
            int messageResId = apply
                    ? R.string.dialog_hyperos_native_proxy_apply_failed
                    : R.string.dialog_hyperos_native_proxy_unmount_failed;
            runOnUiThread(() -> {
                if (!result.success) {
                    showToast(messageResId);
                }
                if (onFinished != null) {
                    onFinished.onFinished(result.success);
                }
            });
        }, "DPIS-HyperOsNativeProxyMount").start();
    }

    private void executeDialogProcessAction(AppListItem item, AppConfigDialogBinder.ProcessAction action) {
        if (action == AppConfigDialogBinder.ProcessAction.RESTART
                && shouldPrepareHyperOsNativeProxyForRestart(item)) {
            // Re-prepare before restart because APK updates can leave an old bind mount
            // pointing at a deleted module native library.
            executeHyperOsNativeProxyMount(item, true, success -> {
                if (success) {
                    executeDialogProcessActionAfterHyperOsProxyReady(item, action);
                }
            });
            return;
        }
        executeDialogProcessActionAfterHyperOsProxyReady(item, action);
    }

    private boolean shouldPrepareHyperOsNativeProxyForRestart(AppListItem item) {
        if (item == null || !item.hyperOsNativeProxyCandidate) {
            return false;
        }
        DpiConfigStore store = getUiConfigStore();
        return store.isTargetDpisEnabled(item.packageName)
                && hasActiveStoredConfig(store, item.packageName);
    }

    private static boolean hasActiveStoredConfig(DpiConfigStore store, String packageName) {
        ViewportTargetSpec viewportTargetSpec = store.getTargetViewportSpec(packageName);
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        return viewportTargetSpec.isEnabled()
                || fontScalePercent != null
                || store.hasTargetAppSpecificConfig(packageName);
    }

    private void executeDialogProcessActionAfterHyperOsProxyReady(
            AppListItem item, AppConfigDialogBinder.ProcessAction action) {
        ProcessActionHandler.Action mappedAction = switch (action) {
            case START -> ProcessActionHandler.Action.START;
            case RESTART -> ProcessActionHandler.Action.RESTART;
            case STOP -> ProcessActionHandler.Action.STOP;
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
        cachedSystemHookEffectiveEnabled = systemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                getUiConfigStore());
    }

    private DpiConfigStore getUiConfigStore() {
        DpiConfigStore sharedStore = DpisApplication.getConfigStore();
        if (sharedStore != null) {
            return sharedStore;
        }
        return new DpiConfigStore(getSharedPreferences(
                DpiConfigStore.GROUP, Context.MODE_PRIVATE));
    }

    private static final class RetainedState {
        final List<AppListItem> appsSnapshot;
        final String query;
        final AppListFilterState filterState;
        final MainWorkspaceMode workspaceMode;
        final int currentPage;
        final SparseArray<Parcelable> pageScrollStates;
        final int[] refreshingPagePositions;

        RetainedState(List<AppListItem> appsSnapshot,
                String query,
                AppListFilterState filterState,
                MainWorkspaceMode workspaceMode,
                int currentPage,
                SparseArray<Parcelable> pageScrollStates,
                int[] refreshingPagePositions) {
            this.appsSnapshot = appsSnapshot;
            this.query = query != null ? query : "";
            this.filterState = filterState != null ? filterState : AppListFilterState.defaultState();
            this.workspaceMode = workspaceMode != null ? workspaceMode : MainWorkspaceMode.APP;
            this.currentPage = currentPage;
            this.pageScrollStates = pageScrollStates != null ? pageScrollStates.clone() : null;
            this.refreshingPagePositions = refreshingPagePositions != null
                    ? refreshingPagePositions.clone()
                    : new int[0];
        }
    }
}
