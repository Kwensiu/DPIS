package com.dpis.module;

import java.util.Locale;

final class AppListFilter {
    enum Tab {
        USER_APPS,
        CONFIGURED_APPS,
        SYSTEM_APPS
    }

    private AppListFilter() {
    }

    static boolean matches(String query,
                           Tab tab,
                           String label,
                           String packageName,
                           boolean systemApp,
                           boolean inScope,
                           Integer viewportWidthDp) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (!normalizedQuery.isEmpty()) {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            String normalizedPackage = packageName.toLowerCase(Locale.ROOT);
            if (!normalizedLabel.contains(normalizedQuery)
                    && !normalizedPackage.contains(normalizedQuery)) {
                return false;
            }
        }
        return switch (tab) {
            case USER_APPS -> !systemApp;
            case CONFIGURED_APPS -> inScope || viewportWidthDp != null;
            case SYSTEM_APPS -> systemApp;
        };
    }
}
