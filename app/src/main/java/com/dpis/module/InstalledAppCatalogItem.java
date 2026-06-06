package com.dpis.module;

import android.graphics.drawable.Drawable;

final class InstalledAppCatalogItem {
    final String label;
    final String packageName;
    final boolean systemApp;
    final boolean hyperOsNativeProxyCandidate;
    volatile Drawable icon;

    InstalledAppCatalogItem(String label,
            String packageName,
            boolean systemApp,
            boolean hyperOsNativeProxyCandidate,
            Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.systemApp = systemApp;
        this.hyperOsNativeProxyCandidate = hyperOsNativeProxyCandidate;
        this.icon = icon;
    }
}
