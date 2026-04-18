package com.dpis.module;

import android.graphics.drawable.Drawable;

final class AppListItem {
    final String label;
    final String packageName;
    final boolean inScope;
    final Integer viewportWidthDp;
    final Integer fontScalePercent;
    final String fontMode;
    final boolean systemApp;
    final Drawable icon;

    AppListItem(String label,
                String packageName,
                boolean inScope,
                Integer viewportWidthDp,
                Integer fontScalePercent,
                String fontMode,
                boolean systemApp,
                Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.inScope = inScope;
        this.viewportWidthDp = viewportWidthDp;
        this.fontScalePercent = fontScalePercent;
        this.fontMode = FontApplyMode.normalize(fontMode);
        this.systemApp = systemApp;
        this.icon = icon;
    }
}
