package com.dpis.module.templates;

import com.dpis.module.DpisConfigStore;


import com.dpis.module.*;


import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.applist.InstalledAppCatalogItem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public final class QuickTemplateTargetsBinder {
    public interface Host {
        PackageManager getPackageManager();

        String getSelfPackageName();

        void runOnUiThread(Runnable runnable);

        View getIconRefreshAnchor();

        void onSaved();

        void onMissingTemplate();

        void showToast(int messageResId);
    }

    private static final String FILTER_PREFS_NAME = "quick_template_target_filters";
    private static final String KEY_FILTER_SHOW_SYSTEM_APPS = "show_system_apps";
    private static final String KEY_FILTER_HIDE_CONFIGURED_APPS = "hide_configured_apps";
    private static final long INSTALLED_APP_CATALOG_TTL_MS = 60_000L;
    private static final int FIRST_SCREEN_ICON_WARMUP_LIMIT = 48;
    private static final long ICON_REFRESH_DEBOUNCE_MS = 120L;

    private final Activity activity;
    private final View rootView;
    private final Host host;
    private final QuickTemplateStore quickTemplateStore;
    private final SharedPreferences filterPreferences;
    private final PackageConfigRepository packageConfigRepository;
    private final ExecutorService appLoadExecutor = Executors.newSingleThreadExecutor();
    private final ArrayList<TargetAppItem> allTargetItems = new ArrayList<>();
    private final LinkedHashSet<String> selectedPackages = new LinkedHashSet<>();
    private final InstalledAppCatalogCoordinator installedAppCatalogCoordinator;

    private QuickTemplateStore.QuickTemplate template;
    private QuickTemplateTargetAdapter adapter;
    private MaterialTextView subtitleView;
    private MaterialTextView emptyView;
    private AppCompatEditText searchInput;
    private ImageButton searchClearButton;
    private boolean filterShowSystemApps;
    private boolean filterHideConfiguredApps;
    private int appLoadRequestId;
    private boolean disposed;

    public QuickTemplateTargetsBinder(Activity activity, View rootView, Host host) {
        this.activity = activity;
        this.rootView = rootView;
        this.host = host;
        SharedPreferences preferences = activity.getSharedPreferences(
                DpisConfigStore.GROUP, Activity.MODE_PRIVATE);
        this.quickTemplateStore = new QuickTemplateStore(preferences);
        this.filterPreferences = activity.getSharedPreferences(
                FILTER_PREFS_NAME, Activity.MODE_PRIVATE);
        this.packageConfigRepository = new PackageConfigRepository(
                DpisApplication.getActiveHookConfigStore(activity));
        this.filterShowSystemApps = filterPreferences.getBoolean(
                KEY_FILTER_SHOW_SYSTEM_APPS, false);
        this.filterHideConfiguredApps = filterPreferences.getBoolean(
                KEY_FILTER_HIDE_CONFIGURED_APPS, false);
        this.installedAppCatalogCoordinator = new InstalledAppCatalogCoordinator(
                createInstalledAppCatalogHost(),
                INSTALLED_APP_CATALOG_TTL_MS,
                FIRST_SCREEN_ICON_WARMUP_LIMIT,
                ICON_REFRESH_DEBOUNCE_MS);
    }

    public boolean bind(String templateId) {
        template = quickTemplateStore.read(templateId);
        if (template == null) {
            host.showToast(R.string.quick_template_target_missing);
            host.onMissingTemplate();
            return false;
        }
        selectedPackages.clear();
        selectedPackages.addAll(template.selectedPackages);
        bindViews();
        bindList();
        loadTargetApps();
        return true;
    }

    public void dispose() {
        disposed = true;
        appLoadExecutor.shutdownNow();
        installedAppCatalogCoordinator.shutdown();
    }

    private void bindViews() {
        subtitleView = rootView.findViewById(R.id.quick_template_targets_subtitle);
        emptyView = rootView.findViewById(R.id.quick_template_targets_empty);
        searchInput = rootView.findViewById(R.id.quick_template_targets_search_input);
        searchClearButton = rootView.findViewById(R.id.quick_template_targets_search_clear_button);
        View saveButton = rootView.findViewById(R.id.quick_template_targets_save_button);
        MaterialTextView titleView = rootView.findViewById(R.id.quick_template_targets_title);
        titleView.setText(activity.getString(R.string.quick_template_targets_title, template.name));
        refreshSelectedCount();
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
        ImageButton filterButton = rootView.findViewById(
                R.id.quick_template_targets_filter_button);
        TouchFeedbackBinder.bindPressHaptic(filterButton);
        filterButton.setOnClickListener(v -> showFilterSheet());
    }

    private void bindList() {
        RecyclerView list = rootView.findViewById(R.id.quick_template_targets_list);
        adapter = new QuickTemplateTargetAdapter(
                selectedPackages,
                this::onSelectionChanged,
                this::onIconLoadRequested);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);
    }

    private void loadTargetApps() {
        if (disposed) {
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
            host.runOnUiThread(() -> onTargetAppsLoaded(requestId, finalLoaded));
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
                    packageConfigRepository.hasRealPackageConfig(item.packageName),
                    item.systemApp,
                    item.icon));
        }
        return loaded;
    }

    private void onTargetAppsLoaded(int requestId, List<TargetAppItem> loaded) {
        if (disposed || requestId != appLoadRequestId) {
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
        subtitleView.setText(activity.getString(
                R.string.quick_template_targets_selected_count,
                selectedPackages.size()));
    }

    private void saveSelection() {
        if (quickTemplateStore.setSelectedPackages(template.id, selectedPackages)) {
            host.showToast(R.string.quick_template_targets_save_success);
            host.onSaved();
            return;
        }
        host.showToast(R.string.quick_template_targets_save_failed);
    }

    private void showFilterSheet() {
        ViewGroup root = rootView.findViewById(android.R.id.content);
        if (root == null) {
            root = activity.findViewById(android.R.id.content);
        }
        View dialogView = LayoutInflater.from(activity).inflate(
                R.layout.dialog_quick_template_target_filters,
                root,
                false);
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
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

    private InstalledAppCatalogCoordinator.Host createInstalledAppCatalogHost() {
        return new InstalledAppCatalogCoordinator.Host() {
            @Override
            public PackageManager getPackageManager() {
                return host.getPackageManager();
            }

            @Override
            public String getSelfPackageName() {
                return host.getSelfPackageName();
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                host.runOnUiThread(runnable);
            }

            @Override
            public View getIconRefreshAnchor() {
                return host.getIconRefreshAnchor();
            }

            @Override
            public void requestAppsLoad() {
                QuickTemplateTargetsBinder.this.loadTargetApps();
            }
        };
    }

    private static String textOf(AppCompatEditText view) {
        return view.getText() != null ? view.getText().toString() : "";
    }

    public static boolean matchesTargetFilters(TargetAppItem item,
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

    public static LinkedHashSet<String> pruneSelectedPackagesToInstalledApps(
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

    public static final class TargetAppItem {
        public final String label;
        public final String packageName;
        public final boolean configured;
        public final boolean systemApp;
        public final Drawable icon;

        public TargetAppItem(String label, String packageName, boolean configured) {
            this(label, packageName, configured, false, null);
        }

        public TargetAppItem(String label,
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
