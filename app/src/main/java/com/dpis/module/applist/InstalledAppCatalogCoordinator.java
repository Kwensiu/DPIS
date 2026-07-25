package com.dpis.module.applist;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class InstalledAppCatalogCoordinator {
    public interface Host {
        PackageManager getPackageManager();

        String getSelfPackageName();

    }

    private final Object installedAppCatalogLock = new Object();
    private final Object installedAppCatalogBuildLock = new Object();

    private final Host host;
    private final long installedAppCatalogTtlMs;

    private List<InstalledAppCatalogItem> installedAppCatalog = Collections.emptyList();
    private long installedAppCatalogLoadedAtMs;

    public InstalledAppCatalogCoordinator(Host host,
            long installedAppCatalogTtlMs) {
        this.host = host;
        this.installedAppCatalogTtlMs = installedAppCatalogTtlMs;
    }

    /** Kept for legacy View binders that still notify visible icon rows. */
    public void onIconLoadRequested(String packageName) {
    }

    /** No executor remains after moving icon resolution into the catalog build. */
    public void shutdown() {
    }

    public List<InstalledAppCatalogItem> loadInstalledAppCatalog(
            boolean forceInstalledAppCatalogReload) {
        PackageManager packageManager = host.getPackageManager();
        List<InstalledAppCatalogItem> catalog = getInstalledAppCatalog(
                packageManager,
                host.getSelfPackageName(),
                forceInstalledAppCatalogReload);
        return catalog;
    }

    /**
     * Legacy target pickers display the full list at once. Keep their rows complete while
     * the Compose app workspace resolves icons only for the page it is about to show.
     */
    public List<InstalledAppCatalogItem> loadInstalledAppCatalogWithIcons(
            boolean forceInstalledAppCatalogReload) {
        List<InstalledAppCatalogItem> catalog = loadInstalledAppCatalog(
                forceInstalledAppCatalogReload);
        PackageManager packageManager = host.getPackageManager();
        for (InstalledAppCatalogItem item : catalog) {
            loadItemIcon(packageManager, item);
        }
        return catalog;
    }

    public List<AppListItem> loadInstalledApps(boolean forceInstalledAppCatalogReload,
            DpisConfigStore store,
            Set<String> scopePackages,
            boolean scopeKnown) {
        List<InstalledAppCatalogItem> catalog = loadInstalledAppCatalog(
                forceInstalledAppCatalogReload);
        Set<String> userVisibleConfiguredPackages = userVisibleConfiguredPackages(store);
        List<AppListItem> result = new ArrayList<>(catalog.size());
        for (InstalledAppCatalogItem item : catalog) {
            boolean inScope = scopePackages != null && scopePackages.contains(item.packageName);
            if (userVisibleConfiguredPackages.contains(item.packageName)) {
                // Configured rows retain the complete, compatibility-aware values used when
                // reopening their editor. Most installed packages have no saved DPIS state.
                result.add(createAppListItem(store, scopePackages, scopeKnown,
                        item.label, item.packageName, item.systemApp,
                        item.hyperOsNativeProxyCandidate, true, null));
            } else {
                result.add(createUnconfiguredAppListItem(
                        item.label, item.packageName, inScope, scopeKnown,
                        item.systemApp, item.hyperOsNativeProxyCandidate, true));
            }
        }
        for (String packageName : configuredPackagesMissingFromCatalog(
                userVisibleConfiguredPackages, catalog)) {
            result.add(createAppListItem(store, scopePackages, scopeKnown,
                    packageName, packageName, false, false, false, null));
        }
        return result;
    }

    private static List<String> configuredPackagesMissingFromCatalog(
            Set<String> userVisibleConfiguredPackages,
            List<InstalledAppCatalogItem> catalog) {
        if (userVisibleConfiguredPackages == null || userVisibleConfiguredPackages.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> installedPackages = new HashSet<>();
        for (InstalledAppCatalogItem item : catalog) {
            installedPackages.add(item.packageName);
        }
        List<String> missing = new ArrayList<>();
        for (String packageName : userVisibleConfiguredPackages) {
            if (!installedPackages.contains(packageName)) {
                missing.add(packageName);
            }
        }
        Collections.sort(missing);
        return missing;
    }

    /**
     * Validates only candidate packages recovered from persistent state. This preserves imported
     * and legacy configuration semantics without running every compatibility lookup for every
     * ordinary installed app.
     */
    static Set<String> userVisibleConfiguredPackages(DpisConfigStore store) {
        if (store == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String packageName : store.getConfiguredPackages()) {
            if (store.hasUserVisiblePackageConfig(packageName)) {
                result.add(packageName);
            }
        }
        return result;
    }

    /**
     * An unconfigured row has no persisted package state to decode. Keep its explicit defaults
     * here so a complete app catalogue does not turn into a full config-store scan.
     */
    static AppListItem createUnconfiguredAppListItem(String label,
            String packageName,
            boolean inScope,
            boolean scopeKnown,
            boolean systemApp,
            boolean hyperOsNativeProxyCandidate,
            boolean installed) {
        return new AppListItem(label, packageName,
                inScope, scopeKnown, null, null,
                ViewportApplyMode.OFF, ViewportTargetType.OFF,
                ViewportTargetSpec.off(), null, FontApplyMode.OFF, null,
                false, null, true,
                scopeKnown && inScope, installed,
                systemApp, hyperOsNativeProxyCandidate, null);
    }

    public static AppListItem createAppListItem(DpisConfigStore store,
            Set<String> scopePackages,
            boolean scopeKnown,
            String label,
            String packageName,
            boolean systemApp,
            boolean hyperOsNativeProxyCandidate,
            boolean installed,
            Drawable icon) {
        Integer viewportWidth = store != null
                ? store.getTargetViewportWidthDp(packageName)
                : null;
        Integer viewportScaleMilliPercent = store != null
                ? store.getTargetViewportScaleMilliPercent(packageName)
                : null;
        ViewportTargetSpec viewportTargetSpec = store != null
                ? store.getTargetViewportSpec(packageName)
                : ViewportTargetSpec.off();
        String viewportTargetType = store != null
                ? store.getTargetViewportType(packageName)
                : ViewportTargetType.OFF;
        String viewportMode = store != null
                ? store.getTargetViewportApplyMode(packageName)
                : ViewportApplyMode.OFF;
        Integer fontScalePercent = store != null
                ? store.getTargetFontScalePercent(packageName)
                : null;
        String fontMode = store != null
                ? store.getTargetFontApplyMode(packageName)
                : FontApplyMode.OFF;
        String typefaceId = store != null
                ? store.getTargetTypefaceId(packageName)
                : null;
        boolean appSpecificConfigActive = store != null
                && store.hasTargetAppSpecificConfig(packageName);
        Integer wechatDpi = store != null ? store.getWechatDpi(packageName) : null;
        boolean dpisEnabled = store == null || store.isTargetDpisEnabled(packageName);
        boolean inScope = scopePackages != null && scopePackages.contains(packageName);
        boolean configured = isUserVisibleConfiguredPackage(
                store,
                packageName,
                scopeKnown,
                inScope);
        return new AppListItem(label, packageName,
                inScope, scopeKnown, viewportWidth,
                viewportScaleMilliPercent, viewportMode, viewportTargetType,
                viewportTargetSpec, fontScalePercent, fontMode, typefaceId,
                appSpecificConfigActive, wechatDpi, dpisEnabled, configured, installed,
                systemApp, hyperOsNativeProxyCandidate, icon);
    }

    public static boolean isUserVisibleConfiguredPackage(DpisConfigStore store,
            String packageName,
            boolean scopeKnown,
            boolean inScope) {
        return (scopeKnown && inScope)
                || (store != null && store.hasUserVisiblePackageConfig(packageName));
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

            List<ApplicationInfo> installedApps;
            // The catalogue needs only the package identity, base ApplicationInfo and label.
            // Manifest metadata is substantially more expensive to marshal for every installed
            // package, and the sole consumer (HyperOS proxy eligibility) is resolved later for
            // the one package the user saves or restarts.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                installedApps = packageManager.getInstalledApplications(
                        PackageManager.ApplicationInfoFlags.of(0L));
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
                        false,
                        applicationInfo,
                        null));
            }
            rebuilt.sort(Comparator.comparing(
                    (InstalledAppCatalogItem item) -> item.label.toLowerCase(Locale.ROOT))
                    .thenComparing(item -> item.packageName));
            List<InstalledAppCatalogItem> snapshot = Collections.unmodifiableList(rebuilt);
            synchronized (installedAppCatalogLock) {
                installedAppCatalog = snapshot;
                installedAppCatalogLoadedAtMs = now;
                return installedAppCatalog;
            }
        }
    }

    private static Drawable loadApplicationIcon(PackageManager packageManager,
            ApplicationInfo applicationInfo) {
        try {
            return applicationInfo.loadIcon(packageManager);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Drawable loadItemIcon(PackageManager packageManager,
            InstalledAppCatalogItem item) {
        Drawable cached = item.icon;
        if (cached != null) {
            return cached;
        }
        Drawable loaded = loadApplicationIcon(packageManager, item.applicationInfo);
        if (loaded != null) {
            item.icon = loaded;
        }
        return loaded;
    }

    private static boolean isSystemApp(ApplicationInfo applicationInfo) {
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }
}
