package com.dpis.module.applist;

import java.util.ArrayList;
import java.util.List;

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
                    item.hasAppSpecificConfig(),
                    item.configured,
                    item.installed,
                    state)) {
                visible.add(item);
            }
        }
        return visible;
    }
}
