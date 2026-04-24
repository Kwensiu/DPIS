package com.dpis.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements DpisApplication.ServiceStateListener {
    private static final long MODE_TOGGLE_ANIM_DURATION_MS = 200L;
    private static final long SEARCH_FAB_ANIM_DURATION_MS = 180L;
    private static final int SEARCH_FAB_SCROLL_TRIGGER_DY = 8;
    private static final String STATE_CURRENT_QUERY = "state.current_query";
    private static final String STATE_CURRENT_PAGE = "state.current_page";
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

    private MainViewModel mainViewModel;
    private AppListPagerAdapter pagerAdapter;
    private ViewPager2 appPager;
    private SparseArray<Parcelable> restoredPageScrollStates;
    private EditText searchInput;
    private FloatingActionButton searchFocusFab;
    private FloatingActionButton helpFab;
    private boolean searchFabHidden;
    private ImageButton searchFilterButton;
    private boolean cachedSystemHookEffectiveEnabled;
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

        RetainedState retainedState = (RetainedState) getLastNonConfigurationInstance();
        String initialQuery = "";
        AppListFilterState initialFilterState = AppListFilterState.defaultState();
        List<AppListItem> initialAppsSnapshot = Collections.emptyList();
        Set<AppListPage> initialRefreshingPages = EnumSet.noneOf(AppListPage.class);
        if (retainedState != null) {
            initialQuery = retainedState.query;
            initialFilterState = retainedState.filterState;
            restoredPageScrollStates = retainedState.pageScrollStates;
            initialRefreshingPages = decodeRefreshingPages(retainedState.refreshingPagePositions);
            initialAppsSnapshot = new ArrayList<>(retainedState.appsSnapshot);
        }
        if (savedInstanceState != null) {
            initialQuery = savedInstanceState.getString(STATE_CURRENT_QUERY, "");
            initialFilterState = new AppListFilterState(
                    savedInstanceState.getBoolean(STATE_FILTER_SHOW_SYSTEM, false),
                    savedInstanceState.getBoolean(STATE_FILTER_INJECTED_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_WIDTH_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_FONT_ONLY, false));
            restoredPageScrollStates = savedInstanceState.getSparseParcelableArray(STATE_PAGE_SCROLL_STATES);
            initialRefreshingPages = decodeRefreshingPages(
                    savedInstanceState.getIntArray(STATE_REFRESHING_PAGES));
        }
        mainViewModel = new MainViewModel(MainUiState.initial(
                initialQuery,
                initialFilterState,
                initialAppsSnapshot,
                initialRefreshingPages));

        searchFilterButton = findViewById(R.id.search_filter_button);
        appPager = findViewById(R.id.app_pager);
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

        TabLayout tabLayout = findViewById(R.id.filter_tabs);
        new TabLayoutMediator(tabLayout, appPager,
                (tab, position) -> tab.setText(getString(AppListPage.fromPosition(position).titleRes())))
                .attach();
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
        searchInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                searchInput.setHint("");
                return;
            }
            CharSequence current = searchInput.getText();
            if (current == null || current.length() == 0) {
                searchInput.setHint(getString(R.string.search_hint));
            }
        });

        View systemSettingsButton = findViewById(R.id.system_settings_button);
        systemSettingsButton
                .setOnClickListener(v -> startActivity(new Intent(this, SystemServerSettingsActivity.class)));

        renderMainUiState(requireUiState());
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
            requestAppsLoad();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        MainUiState state = requireUiState();
        outState.putString(STATE_CURRENT_QUERY, state.query);
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
        dispatchMainUiAction(MainUiAction.requestAppsLoad(forceInstalledAppCatalogReload));
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
        showToast(enabled ? R.string.dialog_dpis_enabled_status : R.string.dialog_dpis_disabled_status);
        requestAppsLoad();
        return true;
    }

    private List<AppListItem> loadInstalledApps(boolean forceInstalledAppCatalogReload) {
        Set<String> scopePackages = new HashSet<>();
        XposedService service = DpisApplication.getXposedService();
        if (service != null) {
            try {
                scopePackages.addAll(service.getScope());
            } catch (RuntimeException ignored) {
                scopePackages.clear();
            }
        }
        return installedAppCatalogCoordinator.loadInstalledApps(
                forceInstalledAppCatalogReload,
                getUiConfigStore(),
                scopePackages);
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
        applyFilter();
        applyRefreshingStatesToPager();
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
            dispatchMainUiAction(MainUiAction.filterChanged(new AppListFilterState(
                    showSystemSwitch.isChecked(),
                    injectedOnlySwitch.isChecked(),
                    widthOnlySwitch.isChecked(),
                    fontOnlySwitch.isChecked())));
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

    private void maybeCheckForUpdatesOnStartup() {
        startupUpdateCheckCoordinator.maybeCheckForUpdatesOnStartup();
    }

    private void launchStartupUpdateDialog(StartupUpdateManifest manifest) {
        startupUpdateDialogCoordinator().showUpdateAvailableDialog(
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                manifest.versionName,
                manifest.versionCode,
                manifest.apkUrl,
                manifest.releasePage);
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
                    createStartupUpdateDialogHost());
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
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_app_config, root, false);
        new AppConfigDialogBinder(this, createAppConfigDialogHost()).bind(
                dialogView, item, systemHooksEnabled);
        new AppConfigDialogCoordinator(this).show(dialogView);
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
            public void executeProcessAction(AppListItem item, AppConfigDialogBinder.ProcessAction action) {
                executeDialogProcessAction(item, action);
            }

            @Override
            public boolean setDpisEnabled(String packageName, boolean enabled) {
                return MainActivity.this.setDpisEnabled(packageName, enabled);
            }

            @Override
            public int[] saveAppConfig(AppListItem item,
                    TextInputEditText viewportInput,
                    TextInputEditText fontScaleInput,
                    String viewportMode,
                    String fontMode) {
                refreshSystemHookEffectiveEnabled();
                return appConfigSaveHandler.save(
                        item,
                        viewportInput,
                        fontScaleInput,
                        viewportMode,
                        fontMode,
                        isSystemHookEnabledFromStore(),
                        getUiConfigStore(),
                        MainActivity.this::requestAppsLoad);
            }

            @Override
            public void showToast(int messageResId) {
                MainActivity.this.showToast(messageResId);
            }
        };
    }

    private void executeDialogProcessAction(AppListItem item, AppConfigDialogBinder.ProcessAction action) {
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
        final int currentPage;
        final SparseArray<Parcelable> pageScrollStates;
        final int[] refreshingPagePositions;

        RetainedState(List<AppListItem> appsSnapshot,
                String query,
                AppListFilterState filterState,
                int currentPage,
                SparseArray<Parcelable> pageScrollStates,
                int[] refreshingPagePositions) {
            this.appsSnapshot = appsSnapshot;
            this.query = query != null ? query : "";
            this.filterState = filterState != null ? filterState : AppListFilterState.defaultState();
            this.currentPage = currentPage;
            this.pageScrollStates = pageScrollStates != null ? pageScrollStates.clone() : null;
            this.refreshingPagePositions = refreshingPagePositions != null
                    ? refreshingPagePositions.clone()
                    : new int[0];
        }
    }
}
