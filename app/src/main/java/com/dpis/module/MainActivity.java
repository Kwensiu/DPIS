package com.dpis.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements DpisApplication.ServiceStateListener {
    private static final String SYSTEM_SCOPE_MODERN = "system";
    private static final String STATE_CURRENT_QUERY = "state.current_query";
    private static final String STATE_CURRENT_PAGE = "state.current_page";
    private static final String STATE_FILTER_SHOW_SYSTEM = "state.filter.show_system";
    private static final String STATE_FILTER_INJECTED_ONLY = "state.filter.injected_only";
    private static final String STATE_FILTER_WIDTH_ONLY = "state.filter.width_only";
    private static final String STATE_FILTER_FONT_ONLY = "state.filter.font_only";
    private static final String STATE_PAGE_SCROLL_STATES = "state.page_scroll_states";
    private static final String STATE_REFRESHING_PAGES = "state.refreshing_pages";

    private final List<AppListItem> allApps = new ArrayList<>();
    private final Object listLock = new Object();
    private final AppLoadCoordinator loadCoordinator = new AppLoadCoordinator();
    private final AppIconMemoryCache appIconCache = new AppIconMemoryCache(256);
    private final Set<AppListPage> refreshingPages = EnumSet.noneOf(AppListPage.class);

    private AppListPagerAdapter pagerAdapter;
    private ViewPager2 appPager;
    private SparseArray<Parcelable> restoredPageScrollStates;
    private String currentQuery = "";
    private AppListFilterState filterState = AppListFilterState.defaultState();
    private EditText searchInput;
    private ImageButton searchFilterButton;
    private Boolean rootAccessCache;
    private boolean cachedSystemHookEffectiveEnabled;

    private enum ProcessAction {
        START,
        RESTART,
        STOP
    }

    private static final class ShellResult {
        final int code;
        final String output;

        ShellResult(int code, String output) {
            this.code = code;
            this.output = output;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        applyInsets();
        refreshSystemHookEffectiveEnabled();

        RetainedState retainedState = (RetainedState) getLastNonConfigurationInstance();
        if (retainedState != null) {
            currentQuery = retainedState.query;
            filterState = retainedState.filterState;
            restoredPageScrollStates = retainedState.pageScrollStates;
            restoreRefreshingPages(retainedState.refreshingPagePositions);
            synchronized (listLock) {
                allApps.clear();
                allApps.addAll(retainedState.appsSnapshot);
            }
        }
        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(STATE_CURRENT_QUERY, "");
            filterState = new AppListFilterState(
                    savedInstanceState.getBoolean(STATE_FILTER_SHOW_SYSTEM, false),
                    savedInstanceState.getBoolean(STATE_FILTER_INJECTED_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_WIDTH_ONLY, false),
                    savedInstanceState.getBoolean(STATE_FILTER_FONT_ONLY, false));
            restoredPageScrollStates =
                    savedInstanceState.getSparseParcelableArray(STATE_PAGE_SCROLL_STATES);
            restoreRefreshingPages(savedInstanceState.getIntArray(STATE_REFRESHING_PAGES));
        }

        searchFilterButton = findViewById(R.id.search_filter_button);
        appPager = findViewById(R.id.app_pager);
        pagerAdapter = new AppListPagerAdapter(
                this::showEditDialog,
                this::onPageRefreshRequested,
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

        searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString() : "";
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        if (!currentQuery.isEmpty()) {
            searchInput.setText(currentQuery);
            searchInput.setSelection(currentQuery.length());
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
        systemSettingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, SystemServerSettingsActivity.class)));

        if (retainedState != null && !retainedState.appsSnapshot.isEmpty()) {
            applyFilter();
        }
        maybeShowStartupDisclaimerDialog();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && searchInput != null && searchInput.hasFocus()) {
            Rect outRect = new Rect();
            searchInput.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
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
        outState.putString(STATE_CURRENT_QUERY, currentQuery);
        outState.putBoolean(STATE_FILTER_SHOW_SYSTEM, filterState.showSystemApps);
        outState.putBoolean(STATE_FILTER_INJECTED_ONLY, filterState.injectedOnly);
        outState.putBoolean(STATE_FILTER_WIDTH_ONLY, filterState.widthConfiguredOnly);
        outState.putBoolean(STATE_FILTER_FONT_ONLY, filterState.fontConfiguredOnly);
        if (appPager != null) {
            outState.putInt(STATE_CURRENT_PAGE, appPager.getCurrentItem());
        }
        if (pagerAdapter != null) {
            outState.putSparseParcelableArray(
                    STATE_PAGE_SCROLL_STATES,
                    pagerAdapter.capturePageScrollStates()
            );
        }
        outState.putIntArray(STATE_REFRESHING_PAGES, captureRefreshingPagePositions());
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        List<AppListItem> snapshot;
        synchronized (listLock) {
            snapshot = new ArrayList<>(allApps);
        }
        int currentPage = appPager != null ? appPager.getCurrentItem() : 0;
        SparseArray<Parcelable> pageScrollStates = pagerAdapter != null
                ? pagerAdapter.capturePageScrollStates()
                : restoredPageScrollStates;
        return new RetainedState(
                snapshot,
                currentQuery,
                filterState,
                currentPage,
                pageScrollStates,
                captureRefreshingPagePositions());
    }

    private void onPageRefreshRequested(AppListPage page) {
        refreshingPages.add(page);
        if (pagerAdapter != null) {
            pagerAdapter.setRefreshing(page, true);
        }
        requestAppsLoad();
    }

    private void restoreRefreshingPages(int[] pagePositions) {
        refreshingPages.clear();
        if (pagePositions == null) {
            return;
        }
        for (int pagePosition : pagePositions) {
            refreshingPages.add(AppListPage.fromPosition(pagePosition));
        }
    }

    private int[] captureRefreshingPagePositions() {
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
        for (AppListPage page : AppListPage.values()) {
            pagerAdapter.setRefreshing(page, refreshingPages.contains(page));
        }
    }

    private void requestAppsLoad() {
        int requestId = loadCoordinator.onLoadRequested();
        if (requestId != AppLoadCoordinator.NO_REQUEST) {
            startAppsLoad(requestId);
        }
    }

    private void startAppsLoad(int requestId) {
        new Thread(() -> {
            List<AppListItem> loaded = null;
            try {
                loaded = loadInstalledApps();
            } catch (Throwable throwable) {
                DpisLog.e("list load failed", throwable);
            }
            List<AppListItem> finalLoaded = loaded;
            runOnUiThread(() -> onAppsLoadFinished(requestId, finalLoaded));
        }, "dpis-load-apps-" + requestId).start();
    }

    private void onAppsLoadFinished(int requestId, List<AppListItem> loaded) {
        AppLoadCoordinator.LoadCompletion completion = loadCoordinator.onLoadFinished(requestId);
        if (completion.shouldApplyResult && loaded != null) {
            synchronized (listLock) {
                allApps.clear();
                allApps.addAll(loaded);
            }
            applyFilter();
        }
        if (completion.nextRequestId != AppLoadCoordinator.NO_REQUEST) {
            startAppsLoad(completion.nextRequestId);
            return;
        }
        if (pagerAdapter != null && !refreshingPages.isEmpty()) {
            for (AppListPage page : AppListPage.values()) {
                if (refreshingPages.remove(page)) {
                    pagerAdapter.setRefreshing(page, false);
                }
            }
        }
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
        ViewCompat.requestApplyInsets(topContainer);
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
        View focused = getCurrentFocus();
        if (viewportInputView != null) {
            viewportInputView.clearFocus();
        }
        if (fontInputView != null) {
            fontInputView.clearFocus();
        }
        if (fallbackFocusView != null) {
            fallbackFocusView.requestFocus();
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View tokenView = focused != null ? focused : fallbackFocusView;
            if (tokenView == null) {
                tokenView = viewportInputView != null ? viewportInputView : fontInputView;
            }
            if (tokenView != null) {
                imm.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
            }
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

    private boolean updateSaveButtonState(TextInputLayout viewportInputLayout,
                                          TextInputEditText viewportInputView,
                                          TextInputLayout fontInputLayout,
                                          TextInputEditText fontInputView,
                                          MaterialButton saveButton) {
        boolean viewportValid = isPositiveIntOrEmpty(viewportInputView);
        boolean fontValid = isFontPercentOrEmpty(fontInputView);
        int defaultStrokeColor = MaterialColors.getColor(
                viewportInputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                viewportInputLayout, androidx.appcompat.R.attr.colorError);
        viewportInputLayout.setError(null);
        fontInputLayout.setError(null);
        viewportInputLayout.setErrorEnabled(false);
        fontInputLayout.setErrorEnabled(false);
        viewportInputLayout.setBoxStrokeColor(viewportValid ? defaultStrokeColor : errorStrokeColor);
        fontInputLayout.setBoxStrokeColor(fontValid ? defaultStrokeColor : errorStrokeColor);
        boolean valid = viewportValid && fontValid;
        saveButton.setEnabled(valid);
        return valid;
    }

    private static boolean isPositiveIntOrEmpty(TextInputEditText inputView) {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return true;
        }
        try {
            return Integer.parseInt(raw) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isFontPercentOrEmpty(TextInputEditText inputView) {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return true;
        }
        try {
            int value = Integer.parseInt(raw);
            return value >= 50 && value <= 300;
        } catch (NumberFormatException ignored) {
            return false;
        }
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

    private String resolveProcessActionLabel(ProcessAction action) {
        return switch (action) {
            case START -> getString(R.string.dialog_process_action_start);
            case RESTART -> getString(R.string.dialog_process_action_restart);
            case STOP -> getString(R.string.dialog_process_action_stop);
        };
    }

    private void executeProcessAction(AppListItem item, ProcessAction action) {
        if (!hasRootAccess()) {
            showToast(R.string.dialog_process_requires_root);
            return;
        }
        if (item.systemApp) {
            String actionLabel = resolveProcessActionLabel(action);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_process_action_confirm_title)
                    .setMessage(getString(R.string.dialog_process_action_confirm_message,
                            actionLabel, item.packageName))
                    .setPositiveButton(R.string.dialog_process_action_confirm_positive,
                            (dialog, which) -> runProcessAction(item.packageName, action))
                    .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                    .show();
            return;
        }
        runProcessAction(item.packageName, action);
    }

    private void runProcessAction(String packageName, ProcessAction action) {
        String actionLabel = resolveProcessActionLabel(action);
        new Thread(() -> {
            ShellResult result;
            if (action == ProcessAction.START) {
                result = runSuCommand("monkey -p " + packageName
                        + " -c android.intent.category.LAUNCHER 1");
            } else if (action == ProcessAction.STOP) {
                result = runSuCommand("am force-stop " + packageName);
            } else {
                result = runSuCommand("am force-stop " + packageName
                        + " && monkey -p " + packageName
                        + " -c android.intent.category.LAUNCHER 1");
            }
            runOnUiThread(() -> {
                if (result.code == 0) {
                    showToast(R.string.dialog_process_action_success, actionLabel, packageName);
                    return;
                }
                String reason = result.output == null || result.output.isEmpty()
                        ? "unknown error"
                        : result.output;
                showToast(R.string.dialog_process_action_failed, actionLabel, reason);
            });
        }, "dpis-process-action").start();
    }

    private boolean hasRootAccess() {
        if (rootAccessCache != null) {
            return rootAccessCache;
        }
        ShellResult result = runSuCommand("id");
        boolean hasRoot = result.code == 0 && result.output.contains("uid=0");
        rootAccessCache = hasRoot;
        return hasRoot;
    }

    private ShellResult runSuCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                 BufferedReader errReader = new BufferedReader(
                         new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
                while ((line = errReader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            }
            int code = process.waitFor();
            return new ShellResult(code, output.toString());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ShellResult(-1, exception.getMessage() != null
                    ? exception.getMessage() : exception.getClass().getSimpleName());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private List<AppListItem> loadInstalledApps() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> installedApps;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            installedApps = packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0));
        } else {
            installedApps = packageManager.getInstalledApplications(0);
        }

        Set<String> scopePackages = new HashSet<>();
        XposedService service = DpisApplication.getXposedService();
        if (service != null) {
            scopePackages.addAll(service.getScope());
        }
        DpiConfigStore store = getUiConfigStore();

        List<AppListItem> result = new ArrayList<>();
        for (ApplicationInfo applicationInfo : installedApps) {
            if (applicationInfo.packageName.equals(getPackageName())) {
                continue;
            }
            boolean systemApp = isSystemApp(applicationInfo);
            String label = packageManager.getApplicationLabel(applicationInfo).toString();
            Drawable icon = loadAppIcon(packageManager, applicationInfo);
            Integer viewportWidth = store != null
                    ? store.getTargetViewportWidthDp(applicationInfo.packageName)
                    : null;
            String viewportMode = store != null
                    ? store.getTargetViewportApplyMode(applicationInfo.packageName)
                    : ViewportApplyMode.OFF;
            Integer fontScalePercent = store != null
                    ? store.getTargetFontScalePercent(applicationInfo.packageName)
                    : null;
            String fontMode = store != null
                    ? store.getTargetFontApplyMode(applicationInfo.packageName)
                    : FontApplyMode.OFF;
            boolean dpisEnabled = store == null
                    || store.isTargetDpisEnabled(applicationInfo.packageName);
            result.add(new AppListItem(label, applicationInfo.packageName,
                    scopePackages.contains(applicationInfo.packageName), viewportWidth, viewportMode,
                    fontScalePercent, fontMode, dpisEnabled, systemApp, icon));
        }
        result.sort(Comparator.comparing((AppListItem item) -> item.label.toLowerCase(Locale.ROOT))
                .thenComparing(item -> item.packageName));
        return result;
    }

    private Drawable loadAppIcon(PackageManager packageManager, ApplicationInfo applicationInfo) {
        String packageName = applicationInfo.packageName;
        Drawable cachedIcon = appIconCache.get(packageName);
        if (cachedIcon != null) {
            return cachedIcon;
        }
        Drawable loadedIcon = applicationInfo.loadIcon(packageManager);
        appIconCache.put(packageName, loadedIcon);
        return loadedIcon;
    }

    private static boolean isSystemApp(ApplicationInfo applicationInfo) {
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }

    private void applyFilter() {
        String query = currentQuery.trim();
        List<AppListItem> snapshot;
        synchronized (listLock) {
            snapshot = new ArrayList<>(allApps);
        }
        if (pagerAdapter == null) {
            return;
        }
        for (AppListPage page : AppListPage.values()) {
            pagerAdapter.submitPage(page, AppListVisibleSections.filter(snapshot, query, page, filterState));
        }
        pagerAdapter.refreshVisibleStatuses();
    }

    private void toggleScope(String packageName,
                             boolean currentlyInScope,
                             Runnable onTurnedInScope,
                             Runnable onTurnedOutScope) {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            showToast(R.string.status_save_requires_init);
            return;
        }
        if (currentlyInScope) {
            try {
                service.removeScope(Collections.singletonList(packageName));
                showToast(R.string.scope_remove_success, packageName);
                if (onTurnedOutScope != null) {
                    onTurnedOutScope.run();
                }
                requestAppsLoad();
            } catch (RuntimeException exception) {
                showToast(R.string.scope_remove_failed);
            }
            return;
        }
        showToast(R.string.system_hooks_scope_request_notice);
        try {
            service.requestScope(Collections.singletonList(packageName),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(List<String> approved) {
                            runOnUiThread(() -> {
                                showToast(R.string.scope_add_success, packageName);
                                if (onTurnedInScope != null) {
                                    onTurnedInScope.run();
                                }
                                requestAppsLoad();
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            runOnUiThread(() -> showToast(R.string.scope_add_failed, message));
                        }
                    });
        } catch (RuntimeException exception) {
            showToast(R.string.scope_add_failed, exception.getMessage());
        }
    }

    private boolean saveAppConfig(AppListItem item, TextInputEditText viewportInput,
                                  TextInputEditText fontScaleInput,
                                  ModeToggle viewportModeToggle,
                                  ModeToggle fontModeToggle) {
        refreshSystemHookEffectiveEnabled();
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        DpiConfigStore store = getUiConfigStore();
        if (store == null) {
            showToast(R.string.status_save_requires_init);
            return false;
        }
        try {
            Integer widthDp = parsePositiveIntOrNull(viewportInput);
            String viewportMode = resolveViewportMode(viewportModeToggle);
            Integer fontScalePercent = parseFontScalePercentOrNull(fontScaleInput);
            String fontMode = resolveFontMode(fontModeToggle);
            boolean viewportEmulationIneffective = widthDp != null
                    && ViewportApplyMode.SYSTEM_EMULATION.equals(
                    ViewportApplyMode.normalize(viewportMode))
                    && !ViewportApplyMode.SYSTEM_EMULATION.equals(
                    EffectiveModeResolver.resolveViewportMode(viewportMode, systemHooksEnabled));
            boolean fontEmulationIneffective = fontScalePercent != null
                    && FontApplyMode.SYSTEM_EMULATION.equals(FontApplyMode.normalize(fontMode))
                    && !FontApplyMode.SYSTEM_EMULATION.equals(
                    EffectiveModeResolver.resolveFontMode(fontMode, systemHooksEnabled));
            boolean emulationRequestedWithoutSystemScope =
                    viewportEmulationIneffective || fontEmulationIneffective;
            boolean changed = true;
            if (widthDp == null) {
                changed = store.clearTargetViewportWidthDp(item.packageName) && changed;
                changed = store.setTargetViewportApplyMode(item.packageName, ViewportApplyMode.OFF)
                        && changed;
            } else {
                changed = store.setTargetViewportWidthDp(item.packageName, widthDp) && changed;
                changed = store.setTargetViewportApplyMode(item.packageName, viewportMode)
                        && changed;
            }
            if (fontScalePercent == null) {
                changed = store.clearTargetFontScalePercent(item.packageName) && changed;
                changed = store.setTargetFontApplyMode(item.packageName, FontApplyMode.OFF) && changed;
            } else {
                changed = store.setTargetFontScalePercent(item.packageName, fontScalePercent) && changed;
                changed = store.setTargetFontApplyMode(item.packageName, fontMode) && changed;
            }
            showToast(changed
                    ? getString(R.string.status_save_success, item.packageName)
                    : getString(R.string.status_save_requires_init));
            if (changed) {
                requestAppsLoad();
            }
            if (changed && emulationRequestedWithoutSystemScope) {
                showToast(R.string.emulation_requires_system_scope_hint);
            }
            return changed;
        } catch (NumberFormatException exception) {
            showToast(R.string.status_save_invalid);
            return false;
        }
    }

    private static Integer parsePositiveIntOrNullSafe(TextInputEditText inputView) {
        try {
            return parsePositiveIntOrNull(inputView);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseFontScalePercentOrNullSafe(TextInputEditText inputView) {
        try {
            return parseFontScalePercentOrNull(inputView);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void updateDialogStatus(MaterialTextView statusView,
                                    boolean inScope,
                                    boolean dpisEnabled,
                                    TextInputEditText viewportInputView,
                                    ModeToggle viewportModeToggle,
                                    TextInputEditText fontInputView,
                                    ModeToggle fontModeToggle,
                                    boolean systemHooksEnabled) {
        Integer widthDp = parsePositiveIntOrNullSafe(viewportInputView);
        Integer fontScalePercent = parseFontScalePercentOrNullSafe(fontInputView);
        String viewportMode = widthDp == null ? ViewportApplyMode.OFF : resolveViewportMode(viewportModeToggle);
        String fontMode = fontScalePercent == null ? FontApplyMode.OFF : resolveFontMode(fontModeToggle);
        String statusText = AppStatusFormatter.format(
                inScope, widthDp, viewportMode, fontScalePercent, fontMode, dpisEnabled);
        String dialogStatusText = AppStatusFormatter.toCompactDisplay(statusText);
        boolean warnConfigSegments = AppStatusFormatter.shouldWarnEmulation(
                widthDp, viewportMode, fontScalePercent, fontMode,
                systemHooksEnabled, dpisEnabled);
        if (warnConfigSegments) {
            int warnColor = MaterialColors.getColor(statusView, androidx.appcompat.R.attr.colorError);
            statusView.setText(AppStatusFormatter.applyConfigSegmentsWarnStyle(dialogStatusText, warnColor));
            return;
        }
        statusView.setText(dialogStatusText);
    }

    private static String resolveFontMode(ModeToggle fontModeToggle) {
        Object modeTag = fontModeToggle.container.getTag();
        if (FontApplyMode.SYSTEM_EMULATION.equals(modeTag)) {
            return FontApplyMode.SYSTEM_EMULATION;
        }
        return FontApplyMode.FIELD_REWRITE;
    }

    private static String resolveViewportMode(ModeToggle viewportModeToggle) {
        Object modeTag = viewportModeToggle.container.getTag();
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(modeTag)) {
            return ViewportApplyMode.SYSTEM_EMULATION;
        }
        return ViewportApplyMode.FIELD_REWRITE;
    }

    private static void bindFontModeToggle(ModeToggle fontModeToggle, String fontMode) {
        String resolved = FontApplyMode.SYSTEM_EMULATION.equals(fontMode)
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        fontModeToggle.container.setTag(resolved);
        updateModeToggleVisual(fontModeToggle, FontApplyMode.SYSTEM_EMULATION.equals(resolved), false);
    }

    private static void toggleFontMode(ModeToggle fontModeToggle) {
        String nextMode = FontApplyMode.FIELD_REWRITE.equals(resolveFontMode(fontModeToggle))
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        bindFontModeToggle(fontModeToggle, nextMode);
        updateModeToggleVisual(fontModeToggle, FontApplyMode.SYSTEM_EMULATION.equals(nextMode), true);
    }

    private static void bindViewportModeToggle(ModeToggle viewportModeToggle,
                                               String viewportMode) {
        String resolved = ViewportApplyMode.SYSTEM_EMULATION.equals(viewportMode)
                ? ViewportApplyMode.SYSTEM_EMULATION
                : ViewportApplyMode.FIELD_REWRITE;
        viewportModeToggle.container.setTag(resolved);
        updateModeToggleVisual(viewportModeToggle,
                ViewportApplyMode.SYSTEM_EMULATION.equals(resolved), false);
    }

    private static void toggleViewportMode(ModeToggle viewportModeToggle) {
        String nextMode = ViewportApplyMode.FIELD_REWRITE.equals(
                resolveViewportMode(viewportModeToggle))
                ? ViewportApplyMode.SYSTEM_EMULATION
                : ViewportApplyMode.FIELD_REWRITE;
        bindViewportModeToggle(viewportModeToggle, nextMode);
        updateModeToggleVisual(viewportModeToggle,
                ViewportApplyMode.SYSTEM_EMULATION.equals(nextMode), true);
    }

    private static void updateModeToggleVisual(ModeToggle toggle,
                                               boolean emulationActive,
                                               boolean animate) {
        int activeTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSecondaryContainer);
        int inactiveTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSurface);
        toggle.emulationLabel.setTextColor(emulationActive ? activeTextColor : inactiveTextColor);
        toggle.replaceLabel.setTextColor(emulationActive ? inactiveTextColor : activeTextColor);
        toggle.emulationLabel.setAlpha(emulationActive ? 1f : 0.66f);
        toggle.replaceLabel.setAlpha(emulationActive ? 0.66f : 1f);
        toggle.emulationLabel.setTypeface(Typeface.DEFAULT,
                emulationActive ? Typeface.BOLD : Typeface.NORMAL);
        toggle.replaceLabel.setTypeface(Typeface.DEFAULT,
                emulationActive ? Typeface.NORMAL : Typeface.BOLD);
        toggle.emulationLabel.setScaleX(emulationActive ? 1.04f : 1f);
        toggle.emulationLabel.setScaleY(emulationActive ? 1.04f : 1f);
        toggle.replaceLabel.setScaleX(emulationActive ? 1f : 1.04f);
        toggle.replaceLabel.setScaleY(emulationActive ? 1f : 1.04f);
        toggle.container.post(() -> {
            int available = toggle.container.getWidth()
                    - toggle.container.getPaddingLeft()
                    - toggle.container.getPaddingRight();
            if (available <= 0) {
                return;
            }
            int half = available / 2;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) toggle.thumb.getLayoutParams();
            if (params.width != half) {
                params.width = half;
                toggle.thumb.setLayoutParams(params);
            }
            float target = emulationActive ? 0f : half;
            if (animate) {
                toggle.thumb.animate().cancel();
                toggle.thumb.animate().translationX(target).setDuration(150L).start();
            } else {
                toggle.thumb.setTranslationX(target);
            }
        });
    }

    private static final class ModeToggle {
        final View container;
        final View thumb;
        final MaterialTextView emulationLabel;
        final MaterialTextView replaceLabel;

        ModeToggle(View container, View thumb, MaterialTextView emulationLabel,
                   MaterialTextView replaceLabel) {
            this.container = container;
            this.thumb = thumb;
            this.emulationLabel = emulationLabel;
            this.replaceLabel = replaceLabel;
        }
    }

    private void bindScopeButton(MaterialButton scopeButton,
                                 boolean inScope,
                                 ColorStateList defaultBgTint,
                                 int defaultStrokeWidth,
                                 int defaultTextColor) {
        int activeBgColor = MaterialColors.getColor(
                scopeButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                scopeButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        scopeButton.setIcon(null);
        scopeButton.setText(R.string.dialog_scope_button);
        scopeButton.setBackgroundTintList(inScope
                ? ColorStateList.valueOf(activeBgColor)
                : defaultBgTint);
        scopeButton.setTextColor(inScope ? activeFgColor : defaultTextColor);
        scopeButton.setStrokeWidth(inScope ? 0 : defaultStrokeWidth);
        scopeButton.setContentDescription(inScope
                ? getString(R.string.scope_remove_button)
                : getString(R.string.scope_add_button));
    }

    private void bindDpisToggleButton(MaterialButton dpisToggleButton,
                                      boolean dpisEnabled,
                                      ColorStateList defaultBgTint,
                                      int defaultStrokeWidth,
                                      ColorStateList defaultIconTint) {
        dpisToggleButton.setText("");
        dpisToggleButton.setIconResource(R.drawable.ic_block_24);
        int activeBgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        boolean disableActive = !dpisEnabled;
        dpisToggleButton.setBackgroundTintList(
                disableActive ? ColorStateList.valueOf(activeBgColor) : defaultBgTint);
        dpisToggleButton.setIconTint(
                disableActive ? ColorStateList.valueOf(activeFgColor) : defaultIconTint);
        dpisToggleButton.setStrokeWidth(disableActive ? 0 : defaultStrokeWidth);
        dpisToggleButton.setContentDescription(getString(
                dpisEnabled ? R.string.dialog_dpis_disable_button : R.string.dialog_dpis_enable_button));
    }

    private static Integer parsePositiveIntOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value <= 0) {
            throw new NumberFormatException("must be positive");
        }
        return value;
    }

    private static Integer parseFontScalePercentOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value < 50 || value > 300) {
            throw new NumberFormatException("font scale out of range");
        }
        return value;
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

        showSystemSwitch.setChecked(filterState.showSystemApps);
        injectedOnlySwitch.setChecked(filterState.injectedOnly);
        widthOnlySwitch.setChecked(filterState.widthConfiguredOnly);
        fontOnlySwitch.setChecked(filterState.fontConfiguredOnly);

        android.widget.CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            filterState = new AppListFilterState(
                    showSystemSwitch.isChecked(),
                    injectedOnlySwitch.isChecked(),
                    widthOnlySwitch.isChecked(),
                    fontOnlySwitch.isChecked());
            applyFilter();
        };
        showSystemSwitch.setOnCheckedChangeListener(listener);
        injectedOnlySwitch.setOnCheckedChangeListener(listener);
        widthOnlySwitch.setOnCheckedChangeListener(listener);
        fontOnlySwitch.setOnCheckedChangeListener(listener);
        dialog.show();
    }

    private void maybeShowStartupDisclaimerDialog() {
        DpiConfigStore store = getUiConfigStore();
        if (store.isStartupDisclaimerAccepted() || isFinishing() || isDestroyed()) {
            return;
        }
        showStartupDisclaimerDialog(store);
    }

    private void showStartupDisclaimerDialog(DpiConfigStore store) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_startup_disclaimer, null, false);
        MaterialCheckBox agreementCheckBox =
                dialogView.findViewById(R.id.startup_disclaimer_checkbox);
        MaterialButton acceptButton =
                dialogView.findViewById(R.id.startup_disclaimer_accept_button);
        MaterialButton exitButton =
                dialogView.findViewById(R.id.startup_disclaimer_exit_button);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((unused, keyCode, event) ->
                keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP);

        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                acceptButton.setEnabled(isChecked));
        acceptButton.setOnClickListener(v -> {
            if (!agreementCheckBox.isChecked()) {
                return;
            }
            if (!store.setStartupDisclaimerAccepted(true)) {
                showToast(R.string.startup_disclaimer_save_failed);
                return;
            }
            dialog.dismiss();
        });
        exitButton.setOnClickListener(v -> finish());
        dialog.show();
    }

    private void showEditDialog(AppListItem item) {
        if (pagerAdapter != null) {
            pagerAdapter.refreshVisibleStatuses();
        }
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_app_config, root, false);
        AppConfigDialogViews views = initDialogViews(dialogView);
        AppConfigDialogState state = bindDialogInitialState(item, views);
        AppConfigDialogActionStyle style = resolveDialogActionStyle(views.startButton);
        refreshDialogState(views, state, style, systemHooksEnabled);
        bindDialogValidation(dialogView, views, state, style, systemHooksEnabled);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled);
        dialog.show();
    }

    private AppConfigDialogViews initDialogViews(View dialogView) {
        return new AppConfigDialogViews(
                dialogView.findViewById(R.id.dialog_app_icon),
                dialogView.findViewById(R.id.dialog_title),
                dialogView.findViewById(R.id.dialog_package),
                dialogView.findViewById(R.id.dialog_status),
                dialogView.findViewById(R.id.dialog_viewport_input_layout),
                dialogView.findViewById(R.id.dialog_viewport_input),
                dialogView.findViewById(R.id.dialog_font_scale_input_layout),
                dialogView.findViewById(R.id.dialog_font_scale_input),
                new ModeToggle(
                        dialogView.findViewById(R.id.dialog_viewport_mode_toggle_button),
                        dialogView.findViewById(R.id.dialog_viewport_mode_toggle_thumb),
                        dialogView.findViewById(R.id.dialog_viewport_mode_emulation_label),
                        dialogView.findViewById(R.id.dialog_viewport_mode_replace_label)),
                new ModeToggle(
                        dialogView.findViewById(R.id.dialog_font_mode_toggle_button),
                        dialogView.findViewById(R.id.dialog_font_mode_toggle_thumb),
                        dialogView.findViewById(R.id.dialog_font_mode_emulation_label),
                        dialogView.findViewById(R.id.dialog_font_mode_replace_label)),
                dialogView.findViewById(R.id.dialog_scope_button),
                dialogView.findViewById(R.id.dialog_start_button),
                dialogView.findViewById(R.id.dialog_restart_button),
                dialogView.findViewById(R.id.dialog_stop_button),
                dialogView.findViewById(R.id.dialog_dpis_toggle_button),
                dialogView.findViewById(R.id.dialog_disable_button),
                dialogView.findViewById(R.id.dialog_save_button));
    }

    private AppConfigDialogState bindDialogInitialState(AppListItem item, AppConfigDialogViews views) {
        views.iconView.setImageDrawable(item.icon);
        views.titleView.setText(item.label);
        views.packageView.setText(item.packageName);
        views.viewportInputView.setText(item.viewportWidthDp != null
                ? String.valueOf(item.viewportWidthDp) : "");
        views.fontInputView.setText(item.fontScalePercent != null
                ? String.valueOf(item.fontScalePercent) : "");
        bindViewportModeToggle(views.viewportModeToggle, item.viewportMode);
        bindFontModeToggle(views.fontModeToggle, item.fontMode);
        updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                views.fontInputLayout, views.fontInputView, views.saveButton);
        return new AppConfigDialogState(item.inScope, item.dpisEnabled);
    }

    private AppConfigDialogActionStyle resolveDialogActionStyle(MaterialButton startButton) {
        ColorStateList defaultActionBgTint = startButton.getBackgroundTintList();
        int defaultActionStrokeWidth = startButton.getStrokeWidth();
        int defaultActionTextColor = MaterialColors.getColor(
                startButton, androidx.appcompat.R.attr.colorPrimary);
        ColorStateList resolvedActionIconTint = startButton.getIconTint();
        if (resolvedActionIconTint == null) {
            resolvedActionIconTint = ColorStateList.valueOf(MaterialColors.getColor(
                    startButton, androidx.appcompat.R.attr.colorPrimary));
        }
        return new AppConfigDialogActionStyle(defaultActionBgTint,
                defaultActionStrokeWidth, defaultActionTextColor, resolvedActionIconTint);
    }

    private void refreshDialogState(AppConfigDialogViews views,
                                    AppConfigDialogState state,
                                    AppConfigDialogActionStyle style,
                                    boolean systemHooksEnabled) {
        updateDialogStatus(
                views.statusView,
                state.scopeSelected,
                state.dpisEnabled,
                views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputView,
                views.fontModeToggle,
                systemHooksEnabled);
        bindScopeButton(views.scopeButton, state.scopeSelected,
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor);
        bindDpisToggleButton(views.dpisToggleButton, state.dpisEnabled,
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionIconTint);
    }

    private void bindDialogValidation(View dialogView,
                                      AppConfigDialogViews views,
                                      AppConfigDialogState state,
                                      AppConfigDialogActionStyle style,
                                      boolean systemHooksEnabled) {
        android.widget.TextView.OnEditorActionListener doneListener = (v, actionId, event) -> {
            boolean isDoneAction = actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnterDown = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (!isDoneAction && !isEnterDown) {
                return false;
            }
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            return true;
        };
        views.viewportInputView.setOnEditorActionListener(doneListener);
        views.fontInputView.setOnEditorActionListener(doneListener);
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                        views.fontInputLayout, views.fontInputView, views.saveButton);
                refreshDialogState(views, state, style, systemHooksEnabled);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        views.viewportInputView.addTextChangedListener(validationWatcher);
        views.fontInputView.addTextChangedListener(validationWatcher);
    }

    private void bindDialogActions(View dialogView,
                                   AppListItem item,
                                   AppConfigDialogViews views,
                                   AppConfigDialogState state,
                                   AppConfigDialogActionStyle style,
                                   boolean systemHooksEnabled) {
        dialogView.setFocusable(true);
        dialogView.setFocusableInTouchMode(true);
        dialogView.setClickable(true);
        dialogView.setOnClickListener(v ->
                clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView));
        views.scopeButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleScope(item.packageName, state.scopeSelected,
                    () -> {
                        state.scopeSelected = true;
                        refreshDialogState(views, state, style, systemHooksEnabled);
                    },
                    () -> {
                        state.scopeSelected = false;
                        refreshDialogState(views, state, style, systemHooksEnabled);
                    });
        });
        views.startButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            executeProcessAction(item, ProcessAction.START);
        });
        views.restartButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            executeProcessAction(item, ProcessAction.RESTART);
        });
        views.stopButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            executeProcessAction(item, ProcessAction.STOP);
        });
        views.dpisToggleButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            boolean nextEnabled = !state.dpisEnabled;
            if (setDpisEnabled(item.packageName, nextEnabled)) {
                state.dpisEnabled = nextEnabled;
                refreshDialogState(views, state, style, systemHooksEnabled);
            }
        });
        views.disableButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            views.viewportInputView.setText("");
            views.fontInputView.setText("");
            bindViewportModeToggle(views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE);
            updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.saveButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            saveAppConfig(item, views.viewportInputView, views.fontInputView,
                    views.viewportModeToggle, views.fontModeToggle);
        });
        views.viewportModeToggle.container.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleViewportMode(views.viewportModeToggle);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindViewportModeToggle(views.viewportModeToggle, ViewportApplyMode.SYSTEM_EMULATION);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindViewportModeToggle(views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.container.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleFontMode(views.fontModeToggle);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.emulationLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.replaceLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
    }

    private static final class AppConfigDialogViews {
        final android.widget.ImageView iconView;
        final MaterialTextView titleView;
        final MaterialTextView packageView;
        final MaterialTextView statusView;
        final TextInputLayout viewportInputLayout;
        final TextInputEditText viewportInputView;
        final TextInputLayout fontInputLayout;
        final TextInputEditText fontInputView;
        final ModeToggle viewportModeToggle;
        final ModeToggle fontModeToggle;
        final MaterialButton scopeButton;
        final MaterialButton startButton;
        final MaterialButton restartButton;
        final MaterialButton stopButton;
        final MaterialButton dpisToggleButton;
        final MaterialButton disableButton;
        final MaterialButton saveButton;

        AppConfigDialogViews(android.widget.ImageView iconView,
                             MaterialTextView titleView,
                             MaterialTextView packageView,
                             MaterialTextView statusView,
                             TextInputLayout viewportInputLayout,
                             TextInputEditText viewportInputView,
                             TextInputLayout fontInputLayout,
                             TextInputEditText fontInputView,
                             ModeToggle viewportModeToggle,
                             ModeToggle fontModeToggle,
                             MaterialButton scopeButton,
                             MaterialButton startButton,
                             MaterialButton restartButton,
                             MaterialButton stopButton,
                             MaterialButton dpisToggleButton,
                             MaterialButton disableButton,
                             MaterialButton saveButton) {
            this.iconView = iconView;
            this.titleView = titleView;
            this.packageView = packageView;
            this.statusView = statusView;
            this.viewportInputLayout = viewportInputLayout;
            this.viewportInputView = viewportInputView;
            this.fontInputLayout = fontInputLayout;
            this.fontInputView = fontInputView;
            this.viewportModeToggle = viewportModeToggle;
            this.fontModeToggle = fontModeToggle;
            this.scopeButton = scopeButton;
            this.startButton = startButton;
            this.restartButton = restartButton;
            this.stopButton = stopButton;
            this.dpisToggleButton = dpisToggleButton;
            this.disableButton = disableButton;
            this.saveButton = saveButton;
        }
    }

    private static final class AppConfigDialogState {
        boolean scopeSelected;
        boolean dpisEnabled;

        AppConfigDialogState(boolean scopeSelected, boolean dpisEnabled) {
            this.scopeSelected = scopeSelected;
            this.dpisEnabled = dpisEnabled;
        }
    }

    private static final class AppConfigDialogActionStyle {
        final ColorStateList defaultActionBgTint;
        final int defaultActionStrokeWidth;
        final int defaultActionTextColor;
        final ColorStateList defaultActionIconTint;

        AppConfigDialogActionStyle(ColorStateList defaultActionBgTint,
                                   int defaultActionStrokeWidth,
                                   int defaultActionTextColor,
                                   ColorStateList defaultActionIconTint) {
            this.defaultActionBgTint = defaultActionBgTint;
            this.defaultActionStrokeWidth = defaultActionStrokeWidth;
            this.defaultActionTextColor = defaultActionTextColor;
            this.defaultActionIconTint = defaultActionIconTint;
        }
    }

    private boolean isSystemHookEnabledFromStore() {
        return cachedSystemHookEffectiveEnabled;
    }

    private void refreshSystemHookEffectiveEnabled() {
        DpiConfigStore store = getUiConfigStore();
        if (store == null) {
            cachedSystemHookEffectiveEnabled = false;
            return;
        }
        boolean desiredEnabled = store.isSystemServerHooksEnabled();
        XposedService service = DpisApplication.getXposedService();
        boolean serviceAvailable = service != null;
        boolean scopeSelected = false;
        if (serviceAvailable) {
            try {
                List<String> scope = service.getScope();
                scopeSelected = scope != null && scope.contains(SYSTEM_SCOPE_MODERN);
            } catch (RuntimeException ignored) {
                scopeSelected = false;
            }
        }
        cachedSystemHookEffectiveEnabled = SystemHookEffectiveView.resolve(
                desiredEnabled,
                serviceAvailable,
                scopeSelected).effectiveEnabled;
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
