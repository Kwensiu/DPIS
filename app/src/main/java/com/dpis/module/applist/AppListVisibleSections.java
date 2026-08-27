package com.dpis.module.applist;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public final class AppListVisibleSections {
    private AppListVisibleSections() {
    }

    public static List<AppListItem> filter(List<AppListItem> source, String query, AppListPage page) {
        return filter(source, query, page, AppListFilterState.noAdditionalConstraints());
    }

    public static List<AppListItem> filter(List<AppListItem> source,
                                    String query,
                                    AppListPage page,
                                    AppListFilterState state) {
        List<AppListItem> visible = new ArrayList<>();
        for (AppListItem item : source) {
            if (AppListFilter.matches(query,
                    page.filterTab(),
                    item.label,
                    item.packageName,
                    item.systemApp,
                    item.inScope,
                    item.viewportTargetSpec.isEnabled() ? item.viewportTargetSpec.activeValue() : null,
                    item.fontScalePercent,
                    item.fontMode,
                    item.typefaceId,
                    item.effectiveFontHookDomainsRaw(),
                    item.dpisEnabled,
                    item.hasAppSpecificConfig(),
                    item.configured,
                    item.installed,
                    state)) {
                visible.add(item);
            }
        }
        AppListFilterState effectiveState = state != null
                ? state : AppListFilterState.noAdditionalConstraints();
        Comparator<AppListItem> comparator;
        switch (effectiveState.sortOrder()) {
            case UPDATED:
                comparator = Comparator.comparingLong(item -> item.lastUpdateTime);
                break;
            case INSTALLED:
                comparator = Comparator.comparingLong(item -> item.firstInstallTime);
                break;
            case NAME:
            default:
                comparator = Comparator.comparing(
                        (AppListItem item) -> item.label == null ? "" : item.label.toLowerCase(),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(item -> item.packageName);
                break;
        }
        if (effectiveState.reverseOrder()) {
            comparator = comparator.reversed();
        }
        visible.sort(comparator);
        return visible;
    }
}
