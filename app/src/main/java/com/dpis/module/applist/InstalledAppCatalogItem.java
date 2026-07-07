package com.dpis.module.applist;

import android.graphics.drawable.Drawable;

public final class InstalledAppCatalogItem {
    public final String label;
    public final String packageName;
    public final boolean systemApp;
    public final boolean hyperOsNativeProxyCandidate;
    public volatile Drawable icon;

    public InstalledAppCatalogItem(String label,
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
