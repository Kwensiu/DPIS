package com.dpis.module.applist;

import com.dpis.module.fonts.FontApplyMode;


import java.util.Locale;

public final class AppListFilter {
    public enum Tab {
        ALL_APPS,
        CONFIGURED_APPS
    }

    private AppListFilter() {
    }

    public static boolean matches(String query, Tab tab, String label, String packageName,
                           boolean systemApp, boolean inScope, Integer viewportWidthDp,
                           Integer fontScalePercent, String fontMode, String typefaceId,
                           boolean appSpecificConfigActive, boolean configured, boolean installed,
                           AppListFilterState state) {
        return matches(query, tab, label, packageName, systemApp, inScope, viewportWidthDp,
                fontScalePercent, fontMode, typefaceId, null, true,
                appSpecificConfigActive, configured, installed, state);
    }

    /** Compatibility overload for callers that do not carry the persisted hook-chain summary. */
    public static boolean matches(String query,
                           Tab tab,
                           String label,
                           String packageName,
                           boolean systemApp,
                           boolean inScope,
                           Integer viewportWidthDp,
                           Integer fontScalePercent,
                           String fontMode,
                           String typefaceId,
                           boolean dpisEnabled,
                           boolean appSpecificConfigActive,
                           boolean configured,
                           boolean installed,
                           AppListFilterState state) {
        return matches(query, tab, label, packageName, systemApp, inScope, viewportWidthDp,
                fontScalePercent, fontMode, typefaceId, null, dpisEnabled, appSpecificConfigActive,
                configured, installed, state);
    }

    public static boolean matches(String query,
                           Tab tab,
                           String label,
                           String packageName,
                           boolean systemApp,
                           boolean inScope,
                           Integer viewportWidthDp,
                           Integer fontScalePercent,
                           String fontMode,
                           String typefaceId,
                           String fontHookDomainsRaw,
                           boolean dpisEnabled,
                           boolean appSpecificConfigActive,
                           boolean configured,
                           boolean installed,
                           AppListFilterState state) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (!normalizedQuery.isEmpty()) {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            String normalizedPackage = packageName.toLowerCase(Locale.ROOT);
            if (!normalizedLabel.contains(normalizedQuery)
                    && !normalizedPackage.contains(normalizedQuery)) {
                return false;
            }
        }
        boolean fontConfigured = fontScalePercent != null
                && FontApplyMode.isEnabled(FontApplyMode.normalize(fontMode));
        boolean typefaceConfigured = typefaceId != null && !typefaceId.isBlank();
        boolean anyFontConfigured = fontConfigured || typefaceConfigured;
        boolean hookConfigured = fontHookDomainsRaw != null && !fontHookDomainsRaw.trim().isEmpty();
        boolean matchesTab = switch (tab) {
            case ALL_APPS -> installed;
            case CONFIGURED_APPS -> configured;
        };
        if (!matchesTab) {
            return false;
        }

        AppListFilterState effectiveState = state != null
                ? state
                : AppListFilterState.noAdditionalConstraints();
        if (!effectiveState.allAppsSelected()
                && ((systemApp && !effectiveState.systemAppsSelected())
                || (!systemApp && !effectiveState.userAppsSelected()))) {
            return false;
        }
        if (effectiveState.injectedOnly() && !inScope) {
            return false;
        }
        if (effectiveState.disabledOnly() && dpisEnabled) return false;
        if (effectiveState.widthConfiguredOnly() && viewportWidthDp == null) {
            return false;
        }
        if (effectiveState.fontConfiguredOnly() && !anyFontConfigured) return false;
        if (effectiveState.typefaceConfiguredOnly() && !typefaceConfigured) return false;
        return !effectiveState.hookConfiguredOnly() || hookConfigured;
    }

}
