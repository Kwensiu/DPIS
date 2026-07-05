package com.dpis.module;

import com.dpis.module.applist.AppListFilterState;

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
                           String fontMode,
                           String typefaceId,
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
        if (!effectiveState.showSystemApps() && systemApp) {
            return false;
        }
        if (effectiveState.injectedOnly() && !inScope) {
            return false;
        }
        if (effectiveState.widthConfiguredOnly() && viewportWidthDp == null) {
            return false;
        }
        return !effectiveState.fontConfiguredOnly() || anyFontConfigured;
    }

}
