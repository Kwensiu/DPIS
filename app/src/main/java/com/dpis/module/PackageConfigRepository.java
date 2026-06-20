package com.dpis.module;

final class PackageConfigRepository {
    private final DpiConfigStore store;

    PackageConfigRepository(DpiConfigStore store) {
        this.store = store;
    }

    boolean hasRealPackageConfig(String packageName) {
        return store != null && store.hasRealPackageConfig(packageName);
    }

    boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
        return store != null && store.writePackageTemplateConfigValue(packageName, value);
    }
}
