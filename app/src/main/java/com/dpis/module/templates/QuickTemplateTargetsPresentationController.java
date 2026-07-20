package com.dpis.module.templates;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.dpis.module.DpisApplication;
import com.dpis.module.DpisConfigStore;
import com.dpis.module.DpisLog;
import com.dpis.module.R;
import com.dpis.module.applist.InstalledAppCatalogCoordinator;
import com.dpis.module.applist.InstalledAppCatalogItem;
import com.dpis.module.config.PackageConfigRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Presentation state for quick-template target selection, shared by portrait and landscape UI.
 * Selection is intentionally draft-only until {@link #save()} succeeds.
 */
public final class QuickTemplateTargetsPresentationController {
    public interface Listener { void onStateChanged(State state); }

    public static final class TargetApp {
        public final String label;
        public final String packageName;
        public final boolean configured;
        public final boolean systemApp;
        public final Drawable icon;
        public final boolean selected;

        TargetApp(String label, String packageName, boolean configured, boolean systemApp,
                Drawable icon, boolean selected) {
            this.label = label;
            this.packageName = packageName;
            this.configured = configured;
            this.systemApp = systemApp;
            this.icon = icon;
            this.selected = selected;
        }
    }

    public static final class State {
        public final String templateId;
        public final String templateName;
        public final List<TargetApp> apps;
        public final int selectedCount;
        public final String query;
        public final boolean showSystemApps;
        public final boolean hideConfiguredApps;
        public final boolean loading;
        public final boolean missingTemplate;

        State(String templateId, String templateName, List<TargetApp> apps, int selectedCount,
                String query, boolean showSystemApps, boolean hideConfiguredApps, boolean loading,
                boolean missingTemplate) {
            this.templateId = templateId;
            this.templateName = templateName;
            this.apps = apps;
            this.selectedCount = selectedCount;
            this.query = query;
            this.showSystemApps = showSystemApps;
            this.hideConfiguredApps = hideConfiguredApps;
            this.loading = loading;
            this.missingTemplate = missingTemplate;
        }
    }

    public static final class SaveResult {
        public final boolean success;
        public final int messageResId;
        SaveResult(boolean success, int messageResId) {
            this.success = success;
            this.messageResId = messageResId;
        }
    }

    private static final String FILTER_PREFS_NAME = "quick_template_target_filters";
    private static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";
    private static final String KEY_HIDE_CONFIGURED_APPS = "hide_configured_apps";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final QuickTemplateStore templates;
    private final SharedPreferences filters;
    private final PackageConfigRepository packageConfigs;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final InstalledAppCatalogCoordinator catalog;
    private final Set<Listener> listeners = new LinkedHashSet<>();
    private final List<RawTargetApp> allApps = new ArrayList<>();
    private final LinkedHashSet<String> selectedPackages = new LinkedHashSet<>();

    private String templateId;
    private String templateName;
    private String query = "";
    private boolean showSystemApps;
    private boolean hideConfiguredApps;
    private boolean loading;
    private boolean missingTemplate;
    private int requestId;
    private boolean disposed;

    public QuickTemplateTargetsPresentationController(Context context) {
        this.context = context;
        SharedPreferences preferences = context.getSharedPreferences(
                DpisConfigStore.GROUP, Context.MODE_PRIVATE);
        templates = new QuickTemplateStore(preferences);
        filters = context.getSharedPreferences(FILTER_PREFS_NAME, Context.MODE_PRIVATE);
        packageConfigs = new PackageConfigRepository(DpisApplication.getActiveHookConfigStore(context));
        showSystemApps = filters.getBoolean(KEY_SHOW_SYSTEM_APPS, false);
        hideConfiguredApps = filters.getBoolean(KEY_HIDE_CONFIGURED_APPS, false);
        catalog = new InstalledAppCatalogCoordinator(new InstalledAppCatalogCoordinator.Host() {
            @Override public PackageManager getPackageManager() { return context.getPackageManager(); }
            @Override public String getSelfPackageName() { return context.getPackageName(); }
            @Override public void runOnUiThread(Runnable runnable) { mainHandler.post(runnable); }
            @Override public android.view.View getIconRefreshAnchor() { return null; }
            @Override public void requestAppsLoad() { reloadApps(); }
            @Override public boolean onIconsLoaded(Map<String, Drawable> icons) {
                if (disposed) {
                    return true;
                }
                if (icons == null || icons.isEmpty()) {
                    return false;
                }
                boolean matched = false;
                boolean changed = false;
                for (int i = 0; i < allApps.size(); i++) {
                    RawTargetApp item = allApps.get(i);
                    Drawable icon = icons.get(item.packageName);
                    if (icon != null) {
                        matched = true;
                        if (icon != item.icon) {
                            allApps.set(i, item.withIcon(icon));
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    publish();
                }
                // A cache refresh is handled incrementally only when it matched an app that is
                // actually present in this presentation. Returning false for an empty/stale
                // presentation lets the catalog coordinator rebuild it once instead of marking
                // the request handled and leaving icons permanently blank.
                return matched;
            }
        }, 60_000L, 48, 120L);
    }

    public void addListener(Listener listener) { if (listener != null) listeners.add(listener); }
    public void removeListener(Listener listener) { listeners.remove(listener); }

    public void load(String id) {
        templateId = id;
        QuickTemplateStore.QuickTemplate template = templates.read(id);
        if (template == null) {
            missingTemplate = true;
            templateName = null;
            selectedPackages.clear();
            loading = false;
            publish();
            return;
        }
        missingTemplate = false;
        templateName = template.name;
        selectedPackages.clear();
        selectedPackages.addAll(template.selectedPackages);
        reloadApps();
    }

    public void setQuery(String value) { query = value != null ? value : ""; publish(); }

    public void setFilters(boolean showSystem, boolean hideConfigured) {
        showSystemApps = showSystem;
        hideConfiguredApps = hideConfigured;
        filters.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, showSystem)
                .putBoolean(KEY_HIDE_CONFIGURED_APPS, hideConfigured).apply();
        publish();
    }

    public void toggleSelection(String packageName, boolean selected) {
        if (selected) selectedPackages.add(packageName); else selectedPackages.remove(packageName);
        publish();
    }

    public SaveResult save() {
        boolean saved = templateId != null && templates.setSelectedPackages(templateId, selectedPackages);
        return new SaveResult(saved, saved ? R.string.quick_template_targets_save_success
                : R.string.quick_template_targets_save_failed);
    }

    public void onIconVisible(String packageName) { catalog.onIconLoadRequested(packageName); }

    public void dispose() { disposed = true; loader.shutdownNow(); catalog.shutdown(); }

    private void reloadApps() {
        if (disposed || missingTemplate) return;
        int currentRequest = ++requestId;
        loading = true;
        publish();
        loader.execute(() -> {
            List<RawTargetApp> loaded = null;
            try {
                List<RawTargetApp> next = new ArrayList<>();
                for (InstalledAppCatalogItem item : catalog.loadInstalledAppCatalog(false)) {
                    next.add(new RawTargetApp(item.label, item.packageName,
                            packageConfigs.hasRealPackageConfig(item.packageName), item.systemApp,
                            item.icon));
                }
                loaded = next;
            } catch (Throwable throwable) {
                // Loading failure is a presentation state, not a reason to strand the sheet in
                // its progress state. Keep the last complete list and converge loading below.
                DpisLog.e("quick template target presentation load failed", throwable);
            }
            List<RawTargetApp> finalLoaded = loaded;
            mainHandler.post(() -> {
                if (disposed || currentRequest != requestId) return;
                if (finalLoaded != null) {
                    allApps.clear();
                    allApps.addAll(finalLoaded);
                    pruneSelection();
                }
                loading = false;
                publish();
            });
        });
    }

    private void pruneSelection() {
        Set<String> installed = new LinkedHashSet<>();
        for (RawTargetApp item : allApps) installed.add(item.packageName);
        selectedPackages.retainAll(installed);
    }

    private void publish() {
        List<TargetApp> visible = new ArrayList<>();
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        for (RawTargetApp item : allApps) {
            if ((!showSystemApps && item.systemApp) || (hideConfiguredApps && item.configured)) continue;
            if (!normalizedQuery.isEmpty() && !item.label.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    && !item.packageName.toLowerCase(Locale.ROOT).contains(normalizedQuery)) continue;
            visible.add(new TargetApp(item.label, item.packageName, item.configured, item.systemApp,
                    item.icon, selectedPackages.contains(item.packageName)));
        }
        State state = new State(templateId, templateName, visible, selectedPackages.size(), query,
                showSystemApps, hideConfiguredApps, loading, missingTemplate);
        for (Listener listener : new LinkedHashSet<>(listeners)) listener.onStateChanged(state);
    }

    private static final class RawTargetApp {
        final String label; final String packageName; final boolean configured; final boolean systemApp; final Drawable icon;
        RawTargetApp(String label, String packageName, boolean configured, boolean systemApp, Drawable icon) {
            this.label = label; this.packageName = packageName; this.configured = configured;
            this.systemApp = systemApp; this.icon = icon;
        }

        RawTargetApp withIcon(Drawable updatedIcon) {
            return new RawTargetApp(label, packageName, configured, systemApp, updatedIcon);
        }
    }
}
