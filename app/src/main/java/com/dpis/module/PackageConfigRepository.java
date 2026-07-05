package com.dpis.module;

import com.dpis.module.templates.TemplateConfigValue;

public final class PackageConfigRepository {
    private final DpisConfigStore store;

    public PackageConfigRepository(DpisConfigStore store) {
        this.store = store;
    }

    public boolean hasRealPackageConfig(String packageName) {
        return store != null && store.hasRealPackageConfig(packageName);
    }

    public boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
        return store != null && store.writePackageTemplateConfigValue(packageName, value);
    }
}
