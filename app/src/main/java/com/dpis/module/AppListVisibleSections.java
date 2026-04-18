package com.dpis.module;

import java.util.ArrayList;
import java.util.List;

final class AppListVisibleSections {
    private AppListVisibleSections() {
    }

    static List<AppListItem> filter(List<AppListItem> source, String query, AppListPage page) {
        return filter(source, query, page, AppListFilterState.noAdditionalConstraints());
    }

    static List<AppListItem> filter(List<AppListItem> source,
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
                    item.viewportWidthDp,
                    item.fontScalePercent,
                    item.fontMode,
                    state)) {
                visible.add(item);
            }
        }
        return visible;
    }
}
