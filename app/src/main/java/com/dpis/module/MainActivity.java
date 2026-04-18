package com.dpis.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        applyInsets();

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
        pagerAdapter = new AppListPagerAdapter(this::showEditDialog, this::onPageRefreshRequested);
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
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(this::requestAppsLoad);
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
        DpiConfigStore store = DpisApplication.getConfigStore();

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
            Integer fontScalePercent = store != null
                    ? store.getTargetFontScalePercent(applicationInfo.packageName)
                    : null;
            String fontMode = store != null
                    ? store.getTargetFontApplyMode(applicationInfo.packageName)
                    : FontApplyMode.OFF;
            result.add(new AppListItem(label, applicationInfo.packageName,
                    scopePackages.contains(applicationInfo.packageName), viewportWidth,
                    fontScalePercent, fontMode, systemApp, icon));
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
    }

    private void toggleScope(AppListItem item) {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (item.inScope) {
            service.removeScope(Collections.singletonList(item.packageName));
            Toast.makeText(this, getString(R.string.scope_remove_success, item.packageName),
                    Toast.LENGTH_SHORT).show();
            requestAppsLoad();
            return;
        }
        service.requestScope(Collections.singletonList(item.packageName),
                new XposedService.OnScopeEventListener() {
                    @Override
                    public void onScopeRequestApproved(List<String> approved) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.scope_add_success, item.packageName),
                                    Toast.LENGTH_SHORT).show();
                            requestAppsLoad();
                        });
                    }

                    @Override
                    public void onScopeRequestFailed(String message) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                getString(R.string.scope_add_failed, message),
                                Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void saveAppConfig(AppListItem item, TextInputEditText viewportInput,
                               TextInputEditText fontScaleInput,
                               MaterialButton fontModeToggleButton) {
        DpiConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        try {
            Integer widthDp = parsePositiveIntOrNull(viewportInput);
            Integer fontScalePercent = parseFontScalePercentOrNull(fontScaleInput);
            String fontMode = resolveFontMode(fontModeToggleButton);
            boolean changed = true;
            if (widthDp == null) {
                changed = store.clearTargetViewportWidthDp(item.packageName) && changed;
            } else {
                changed = store.setTargetViewportWidthDp(item.packageName, widthDp) && changed;
            }
            if (fontScalePercent == null) {
                changed = store.clearTargetFontScalePercent(item.packageName) && changed;
                changed = store.setTargetFontApplyMode(item.packageName, FontApplyMode.OFF) && changed;
            } else {
                changed = store.setTargetFontScalePercent(item.packageName, fontScalePercent) && changed;
                changed = store.setTargetFontApplyMode(item.packageName, fontMode) && changed;
            }
            Toast.makeText(this,
                    changed
                            ? getString(R.string.status_save_success, item.packageName)
                            : getString(R.string.status_save_requires_init),
                    Toast.LENGTH_SHORT).show();
            if (changed) {
                requestAppsLoad();
            }
        } catch (NumberFormatException exception) {
            Toast.makeText(this, getString(R.string.status_save_invalid), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private static String resolveFontMode(MaterialButton fontModeToggleButton) {
        Object modeTag = fontModeToggleButton.getTag();
        if (FontApplyMode.FIELD_REWRITE.equals(modeTag)) {
            return FontApplyMode.FIELD_REWRITE;
        }
        return FontApplyMode.SYSTEM_EMULATION;
    }

    private static void bindFontModeToggle(MaterialButton fontModeToggleButton, String fontMode) {
        String resolved = FontApplyMode.FIELD_REWRITE.equals(fontMode)
                ? FontApplyMode.FIELD_REWRITE
                : FontApplyMode.SYSTEM_EMULATION;
        fontModeToggleButton.setTag(resolved);
        fontModeToggleButton.setText(FontApplyMode.FIELD_REWRITE.equals(resolved)
                ? R.string.dialog_font_mode_fallback
                : R.string.dialog_font_mode_emulation);
    }

    private static void toggleFontMode(MaterialButton fontModeToggleButton) {
        String nextMode = FontApplyMode.FIELD_REWRITE.equals(resolveFontMode(fontModeToggleButton))
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        bindFontModeToggle(fontModeToggleButton, nextMode);
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
        if (value < 50 || value > 250) {
            throw new NumberFormatException("font scale out of range");
        }
        return value;
    }

    private void disableAppConfig(AppListItem item) {
        DpiConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        boolean cleared = store.clearTargetViewportWidthDp(item.packageName);
        cleared = store.clearTargetFontScalePercent(item.packageName) && cleared;
        cleared = store.setTargetFontApplyMode(item.packageName, FontApplyMode.OFF) && cleared;
        Toast.makeText(this,
                cleared
                        ? getString(R.string.status_save_disabled, item.packageName)
                        : getString(R.string.status_save_requires_init),
                Toast.LENGTH_SHORT).show();
        if (cleared) {
            requestAppsLoad();
        }
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_list_filters, null, false);
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

    private void showEditDialog(AppListItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_config, null, false);
        android.widget.ImageView iconView = dialogView.findViewById(R.id.dialog_app_icon);
        MaterialTextView titleView = dialogView.findViewById(R.id.dialog_title);
        MaterialTextView packageView = dialogView.findViewById(R.id.dialog_package);
        MaterialTextView statusView = dialogView.findViewById(R.id.dialog_status);
        TextInputEditText viewportInputView = dialogView.findViewById(R.id.dialog_viewport_input);
        TextInputEditText fontInputView = dialogView.findViewById(R.id.dialog_font_scale_input);
        MaterialButton fontModeToggleButton = dialogView.findViewById(R.id.dialog_font_mode_toggle_button);
        MaterialButton scopeButton = dialogView.findViewById(R.id.dialog_scope_button);
        MaterialButton disableButton = dialogView.findViewById(R.id.dialog_disable_button);
        MaterialButton saveButton = dialogView.findViewById(R.id.dialog_save_button);

        iconView.setImageDrawable(item.icon);
        titleView.setText(item.label);
        packageView.setText(item.packageName);
        statusView.setText(AppStatusFormatter.format(
                item.inScope, item.viewportWidthDp, item.fontScalePercent, item.fontMode));
        viewportInputView.setText(item.viewportWidthDp != null
                ? String.valueOf(item.viewportWidthDp) : "");
        fontInputView.setText(item.fontScalePercent != null
                ? String.valueOf(item.fontScalePercent) : "");
        bindFontModeToggle(fontModeToggleButton, item.fontMode);
        scopeButton.setText(item.inScope
                ? getString(R.string.scope_remove_button)
                : getString(R.string.scope_add_button));

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        scopeButton.setOnClickListener(v -> {
            toggleScope(item);
            dialog.dismiss();
        });
        disableButton.setOnClickListener(v -> {
            disableAppConfig(item);
            dialog.dismiss();
        });
        saveButton.setOnClickListener(v -> {
            saveAppConfig(item, viewportInputView, fontInputView, fontModeToggleButton);
            dialog.dismiss();
        });
        fontModeToggleButton.setOnClickListener(v -> toggleFontMode(fontModeToggleButton));
        dialog.show();
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
