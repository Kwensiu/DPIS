package com.dpis.module;

import java.util.Locale;

final class AppListFilter {
    enum Tab {
        ALL_APPS,
        CONFIGURED_APPS
    }

    private AppListFilter() {
    }

    static boolean matches(String query,
                           Tab tab,
                           String label,
                           String packageName,
                           boolean systemApp,
                           boolean inScope,
                           Integer viewportWidthDp,
                           Integer fontScalePercent,
                           String fontMode) {
        return matches(query, tab, label, packageName, systemApp, inScope,
                viewportWidthDp, fontScalePercent, fontMode,
                AppListFilterState.noAdditionalConstraints());
    }

    static boolean matches(String query,
                           Tab tab,
                           String label,
                           String packageName,
                           boolean systemApp,
                           boolean inScope,
                           Integer viewportWidthDp,
                           Integer fontScalePercent,
                           String fontMode,
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
        boolean matchesTab = switch (tab) {
            case ALL_APPS -> true;
            case CONFIGURED_APPS -> inScope || viewportWidthDp != null || fontConfigured;
        };
        if (!matchesTab) {
            return false;
        }

        AppListFilterState effectiveState = state != null
                ? state
                : AppListFilterState.noAdditionalConstraints();
        if (!effectiveState.showSystemApps && systemApp) {
            return false;
        }
        if (effectiveState.injectedOnly && !inScope) {
            return false;
        }
        if (effectiveState.widthConfiguredOnly && viewportWidthDp == null) {
            return false;
        }
        return !effectiveState.fontConfiguredOnly || fontConfigured;
    }
}
