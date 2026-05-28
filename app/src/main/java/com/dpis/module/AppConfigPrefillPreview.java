package com.dpis.module;

final class AppConfigPrefillPreview {
    private AppConfigPrefillPreview() {
    }

    static AppListItem applyIfEligible(AppListItem item,
            DpiConfigStore store,
            TemplateConfigValue globalPrefill) {
        if (item == null || store == null || globalPrefill == null || !globalPrefill.hasAnyValue()) {
            return item;
        }
        if (store.hasRealPackageConfig(item.packageName)) {
            return item;
        }
        return item.withGlobalPrefillPreview(globalPrefill);
    }
}
