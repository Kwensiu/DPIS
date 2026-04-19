package com.dpis.module;

import android.graphics.drawable.Drawable;

final class AppListItem {
    final String label;
    final String packageName;
    final boolean inScope;
    final Integer viewportWidthDp;
    final String viewportMode;
    final Integer fontScalePercent;
    final String fontMode;
    final boolean dpisEnabled;
    final boolean systemApp;
    final Drawable icon;

    AppListItem(String label,
                String packageName,
                boolean inScope,
                Integer viewportWidthDp,
                String viewportMode,
                Integer fontScalePercent,
                String fontMode,
                boolean dpisEnabled,
                boolean systemApp,
                Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.inScope = inScope;
        this.viewportWidthDp = viewportWidthDp;
        this.viewportMode = ViewportApplyMode.normalize(viewportMode);
        this.fontScalePercent = fontScalePercent;
        this.fontMode = FontApplyMode.normalize(fontMode);
        this.dpisEnabled = dpisEnabled;
        this.systemApp = systemApp;
        this.icon = icon;
    }
}
