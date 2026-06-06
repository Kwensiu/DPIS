package com.dpis.module;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QuickTemplateTargetSelectionActivity extends LocalizedActivity {
    static final String EXTRA_TEMPLATE_ID = "quick_template_targets.template_id";
    private static final String FILTER_PREFS_NAME = "quick_template_target_filters";
    private static final String KEY_FILTER_SHOW_SYSTEM_APPS = "show_system_apps";
    private static final String KEY_FILTER_HIDE_CONFIGURED_APPS = "hide_configured_apps";
    private static final long INSTALLED_APP_CATALOG_TTL_MS = 60_000L;
    private static final int FIRST_SCREEN_ICON_WARMUP_LIMIT = 48;
    private static final long ICON_REFRESH_DEBOUNCE_MS = 120L;

    private QuickTemplateStore quickTemplateStore;
    private SharedPreferences filterPreferences;
    private DpiConfigStore configStore;
    private InstalledAppCatalogCoordinator installedAppCatalogCoordinator;
    private final ExecutorService appLoadExecutor = Executors.newSingleThreadExecutor();
    private QuickTemplateStore.QuickTemplate template;
    private QuickTemplateTargetAdapter adapter;
    private MaterialTextView subtitleView;
    private MaterialTextView emptyView;
    private AppCompatEditText searchInput;
    private ImageButton searchClearButton;
    private MaterialButton saveButton;
    private View toolbar;
    private boolean filterShowSystemApps;
    private boolean filterHideConfiguredApps;
    private int appLoadRequestId;
    private boolean destroyed;

    private final ArrayList<TargetAppItem> allTargetItems = new ArrayList<>();
    private final LinkedHashSet<String> selectedPackages = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_template_targets);
        SharedPreferences preferences = getSharedPreferences(DpiConfigStore.GROUP, MODE_PRIVATE);
        quickTemplateStore = new QuickTemplateStore(preferences);
        filterPreferences = getSharedPreferences(FILTER_PREFS_NAME, MODE_PRIVATE);
        filterShowSystemApps = filterPreferences.getBoolean(KEY_FILTER_SHOW_SYSTEM_APPS, false);
        filterHideConfiguredApps = filterPreferences.getBoolean(
                KEY_FILTER_HIDE_CONFIGURED_APPS,
                false);
        configStore = getHookConfigStore();
        installedAppCatalogCoordinator = new InstalledAppCatalogCoordinator(
                createInstalledAppCatalogHost(),
                INSTALLED_APP_CATALOG_TTL_MS,
                FIRST_SCREEN_ICON_WARMUP_LIMIT,
                ICON_REFRESH_DEBOUNCE_MS);
        String templateId = getIntent() != null
                ? getIntent().getStringExtra(EXTRA_TEMPLATE_ID)
                : null;
        template = quickTemplateStore.read(templateId);
        if (template == null) {
            showToast(R.string.quick_template_target_missing);
            finish();
            return;
        }
        selectedPackages.addAll(template.selectedPackages);
        bindViews();
        bindToolbar();
        applyInsets();
        bindList();
        loadTargetApps();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        appLoadExecutor.shutdownNow();
        if (installedAppCatalogCoordinator != null) {
            installedAppCatalogCoordinator.shutdown();
        }
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.quick_template_targets_toolbar);
        subtitleView = findViewById(R.id.quick_template_targets_subtitle);
        emptyView = findViewById(R.id.quick_template_targets_empty);
        searchInput = findViewById(R.id.quick_template_targets_search_input);
        searchClearButton = findViewById(R.id.quick_template_targets_search_clear_button);
        saveButton = findViewById(R.id.quick_template_targets_save_button);
        MaterialTextView titleView = findViewById(R.id.quick_template_targets_title);
        titleView.setText(getString(R.string.quick_template_targets_title, template.name));
        subtitleView.setText(getString(
                R.string.quick_template_targets_selected_count,
                selectedPackages.size()));
        TouchFeedbackBinder.bindPressHaptic(saveButton);
        saveButton.setOnClickListener(v -> saveSelection());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchClearButton.setVisibility(
                        s == null || s.length() == 0 ? View.GONE : View.VISIBLE);
                applyTargetFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchClearButton.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
        });
        ImageButton filterButton = findViewById(R.id.quick_template_targets_filter_button);
        TouchFeedbackBinder.bindPressHaptic(filterButton);
        filterButton.setOnClickListener(v -> showFilterSheet());
    }

    private void bindToolbar() {
        AppCompatImageButton backButton = findViewById(R.id.quick_template_targets_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void applyInsets() {
        final int baseTopPadding = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        RecyclerView list = findViewById(R.id.quick_template_targets_list);
        final int baseBottomPadding = list.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(list, (view, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), baseBottomPadding + navigationBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
        ViewCompat.requestApplyInsets(list);
    }

    private void bindList() {
        RecyclerView list = findViewById(R.id.quick_template_targets_list);
        adapter = new QuickTemplateTargetAdapter(
                selectedPackages,
                this::onSelectionChanged,
                this::onIconLoadRequested);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    private void loadTargetApps() {
        if (destroyed) {
            return;
        }
        int requestId = ++appLoadRequestId;
        appLoadExecutor.execute(() -> {
            List<TargetAppItem> loaded = null;
            try {
                loaded = buildTargetItems();
            } catch (Throwable throwable) {
                DpisLog.e("quick template target list load failed", throwable);
            }
            List<TargetAppItem> finalLoaded = loaded;
            runOnUiThread(() -> onTargetAppsLoaded(requestId, finalLoaded));
        });
    }

    private List<TargetAppItem> buildTargetItems() {
        ArrayList<TargetAppItem> loaded = new ArrayList<>();
        List<InstalledAppCatalogItem> catalog =
                installedAppCatalogCoordinator.loadInstalledAppCatalog(false);
        for (InstalledAppCatalogItem item : catalog) {
            loaded.add(new TargetAppItem(
                    item.label,
                    item.packageName,
                    configStore.hasRealPackageConfig(item.packageName),
                    item.systemApp,
                    item.icon));
        }
        return loaded;
    }

    private void onTargetAppsLoaded(int requestId, List<TargetAppItem> loaded) {
        if (destroyed || requestId != appLoadRequestId) {
            return;
        }
        allTargetItems.clear();
        if (loaded != null) {
            allTargetItems.addAll(loaded);
        }
        pruneSelectedPackagesToInstalledApps(selectedPackages, allTargetItems);
        refreshSelectedCount();
        applyTargetFilters();
    }

    private void onIconLoadRequested(String packageName) {
        installedAppCatalogCoordinator.onIconLoadRequested(packageName);
    }

    private void applyTargetFilters() {
        String query = textOf(searchInput).trim().toLowerCase(Locale.ROOT);
        ArrayList<TargetAppItem> filtered = new ArrayList<>();
        for (TargetAppItem item : allTargetItems) {
            if (matchesTargetFilters(
                    item,
                    query,
                    filterShowSystemApps,
                    filterHideConfiguredApps)) {
                filtered.add(item);
            }
        }
        adapter.submit(filtered);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onSelectionChanged(String packageName, boolean selected) {
        if (selected) {
            selectedPackages.add(packageName);
        } else {
            selectedPackages.remove(packageName);
        }
        refreshSelectedCount();
    }

    private void refreshSelectedCount() {
        subtitleView.setText(getString(
                R.string.quick_template_targets_selected_count,
                selectedPackages.size()));
    }

    private void saveSelection() {
        if (quickTemplateStore.setSelectedPackages(template.id, selectedPackages)) {
            showToast(R.string.quick_template_targets_save_success);
            finish();
            return;
        }
        showToast(R.string.quick_template_targets_save_failed);
    }

    private void showFilterSheet() {
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_quick_template_target_filters,
                root,
                false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        MaterialSwitch showSystemSwitch = dialogView.findViewById(
                R.id.quick_template_targets_filter_show_system_switch);
        MaterialSwitch hideConfiguredSwitch = dialogView.findViewById(
                R.id.quick_template_targets_filter_hide_configured_switch);
        showSystemSwitch.setChecked(filterShowSystemApps);
        hideConfiguredSwitch.setChecked(filterHideConfiguredApps);

        android.widget.CompoundButton.OnCheckedChangeListener listener = (
                buttonView,
                isChecked) -> {
            filterShowSystemApps = showSystemSwitch.isChecked();
            filterHideConfiguredApps = hideConfiguredSwitch.isChecked();
            filterPreferences.edit()
                    .putBoolean(KEY_FILTER_SHOW_SYSTEM_APPS, filterShowSystemApps)
                    .putBoolean(KEY_FILTER_HIDE_CONFIGURED_APPS, filterHideConfiguredApps)
                    .apply();
            applyTargetFilters();
        };
        showSystemSwitch.setOnCheckedChangeListener(listener);
        hideConfiguredSwitch.setOnCheckedChangeListener(listener);
        dialog.show();
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private DpiConfigStore getHookConfigStore() {
        return DpisApplication.getActiveHookConfigStore(this);
    }

    private InstalledAppCatalogCoordinator.Host createInstalledAppCatalogHost() {
        return new InstalledAppCatalogCoordinator.Host() {
            @Override
            public PackageManager getPackageManager() {
                return QuickTemplateTargetSelectionActivity.this.getPackageManager();
            }

            @Override
            public String getSelfPackageName() {
                return QuickTemplateTargetSelectionActivity.this.getPackageName();
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                QuickTemplateTargetSelectionActivity.this.runOnUiThread(runnable);
            }

            @Override
            public View getIconRefreshAnchor() {
                return findViewById(R.id.quick_template_targets_list);
            }

            @Override
            public void requestAppsLoad() {
                QuickTemplateTargetSelectionActivity.this.loadTargetApps();
            }
        };
    }

    private static String textOf(AppCompatEditText view) {
        return view.getText() != null ? view.getText().toString() : "";
    }

    static boolean matchesTargetFilters(TargetAppItem item,
            String normalizedQuery,
            boolean showSystemApps,
            boolean hideConfiguredApps) {
        if (item == null) {
            return false;
        }
        if (!showSystemApps && item.systemApp) {
            return false;
        }
        if (hideConfiguredApps && item.configured) {
            return false;
        }
        return matchesQuery(item, normalizedQuery);
    }

    private static boolean matchesQuery(TargetAppItem item, String normalizedQuery) {
        String query = normalizedQuery != null ? normalizedQuery : "";
        return query.isEmpty()
                || item.label.toLowerCase(Locale.ROOT).contains(query)
                || item.packageName.toLowerCase(Locale.ROOT).contains(query);
    }

    static LinkedHashSet<String> pruneSelectedPackagesToInstalledApps(
            Set<String> selectedPackages,
            List<TargetAppItem> installedItems) {
        LinkedHashSet<String> installedPackages = new LinkedHashSet<>();
        if (installedItems != null) {
            for (TargetAppItem item : installedItems) {
                if (item != null && item.packageName != null && !item.packageName.isBlank()) {
                    installedPackages.add(item.packageName.trim());
                }
            }
        }
        LinkedHashSet<String> pruned = new LinkedHashSet<>();
        if (selectedPackages != null) {
            for (String packageName : selectedPackages) {
                if (packageName == null) {
                    continue;
                }
                String trimmed = packageName.trim();
                if (installedPackages.contains(trimmed)) {
                    pruned.add(trimmed);
                }
            }
            selectedPackages.clear();
            selectedPackages.addAll(pruned);
        }
        return pruned;
    }

    static final class TargetAppItem {
        final String label;
        final String packageName;
        final boolean configured;
        final boolean systemApp;
        final Drawable icon;

        TargetAppItem(String label, String packageName, boolean configured) {
            this(label, packageName, configured, false, null);
        }

        TargetAppItem(String label,
                String packageName,
                boolean configured,
                boolean systemApp,
                Drawable icon) {
            this.label = label != null ? label : packageName;
            this.packageName = packageName;
            this.configured = configured;
            this.systemApp = systemApp;
            this.icon = icon;
        }
    }
}
