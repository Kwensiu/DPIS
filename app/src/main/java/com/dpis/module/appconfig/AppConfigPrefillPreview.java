package com.dpis.module.appconfig;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.PackageConfigRepository;

import com.dpis.module.applist.AppListItem;

import com.dpis.module.templates.TemplateConfigValue;

public final class AppConfigPrefillPreview {
    private AppConfigPrefillPreview() {
    }

    public static AppListItem applyIfEligible(AppListItem item,
            DpisConfigStore store,
            TemplateConfigValue globalPrefill) {
        if (store == null) {
            return item;
        }
        return applyIfEligible(item, new PackageConfigRepository(store), globalPrefill);
    }

    private static AppListItem applyIfEligible(AppListItem item,
            PackageConfigRepository packageConfigRepository,
            TemplateConfigValue globalPrefill) {
        if (item == null
                || packageConfigRepository == null
                || globalPrefill == null
                || !globalPrefill.hasAnyValue()) {
            return item;
        }
        if (packageConfigRepository.hasRealPackageConfig(item.packageName)) {
            return item;
        }
        return item.withGlobalPrefillPreview(globalPrefill);
    }
}
