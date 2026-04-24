package com.dpis.module;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class InstalledAppCatalogCoordinator {
    interface Host {
        PackageManager getPackageManager();

        String getSelfPackageName();

        void runOnUiThread(Runnable runnable);

        View getIconRefreshAnchor();

        void requestAppsLoad();
    }

    private final Object installedAppCatalogLock = new Object();
    private final Object installedAppCatalogBuildLock = new Object();
    private final Object iconWarmupLock = new Object();
    private final Object iconRequestLock = new Object();
    private final AppIconMemoryCache appIconCache = new AppIconMemoryCache(256);
    private final Set<String> pendingOnDemandIconLoads = new HashSet<>();
    private final ExecutorService appIconWarmupExecutor = Executors.newSingleThreadExecutor();

    private final Host host;
    private final long installedAppCatalogTtlMs;
    private final int firstScreenIconWarmupLimit;
    private final long iconRefreshDebounceMs;

    private List<InstalledAppCatalogItem> installedAppCatalog = Collections.emptyList();
    private Map<String, InstalledAppCatalogItem> installedAppCatalogIndex = Collections.emptyMap();
    private long installedAppCatalogLoadedAtMs;
    private boolean firstScreenIconWarmupScheduled;
    private boolean firstScreenIconWarmupFinished;
    private boolean iconRefreshQueued;

    InstalledAppCatalogCoordinator(Host host,
            long installedAppCatalogTtlMs,
            int firstScreenIconWarmupLimit,
            long iconRefreshDebounceMs) {
        this.host = host;
        this.installedAppCatalogTtlMs = installedAppCatalogTtlMs;
        this.firstScreenIconWarmupLimit = firstScreenIconWarmupLimit;
        this.iconRefreshDebounceMs = iconRefreshDebounceMs;
    }

    void shutdown() {
        appIconWarmupExecutor.shutdownNow();
    }

    List<AppListItem> loadInstalledApps(boolean forceInstalledAppCatalogReload,
            DpiConfigStore store,
            Set<String> scopePackages) {
        PackageManager packageManager = host.getPackageManager();
        List<InstalledAppCatalogItem> catalog = getInstalledAppCatalog(
                packageManager,
                host.getSelfPackageName(),
                forceInstalledAppCatalogReload);
        maybeScheduleFirstScreenIconWarmup(packageManager, catalog);

        List<AppListItem> result = new ArrayList<>(catalog.size());
        for (InstalledAppCatalogItem item : catalog) {
            Integer viewportWidth = store != null
                    ? store.getTargetViewportWidthDp(item.packageName)
                    : null;
            String viewportMode = store != null
                    ? store.getTargetViewportApplyMode(item.packageName)
                    : ViewportApplyMode.OFF;
            Integer fontScalePercent = store != null
                    ? store.getTargetFontScalePercent(item.packageName)
                    : null;
            String fontMode = store != null
                    ? store.getTargetFontApplyMode(item.packageName)
                    : FontApplyMode.OFF;
            boolean dpisEnabled = store == null
                    || store.isTargetDpisEnabled(item.packageName);
            Drawable icon = resolveDisplayIcon(item);
            result.add(new AppListItem(item.label, item.packageName,
                    scopePackages.contains(item.packageName), viewportWidth, viewportMode,
                    fontScalePercent, fontMode, dpisEnabled, item.systemApp, icon));
        }
        return result;
    }

    void onIconLoadRequested(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        synchronized (iconRequestLock) {
            if (appIconCache.get(packageName) != null) {
                return;
            }
            if (!pendingOnDemandIconLoads.add(packageName)) {
                return;
            }
        }
        appIconWarmupExecutor.execute(() -> {
            boolean loaded = false;
            try {
                PackageManager packageManager = host.getPackageManager();
                ApplicationInfo applicationInfo = findApplicationInfo(packageManager, packageName);
                if (applicationInfo != null) {
                    loaded = loadAppIcon(packageManager, applicationInfo) != null;
                }
            } finally {
                synchronized (iconRequestLock) {
                    pendingOnDemandIconLoads.remove(packageName);
                }
            }
            if (loaded) {
                scheduleIconRefresh();
            }
        });
    }

    private List<InstalledAppCatalogItem> getInstalledAppCatalog(PackageManager packageManager,
            String selfPackageName,
            boolean forceReload) {
        long now = SystemClock.elapsedRealtime();
        synchronized (installedAppCatalogLock) {
            boolean cacheFresh = !installedAppCatalog.isEmpty()
                    && now - installedAppCatalogLoadedAtMs <= installedAppCatalogTtlMs;
            if (!forceReload && cacheFresh) {
                return installedAppCatalog;
            }
        }
        synchronized (installedAppCatalogBuildLock) {
            now = SystemClock.elapsedRealtime();
            synchronized (installedAppCatalogLock) {
                boolean cacheFresh = !installedAppCatalog.isEmpty()
                        && now - installedAppCatalogLoadedAtMs <= installedAppCatalogTtlMs;
                if (!forceReload && cacheFresh) {
                    return installedAppCatalog;
                }
            }

            Map<String, Drawable> previousIcons = new HashMap<>();
            synchronized (installedAppCatalogLock) {
                for (Map.Entry<String, InstalledAppCatalogItem> entry : installedAppCatalogIndex.entrySet()) {
                    Drawable icon = entry.getValue().icon;
                    if (icon != null) {
                        previousIcons.put(entry.getKey(), icon);
                    }
                }
            }

            List<ApplicationInfo> installedApps;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                installedApps = packageManager.getInstalledApplications(
                        PackageManager.ApplicationInfoFlags.of(0));
            } else {
                installedApps = packageManager.getInstalledApplications(0);
            }

            List<InstalledAppCatalogItem> rebuilt = new ArrayList<>();
            for (ApplicationInfo applicationInfo : installedApps) {
                if (applicationInfo.packageName.equals(selfPackageName)) {
                    continue;
                }
                boolean systemApp = isSystemApp(applicationInfo);
                String label = packageManager.getApplicationLabel(applicationInfo).toString();
                rebuilt.add(new InstalledAppCatalogItem(
                        label,
                        applicationInfo.packageName,
                        systemApp,
                        previousIcons.get(applicationInfo.packageName)));
            }
            rebuilt.sort(Comparator.comparing(
                    (InstalledAppCatalogItem item) -> item.label.toLowerCase(Locale.ROOT))
                    .thenComparing(item -> item.packageName));
            List<InstalledAppCatalogItem> snapshot = Collections.unmodifiableList(rebuilt);
            Map<String, InstalledAppCatalogItem> index = new HashMap<>();
            for (InstalledAppCatalogItem item : rebuilt) {
                index.put(item.packageName, item);
            }
            synchronized (installedAppCatalogLock) {
                installedAppCatalog = snapshot;
                installedAppCatalogIndex = index;
                installedAppCatalogLoadedAtMs = now;
                return installedAppCatalog;
            }
        }
    }

    private Drawable resolveDisplayIcon(InstalledAppCatalogItem item) {
        if (item.icon != null) {
            return item.icon;
        }
        Drawable cached = appIconCache.get(item.packageName);
        if (cached != null) {
            rememberInstalledCatalogIcon(item.packageName, cached);
        }
        return cached;
    }

    private void maybeScheduleFirstScreenIconWarmup(PackageManager packageManager,
            List<InstalledAppCatalogItem> catalog) {
        if (catalog.isEmpty()) {
            return;
        }
        synchronized (iconWarmupLock) {
            if (firstScreenIconWarmupScheduled || firstScreenIconWarmupFinished) {
                return;
            }
            firstScreenIconWarmupScheduled = true;
        }
        appIconWarmupExecutor.execute(() -> {
            int warmedCount = 0;
            int limit = Math.min(firstScreenIconWarmupLimit, catalog.size());
            for (int i = 0; i < limit; i++) {
                String packageName = catalog.get(i).packageName;
                if (appIconCache.get(packageName) != null) {
                    continue;
                }
                ApplicationInfo applicationInfo = findApplicationInfo(packageManager, packageName);
                if (applicationInfo == null) {
                    continue;
                }
                Drawable warmed = loadAppIcon(packageManager, applicationInfo);
                if (warmed != null) {
                    warmedCount++;
                }
            }
            synchronized (iconWarmupLock) {
                firstScreenIconWarmupFinished = true;
                firstScreenIconWarmupScheduled = false;
            }
            if (warmedCount > 0) {
                scheduleIconRefresh();
            }
        });
    }

    private void scheduleIconRefresh() {
        synchronized (iconRequestLock) {
            if (iconRefreshQueued) {
                return;
            }
            iconRefreshQueued = true;
        }
        host.runOnUiThread(() -> {
            View anchor = host.getIconRefreshAnchor();
            if (anchor == null) {
                synchronized (iconRequestLock) {
                    iconRefreshQueued = false;
                }
                host.requestAppsLoad();
                return;
            }
            anchor.postDelayed(() -> {
                synchronized (iconRequestLock) {
                    iconRefreshQueued = false;
                }
                host.requestAppsLoad();
            }, iconRefreshDebounceMs);
        });
    }

    private static ApplicationInfo findApplicationInfo(PackageManager packageManager, String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0));
            }
            return packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    private Drawable loadAppIcon(PackageManager packageManager, ApplicationInfo applicationInfo) {
        String packageName = applicationInfo.packageName;
        Drawable cachedIcon = appIconCache.get(packageName);
        if (cachedIcon != null) {
            rememberInstalledCatalogIcon(packageName, cachedIcon);
            return cachedIcon;
        }
        Drawable loadedIcon = applicationInfo.loadIcon(packageManager);
        appIconCache.put(packageName, loadedIcon);
        rememberInstalledCatalogIcon(packageName, loadedIcon);
        return loadedIcon;
    }

    private void rememberInstalledCatalogIcon(String packageName, Drawable icon) {
        if (packageName == null || packageName.isEmpty() || icon == null) {
            return;
        }
        synchronized (installedAppCatalogLock) {
            InstalledAppCatalogItem item = installedAppCatalogIndex.get(packageName);
            if (item != null) {
                item.icon = icon;
            }
        }
    }

    private static boolean isSystemApp(ApplicationInfo applicationInfo) {
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }

    private static final class InstalledAppCatalogItem {
        final String label;
        final String packageName;
        final boolean systemApp;
        volatile Drawable icon;

        InstalledAppCatalogItem(String label, String packageName, boolean systemApp, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.systemApp = systemApp;
            this.icon = icon;
        }
    }
}
