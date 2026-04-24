package com.dpis.module;

abstract class MainUiEffect {
    private MainUiEffect() {
    }

    static final class StartAppsLoad extends MainUiEffect {
        final int requestId;
        final boolean forceInstalledAppCatalogReload;

        StartAppsLoad(int requestId, boolean forceInstalledAppCatalogReload) {
            this.requestId = requestId;
            this.forceInstalledAppCatalogReload = forceInstalledAppCatalogReload;
        }
    }
}
