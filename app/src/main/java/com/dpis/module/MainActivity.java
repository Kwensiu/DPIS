package com.dpis.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements DpisApplication.ServiceStateListener {
    private static final long MODE_TOGGLE_ANIM_DURATION_MS = 200L;
    private static final long SEARCH_FAB_ANIM_DURATION_MS = 180L;
    private static final int SEARCH_FAB_SCROLL_TRIGGER_DY = 8;
    private static final String SYSTEM_SCOPE_MODERN = "system";
    private static final String STATE_CURRENT_QUERY = "state.current_query";
    private static final String STATE_CURRENT_PAGE = "state.current_page";
    private static final String STATE_FILTER_SHOW_SYSTEM = "state.filter.show_system";
    private static final String STATE_FILTER_INJECTED_ONLY = "state.filter.injected_only";
    private static final String STATE_FILTER_WIDTH_ONLY = "state.filter.width_only";
    private static final String STATE_FILTER_FONT_ONLY = "state.filter.font_only";
    private static final String STATE_PAGE_SCROLL_STATES = "state.page_scroll_states";
    private static final String STATE_REFRESHING_PAGES = "state.refreshing_pages";
    private static final String UPDATE_PROMPT_PREFS = "dpis.update_prompt";
    private static final String KEY_LAST_UPDATE_CHECK_TIMESTAMP = "last_update_check_timestamp";
    private static final String KEY_LAST_UPDATE_CHECK_FAILED = "last_update_check_failed";
    private static final String KEY_LAST_PROMPTED_UPDATE_VERSION_CODE = "last_prompted_update_version_code";
    private static final long UPDATE_STARTUP_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final long UPDATE_STARTUP_CHECK_FAILURE_RETRY_INTERVAL_MS = 30L * 60L * 1000L;
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;
    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^(\\d+)");

    private final List<AppListItem> allApps = new ArrayList<>();
    private final Object listLock = new Object();
    private final AppLoadCoordinator loadCoordinator = new AppLoadCoordinator();
    private final AppIconMemoryCache appIconCache = new AppIconMemoryCache(256);
    private final Set<AppListPage> refreshingPages = EnumSet.noneOf(AppListPage.class);
    private final ExecutorService startupUpdateExecutor = Executors.newSingleThreadExecutor();

    private AppListPagerAdapter pagerAdapter;
    private ViewPager2 appPager;
    private SparseArray<Parcelable> restoredPageScrollStates;
    private String currentQuery = "";
    private AppListFilterState filterState = AppListFilterState.defaultState();
    private EditText searchInput;
    private FloatingActionButton searchFocusFab;
    private FloatingActionButton helpFab;
    private boolean searchFabHidden;
    private ImageButton searchFilterButton;
    private Boolean rootAccessCache;
    private boolean cachedSystemHookEffectiveEnabled;
    private volatile boolean startupUpdateCheckInProgress;
    private volatile boolean updateDownloadInProgress;
    private volatile boolean updateDownloadCancelRequested;
    private volatile Future<?> activeDownloadFuture;
    private volatile HttpURLConnection activeDownloadConnection;

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

    private static final class StartupUpdateManifest {
        final String versionName;
        final int versionCode;
        final String apkUrl;
        final String releasePage;

        StartupUpdateManifest(String versionName,
                int versionCode,
                String apkUrl,
                String releasePage) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.apkUrl = apkUrl;
            this.releasePage = releasePage;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        searchFocusFab = findViewById(R.id.search_focus_fab);
        helpFab = findViewById(R.id.help_fab);
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
            restoredPageScrollStates = savedInstanceState.getSparseParcelableArray(STATE_PAGE_SCROLL_STATES);
            restoreRefreshingPages(savedInstanceState.getIntArray(STATE_REFRESHING_PAGES));
        }

        searchFilterButton = findViewById(R.id.search_filter_button);
        appPager = findViewById(R.id.app_pager);
        pagerAdapter = new AppListPagerAdapter(
                this::showEditDialog,
                this::onPageRefreshRequested,
                this::onPageListScrolled,
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
                currentQuery = s != null ? s.toString() : "";
                applyFilter();
                searchClearButton.setVisibility(
                        currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchClearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
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
        systemSettingsButton
                .setOnClickListener(v -> startActivity(new Intent(this, SystemServerSettingsActivity.class)));

        if (retainedState != null && !retainedState.appsSnapshot.isEmpty()) {
            applyFilter();
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
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelActiveUpdateDownload();
        startupUpdateExecutor.shutdownNow();
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
                    pagerAdapter.capturePageScrollStates());
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

    private void onPageListScrolled(AppListPage page, int dy) {
        if (dy >= SEARCH_FAB_SCROLL_TRIGGER_DY) {
            hideSearchFocusFab();
            return;
        }
        if (dy <= -SEARCH_FAB_SCROLL_TRIGGER_DY) {
            showSearchFocusFab();
        }
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

    private static void showSaveButtonFeedback(MaterialButton saveButton) {
        if (saveButton == null)
            return;
        // Tag holds Object[]: [0]=restoreText (CharSequence), [1]=restoreRunnable
        // (Runnable)
        CharSequence restoreText;
        Object[] tag = saveButton.getTag() instanceof Object[] ? (Object[]) saveButton.getTag() : null;
        if (tag != null && tag[0] instanceof CharSequence) {
            restoreText = (CharSequence) tag[0];
            if (tag[1] instanceof Runnable) {
                saveButton.removeCallbacks((Runnable) tag[1]);
            }
        } else {
            restoreText = saveButton.getText();
        }
        saveButton.setText(R.string.status_save_success_inline);
        Runnable restore = () -> {
            if (saveButton.isAttachedToWindow()) {
                saveButton.setText(restoreText);
            }
        };
        saveButton.setTag(new Object[] { restoreText, restore });
        saveButton.postDelayed(restore, 1500);
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
                            (dialog, which) -> runProcessAction(item.packageName, item.label, action))
                    .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                    .show();
            return;
        }
        runProcessAction(item.packageName, item.label, action);
    }

    private void runProcessAction(String packageName, String appLabel, ProcessAction action) {
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
                    showToast(R.string.dialog_process_action_success, actionLabel, appLabel);
                    return;
                }
                String reason = result.output == null || result.output.isEmpty()
                        ? "unknown error"
                        : result.output;
                showToast(R.string.dialog_process_action_failed, actionLabel, appLabel, reason);
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
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
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
                    ? exception.getMessage()
                    : exception.getClass().getSimpleName());
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

    private void toggleScope(String packageName, String appLabel,
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
                showToast(R.string.scope_remove_success, appLabel);
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
                                showToast(R.string.scope_add_success, appLabel);
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

    /**
     * Saves app config. Returns int[2]: [0]=1 if saved, 0 if not; [1]=hint string
     * res id or 0.
     */
    private int[] saveAppConfig(AppListItem item, TextInputEditText viewportInput,
            TextInputEditText fontScaleInput,
            ModeToggle viewportModeToggle,
            ModeToggle fontModeToggle) {
        refreshSystemHookEffectiveEnabled();
        boolean systemHooksEnabled = isSystemHookEnabledFromStore();
        DpiConfigStore store = getUiConfigStore();
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
            boolean emulationRequestedWithoutSystemScope = viewportEmulationIneffective || fontEmulationIneffective;
            boolean changed = true;
            int hint = 0;
            if (store == null) {
                hint = R.string.status_save_requires_init;
                return new int[] { 1, hint };
            }
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
            if (changed) {
                requestAppsLoad();
            }
            if (changed && emulationRequestedWithoutSystemScope) {
                hint = R.string.emulation_requires_system_scope_hint;
            }
            return new int[] { 1, hint };
        } catch (NumberFormatException exception) {
            return new int[] { 0, R.string.status_save_invalid };
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
        boolean warnViewport = AppStatusFormatter.shouldWarnViewportEmulation(
                widthDp, viewportMode, systemHooksEnabled, dpisEnabled);
        boolean warnFont = AppStatusFormatter.shouldWarnFontEmulation(
                fontScalePercent, fontMode, systemHooksEnabled, dpisEnabled);
        if (warnViewport || warnFont) {
            int warnColor = MaterialColors.getColor(statusView, androidx.appcompat.R.attr.colorError);
            statusView.setText(AppStatusFormatter.applyConfigSegmentsWarnStyle(
                    dialogStatusText, warnColor, warnViewport, warnFont));
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

    private static void bindFontModeToggle(ModeToggle fontModeToggle,
            String fontMode,
            boolean animate) {
        String resolved = FontApplyMode.SYSTEM_EMULATION.equals(fontMode)
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        fontModeToggle.container.setTag(resolved);
        updateModeToggleVisual(fontModeToggle, FontApplyMode.SYSTEM_EMULATION.equals(resolved), animate);
    }

    private static void toggleFontMode(ModeToggle fontModeToggle) {
        String nextMode = FontApplyMode.FIELD_REWRITE.equals(resolveFontMode(fontModeToggle))
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        bindFontModeToggle(fontModeToggle, nextMode, true);
    }

    private static void bindViewportModeToggle(ModeToggle viewportModeToggle,
            String viewportMode,
            boolean animate) {
        String resolved = ViewportApplyMode.SYSTEM_EMULATION.equals(viewportMode)
                ? ViewportApplyMode.SYSTEM_EMULATION
                : ViewportApplyMode.FIELD_REWRITE;
        viewportModeToggle.container.setTag(resolved);
        updateModeToggleVisual(viewportModeToggle,
                ViewportApplyMode.SYSTEM_EMULATION.equals(resolved), animate);
    }

    private static void toggleViewportMode(ModeToggle viewportModeToggle) {
        String nextMode = ViewportApplyMode.FIELD_REWRITE.equals(
                resolveViewportMode(viewportModeToggle))
                        ? ViewportApplyMode.SYSTEM_EMULATION
                        : ViewportApplyMode.FIELD_REWRITE;
        bindViewportModeToggle(viewportModeToggle, nextMode, true);
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
                toggle.thumb.animate()
                        .translationX(target)
                        .setDuration(MODE_TOGGLE_ANIM_DURATION_MS)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
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
        int scopeTextRes = inScope ? R.string.scope_remove_button : R.string.scope_add_button;
        scopeButton.setText(scopeTextRes);
        scopeButton.setBackgroundTintList(inScope
                ? ColorStateList.valueOf(activeBgColor)
                : defaultBgTint);
        scopeButton.setTextColor(inScope ? activeFgColor : defaultTextColor);
        scopeButton.setStrokeWidth(inScope ? 0 : defaultStrokeWidth);
        scopeButton.setContentDescription(getString(scopeTextRes));
    }

    private void bindDpisToggleButton(MaterialButton dpisToggleButton,
            boolean dpisEnabled,
            ColorStateList defaultBgTint,
            int defaultStrokeWidth,
            int defaultTextColor) {
        String buttonText = getString(
                dpisEnabled ? R.string.dialog_dpis_disable_button : R.string.dialog_dpis_enable_button);
        dpisToggleButton.setText(buttonText);
        dpisToggleButton.setIcon(null);
        int activeBgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        boolean enabledActive = dpisEnabled;
        dpisToggleButton.setBackgroundTintList(
                enabledActive ? ColorStateList.valueOf(activeBgColor) : defaultBgTint);
        dpisToggleButton.setTextColor(enabledActive ? activeFgColor : defaultTextColor);
        dpisToggleButton.setStrokeWidth(enabledActive ? 0 : defaultStrokeWidth);
        dpisToggleButton.setContentDescription(buttonText);
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

    private boolean maybeShowStartupDisclaimerDialog() {
        DpiConfigStore store = getUiConfigStore();
        if (store.isStartupDisclaimerAccepted() || isFinishing() || isDestroyed()) {
            return false;
        }
        showStartupDisclaimerDialog(store, this::maybeCheckForUpdatesOnStartup);
        return true;
    }

    private void maybeCheckForUpdatesOnStartup() {
        if (startupUpdateCheckInProgress || isFinishing() || isDestroyed()) {
            return;
        }
        long now = System.currentTimeMillis();
        long lastCheckTimestamp = getLastUpdateCheckTimestamp();
        long startupCheckInterval = wasLastUpdateCheckFailed()
                ? UPDATE_STARTUP_CHECK_FAILURE_RETRY_INTERVAL_MS
                : UPDATE_STARTUP_CHECK_INTERVAL_MS;
        if (now - lastCheckTimestamp < startupCheckInterval) {
            return;
        }
        startupUpdateCheckInProgress = true;

        final String manifestUrl = getString(R.string.about_update_manifest_url);
        startupUpdateExecutor.execute(() -> {
            boolean requestSucceeded = false;
            try {
                StartupUpdateManifest manifest = fetchUpdateManifest(manifestUrl);
                requestSucceeded = true;
                boolean hasUpdate = isRemoteVersionNewer(
                        manifest.versionCode,
                        manifest.versionName,
                        BuildConfig.VERSION_CODE,
                        BuildConfig.VERSION_NAME);
                if (!hasUpdate) {
                    return;
                }
                int lastPromptedVersionCode = getLastPromptedUpdateVersionCode();
                if (manifest.versionCode <= lastPromptedVersionCode) {
                    return;
                }
                runOnUiThread(() -> launchStartupUpdateDialog(manifest));
            } catch (Exception ignored) {
                // Ignore startup update check failures silently.
            } finally {
                setLastUpdateCheckTimestamp(System.currentTimeMillis());
                setLastUpdateCheckFailed(!requestSucceeded);
                startupUpdateCheckInProgress = false;
            }
        });
    }

    private void launchStartupUpdateDialog(StartupUpdateManifest manifest) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        UpdateAvailableDialog.DialogHandle dialogHandle = UpdateAvailableDialog.create(
                this,
                getString(R.string.about_update_available_title),
                getString(
                        R.string.about_update_available_message,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        manifest.versionName,
                        manifest.versionCode));
        showStartupDialogIdleState(
                dialogHandle.primaryButton,
                dialogHandle.cancelButton,
                dialogHandle.progressView,
                dialogHandle.progressTextView);

        dialogHandle.cancelButton.setOnClickListener(v -> {
            setLastPromptedUpdateVersionCode(manifest.versionCode);
            if (updateDownloadInProgress) {
                cancelActiveUpdateDownload();
                return;
            }
            dialogHandle.dialog.dismiss();
        });

        String releasePageUrl = manifest.releasePage.isEmpty()
                ? getString(R.string.about_releases_url)
                : manifest.releasePage;
        boolean hasDirectDownload = manifest.apkUrl != null && !manifest.apkUrl.trim().isEmpty();
        if (!hasDirectDownload) {
            dialogHandle.primaryButton.setText(R.string.about_update_action_view_release);
            dialogHandle.primaryButton.setOnClickListener(v -> {
                setLastPromptedUpdateVersionCode(manifest.versionCode);
                dialogHandle.dialog.dismiss();
                openUrl(releasePageUrl);
            });
            dialogHandle.dialog.show();
            return;
        }

        dialogHandle.primaryButton.setText(R.string.about_update_action_download);
        dialogHandle.primaryButton.setOnClickListener(v -> {
            setLastPromptedUpdateVersionCode(manifest.versionCode);
            startStartupUpdateDownload(
                    manifest.versionName,
                    manifest.apkUrl,
                    dialogHandle.dialog,
                    dialogHandle.primaryButton,
                    dialogHandle.cancelButton,
                    dialogHandle.progressView,
                    dialogHandle.progressTextView);
        });
        dialogHandle.dialog.setOnDismissListener(unused -> cancelActiveUpdateDownload());
        dialogHandle.dialog.show();
    }

    private void startStartupUpdateDownload(String targetVersionName,
            String downloadUrl,
            androidx.appcompat.app.AlertDialog dialog,
            MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        if (updateDownloadInProgress) {
            showToast(R.string.about_update_download_in_progress);
            return;
        }
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            showToast(R.string.about_update_download_failed);
            return;
        }

        Uri downloadUri = Uri.parse(downloadUrl);
        String scheme = downloadUri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            showToast(R.string.about_update_download_https_required);
            return;
        }

        final File targetFile;
        try {
            UpdatePackageInstaller.clearUpdateCache(this);
            targetFile = UpdatePackageInstaller.prepareTargetFile(this, targetVersionName);
        } catch (RuntimeException ignored) {
            showToast(R.string.about_update_download_failed);
            return;
        }

        updateDownloadInProgress = true;
        updateDownloadCancelRequested = false;
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        showStartupDialogDownloadingState(primaryButton, cancelButton, progressView, progressTextView);

        activeDownloadFuture = startupUpdateExecutor.submit(() -> executeStartupApkDownload(
                downloadUri,
                targetFile,
                dialog,
                primaryButton,
                cancelButton,
                progressView,
                progressTextView));
    }

    private void executeStartupApkDownload(Uri downloadUri,
            File targetFile,
            androidx.appcompat.app.AlertDialog dialog,
            MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(downloadUri.toString()).openConnection();
            activeDownloadConnection = connection;
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(UPDATE_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(UPDATE_READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/octet-stream,*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }

            long totalBytes = connection.getContentLengthLong();
            runOnUiThread(() -> prepareProgressView(progressView, progressTextView, totalBytes));

            try (InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                long downloadedBytes = 0L;
                long lastUiUpdateAt = 0L;
                int lastProgress = -1;

                while (true) {
                    if (updateDownloadCancelRequested || Thread.currentThread().isInterrupted()) {
                        throw new DownloadCanceledException();
                    }
                    int read = inputStream.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    outputStream.write(buffer, 0, read);
                    downloadedBytes += read;

                    long now = System.currentTimeMillis();
                    if (now - lastUiUpdateAt < DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS
                            && totalBytes > 0L) {
                        continue;
                    }

                    lastUiUpdateAt = now;
                    if (totalBytes > 0L) {
                        int progress = (int) Math.min(100L, (downloadedBytes * 100L) / totalBytes);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            long finalDownloadedBytes = downloadedBytes;
                            runOnUiThread(() -> updateProgressView(
                                    progressView,
                                    progressTextView,
                                    progress,
                                    finalDownloadedBytes,
                                    totalBytes));
                        }
                    } else {
                        long finalDownloadedBytes = downloadedBytes;
                        runOnUiThread(() -> updateProgressViewWithoutTotal(
                                progressView,
                                progressTextView,
                                finalDownloadedBytes));
                    }
                }
                outputStream.flush();
            }

            if (updateDownloadCancelRequested) {
                throw new DownloadCanceledException();
            }

            verifyDownloadedApk(targetFile);
            UpdatePackageInstaller.persistDownloadedFile(this, targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                launchPackageInstaller(targetFile);
            });
        } catch (DownloadCanceledException ignored) {
            safeDeleteFile(targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showStartupDialogIdleState(primaryButton, cancelButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                showToast(R.string.about_update_download_canceled);
            });
        } catch (UntrustedUpdateException ignored) {
            safeDeleteFile(targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showStartupDialogIdleState(primaryButton, cancelButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                showToast(R.string.about_update_download_untrusted);
            });
        } catch (Exception ignored) {
            safeDeleteFile(targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showStartupDialogIdleState(primaryButton, cancelButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                showToast(R.string.about_update_download_failed);
            });
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            activeDownloadConnection = null;
            activeDownloadFuture = null;
            updateDownloadInProgress = false;
            updateDownloadCancelRequested = false;
        }
    }

    private void cancelActiveUpdateDownload() {
        if (!updateDownloadInProgress) {
            return;
        }
        updateDownloadCancelRequested = true;
        HttpURLConnection connection = activeDownloadConnection;
        if (connection != null) {
            connection.disconnect();
        }
        Future<?> future = activeDownloadFuture;
        if (future != null) {
            future.cancel(true);
        }
    }

    private void showStartupDialogIdleState(MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        primaryButton.setEnabled(true);
        primaryButton.setText(R.string.about_update_action_download);
        cancelButton.setText(R.string.about_update_action_cancel_dialog);
        progressView.setVisibility(View.GONE);
        progressTextView.setVisibility(View.GONE);
    }

    private void showStartupDialogDownloadingState(MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        primaryButton.setEnabled(false);
        cancelButton.setText(R.string.about_update_action_cancel_download);
        progressView.setVisibility(View.VISIBLE);
        progressTextView.setVisibility(View.VISIBLE);
        progressView.setIndeterminate(true);
        progressTextView.setText(R.string.about_update_download_progress_preparing);
    }

    private void prepareProgressView(LinearProgressIndicator progressView,
            MaterialTextView progressTextView,
            long totalBytes) {
        if (totalBytes > 0L) {
            progressView.setIndeterminate(false);
            progressView.setProgress(0);
            updateProgressView(progressView, progressTextView, 0, 0L, totalBytes);
            return;
        }
        progressView.setIndeterminate(true);
        updateProgressViewWithoutTotal(progressView, progressTextView, 0L);
    }

    private void updateProgressView(LinearProgressIndicator progressView,
            MaterialTextView progressTextView,
            int progress,
            long downloadedBytes,
            long totalBytes) {
        if (progressView.isIndeterminate()) {
            progressView.setIndeterminate(false);
        }
        progressView.setProgress(progress);
        progressTextView.setText(getString(
                R.string.about_update_download_progress_with_percent,
                progress,
                formatBytes(downloadedBytes),
                formatBytes(totalBytes)));
    }

    private void updateProgressViewWithoutTotal(LinearProgressIndicator progressView,
            MaterialTextView progressTextView,
            long downloadedBytes) {
        progressView.setIndeterminate(true);
        progressTextView.setText(getString(
                R.string.about_update_download_progress_without_total,
                formatBytes(downloadedBytes)));
    }

    private void verifyDownloadedApk(File apkFile) throws UntrustedUpdateException {
        PackageManager packageManager = getPackageManager();
        PackageInfo downloadedPackage = readArchivePackageInfo(packageManager, apkFile);
        if (downloadedPackage == null
                || downloadedPackage.packageName == null
                || !getPackageName().equals(downloadedPackage.packageName)) {
            throw new UntrustedUpdateException();
        }

        PackageInfo installedPackage;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                installedPackage = packageManager.getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                installedPackage = packageManager.getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNATURES);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new UntrustedUpdateException();
        }

        Set<String> downloadedSignatures = extractSigningFingerprints(downloadedPackage);
        Set<String> installedSignatures = extractSigningFingerprints(installedPackage);
        if (downloadedSignatures.isEmpty()
                || installedSignatures.isEmpty()
                || !downloadedSignatures.equals(installedSignatures)) {
            throw new UntrustedUpdateException();
        }
    }

    private static PackageInfo readArchivePackageInfo(PackageManager packageManager, File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageManager.getPackageArchiveInfo(
                    apkFile.getAbsolutePath(),
                    PackageManager.GET_SIGNING_CERTIFICATES);
        }
        return packageManager.getPackageArchiveInfo(
                apkFile.getAbsolutePath(),
                PackageManager.GET_SIGNATURES);
    }

    private static Set<String> extractSigningFingerprints(PackageInfo packageInfo) {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo == null) {
                return new HashSet<>();
            }
            signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();
        } else {
            signatures = packageInfo.signatures;
        }
        return signaturesToFingerprints(signatures);
    }

    private static Set<String> signaturesToFingerprints(Signature[] signatures) {
        Set<String> fingerprints = new HashSet<>();
        if (signatures == null) {
            return fingerprints;
        }
        for (Signature signature : signatures) {
            if (signature == null) {
                continue;
            }
            fingerprints.add(toSha256Hex(signature.toByteArray()));
        }
        return fingerprints;
    }

    private static String toSha256Hex(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(Character.forDigit((item >> 4) & 0xF, 16));
            builder.append(Character.forDigit(item & 0xF, 16));
        }
        return builder.toString();
    }

    private void launchPackageInstaller(File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            Intent settingsIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivity(settingsIntent);
            showToast(R.string.about_update_install_permission_required);
            return;
        }
        try {
            Uri contentUri = UpdatePackageInstaller.getInstallUri(this, apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, UpdatePackageInstaller.APK_MIME_TYPE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(installIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException ignored) {
            showToast(R.string.about_update_install_failed);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = { "KB", "MB", "GB", "TB" };
        int unitIndex = -1;
        do {
            value /= 1024.0;
            unitIndex++;
        } while (value >= 1024.0 && unitIndex < units.length - 1);
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex]);
    }

    private static void safeDeleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        // noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static final class DownloadCanceledException extends IOException {
    }

    private static final class UntrustedUpdateException extends IOException {
    }

    private SharedPreferences getUpdatePromptPrefs() {
        return getSharedPreferences(UPDATE_PROMPT_PREFS, Context.MODE_PRIVATE);
    }

    private long getLastUpdateCheckTimestamp() {
        return getUpdatePromptPrefs().getLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, 0L);
    }

    private boolean wasLastUpdateCheckFailed() {
        return getUpdatePromptPrefs().getBoolean(KEY_LAST_UPDATE_CHECK_FAILED, false);
    }

    private void setLastUpdateCheckTimestamp(long timestamp) {
        getUpdatePromptPrefs()
                .edit()
                .putLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, timestamp)
                .apply();
    }

    private void setLastUpdateCheckFailed(boolean failed) {
        getUpdatePromptPrefs()
                .edit()
                .putBoolean(KEY_LAST_UPDATE_CHECK_FAILED, failed)
                .apply();
    }

    private int getLastPromptedUpdateVersionCode() {
        return getUpdatePromptPrefs().getInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, 0);
    }

    private void setLastPromptedUpdateVersionCode(int versionCode) {
        getUpdatePromptPrefs()
                .edit()
                .putInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, versionCode)
                .apply();
    }

    private static StartupUpdateManifest fetchUpdateManifest(String manifestUrl)
            throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(manifestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(UPDATE_CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(UPDATE_READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }
            String body = readUtf8(connection.getInputStream());
            JSONObject object = new JSONObject(body);
            String versionName = object.optString("version", "").trim();
            int versionCode = object.optInt("versionCode", 0);
            String apkUrl = object.optString("apkUrl", "").trim();
            String releasePage = object.optString("releasePage", "").trim();
            if (versionName.isEmpty() || versionCode <= 0) {
                throw new IOException("Invalid update manifest payload");
            }
            return new StartupUpdateManifest(versionName, versionCode, apkUrl, releasePage);
        } finally {
            connection.disconnect();
        }
    }

    private static String readUtf8(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    private static boolean isRemoteVersionNewer(int remoteCode,
            String remoteName,
            int localCode,
            String localName) {
        if (remoteCode > localCode) {
            return true;
        }
        if (remoteCode < localCode) {
            return false;
        }
        return compareSemVer(remoteName, localName) > 0;
    }

    private static int compareSemVer(String left, String right) {
        int[] leftParts = parseSemVer(left);
        int[] rightParts = parseSemVer(right);
        if (leftParts == null || rightParts == null) {
            return 0;
        }
        for (int i = 0; i < leftParts.length; i++) {
            if (leftParts[i] == rightParts[i]) {
                continue;
            }
            return leftParts[i] > rightParts[i] ? 1 : -1;
        }
        return 0;
    }

    private static int[] parseSemVer(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        String[] segments = normalized.split("\\.");
        if (segments.length < 3) {
            return null;
        }

        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            Matcher matcher = LEADING_NUMBER_PATTERN.matcher(segments[i]);
            if (!matcher.find()) {
                return null;
            }
            try {
                result[i] = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private void showStartupDisclaimerDialog(DpiConfigStore store, Runnable onAccepted) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_startup_disclaimer, null, false);
        MaterialCheckBox agreementCheckBox = dialogView.findViewById(R.id.startup_disclaimer_checkbox);
        MaterialButton acceptButton = dialogView.findViewById(R.id.startup_disclaimer_accept_button);
        MaterialButton exitButton = dialogView.findViewById(R.id.startup_disclaimer_exit_button);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((unused, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP);

        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> acceptButton.setEnabled(isChecked));
        acceptButton.setOnClickListener(v -> {
            if (!agreementCheckBox.isChecked()) {
                return;
            }
            if (!store.setStartupDisclaimerAccepted(true)) {
                showToast(R.string.startup_disclaimer_save_failed);
                return;
            }
            dialog.dismiss();
            if (onAccepted != null) {
                onAccepted.run();
            }
        });
        exitButton.setOnClickListener(v -> finish());
        dialog.show();
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
        AppConfigDialogActionStyle style = resolveDialogActionStyle(views.scopeButton);
        refreshDialogState(views, state, style, systemHooksEnabled);
        bindDialogValidation(dialogView, views, state, style, systemHooksEnabled);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setHalfExpandedRatio(0.5f);
        dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);
        dialog.setOnShowListener(d -> {
            int surfaceColor = MaterialColors.getColor(
                    dialogView, com.google.android.material.R.attr.colorSurface);
            dialog.getWindow().setNavigationBarColor(surfaceColor);

            // When keyboard opens in half-expanded, expand the sheet; restore on close
            android.widget.FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (view, insets) -> {
                    boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                    com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior
                            .from(view);
                    if (keyboardVisible) {
                        if (behavior
                                .getState() == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED) {
                            behavior.setState(
                                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                        }
                    } else {
                        if (behavior
                                .getState() == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {
                            behavior.setState(
                                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED);
                        }
                    }
                    return insets;
                });
            }
        });
        dialog.show();

        // Refine half-expanded ratio after layout so the advanced section is hidden
        View expandAnchor = dialogView.findViewById(R.id.dialog_expand_anchor);
        if (expandAnchor != null) {
            expandAnchor.post(() -> {
                android.widget.FrameLayout bottomSheet = dialog.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet == null)
                    return;
                View parent = (View) bottomSheet.getParent();
                int parentHeight = parent.getHeight();
                if (parentHeight <= 0)
                    return;

                com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior
                        .from(bottomSheet);

                // Set half-expanded ratio so the advanced section is hidden below the fold
                int[] anchorPos = new int[2];
                expandAnchor.getLocationOnScreen(anchorPos);
                int anchorBottom = anchorPos[1] + expandAnchor.getHeight();
                int[] sheetPos = new int[2];
                bottomSheet.getLocationOnScreen(sheetPos);
                float ratio = (float) (anchorBottom - sheetPos[1]) / parentHeight;
                // With fitToContents=true, ratio must be < contentHeight/parentHeight
                int contentHeight = dialogView.getHeight();
                float maxRatio = (float) contentHeight / parentHeight - 0.05f;
                ratio = Math.min(ratio, maxRatio);
                ratio = Math.min(Math.max(ratio, 0.3f), 0.75f);
                behavior.setHalfExpandedRatio(ratio);
            });
        }
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
                ? String.valueOf(item.viewportWidthDp)
                : "");
        views.fontInputView.setText(item.fontScalePercent != null
                ? String.valueOf(item.fontScalePercent)
                : "");
        bindViewportModeToggle(views.viewportModeToggle, item.viewportMode, false);
        bindFontModeToggle(views.fontModeToggle, item.fontMode, false);
        updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                views.fontInputLayout, views.fontInputView, views.saveButton);
        return new AppConfigDialogState(item.inScope, item.dpisEnabled);
    }

    private AppConfigDialogActionStyle resolveDialogActionStyle(MaterialButton baseButton) {
        ColorStateList defaultActionBgTint = baseButton.getBackgroundTintList();
        int defaultActionStrokeWidth = baseButton.getStrokeWidth();
        int defaultActionTextColor = MaterialColors.getColor(
                baseButton, androidx.appcompat.R.attr.colorPrimary);
        return new AppConfigDialogActionStyle(defaultActionBgTint,
                defaultActionStrokeWidth, defaultActionTextColor);
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
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor);
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
        dialogView.setOnClickListener(
                v -> clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView));
        views.scopeButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleScope(item.packageName, item.label, state.scopeSelected,
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
            bindViewportModeToggle(views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE, true);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.saveButton.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            int[] result = saveAppConfig(item, views.viewportInputView, views.fontInputView,
                    views.viewportModeToggle, views.fontModeToggle);
            if (result[0] == 1) {
                showSaveButtonFeedback(views.saveButton);
            }
            if (result[1] != 0) {
                showToast(result[1]);
            }
        });
        views.viewportModeToggle.container.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleViewportMode(views.viewportModeToggle);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindViewportModeToggle(
                    views.viewportModeToggle, ViewportApplyMode.SYSTEM_EMULATION, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindViewportModeToggle(
                    views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.container.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleFontMode(views.fontModeToggle);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.emulationLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.replaceLabel.setOnClickListener(v -> {
            clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
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

        AppConfigDialogActionStyle(ColorStateList defaultActionBgTint,
                int defaultActionStrokeWidth,
                int defaultActionTextColor) {
            this.defaultActionBgTint = defaultActionBgTint;
            this.defaultActionStrokeWidth = defaultActionStrokeWidth;
            this.defaultActionTextColor = defaultActionTextColor;
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
